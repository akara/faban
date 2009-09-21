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
package com.sun.specweb2005;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.NameValuePair;
import com.sun.faban.harness.*;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.faban.harness.RunContext.*;
import static com.sun.faban.harness.util.FileHelper.copyFile;


/**
 * Harness hook for SPECweb2005 drives the process for executing
 * SPECweb2005.
 *
 * @author Sreekanth Setty
 */
public class SpecWeb2005Benchmark {

    static Logger logger = Logger.getLogger(
                                        SpecWeb2005Benchmark.class.getName());
    String clientJar;
    String testType;
    String runDir;
    String runID;
    ParamRepository par;
    CommandHandle handle;
    private Calendar startTime;
    List<NameValuePair<Integer>> hostsPorts;

    /**
     * Allows benchmark to validate the configuration file. Note that no
     * execution facility is available during validation. This method is just
     * for validation and modifications of the run configuration.
     *
     * @throws Exception if any error occurred.
     * @see com.sun.faban.harness.RunContext#exec(com.sun.faban.common.Command)
     */
    @Validate public void validate() throws Exception {

        // add runControl parameters due to faban requirement
        par = getParamRepository();

        int rampUp = Integer.parseInt(par.getParameter(
                                        "fa:runConfig/threadRampupSeconds"));
        rampUp += Integer.parseInt(par.getParameter(
                                        "fa:runConfig/warmupSeconds"));

        // Faban uses some standard names such as rampUp
        par.setParameter("fa:runConfig/fa:runControl/fa:rampUp",
                                                String.valueOf(rampUp));


        // Note that runConfig/clients is used during the generation
        // of Test.config file. And runConfig/hostConfig/host is used for
        // starting the faban processes
        hostsPorts = par.getHostPorts(
                                "fa:runConfig/fa:hostConfig/fa:hostPorts");
        // Do all translations
        runDir = getOutDir();
        runID = getRunId();
        testType = par.getParameter("fa:runConfig/testtype");
        clientJar = par.getParameter("fa:runConfig/clientDir");

        // Translate run.xml into Test.config.
        StreamSource stylesheet = new StreamSource(getBenchmarkDir() +
                "META-INF" + File.separator + "run.xsl");
        StreamSource src = new StreamSource(getParamFile());
        StreamResult result = new StreamResult(
                                  new File(runDir, "Test.config"));
        Transformer t = TransformerFactory.newInstance().
                            newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);

        // Translate run.xml into appropriate
        // (banking/ecommerce/support) config file.
        stylesheet = new StreamSource(getBenchmarkDir() + "META-INF" +
                File.separator + testType + ".xsl");
        result = new StreamResult(
                                  new File(runDir, testType + ".config"));
        t = TransformerFactory.newInstance().
                            newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);

        // Translate run.xml into Testbed.config
        stylesheet = new StreamSource(getBenchmarkDir() + "META-INF" +
                File.separator + "testbed.xsl");
        result = new StreamResult(new File(runDir, "Testbed.config"));
        t = TransformerFactory.newInstance().
                            newTransformer(stylesheet);
        t.setParameter("outputDir", runDir);
        t.transform(src, result);
    }

    private void start_clients() throws Exception {
        // The run's output dir is always run 1 under the assigned output dir.
        // We just need to move the files down after the run (or do we need to?)

        String javaOptions = par.getParameter("fh:jvmConfig/fh:jvmOptions");
        // replacing the GC option with a blank space
        String javaOptionsNoGC = javaOptions.replaceFirst("-Xloggc:\\S+", " ");

        cleanOldFiles(par.getTokenizedValue(
                                    "fa:runConfig/fa:hostConfig/fa:host"));

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

    // The code looks easier this way passing the code block to execute on
    // a remote host. But semantically it can be more complicated. If the
    // object passed has an enclosing object, the enclosing object and all it's
    // state must be serializable. To create a static anonymous inner class
    // not to serialize the enclosing object, we need to do this in a static
    // method. Also, although it looks like all the enclosing class'
    // variables are accessible syntactically, their behavior is often
    // undefined when running remotely. So we need to make sure that this
    // anonymous inner class does not make such assumptions. Access to the
    // RunContext gives undefined results, for example.
    private static void cleanOldFiles(String[] hosts) {
        // Clean the result/error/gc files and restart the client driver
        // processes. The processes will be killed at the end of the run.
        for (String hostName : hosts) {
            final String tmp = getTmpDir(hostName);
            try {
                exec(hostName, new RemoteCallable() {
                    public Serializable call() throws Exception {
                        // Now running on the target host.
                        // The RunContext is virtually non-existent here
                        // RunContext methods have undefined behavior.
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
     * This method is responsible for starting the benchmark run.
     * @throws Exception Error starting the run
     */
    @StartRun public void start() throws Exception {
        // The run's output dir is always run 1 under the assigned output dir.
        // We just need to move the files down after the run (or do we need to?)

        start_clients();
        // run >>
        // 1. start the run.

        // We need to add the jfreechart libs to the prime client.
        StringBuilder classpath = new StringBuilder();
        File clientDir = new File(clientJar).getParentFile();
        File[] files = clientDir.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(".jar")) {
                if (fileName.startsWith("jcommon") ||
                        fileName.startsWith("jfreechart")) {
                    String absolutePath = file.getAbsolutePath();
                    classpath.append(absolutePath).append(
                            File.pathSeparatorChar);
                }
            }
        }
        classpath.append(clientJar);

        String cmd = "java -server -Xmx800m -Xms800m -classpath " +
                     classpath + "  specweb";
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
    @EndRun public void end() throws Exception {

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

        copyFile(resultHtml.getAbsolutePath(),
                runDir + "SPECWeb-result.html", false);


        BufferedReader reader = new BufferedReader(new FileReader(
                                    new File(resultsDir, resTxt)));
        logger.fine("Text file: " + resultsDir + resTxt);
        // String sessions = par.getParameter("runConfig/sessions");
        Writer writer = new FileWriter(runDir + "summary.xml");
        SummaryParser parser =
                new SummaryParser(getRunId(), startTime, endTime, logger);
        parser.convert(reader, writer);
        writer.close();
        reader.close();
    }
}
