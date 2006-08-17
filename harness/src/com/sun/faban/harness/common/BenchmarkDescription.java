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
 * $Id: BenchmarkDescription.java,v 1.5 2006/08/17 23:22:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.common;

import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.faban.harness.util.DeployUtil;

/**
 * This is a value class describing the benchmark.
 *
 * @author Akara Sucharitakul
 */
public class BenchmarkDescription implements Serializable {

    /** The max time to cache a map, 10 secs. */
    public static final int MAP_TIMEOUT = 10000;

    /** The short name of the benchmark, and also the directory names. */
    public String shortName;

    /** The full name of the benchmark. This name may contain spaces. */
    public String name;

    /** The version of the benchmark. */
    public String version;

    /** The form used to configure the benchmark. */
    public String configForm;

    /** The file used for configuring the benchmark. */
    public String configFileName;

    /** The result file path relative to the output directory */
    public String resultFilePath = "summary.xml";

    /** The benchmark class name. */
    public String benchmarkClass;

    /** The benchmark metric. */
    public String metric;

    /** The name of the benchmark scale. */
    public String scaleName;

    /** The name of the benchmark unit. */
    public String scaleUnit;

    static final Logger logger = Logger.getLogger(
            BenchmarkDescription.class.getName());
    static final XPath xPath = XPathFactory.newInstance().newXPath();
    static final DocumentBuilder parser;

    static HashMap<String, BenchmarkDescription> benchNameMap;
    static HashMap<String, BenchmarkDescription> benchDirMap;
    static long mapTimeStamp = 0l;

    static {
        try {
            parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.log(Level.SEVERE, "Cannot create DOM DocumentBuilder", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Obtains a map of benchmarks using the directory (or short name)
     * as the key.
     * @return The directory map of currently deployed benchmarks
     */
    public static Map<String, BenchmarkDescription> getBenchDirMap() {
        checkMaps(true);
        return benchDirMap;
    }

    public static Map<String, BenchmarkDescription> getBenchDirMap(
            boolean deploy) {
        checkMaps(deploy);
        return benchDirMap;
    }

    /**
     * Obtains a map of benchmarks using the full name as the key.
     * @return The name map of currently deployed benchmarks
     */
    public static HashMap<String, BenchmarkDescription> getBenchNameMap() {
        checkMaps(true);
        return benchNameMap;
    }

    /**
     * Obtains the benchmark description for a certain deploy directory
     * or short name.
     * @param dir The deploy directory or short name of the benchmark
     * @return The benchmark description or null of none exists
     */
    public static BenchmarkDescription getDescription(String dir) {
        checkMaps(true);
        return (BenchmarkDescription) benchDirMap.get(dir);
    }

    /**
     * Returns the name to display on the banner. If only one benchmark
     * is deployed, the benchmark name and version will be used for the
     * banner. Otherwise the name "Faban" and the faban version will be used
     * for the banner.
     * @return The name to display on the banner page
     */
    public static String getBannerName() {
        checkMaps(true);
        if (benchDirMap.size() == 1) {
            return ((BenchmarkDescription) benchDirMap.values().iterator().
                    next()).name;
        }
        return Config.HARNESS_NAME;
    }

    /**
     * Returns the version number to display on the banner. If only one
     * benchmark is deployed, the benchmark name and version will be used for
     * the banner. Otherwise the name "Faban" and the faban version will be used
     * for the banner.
     * @return The version to display on the banner
     */
    public static String getBannerVersion() {
        if (benchDirMap.size() == 1) {
            return ((BenchmarkDescription)benchDirMap.values().iterator().
                    next()).version;
        }
        return Config.HARNESS_VERSION;
    }



    private static void checkMaps(boolean deploy) {
        if (System.currentTimeMillis() - mapTimeStamp > MAP_TIMEOUT)
            generateMaps(deploy);
    }


    private static void generateMaps(boolean deploy) {
        HashMap<String, BenchmarkDescription> nameMap =
                new HashMap<String, BenchmarkDescription>();
        HashMap<String, BenchmarkDescription> dirMap =
                new HashMap<String, BenchmarkDescription>();

        if (deploy)
            DeployUtil.checkDeploy();

        File[] benchmarks = DeployUtil.BENCHMARKDIR.listFiles();
        for (int i = 0; i < benchmarks.length; i++) {
            logger.finest("Found benchmark directory " + benchmarks[i]);
            BenchmarkDescription desc = readDescription(benchmarks[i].getName(),
                                        benchmarks[i].getAbsolutePath());
            if (desc == null)
                continue;
            BenchmarkDescription otherDesc = (BenchmarkDescription)
                                       nameMap.put(desc.name, desc);
            if (otherDesc == null) {
                dirMap.put(desc.shortName, desc);
            } else {
                logger.log(Level.WARNING, "benchmark " + desc.name +
                        " found duplicate in " + otherDesc.
                        shortName + " and " + desc.shortName +
                        ". Ignoring " + desc.shortName + '.');
                nameMap.put(otherDesc.name, otherDesc);
            }
        }
        benchNameMap = nameMap;
        benchDirMap = dirMap;
        mapTimeStamp = System.currentTimeMillis();
    }

   /**
    * Reads the benchmark description from a directory. This can be either
    * the benchmark deployment directory or the benchmark result dir. The
    * benchmark directory is given as an absolute path.
    * @param shortName The short benchmark name, equals the deployment directory
    * @param dir The benchmark directory
    * @return The benchmark description object.
    */
    public static BenchmarkDescription readDescription(String shortName, String dir) {
        BenchmarkDescription desc = null;
        String metaInf = dir + File.separator + "META-INF" + File.separator;
        File benchmarkXml = new File(metaInf + "benchmark.xml");
        if (benchmarkXml.exists())
            try {
                Node root = parser.parse(benchmarkXml).getDocumentElement();
                desc = new BenchmarkDescription();
                desc.shortName = shortName ;

                desc.configFileName = xPath.evaluate("config-file-name", root);
                if (desc.configFileName == null ||
                        desc.configFileName.length() == 0)
                    throw new IOException("Element <config-file-name> empty " +
                            "or missing in " + benchmarkXml.getAbsolutePath());

                desc.benchmarkClass = xPath.evaluate("benchmark-class", root);

                readFabanDescription(desc, metaInf);

                if (desc.benchmarkClass == null ||
                        desc.benchmarkClass.length() == 0)
                    throw new IOException("Element <benchmark-class> empty " +
                            "or missing in " + benchmarkXml.getAbsolutePath());

                String value = xPath.evaluate("name", root);
                if (value != null && value.length() > 0)
                    desc.name = value;
                if (desc == null)
                    throw new IOException("Element <name> empty or missing " +
                            "in " + benchmarkXml.getAbsolutePath());

                value = xPath.evaluate("version", root);
                if (value != null && value.length() > 0)
                    desc.version = value;
                if (desc.version == null)
                    throw new IOException("Element <version> empty or " +
                            "missing in " + benchmarkXml.getAbsolutePath());

                desc.configForm = xPath.evaluate("config-form", root);
                if (desc.configForm == null || desc.configForm.length() == 0)
                    throw new IOException("Element <config-form> empty or " +
                            "missing in " + benchmarkXml.getAbsolutePath());

                value = xPath.evaluate("result-file-path", root);
                if (value != null && value.length() > 0)
                    desc.resultFilePath = value;

                value = xPath.evaluate("metric", root);
                if (value != null && value.length() > 0)
                    desc.metric = value;
                if (desc.metric == null)
                    throw new IOException("Element <metric> empty or " +
                            "missing in " + benchmarkXml.getAbsolutePath());

                value = xPath.evaluate("scaleName", root);
                if (value != null && value.length() > 0)
                    desc.scaleName = value;
                if (desc.scaleName == null)
                    throw new IOException("Element <scaleName> empty or " +
                            "missing in " + benchmarkXml.getAbsolutePath());

                value = xPath.evaluate("scaleUnit", root);
                if (value != null && value.length() > 0)
                    desc.scaleUnit = value;
                if (desc.scaleUnit == null)
                    desc.scaleUnit = "";

            } catch (Exception e) {
                desc = null;
                logger.log(Level.WARNING, "Error reading benchmark " +
                        "descriptor for " + dir, e);
            }
        return desc;
    }

    private static void readFabanDescription(BenchmarkDescription desc,
                                             String metaInf) {
        File fabanXml = new File(metaInf + "faban.xml");
        if (fabanXml.exists())
            try {
                Node root = parser.parse(fabanXml).getDocumentElement();
                desc.name = xPath.evaluate("name", root);
                desc.version = xPath.evaluate("version", root);
                desc.metric = xPath.evaluate("metric", root);
                desc.scaleName = xPath.evaluate("scaleName", root);
                desc.scaleUnit = xPath.evaluate("scaleUnit", root);

                if (desc.benchmarkClass == null ||
                    desc.benchmarkClass.length() == 0)
                    desc.benchmarkClass =
                            "com.sun.faban.harness.DefaultFabanBenchmark";

            } catch (Exception e) {
                logger.log(Level.WARNING, "Error reading faban driver " +
                        "descriptor for " + desc.shortName, e);
            }
    }

    private BenchmarkDescription() {
    }
}
