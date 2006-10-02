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
 * $Id: DeployUtil.java,v 1.6 2006/10/02 20:44:27 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.util;

import com.sun.faban.common.Command;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.engine.RunQ;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;
import java.text.SimpleDateFormat;

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
    public static void unjar(String benchName)
            throws IOException, InterruptedException {

        logger.info("Redeploying " + benchName);

        String jarName = benchName + ".jar";
        File dir = new File(Config.BENCHMARK_DIR, benchName);

        if (dir.exists())
            FileHelper.recursiveDelete(dir);
        dir.mkdir();

        String jarCmd = getJavaHome() + File.separator + "bin" +
                        File.separator + "jar";
        Command cmd = new Command(jarCmd + " xf " + Config.BENCHMARK_DIR +
                                  jarName);
        cmd.setWorkingDirectory(Config.BENCHMARK_DIR + benchName);
        cmd.execute();
    }

    // Get JAVA_HOME and clip off the /jre subpath if necessary
    public static String getJavaHome() {
        String javaHome = System.getProperty("java.home");
        String suffix = File.separator + "jre";
        if (javaHome.endsWith(suffix))
            javaHome = javaHome.substring(0, javaHome.length() -
                       suffix.length());
        return javaHome;
    }

    public static void generateDD(String dir) {
        String benchDir = Config.BENCHMARK_DIR + File.separator + dir +
                          File.separator;
        String metaInf = benchDir + "META-INF" + File.separator;
        String xmlPath = metaInf + "benchmark.xml";
        File benchmarkXml = new File(xmlPath);
        if (benchmarkXml.exists())
            try {
                DocumentBuilder parser = DocumentBuilderFactory.newInstance().
                        newDocumentBuilder();
                Node root = parser.parse(benchmarkXml).getDocumentElement();
                XPath xPath = XPathFactory.newInstance().newXPath();
                String configFile = xPath.evaluate("config-file-name", root);
                if (configFile == null || configFile.length() == 0)
                    throw new IOException("Element <config-file-name> empty " +
                            "or missing in " + benchmarkXml.getAbsolutePath());

                String benchLib = benchDir + "lib";
                File benchLibDir = new File(benchLib);
                String[] jarFiles = benchLibDir.list();
                String classpath = Config.LIB_DIR + "fabandriver.jar";
                for (int i = 0; i < jarFiles.length; i++)
                    if (jarFiles[i].endsWith(".jar"))
                        classpath += File.pathSeparator + benchLib +
                                     File.separator + jarFiles[i];

                String ddCmd = getJavaHome() + File.separator + "bin" +
                               File.separator + "java -classpath " + classpath +
                               " -Dbenchmark.config=" + configFile +
                               " -Dbenchmark.ddfile=faban.xml" +
                               " com.sun.faban.driver.util.DDGenerator";

                Command cmd = new Command(ddCmd);
                cmd.setWorkingDirectory(metaInf);
                cmd.execute();

            } catch (Exception e) {
                Logger logger = Logger.getLogger(DeployUtil.class.getName());
                logger.log(Level.WARNING, "Error generating FabanDriver " +
                        "descriptor for " + dir, e);
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
                }
            }
        }
    }
}
