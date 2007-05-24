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
 * $Id: SpecWeb2005Benchmark.java,v 1.1 2007/05/24 01:14:23 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.specweb2005;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.NameValuePair;
import com.sun.faban.harness.Benchmark;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.RemoteCallable;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.faban.harness.RunContext.*;
import static com.sun.faban.harness.util.FileHelper.copyFile;


/**
 * Harness hook for SPECweb2005 drives the process for executing
 * SPECweb2005.
 *
 * @author Sreekanth Setty
 */
public class SpecWeb2005Benchmark implements Benchmark {

    static Logger logger = Logger.getLogger(
                                        SpecWeb2005Benchmark.class.getName());
    String clientJar;
    String testType;
    String runDir;
    String runID;
    ParamRepository par;
    CommandHandle handle;
    String dbServer;
    private Calendar startTime;
    private LinkedHashSet<String> hostsSet;
    ArrayList<NameValuePair<Integer>> hostsPorts;

    /**
     * Allows benchmark to validate the configuration file. Note that no
     * execution facility is available during validation. This method is just
     * for validation and modifications of the run configuration.
     *
     * @throws Exception if any error occurred.
     * @see com.sun.faban.harness.RunContext#exec(com.sun.faban.common.Command)
     */
    public void validate() throws Exception {

        // add runControl parameters due to faban requirement
        par = getParamRepository();

        int rampUp = Integer.parseInt(par.getParameter(
                "runConfig/threadRampupSeconds"));
        rampUp += Integer.parseInt(par.getParameter("runConfig/warmupSeconds"));

        // Faban uses some standard names such as rampUp
        par.setParameter("runConfig/runControl/rampUp", String.valueOf(rampUp));


        // Note that runConfig/clients is used during the generation
        // of Test.config file. And runConfig/hostConfig/host is used for
        // starting the faban processes
        String clients = par.getParameter("runConfig/clients");
        // replacing all the newline characters and other white space
        // characters with a blank space

        clients = clients.replaceAll("\\s", " ");
        par.setParameter("runConfig/clients", clients);

        // Find the patterns that have either hostname or hostname:port values
        Pattern p1 = Pattern.compile("([a-zA-Z_0-9-]+):?(\\w*)\\s*");
        Matcher m1 = p1.matcher(clients + ' '); // add a whitespace at end

        hostsSet = new LinkedHashSet<String>();
        hostsPorts = new ArrayList<NameValuePair<Integer>>();

        //   Fill up the hosts set with names of all the hosts
        for (boolean found = m1.find(); found; found = m1.find()) {
            NameValuePair<Integer> hostPort = new NameValuePair<Integer>();
            hostPort.name = m1.group(1);
            String port = m1.group(2);
            if (port != null && port.length() > 1)
                hostPort.value = new Integer(port);
            logger.fine("adding host:" + hostPort.name);
            hostsSet.add(hostPort.name);
            hostsPorts.add(hostPort);
        }

        // Now extract the unique hosts
        StringBuffer hosts = new StringBuffer();
        for (String host : hostsSet) {
            hosts.append(host);
            hosts.append(' ');
        }

        par.setParameter("runConfig/hostConfig/host", hosts.toString().trim());

        logger.info("Hosts: " + par.getParameter("runConfig/hostConfig/host"));        

        par.save();
    }

    /**
     * This method is called to configure the specific benchmark run
     * Tasks done in this method include reading user parameters,
     * logging them and initializing various local variables.
     */
    public void configure() throws Exception {
        // Add additional configuration needs such as restarting/reconfiguring
        // servers here.
        // configure >>

        runDir = getOutDir();
        runID = getRunId();
        testType = par.getParameter("runConfig/testtype");
        clientJar = par.getParameter("runConfig/clientDir");

        // 1. translate run.xml into Test.config.
        StreamSource stylesheet = new StreamSource(getBenchmarkDir() +
                "META-INF" + File.separator + "run.xsl");
        StreamSource src = new StreamSource(getParamFile());
        StreamResult result = new StreamResult(
                                  new File(runDir, "Test.config"));
        Transformer t = TransformerFactory.newInstance().
                            newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);

        // 2. translate run.xml into appropriate (banking/ecommerce/support)
        //    config file.
        stylesheet = new StreamSource(getBenchmarkDir() + "META-INF" +
                File.separator + testType + ".xsl");
        result = new StreamResult(
                                  new File(runDir, testType + ".config"));
        t = TransformerFactory.newInstance().
                            newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);

        // 3. translate run.xml into Testbed.config
        stylesheet = new StreamSource(getBenchmarkDir() + "META-INF" +
                File.separator + "testbed.xsl");
        result = new StreamResult(new File(runDir, "Testbed.config"));
        t = TransformerFactory.newInstance().
                            newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);
    }

    /**
     * This method is responsible for starting the benchmark run
     * @throws java.lang.Exception
     */
    private void start_clients() throws Exception {
        // The run's output dir is always run 1 under the assigned output dir.
        // We just need to move the files down after the run (or do we need to?)

        String javaOptions = par.getParameter("jvmConfig/javaOptions");
        // replacing the GC option with a blank space
        String javaOptionsNoGC = javaOptions.replaceFirst("-Xloggc:\\S+", " ");

        cleanOldFiles(hostsSet);

        for (NameValuePair hostPort : hostsPorts) {
            String tmp = getTmpDir(hostPort.name);
            String out = tmp + "out." + runID;
            String errors = tmp + "errors." + runID;
            String gc = tmp + "gc." + runID;

            if (hostPort.value != null) {
                out += "." + hostPort.value;
                errors += "." + hostPort.value;
                gc += "." + hostPort.value;
            }

            // command to start to a client driver process
            String cmd = "java " +  javaOptionsNoGC +   " -Xloggc:" + gc +
                            " -classpath " + clientJar + " specwebclient ";
            if (hostPort.value != null)
                cmd += " -p " +  hostPort.value;

            logger.info("Starting the client on " + hostPort.name + ": " + cmd);
            Command client = new Command(cmd);
            client.setSynchronous(false);
            client.setOutputFile(Command.STDOUT, out);
            client.setOutputFile(Command.STDERR, errors);
            // Using capture will enable logging to the files, in 8K chunks.
            client.setStreamHandling(Command.STDOUT, Command.CAPTURE);
            client.setStreamHandling(Command.STDERR, Command.CAPTURE);
            // client.setWorkingDirectory("/export/home/w2005");

            exec(hostPort.name, client);
        }
    }

    // Needs to be static so anonymous inner class is static.
    private static void cleanOldFiles(Set<String> hostsSet) {
        // Clean the result/error/gc files and restart the client driver
        // processes. The processes will be killed at the end of the run.
        for (String hostName : hostsSet) {
            try {
                exec(hostName, new RemoteCallable() {
                    public Serializable call() throws Exception {
                        // Now running on the target host, tmpdir is local.
                        String tmp = getTmpDir(null);
                        logger.info("Cleaning the temporary files in " + tmp);
                        // Now running on the target host, tmpdir is local.
                        File tmpDir = new File(tmp);
                        File[] fileList = tmpDir.listFiles();
                        for (File f : fileList) {
                           if (f.getName().startsWith("out") ||
                               f.getName().startsWith("err") ||
                               f.getName().startsWith("gc"))
                                if (f.isFile() && !f.delete())
                                    logger.info("Could not delete file: " +
                                                    tmp + f.getName());
                        }
                        return null;
                    }
                });
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception deleting files", e);
            }
        }
    }

    /**
     * This method is responsible for starting the benchmark run
     * @throws java.lang.Exception 
     */
    public void start() throws Exception {
        // The run's output dir is always run 1 under the assigned output dir.
        // We just need to move the files down after the run (or do we need to?)

        start_clients();
        // run >>
        // 1. start the run.
        String cmd = "java -server -Xmx800m -Xms800m -classpath " +
                     clientJar + "  specweb";
        logger.info("Starting the Master: " + cmd);
        Command c = new Command(cmd);

        // 2. Trickle the log.
        c.setSynchronous(false);
        c.setOutputFile(Command.STDOUT, "result.txt");
        // Using trickle will enable the Server to see the data
        c.setStreamHandling(Command.STDOUT, Command.TRICKLE_LOG);
        c.setStreamHandling(Command.STDERR, Command.TRICKLE_LOG);
        c.setWorkingDirectory(runDir);

        startTime = Calendar.getInstance();
        handle = exec(c);

    }

    /**
     * This method is responsible for waiting for all commands started and
     * run all postprocessing needed.
     *
     * @throws Exception if any error occurred.
     */
    public void end() throws Exception {

        // 4. Wait for run finish.
        handle.waitFor();

        Calendar endTime = Calendar.getInstance();

        File resultsDir = new File(runDir, "results");
        if (!resultsDir.isDirectory())
            throw new Exception("The results directory not found!");

        String resHtml = null;
        String resTxt = null;
        String[] resultFiles = resultsDir.list();
        for (String resultFile : resultFiles) {
            if (resultFile.indexOf("html") >= 0)
                resHtml = resultFile;
            else if (resultFile.indexOf("txt") >= 0)
                resTxt = resultFile;
        }
        if (resHtml == null)
            throw new IOException("SPECweb2005 output (html) file not found");

        if (resTxt == null)
            throw new IOException("SPECweb2005 output (txt) file not found");

        File resultHtml = new File(resultsDir, resHtml);

        copyFile(resultsDir.getAbsolutePath() + File.separatorChar +
                        resultHtml, runDir + " SPECWeb-result.html", false);


        BufferedReader reader = new BufferedReader(
                                    new FileReader(resultsDir + resTxt));
        logger.fine("Text file: " + resultsDir + resTxt);
        // String sessions = par.getParameter("runConfig/sessions");
        Writer writer = new FileWriter(runDir + "summary.xml");
        SummaryParser parser =
                new SummaryParser(getRunId(), startTime, endTime, logger);
        parser.convert(reader, writer);
        writer.close();
        reader.close();
    }

    /**
     * This method aborts the current benchmark run and is
     * called when a user asks for a run to be killed
     */
    public void kill() {
    }
}
