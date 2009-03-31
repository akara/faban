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
 * $Id: DeployUtil.java,v 1.17 2009/03/31 00:23:25 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.util;

import com.sun.faban.common.Command;
import com.sun.faban.common.Utilities;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.engine.RunQ;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import org.w3c.dom.NodeList;

/**
 * Benchmark deployment utility used to check/generate the deployment
 * descriptors, check proper deployment of all benchmarks or individual
 * benchmarks, and deploy them if appropriate.
 *
 * @author Akara Sucharitakul
 */
public class DeployUtil {

    public static final File BENCHMARKDIR;

    private static Logger logger;
    static {
        logger = Logger.getLogger(DeployUtil.class.getName());
        BENCHMARKDIR = new File(Config.BENCHMARK_DIR);
        if (!BENCHMARKDIR.isDirectory()) {
            logger.severe(Config.BENCHMARK_DIR + " is not a directory");
            throw new RuntimeException(Config.BENCHMARK_DIR +
                    " is not a directory");
        }
    }


    /**
     * Unjars the content a benchmark jar file into the benchmark directory.
     * @param benchName The name of the benchmark
     */
    public static void unjar(String benchName) throws IOException {

        logger.info("Redeploying " + benchName);

        String jarName = benchName + ".jar";
        File dir = new File(Config.BENCHMARK_DIR, benchName);

        if (dir.exists())
            FileHelper.recursiveDelete(dir);
        dir.mkdir();

        FileHelper.unjar(Config.BENCHMARK_DIR + jarName, dir.getAbsolutePath());
    }

    public static void generateDD(String dir) {
        String benchDir = Config.BENCHMARK_DIR + File.separator + dir +
                          File.separator;
        String metaInf = benchDir + "META-INF" + File.separator;
        String xmlPath = metaInf + "benchmark.xml";
        File benchmarkXml = new File(xmlPath);
        String configFile = null;
            try {
                if (benchmarkXml.exists()) {
                    DocumentBuilder parser = DocumentBuilderFactory.
                            newInstance().newDocumentBuilder();
                    Node root = parser.parse(benchmarkXml).getDocumentElement();
                    XPath xPath = XPathFactory.newInstance().newXPath();
                    configFile = xPath.evaluate("config-file-name", root);
                }

                if (configFile == null || configFile.length() == 0)
                    configFile = "run.xml";

                File libDir = new File(Config.LIB_DIR);
                String[] jarFiles = libDir.list();
                String classpath = null;
                for (int i = 0; i < jarFiles.length; i++)
                    if (jarFiles[i].endsWith(".jar")) {
                        if (classpath == null)
                            classpath = Config.LIB_DIR + jarFiles[i];
                        else
                            classpath += File.pathSeparator + Config.LIB_DIR +
                                     jarFiles[i];
                    }

                String benchLib = benchDir + "lib";
                File benchLibDir = new File(benchLib);
                jarFiles = benchLibDir.list();
                if (jarFiles == null)
                    throw new IOException("Benchmark lib directory " +
                                        benchLib + " empty or non-existent");

                for (int i = 0; i < jarFiles.length; i++)
                    if (jarFiles[i].endsWith(".jar"))
                        classpath += File.pathSeparator + benchLib +
                                     File.separator + jarFiles[i];

                ArrayList<String> ddCmd = new ArrayList<String>();
                ddCmd.add(Utilities.getJavaHome() + File.separator +
                               "bin" + File.separator + "java");
                ddCmd.add("-classpath");
                ddCmd.add(classpath);
                ddCmd.add("-Dbenchmark.config=" + configFile);
                ddCmd.add("-Dbenchmark.ddfile=faban.xml");
                ddCmd.add("-Djava.util.logging.config.file=" +
                               Config.CONFIG_DIR + "logging.properties");
                ddCmd.add("com.sun.faban.driver.util.DDGenerator");

                Command cmd = new Command(ddCmd);
                cmd.setWorkingDirectory(metaInf);
                cmd.execute();

            } catch (Exception e) {
                Logger logger = Logger.getLogger(DeployUtil.class.getName());
                logger.log(Level.WARNING, "Error generating FabanDriver " +
                        "descriptor for " + dir, e);
            }
    }

    public static void generateXform(String dir) throws Exception {
        String benchDir = Config.BENCHMARK_DIR + File.separator + dir +
                          File.separator;
        XPath xPath = XPathFactory.newInstance().newXPath();
        DocumentBuilder parser = DocumentBuilderFactory.
                            newInstance().newDocumentBuilder();
        String metaInf = benchDir + "META-INF" + File.separator;
        String xmlPath = metaInf + "benchmark.xml";
        File benchmarkXml = new File(xmlPath);
        String configForm = null;
        String configFile = null;
        
        if (benchmarkXml.exists()) {
            Node root = parser.parse(benchmarkXml).getDocumentElement();
            configForm = xPath.evaluate("config-form", root);
            configFile = xPath.evaluate("config-file-name", root);
        }

        if (configForm == null || configForm.length() == 0) {
            configForm = "config.xhtml";
        }

        if (configFile == null || configFile.length() == 0){
            configFile = "run.xml";
        }

        File configFormFile = new File(metaInf + configForm);
        if (!configFormFile.exists()) {
            File runFile = new File(metaInf + configFile);
            if (runFile.exists()) {
                File templateFile = new File(Config.FABAN_HOME + "resources/config-template.xhtml");
                XformsGenerator.generate(runFile, configFormFile, templateFile);
            }
        }

    }

    public static boolean canDeploy(String benchName) {
        RunQ runQ = RunQ.getHandle();

        if (benchName.equals(runQ.getCurrentBenchmark()))
            return false;

        String[][] pendingRuns = runQ.listRunQ();
        if (pendingRuns != null)
            for (int i = 0; i < pendingRuns.length; i++)
                if (benchName.equals(pendingRuns[i][RunQ.BENCHNAME]))
                    return false;
        return true;
    }

    public static void checkDeploy() {
        // Check for any jar files dropped in here, deploy if needed.
        File[] jarFiles = BENCHMARKDIR.listFiles();
        for (int i = 0; i < jarFiles.length; i++) {
            if (jarFiles[i].isDirectory())
                continue;
            String suffix = ".jar";
            String jarName = jarFiles[i].getName();
            if (!jarName.endsWith(suffix))
                continue;
            String benchName = jarName.substring(0,
                             jarName.length() - suffix.length());
            checkDeploy(jarFiles[i], benchName);
        }
    }

    public static void checkDeploy(File jarFile, String benchName) {
        File benchDir = new File(BENCHMARKDIR, benchName);
        if (benchDir.isDirectory()) {
            if (jarFile.lastModified() > benchDir.lastModified())
                checkDeploy(benchName);
        } else {
            checkDeploy(benchName);
        }
    }

    public static void checkDeploy(String benchName) {
        if (canDeploy(benchName))
            try {
                unjar(benchName);
                generateDD(benchName);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error deploying benchmark \"" +
                        benchName + "\"", e);
            }
    }

    public static void clearConfig(String benchName) {

        // 1. Figure out the config file name.
        BenchmarkDescription benchDesc = BenchmarkDescription.
                getDescription(benchName);
        if (benchDesc == null)
            return;
        
        String configFileName = benchDesc.configFileName + '.' + benchName;

        // 2. Go to the config/profiles directory and rename all config
        // files underneath.
        File[] profiles = new File(Config.PROFILES_DIR).listFiles();
        if (profiles != null && profiles.length > 0) {
            String date = null;
            for (File profile : profiles) {
                File configFile = new File(profile, configFileName);
                if (configFile.exists()) {
                    if (date == null) {
                        SimpleDateFormat format =
                                new SimpleDateFormat("yyMMddHHmmss");
                        date = "." + format.format(new Date());
                    }
                    File newName = new File(profile, configFileName + date);
                    configFile.renameTo(newName);
                } else if (profile.isFile() &&
                           configFileName.equals(profile.getName())) {
                    if (date == null) {
                        SimpleDateFormat format =
                                new SimpleDateFormat("yyMMddHHmmss");
                        date = "." + format.format(new Date());
                    }
                    File newName = new File(Config.PROFILES_DIR,
                                                configFileName + date);
                    profile.renameTo(newName);
                }
            }
        }
    }
}
