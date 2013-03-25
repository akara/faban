/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.sun.com/cddl/cddl.html or
 * install_dir/legal/LICENSE
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at install_dir/legal/LICENSE.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.common.TextTable;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.security.Acl;
import com.sun.faban.harness.util.FileHelper;
import com.sun.faban.harness.util.XMLException;
import com.sun.faban.harness.util.XMLReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * Run Analyzer that handles all the backend tasks to analyze the runs.
 *
 * @author Akara Sucharitakul
 */
public class RunAnalyzer {
    XMLReader reader;
    ArrayList<String> rtPercentList[];

    /**
     * Analysis types.
     */
    public enum Type {

        /**
         * Comparison.
         */
        COMPARE,

        /**
         * Averaging.
         */
        AVERAGE;

        /**
         * Returns the string representation of the analysis type.
         *
         * @return The string representation of the enum elements in lower case
         */
        public String toString() {
            return name().toLowerCase();
        }
    }

    ;

    private static Logger logger =
            Logger.getLogger(RunAnalyzer.class.getName());

    /**
     * Provides a suggested analysis name for the analysis based on the
     * analysis type and and run ids that are used.
     * The run id can come in the form of bench.seq or
     * host.bench.seq. We try to keep the suggested name
     * as short as possible. We have three formats:
     * <ol>
     * <li>Generic format: type_runId1_runId2...</li>
     * <li>Same benchmark: type-bench_<host.>seq1_<host>.seq2...</li>
     * <li>Same host, same benchmark: type-bench-host_seq1_seq2...</li>
     * </ol>
     *
     * @param type         Whether it is a compare or an average
     * @param runIdStrings The run ids to include in the analysis
     * @return The suggested analysis name
     */
    public static String suggestName(Type type, String[] runIdStrings) {
        StringBuilder suggestion = new StringBuilder();
        HashSet<String> benchNameSet = new HashSet<String>();
        HashSet<String> hostNameSet = new HashSet<String>();
        suggestion.append(type);

        RunId[] runIds = new RunId[runIdStrings.length];
        String benchName = null;
        String hostName = null;
        for (int i = 0; i < runIdStrings.length; i++) {
            RunId runId = new RunId(runIdStrings[i]);
            benchName = runId.getBenchName();
            hostName = runId.getHostName();
            benchNameSet.add(benchName);
            hostNameSet.add(hostName);
            runIds[i] = runId;
        }

        if (benchNameSet.size() == 1) {
            suggestion.append('-').append(benchName);
            if (hostNameSet.size() == 1) {
                if (hostName.length() > 0)
                    suggestion.append('-').append(hostName);
                for (RunId runId : runIds)
                    suggestion.append('_').append(runId.getRunSeq());
            } else {
                for (RunId runId : runIds) {
                    suggestion.append('_');
                    hostName = runId.getHostName();
                    if (hostName.length() > 0)
                        suggestion.append(hostName).append('.');
                    suggestion.append(runId.getRunSeq());
                }
            }
        } else {
            for (RunId runId : runIds)
                suggestion.append('_').append(runId);
        }
        return suggestion.toString();

    }

    /**
     * Checks whether the analysis with the given name exists.
     *
     * @param name The analysis name
     * @return true if the analysis exists, false otherwise
     */
    public static boolean exists(String name) {
        File analysisDir = new File(Config.ANALYSIS_DIR + name);
        File resultFile = new File(analysisDir, "index.html");
        if (resultFile.exists()) {
            return true;
        } else if (analysisDir.isDirectory()) {
            FileHelper.recursiveDelete(analysisDir);
            return false;
        } else {
            return false;
        }
    }

    /**
     * Removes the analysis results.
     *
     * @param name The name of the result to remove
     */
    public static void clear(String name) {
        File analysisDir = new File(Config.ANALYSIS_DIR + name);
        if (analysisDir.isDirectory()) {
            FileHelper.recursiveDelete(analysisDir);
        }
    }

    /**
     * Executes the run analysis.
     *
     * @param type         The type of the analysis
     * @param runIdStrings The run ids to analyze, in form of String array
     * @param output       The name of the analysis results
     * @param user         The user name requesting this analysis
     * @throws IOException The analysis failed and results are not generated
     */
    public void analyze(Type type, String[] runIdStrings,
                        String output, String user)
            throws IOException {
        File analysisDir = new File(Config.ANALYSIS_DIR + output);
        if (!analysisDir.mkdirs()) {
            throw new IOException("Failed creating directory " +
                    analysisDir + '!');
        }

        // Before we put anything in, we deal with security.
        File metaDir = new File(analysisDir, "META-INF");
        metaDir.mkdir();

        // Merge the ACLs from all the source
        Acl.merge(runIdStrings, output);

        if (user != null)
            FileHelper.writeStringToFile(user, new File(metaDir, "submitter"));

        try {
            if (type == Type.COMPARE)
                compare(runIdStrings, Config.ANALYSIS_DIR + output);
        } catch (IOException ie) {
            FileHelper.recursiveDelete(analysisDir);
            throw new IOException("Failed creating analysis.");
        }
    }

    /*
    * Process the list of rundirs and print out the comparison report
    * @param runDirs String array
    * @param outDir directory to write output report to
    * @throws IOException if runDir or certain files in it can't be accessed or
    *            file format is incorrect
    */
    public void compare(String runDirs[], String outDir) throws IOException {
        String outFile = outDir + File.separator + "compare.xan";
        TextTable infoTable, thruTable, opAvgThruTable = null, opThruTable;
        TextTable respTable, percentRespTable[] = null, avgRespTable = null, cpuTable = null;
        String thruMetric = null, respMetric = null;
        PrintWriter p;
        ArrayList<String> rtAvgList;
        ArrayList<String> rtPercentNames = null;
        ArrayList<String> opNames = new ArrayList<String>();

        ArrayList<Double> thruList[] = new ArrayList[runDirs.length];
        ArrayList<Double> opThruList[][] = null;
        ArrayList<Double> timeVals = new ArrayList<Double>();
        ArrayList<Double> timeDistVals = new ArrayList<Double>();
        List<String> cpuList[];
        int maxThruRows = 0, maxDistRows = 0;
        ArrayList<Double> respList[][] = null;
        ArrayList<Integer> respDistList[][] = null;
        infoTable = new TextTable(runDirs.length, 4);
        infoTable.setHeader(0, "RunID");
        infoTable.setHeader(1, "Avg. Throughput");
        infoTable.setHeader(2, "Passed");
        infoTable.setHeader(3, "Description");


        p = openOutFile(outFile, "Compare");
        for (int i = 0; i < runDirs.length; i++) {
            String sumFile = getSumFile(runDirs[i]);
            String detFile = getDetFile(runDirs[i]);

            reader = new XMLReader(sumFile, false, false);

            // parse Run Info section of summary file
            getRunInfo(runDirs[i], reader, infoTable, i);

            // parse throughput section of detail.xan
            DetailReport detail = new DetailReport(detFile);
            thruList[i] = detail.getThruput();
            ArrayList<Double> thisTimeVals = detail.getTimes();

            if (thruList[i].size() > maxThruRows) {
                maxThruRows = thruList[i].size();
                timeVals = thisTimeVals;
            }
            ArrayList<Double> thisDistTimeVals = detail.getTimesDist();
            if (thisDistTimeVals.size() > maxDistRows) {
                maxDistRows = thisDistTimeVals.size();
                timeDistVals = thisDistTimeVals;
            }

            // read the metric from the first file
            if (i == 0) {
                respMetric = getRespUnit(reader);
                thruMetric = getThruUnit(reader);
            }

            // Now get the response times
            rtAvgList = new ArrayList<String>();
            rtPercentNames = new ArrayList<String>();
            try {
                getResponseTimes(reader, opNames, rtAvgList, rtPercentNames);
            } catch (IOException ie) {
                throw new IOException(ie.getMessage() + " : " + sumFile);
            }

            // Get cpu util.
            cpuList = getCpuUtil(Config.OUT_DIR + runDirs[i]);

            if (i == 0) {
                respList = new ArrayList[runDirs.length][opNames.size()];
                respDistList = new ArrayList[runDirs.length][opNames.size()];

                // thru. table on a per operation basis
                opAvgThruTable = new TextTable(runDirs.length, opNames.size() + 1);
                opAvgThruTable.setHeader(0, "RunID");

                opThruList = new ArrayList[runDirs.length][opNames.size()];

                /*
                 * We have the following types of RT tables:
                 * a) Avg. RT which simply has one row per run listing avg RT. of each operation
                 * b) nth Percentile RT tables - same info as above
                 * c) Detailed RT - one table per operation listing RT over time
                 */
                avgRespTable = new TextTable(runDirs.length, opNames.size() + 1);
                avgRespTable.setHeader(0, "RunID");

                //percentile resp. tables (e.g. 90th, 99th)
                percentRespTable = new TextTable[rtPercentNames.size()];
                for (int j = 0; j < rtPercentNames.size(); j++) {
                    percentRespTable[j] = new TextTable(runDirs.length, opNames.size() + 1);
                    percentRespTable[j].setHeader(0, "RunID");
                }

                for (int j = 0; j < opNames.size(); j++) {
                    opAvgThruTable.setHeader(j + 1, opNames.get(j));
                    avgRespTable.setHeader(j + 1, opNames.get(j));
                    for (int k = 0; k < rtPercentNames.size(); k++) {
                        percentRespTable[k].setHeader(j + 1, opNames.get(j));
                    }
                }

                /*** CPU info is tough for comparison. The hosts may not be the same */
                // CPU info. We print 1 row per RunID with each col. being cpu% for one host
                cpuTable = new TextTable(runDirs.length, cpuList.length + 1);
                cpuTable.setHeader(0, "RunID");
                for (int j = 0; j < cpuList.length; j++) {
                    cpuTable.setHeader(j + 1, cpuList[j].get(0));    // hostname
                }
            }


            // Get avg. thruput per operation
            opAvgThruTable.setField(i, 0, runDirs[i]);
            avgRespTable.setField(i, 0, runDirs[i]);
            for (int j = 0; j < opNames.size(); j++) {
                opAvgThruTable.setField(i, j + 1, String.format("%4.3f", detail.getOpAvgThruput(j)));
                opThruList[i][j] = detail.getOpThruput(j);
                respList[i][j] = detail.getOpRT(j);
                respDistList[i][j] = detail.getOpRTDist(j);
                avgRespTable.setField(i, j + 1, rtAvgList.get(j));
                for (int k = 0; k < rtPercentNames.size(); k++) {
                    percentRespTable[k].setField(i, 0, runDirs[i]);
                    percentRespTable[k].setField(i, j + 1, rtPercentList[k].get(j));
                }
            }
            cpuTable.setField(i, 0, runDirs[i]);
            for (int j = 0; j < cpuList.length; j++) {
                cpuTable.setField(i, j + 1, cpuList[j].get(1));  // avg. util
            }
        }

        // Create the thruput tables. We create it with the largest #rows
        thruTable = new TextTable(maxThruRows, runDirs.length + 1); //1st col is time
        thruTable.setHeader(0, "Time");
        for (int i = 0; i < timeVals.size(); i++)
            thruTable.setField(i, 0, timeVals.get(i).toString());

        for (int i = 0; i < runDirs.length; i++) {
            thruTable.setHeader(i + 1, runDirs[i]);
            int j;
            for (j = 0; j < thruList[i].size(); j++) {
                thruTable.setField(j, i + 1, thruList[i].get(j).toString());
            }
            int rem = timeVals.size() - thruList[i].size();
            // Fill remaning timevals with dash - null value for .xan
            for (; rem > 0; rem--) {
                thruTable.setField(j++, i + 1, "-");
            }
        }
        // Print out the TextTables
        p.println("Section: Run Information");
        p.println(infoTable.toString());

        p.println("Section: Overall Throughput (" + thruMetric + ")");
        p.println("Display: Line");
        p.println(thruTable.toString());

        p.println("Section: Summary Throughput Per Operation (" + thruMetric + ")");
        p.println(opAvgThruTable.toString());

        // Print detailed thruput only if more than one operation as otherwise overall thruput is same as detail
        if (opNames.size() > 1) {
            for (int k = 0; k < opNames.size(); k++) {
                p.println("Section: Detailed Throughput For  Operation '" +
                        opNames.get(k) + "' (" + thruMetric + ")");
                p.println("Display: Line");
                opThruTable = new TextTable(maxThruRows, runDirs.length + 1);
                opThruTable.setHeader(0, "Time");
                for (int i = 0; i < timeVals.size(); i++)
                    opThruTable.setField(i, 0, timeVals.get(i).toString());

                for (int i = 0; i < runDirs.length; i++) {
                    opThruTable.setHeader(i + 1, runDirs[i]);
                    int j;
                    for (j = 0; j < opThruList[i][k].size(); j++) {
                        opThruTable.setField(j, i + 1, opThruList[i][k].get(j).toString());
                    }
                    // Fill with dash for remaining time intervals (if any)
                    int rem = timeVals.size() - opThruList[i][k].size();
                    for (; rem > 0; rem--) {
                        opThruTable.setField(j++, i + 1, "-");
                    }
                }
                p.println(opThruTable.toString());
            }
        }

        p.println("Section: Average Response Times (" + respMetric + ")");
        p.println(avgRespTable.toString());

        // Print nth percentile RT info
        for (int j = 0; j < rtPercentList.length; j++) {
            p.println("Section: " + rtPercentNames.get(j) + " Percentile Response Times (" + respMetric + ")");
            p.println(percentRespTable[j].toString());
        }

        for (int k = 0; k < opNames.size(); k++) {
            p.println("Section: Average Response Times for Operation '" +
                    opNames.get(k) + "' (" + respMetric + ")");
            p.println("Display: Line");
            respTable = new TextTable(maxThruRows, runDirs.length + 1);
            respTable.setHeader(0, "Time");

            // Set time column for all rows
            for (int i = 0; i < timeVals.size(); i++)
                respTable.setField(i, 0, timeVals.get(i).toString());

            for (int i = 0; i < runDirs.length; i++) {
                respTable.setHeader(i + 1, runDirs[i]);
                int j;
                for (j = 0; j < respList[i][k].size(); j++) {
                    respTable.setField(j, i + 1, respList[i][k].get(j).toString());
                }
                int rem = timeVals.size() - respList[i][k].size();
                for (; rem > 0; rem--) {
                    respTable.setField(j++, i + 1, "-");
                }
            }
            p.println(respTable.toString());
        }

        // Print distribution of response times
        for (int k = 0; k < opNames.size(); k++) {
            p.println("Section: Distribution of Response Times for Operation '" +
                    opNames.get(k) + "' (" + respMetric + ")");
            p.println("Display: Line");
            respTable = new TextTable(maxDistRows, runDirs.length + 1);
            respTable.setHeader(0, "Time");

            // Set time column for all rows
            for (int i = 0; i < timeDistVals.size(); i++)
                respTable.setField(i, 0, timeDistVals.get(i).toString());

            for (int i = 0; i < runDirs.length; i++) {
                respTable.setHeader(i + 1, runDirs[i]);
                int j;
                for (j = 0; j < respDistList[i][k].size(); j++) {
                    respTable.setField(j, i + 1, respDistList[i][k].get(j).toString());
                }
                int rem = timeDistVals.size() - respDistList[i][k].size();
                for (; rem > 0; rem--) {
                    respTable.setField(j++, i + 1, "-");
                }
            }
            p.println(respTable.toString());
        }

        // Print CPU utilization
        p.println("Section: Average CPU Utilization");
        p.println(cpuTable.toString());

        p.close();
        return;
    }

    PrintWriter openOutFile(String file, String type) throws IOException {
        PrintWriter p = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        p.println("Title: " + type + " Report");
        p.println();
        return (p);
    }

    private String getSumFile(String runDir) throws IOException {
        String sumFile = Config.OUT_DIR + runDir + File.separator + "summary.xml";
        File f = new File(sumFile);
        if (!f.exists()) {
            throw new IOException(sumFile + " does not exist");
        }
        return (sumFile);
    }

    private String getDetFile(String runDir) throws IOException {
        String file = Config.OUT_DIR + runDir + File.separator + "detail.xan";
        if (!new File(file).exists()) {
            throw new IOException("detail.xan does not exist in " + runDir);
        }
        return (file);
    }

    private File[] getVmstatXanFile(String runDir) throws IOException {
        ArrayList<String> vmFileNames = new ArrayList<String>();
        File dirf = new File(runDir);
        File[] vmfiles = dirf.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.startsWith("vmstat.xan.");
            }
        });
        return (vmfiles);
    }

    /*
    * Get the throughput unit for this run
    * @param XMLReader for summary.xml of this run
    * @return String unit
    */

    String getThruUnit(XMLReader r) {

        List<String> m;
        m = r.getAttributeValues("benchSummary/metric", "unit");
        String unit = m.get(0);
        return (unit);
    }

    String getRespUnit(XMLReader r) {

        List<String> m;
        m = r.getAttributeValues("driverSummary/responseTimes", "unit");
        String unit = m.get(0);
        return (unit);
    }

    /*
    * @method getResponseTimes
    * @param List opnames - list to add operation names to
    * @param List rtAvgList - list to add avg. RT
    * @param List rt90List - list to add 90% RT
    */
    private boolean first = true;

    void getResponseTimes(XMLReader reader, List opNames, List rtAvgList, List rtPercentNames)
            throws IOException {
        Element root = reader.getRootNode();

        Node operationsNode = reader.getNode("driverSummary/responseTimes", root);

        if (operationsNode.getNodeType() != Node.ELEMENT_NODE) {
            throw new IOException("Error: Can't find node driverSummary/responseTimes");

        }
        Element tse = (Element) operationsNode;
        NodeList ops = reader.getNodes("operation", tse);

        for (int j = 0; j < ops.getLength(); j++) {
            Element op = (Element) ops.item(j);
            if (first) { // Add names only first time
                NamedNodeMap attrList = op.getAttributes();
                for (int k = 0; k < attrList.getLength(); k++) {
                    if (attrList.item(k).getNodeType()
                            == Node.ATTRIBUTE_NODE) {
                        if (attrList.item(k).getNodeName().equals("name")) {
                            opNames.add(attrList.item(k).getNodeValue());
                            break;  // no need to check other attributes
                        }
                    }
                }
            }
            // The old style 90th percentile result is just p90th
            // The new style is <percentile nth="90" suffix="th" limit="100.000">1.000</percentile>
            NodeList pers = reader.getNodes("percentile", op);
            try {
                if (pers == null || pers.getLength() == 0) { // No percentile found. Look for old style

                    if (j == 0) {
                        rtPercentNames.add("90th");
                        rtPercentList = new ArrayList[1];
                        rtPercentList[0] = new ArrayList<String>();
                    }
                    rtPercentList[0].add(reader.getValue("p90th", op));

                } else {
                    if (j == 0) {
                        int numPercent = pers.getLength();  // number of percentile entries
                        rtPercentList = new ArrayList[numPercent];
                    }
                    for (int l = 0; l < pers.getLength(); l++) {
                        Element p = (Element) pers.item(l);
                        // Check if this is a 90th percentile element
                        NamedNodeMap attrL = p.getAttributes();
                        for (int k1 = 0; k1 < attrL.getLength(); k1++) {
                            Node attr = attrL.item(k1);
                            if (attr.getNodeType() == Node.ATTRIBUTE_NODE) {
                                if ("nth".equals(attr.getNodeName())) {
                                    if (j == 0) {
                                        rtPercentNames.add(attr.getNodeValue());
                                        rtPercentList[l] = new ArrayList<String>();
                                    }
                                    rtPercentList[l].add(p.getNodeValue());
                                }
                            }
                        }
                    }
                }
                rtAvgList.add(reader.getValue("avg", op));
            } catch (XMLException xe) {
                // just ignore. No data will get added
            }
        }
        first = false;
    }

    /*
    * parse Run Info from summary file
    * This method parses global run information and sets the TextTable elements appropriately
    * @param String wlchar.xan file
    * @param TextTable where values need to be entered
    * @param int idx into TextTable for this runid
    */

    private void getRunInfo(String runDir, XMLReader sumReader, TextTable t, int idx) throws IOException {
        String runFile = Config.OUT_DIR + runDir + File.separator + "run.xml";
        XMLReader r = new XMLReader(runFile, true, false);
        String description = r.getValue("fh:description");

        String runId = sumReader.getValue("benchSummary/runId");
        String metricVal = sumReader.getValue("benchSummary/metric");
        //List<String> m = sumReader.getAttributeValues("benchSummary/metric", "unit");
        //String metric = m.get(0);
        String passed = sumReader.getValue("benchSummary/passed");
        t.setField(idx, 0, runId);
        t.setField(idx, 1, metricVal);
        t.setField(idx, 2, passed);
        t.setField(idx, 3, description);
    }

    /*
    * @method getCpuUtil
    * This method reads the vmstat.xan.* file to get the total cpu% for
    * each host in the test configuration.
    * @param String runDir
    * @return ArrayList<String>[] - list of host,util pairs
    * @throws IOException if it can't find/open vmstat.xan.* or if the file contents are not as expected
    *
    */
    ArrayList<String>[] getCpuUtil(String runDir) throws IOException {
        String tokens[];
        ArrayList<String> hostUtils[];
        int usr = 0, sys = 0, total;
        File files[] = getVmstatXanFile(runDir);
        hostUtils = new ArrayList[files.length];
        int i = 0;
        for (File file : files) {
            // Read the total cpu util.
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            String filename = file.getName();

            while ((line = br.readLine()) != null) {
                if (line.contains("CPU (usr)")) {
                    // process the line.
                    tokens = line.split("\\s+");
                    if (tokens.length != 5) {
                        logger.warning("Error processing " + filename + ". Number of tokens on CPU (usr) line is " + tokens.length);
                        break;
                    }
                    usr = Integer.parseInt(tokens[2]);
                } else if (line.contains("CPU (sys)")) {
                    tokens = line.split("\\s+");
                    if (tokens.length != 5) {
                        logger.warning("Error processing " + filename + ". Number of tokens on CPU (sys) line is " + tokens.length);
                        break;
                    }
                    sys = Integer.parseInt(tokens[2]);
                    break;
                }

            }
            br.close();
            total = usr + sys;
            String host = filename.substring("vmstat.xan.".length());   // get the hostname that follows 'vmstat.xan.'
            hostUtils[i] = new ArrayList<String>();
            hostUtils[i].add(host); //hostname
            hostUtils[i].add(Integer.toString(total)); //cpu util
            i++;
        }

        return (hostUtils);
    }


    /*
     * @param args the command line arguments which should be the list of runDirs
     */

    public static void main(String[] args) {
        // For testing purposes, output will be in /tmp
        try {
            RunAnalyzer sc = new RunAnalyzer();
            sc.compare(args, "/tmp/compare/c.xan");
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

    }
}
