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
 * $Id: Result.java,v 1.25 2009/02/14 05:34:17 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.security.AccessController;
import com.sun.faban.harness.util.FileHelper;
import com.sun.faban.harness.util.XMLReader;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Result class scans through the output directory and compiles a list of run
 * results. Enhance this to enable the user to sort it using different options.
 */
public class Result {

    private static Logger logger = Logger.getLogger(Result.class.getName());
    private static ConcurrentHashMap<String, Result> resultCache =
            new ConcurrentHashMap<String, Result>(1024);

    // The output format
    private SimpleDateFormat dateFormat = new SimpleDateFormat(
                              "EEE'&#160;'MM/dd/yy HH:mm:ss'&#160;'z");
    // The format in the result file
    private SimpleDateFormat parseFormat = new SimpleDateFormat(
                              "EEE MMM dd HH:mm:ss z yyyy");

    private long modTime = 0;
    public RunId runId;
    public String description;
    public String tags;
    public ResultField<String> result = new ResultField<String>();
    private String scaleName;
    public String scale;
    private String scaleUnit;
    public ResultField<Double> metric = new ResultField<Double>();
    private String metricUnit;
    public ResultField<String> status = new ResultField<String>();
    public ResultField<Long> dateTime;
    public String submitter;

    /**
     * This getInstance0 assumes you've checked the run id is found. It will
     * return an instance. If the run id is not there, the behavior is not
     * defined. This is why it is a private interface.
     * @param runId The run id of the run
     * @return A result instance.
     */
    private static Result getInstance0(RunId runId) {
        Result result = new Result(runId);
        Result oldResult = resultCache.putIfAbsent(runId.toString(), result);
        if (oldResult != null)
            result = oldResult;
        result.refresh();
        return result;
    }

    /**
     * Obtains the result of a certain run id.
     * @param runId The run id to query
     * @return An instance of the result, or null if such runId is not found
     */
    public static Result getInstance(RunId runId) {
        if (runId.getResultDir().isDirectory())
            return getInstance0(runId);
        return null;
    }

    private Result(RunId runId) {
        this.runId = runId;
    }

    private synchronized void refresh() {

        File resultDir = runId.getResultDir();

        long modTime = resultDir.lastModified();
        if (modTime <= this.modTime) {
            logger.finer("Run " + runId + " already cached.");
            return;
        }
        logger.finer("Fetching run " + runId + " from disk.");
        this.modTime = modTime;

        String shortName = runId.getBenchName();        

        BenchmarkDescription desc = BenchmarkDescription.
                readDescription(shortName, resultDir.getAbsolutePath());
        if (desc == null) {
            Map<String, BenchmarkDescription> benchMap =
                    BenchmarkDescription.getBenchDirMap();
            desc = (BenchmarkDescription) benchMap.get(shortName);
        }
        String href = null;

        // run result and HREF to the summary or log file.
        File resultFile = new File(resultDir, "summary.xml");
        if (resultFile.exists() && resultFile.length() > 0) {
            result.value = "PASSED";
            href = "<a href=\"resultframe.jsp?runId=" +
                    this.runId + "&amp;result=" +
                    desc.resultFilePath + "\">";

            //Use the XMLReader and locate the <passed> elements
            XMLReader reader = new XMLReader(resultFile.
                    getAbsolutePath());

            // Obtain the metric before we break pass/fail.
            metric.text = reader.getValue("benchSummary/metric");
            if (metric.text != null && metric.text.length() > 0)
                metric.value = new Double(metric.text);
            try {
                Date runTime = parseFormat.parse(
                        reader.getValue("benchSummary/endTime"));
                dateTime = new ResultField<Long>();
                dateTime.text = dateFormat.format(runTime);
                dateTime.value = runTime.getTime();
            } catch (ParseException e) {
                // Do nothing. result.dateTime will be null and
                // later we'll use the param file's mod dateTime
                // for this field instead.
            }

            List passedList = reader.getValues("passed");
            for(Object passed : passedList) {
                if(((String) passed).toUpperCase().indexOf("FALSE") != -1) {
                    result.value = "FAILED";
                    break;
                }
            }
        }

        // Put the hyperlink to the results
        if(href != null)
            result.text = href + result.value + "</a>";

        String[] statusFileContent = getStatus(runId.toString());

        status.value = statusFileContent[0];
        StringBuilder b = new StringBuilder(
            "<a href=\"resultframe.jsp?runId=");
        b.append(this.runId);
        b.append("&amp;result=");
        b.append(desc.resultFilePath);
        b.append("&amp;show=logs\">");
        b.append(status.value);
        b.append("</a>");
        status.text = b.toString();

        String paramFileName = resultDir.getAbsolutePath() +
                File.separator + desc.configFileName;
        File paramFile = new File(paramFileName);
        if (dateTime == null && statusFileContent[1] != null) {
            try {
                Date runTime = parseFormat.parse(statusFileContent[1]);
                dateTime = new ResultField<Long>();
                dateTime.text = dateFormat.format(runTime);
                dateTime.value = runTime.getTime();
            } catch (ParseException e) {
                // Do nothing. result.dateTime will be null and
                // later we'll use the param file's mod dateTime
                // for this field instead.
            }
        }
        if (paramFile.isFile()) {

            // Compatible with old versions of Config.RESULT_INFO
            // Old version does not have timestamp in RESULT_INFO
            // So we need to establish it from the paramFile timestamp.
            // This block may be removed in future.
            if (dateTime == null) {
                dateTime = new ResultField<Long>();
                dateTime.value = paramFile.lastModified();
                dateTime.text = dateFormat.format(
                        new Date(dateTime.value.longValue()));
            }
            // End compatibility block

            ParamRepository par = new ParamRepository(paramFileName, false);
            description = par.getParameter("fa:runConfig/fh:description");
            scale = par.getParameter("fa:runConfig/fa:scale");

            if (desc.scaleName == null)
                desc.scaleName = "";
            if (desc.scaleUnit == null)
                desc.scaleUnit = "";
            if (desc.metric == null)
                desc.metric = "";
            scaleName = desc.scaleName;
            scaleUnit = desc.scaleUnit;
            metricUnit = desc.metric;
            // Now we need to fix up all the nulls.

            // First, if we're dealing with totally blank results or just
            // a directory, we just ignore this directory altogether.
            if (result.value == null && status.value == null &&
                    dateTime.value == null)
                return;

            // Then if individual pieces are missing
            if (result.value == null) {
                result.value = "zzzzzz";
                result.text = "N/A";
            }

            if (status.value == null) {
                status.value = "zzzzzz";
                status.text = "N/A";
            }

            if (dateTime == null) {
                dateTime = new ResultField<Long>();
                dateTime.text = "N/A";
                dateTime.value = 0l;
            }

            if (description == null ||
                description.length() == 0)
                description = "UNAVAILABLE";
        }

        File submitterFile = new File(resultDir, "META-INF" + File.separator +
                                      "submitter");
        if (submitterFile.exists())
            try {
                submitter = FileHelper.readStringFromFile(submitterFile).trim();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error reading submitter file for " +
                        "run " + runId, e);
            }

        if (submitter == null)
            submitter = "N/A";

        File tagsFile = new File(resultDir, "META-INF" + File.separator +
                                            "tags");
        if (tagsFile.exists())
             try {
                tags = FileHelper.readContentFromFile(tagsFile).trim();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error reading tags file for " +
                        "run " + runId, e);
            }

        if (tags == null)
            tags = "N/A";
    }

    /**
     * Obtains the current status of a run.
     * @param runId The id of the run in question
     * @return The current status string or "UNKNOWN" in error cases
     */
    public static String[] getStatus(String runId) {
        char[] cBuf = null;
        String[] status = new String[2];
        int length = -1;
        try {
            FileReader reader = new FileReader(Config.OUT_DIR + runId +
                    '/' + Config.RESULT_INFO);
            cBuf = new char[128];
            length = reader.read(cBuf);
            reader.close();
        } catch (IOException e) {
            // Do nothing, length = -1.
        }

        if (length == -1) {
            status[0] = "UNKNOWN";
        } else {
            String content = new String(cBuf, 0, length);
            int idx = content.indexOf('\t');
            if (idx != -1) {
                status[0] = content.substring(0, idx).trim();
                status[1] = content.substring(++idx);
            } else {
                status[0] = content.trim();
            }
        }
        return status;
    }

    public static TableModel getTagSearchResultTable(Set<String> runIds, TagEngine te) {
        ArrayList<Result> resultList = new ArrayList<Result>(runIds.size());
        HashSet<String> scaleNames = new HashSet<String>();
        HashSet<String> scaleUnits = new HashSet<String>();
        HashSet<String> metricUnits = new HashSet<String>();
        Result res = null;
        for (String runid : runIds) {
            try {
                RunId runId = new RunId(runid);
                res = getInstance(runId);
                if (res == null){
                    te.removeRunId(runid);
                    File filename = new File(Config.OUT_DIR + "/tagenginefile");
                    ObjectOutputStream out = new ObjectOutputStream(
                            new FileOutputStream(filename));
                    out.writeObject(te);
                    out.close();
                }
                scaleNames.add(res.scaleName);
                scaleUnits.add(res.scaleUnit);
                metricUnits.add(res.metricUnit);
                resultList.add(res);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot read result dir " + runid, e);
            }
        }
        TableModel table = new TableModel(9);
        table.setHeader(0, "RunID");
        table.setHeader(1, "Description");
        table.setHeader(2, "Result");

        boolean singleScale = false;
        if (scaleNames.size() == 1 && scaleUnits.size() == 1) {
            singleScale = true;
            if (res.scaleName.length() > 0 &&
                    res.scaleUnit.length() > 0)
                table.setHeader(3, res.scaleName + " (" +
                        res.scaleUnit + ')');
            else if (res.scaleName.length() > 0)
                table.setHeader(3, res.scaleName);
            else if (res.scaleUnit.length() > 0)
                table.setHeader(3, res.scaleUnit);
            else
                table.setHeader(3, "Scale");

        } else {
            table.setHeader(3, "Scale");
        }

        boolean singleMetric = false;
        if (metricUnits.size() == 1) {
            singleMetric = true;
            if (res.metricUnit.length() > 0)
                table.setHeader(4, res.metricUnit);
            else
                table.setHeader(4, "Metric");
        } else {
            table.setHeader(4, "Metric");
        }

        table.setHeader(5, "Status");
        table.setHeader(6, "Date/Time");
        table.setHeader(7, "Submitter");
        table.setHeader(8, "Tags");

        StringBuilder b = new StringBuilder();

        for (Result result : resultList) {
            int idx = table.addRow();
            Comparable[] row = table.getRow(idx);
            row[0] = result.runId;
            row[1] = result.description;
            row[2] = result.result;
            ResultField<Integer> scale = new ResultField<Integer>();
            row[3] = scale;
            if (result.scale == null || result.scale.length() < 1) {
                scale.text = "N/A";
                scale.value = Integer.MIN_VALUE;
            } else if (singleScale) {
                scale.text = result.scale;
                scale.value = new Integer(result.scale);
            } else {
                if (result.scaleName.length() > 0)
                    b.append(result.scaleName).append(' ');
                b.append(result.scale);
                if (result.scaleUnit.length() > 0)
                    b.append(' ').append(result.scaleUnit);
                scale.text = b.toString();
                scale.value = new Integer(res.scale);
                b.setLength(0);
            }

            ResultField<Double> metric = new ResultField<Double>();
            row[4] = metric;
            if (result.metric.text == null) {
                metric.text = "N/A";
                metric.value = -1d;
            } else if (singleMetric) {
                metric.text = result.metric.text;
                metric.value = result.metric.value;
            } else {
                b.append(result.metric);
                if (result.metricUnit.length() > 0)
                    b.append(' ').append(result.metricUnit);
                metric.text = b.toString();
                metric.value = result.metric.value;
                b.setLength(0);
            }
            row[5] = result.status;
            row[6] = result.dateTime;
            row[7] = result.submitter;
            row[8] = result.tags;
        }
        table.sort(6, SortDirection.DESCENDING);
        return table;
    }
    
    public static TableModel getResultTable(Subject user) {

        File[] dirs = new File(Config.OUT_DIR).listFiles();
        ArrayList<Result> resultList = new ArrayList<Result>(dirs.length);
        HashSet<String> scaleNames = new HashSet<String>();
        HashSet<String> scaleUnits = new HashSet<String>();
        HashSet<String> metricUnits = new HashSet<String>();

        Result result0 = null;
        for (File runDir : dirs) {
            if (!runDir.isDirectory())
                continue;
            String runIdS = runDir.getName();
            try {
                RunId runId = new RunId(runIdS);
                if (!AccessController.isViewAllowed(user, runIdS))
                    continue;
                result0 = getInstance0(runId);
                scaleNames.add(result0.scaleName);
                scaleUnits.add(result0.scaleUnit);
                metricUnits.add(result0.metricUnit);
                resultList.add(result0);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot read result dir " + runIdS, e);
            }
        }

        TableModel table = new TableModel(9);
        table.setHeader(0, "RunID");
        table.setHeader(1, "Description");
        table.setHeader(2, "Result");

        boolean singleScale = false;
        if (scaleNames.size() == 1 && scaleUnits.size() == 1) {
            singleScale = true;
            if (result0.scaleName.length() > 0 &&
                    result0.scaleUnit.length() > 0)
                table.setHeader(3, result0.scaleName + " (" +
                        result0.scaleUnit + ')');
            else if (result0.scaleName.length() > 0)
                table.setHeader(3, result0.scaleName);
            else if (result0.scaleUnit.length() > 0)
                table.setHeader(3, result0.scaleUnit);
            else
                table.setHeader(3, "Scale");

        } else {
            table.setHeader(3, "Scale");
        }

        boolean singleMetric = false;
        if (metricUnits.size() == 1) {
            singleMetric = true;
            if (result0.metricUnit.length() > 0)
                table.setHeader(4, result0.metricUnit);
            else
                table.setHeader(4, "Metric");
        } else {
            table.setHeader(4, "Metric");
        }

        table.setHeader(5, "Status");
        table.setHeader(6, "Date/Time");
        table.setHeader(7, "Submitter");
        table.setHeader(8, "Tags");

        StringBuilder b = new StringBuilder();

        for (Result result : resultList) {
            int idx = table.addRow();
            Comparable[] row = table.getRow(idx);
            row[0] = result.runId;
            row[1] = result.description;
            row[2] = result.result;
            ResultField<Integer> scale = new ResultField<Integer>();
            row[3] = scale;
            if (result.scale == null || result.scale.length() < 1) {
                scale.text = "N/A";
                scale.value = Integer.MIN_VALUE;
            } else if (singleScale) {
                scale.text = result.scale;
                scale.value = new Integer(result.scale);
            } else {
                if (result.scaleName.length() > 0)
                    b.append(result.scaleName).append(' ');
                b.append(result.scale);
                if (result.scaleUnit.length() > 0)
                    b.append(' ').append(result.scaleUnit);
                scale.text = b.toString();
                scale.value = new Integer(result0.scale);
                b.setLength(0);
            }

            ResultField<Double> metric = new ResultField<Double>();
            row[4] = metric;
            if (result.metric.text == null) {
                metric.text = "N/A";
                metric.value = -1d;
            } else if (singleMetric) {
                metric.text = result.metric.text;
                metric.value = result.metric.value;
            } else {
                b.append(result.metric);
                if (result.metricUnit.length() > 0)
                    b.append(' ').append(result.metricUnit);
                metric.text = b.toString();
                metric.value = result.metric.value;
                b.setLength(0);
            }
            row[5] = result.status;
            row[6] = result.dateTime;
            row[7] = result.submitter;
            row[8] = result.tags;
        }

        table.sort(6, SortDirection.DESCENDING);
        return table;
    }

    public static class ResultField<T extends Comparable>
            implements Comparable {

        String text;
        T value;

        public int compareTo(Object o) {
            ResultField other = (ResultField) o;
            return value.compareTo(other.value);
        }

        public String toString() {
            return text;
        }
    }
}
