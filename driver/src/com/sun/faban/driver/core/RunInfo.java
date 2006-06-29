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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: RunInfo.java,v 1.1 2006/06/29 18:51:33 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.RunControl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Handler;

/**
 * RunInfo
 * This class contains the run parameters used for the run.
 * They directly reflect the run configuration.
 */
public class RunInfo implements Serializable {
    public String resultsDir;
    public int scale = 1;
    public boolean audit = false;
    public String runId;
    public int rampUp;
    public int rampDown;
    public int stdyState;
    public boolean simultaneousStart = false;
    public boolean parallelAgentThreadStart = false;
    public int msBetweenThreadStart = 0;
    public int benchStartTime = Integer.MAX_VALUE;
    public long start = Long.MAX_VALUE;			// benchStartTime in actual time
    public int maxRunTime = 6;  // 6 hrs
    public int graphInterval = 30; // 30 seconds
    public boolean runtimeStatsEnabled = false;
    public int runtimeStatsInterval = 30;
    public DriverConfig driverConfig;
    public AgentInfo agentInfo;

    transient DriverConfig[] driverConfigs;

    private static transient ConfigurationReader reader;
    private static transient RunInfo instance;
    public transient Handler logHandler;

    private RunInfo() {
    }

    /**
     * Obtains the defining class name from the configuration file.
     * @return The name of the defining class
     * @throws Exception An error occurred reading the configuration
     */
    public static String getDefiningClassName() throws Exception {
        reader = new ConfigurationReader();
        return reader.getDefiningClassName();
    }

    /**
     * Reads all the configuration parameters from the file and combines
     * it with the benchmark definition.
     * @param benchDef The benchmark definition
     * @return The resulting RunInfo structure
     * @throws Exception If there is an error reading the configuration,
     *         or it mesmatches the definition
     */
    public static RunInfo read(BenchmarkDefinition benchDef) throws Exception {
        RunInfo runInfo = reader.getRunInfo(benchDef);
        reader = null;
        instance = runInfo;
        return runInfo;
    }

    /**
     * Obtains the singleton RunInfo instance for this JVM.
     * @return The singleton instance or null if it has not been initialized
     */
    public static RunInfo getInstance() {
        return instance;
    }

    /**
     * The method postDeserialize re-establishes the non-serializable fields.
     * @throws ClassNotFoundException
     */
    public void postDeserialize() throws ClassNotFoundException {
        if (instance == null)
            instance = this;
        if (driverConfig.driverClass == null)
            driverConfig.driverClass = Class.forName(driverConfig.className);
        BenchmarkDefinition.refillOperations(
                driverConfig.driverClass, driverConfig.mix[0].operations);
        if (driverConfig.mix[1] != null)
        BenchmarkDefinition.refillOperations(
                driverConfig.driverClass, driverConfig.mix[1].operations);

    }


    /**
     * Information passed to individual agents. This changes from
     * one to another agent.
     */
    public static class AgentInfo implements Serializable {
        public int agentNumber;
        public int startThreadNumber;
        public int threads;
        public double agentScale;
        public transient String agentType;
    }

    public static class DriverConfig extends BenchmarkDefinition.Driver {
        public RunControl runControl;
        public int numAgents = -1; // Defaults to the actual number of agents.
        public int numThreads = -1; // Overrides the threadPerScale
        int maxRunTime;
        int graphInterval;
        public String runtimeStatsTarget;
        public Element rootElement;
        public Element properties;

        DriverConfig(BenchmarkDefinition.Driver driverDef) {
            name = driverDef.name;
            metric = driverDef.metric;
            opsUnit = driverDef.opsUnit;
            threadPerScale = driverDef.threadPerScale;
            if (driverDef.mix[1] != null)
                mix[1] = (Mix) driverDef.mix[1].clone();
            mix[0] = (Mix) driverDef.mix[0].clone();
                        // Copy operation references into a flat array.
            int totalOps = driverDef.operations.length;
            operations = new BenchmarkDefinition.Operation[totalOps];
            for (int j = 0; j < mix[0].operations.length; j++)
                operations[j] = mix[0].operations[j];
            if (mix[1] != null)
                for (int j = 0; j < mix[1].operations.length; j++)
                    operations[j + mix[0].operations.length] =
                            mix[1].operations[j];

            className = driverDef.className;
            driverClass = driverDef.driverClass;
        }
    }

    /**
     * Description of new class
     *
     * @author Akara Sucharitakul
     */
    static class ConfigurationReader {

        RunInfo runInfo;
        String configFileName;
        Exception parseException = null;
        String definingClassName = null;
        Element rootElement;
        Object runConfigNode;
        XPath xp;


        ConfigurationReader() throws Exception {
            XPathFactory xf = XPathFactory.newInstance();
            xp = xf.newXPath();
            configFileName = System.getProperty("benchmark.config");
            if (configFileName == null)
                throw new IOException("Property \"benchmark.config\" not set.");
            DocumentBuilderFactory domFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
            Document doc = domBuilder.parse(new File(configFileName));
            rootElement = doc.getDocumentElement();
            String rootElementName = rootElement.getNodeName();
            if ("runConfig".equals(rootElementName))
                runConfigNode = rootElement;
            else
                runConfigNode = xp.evaluate("runConfig", rootElement,
                        XPathConstants.NODE);
            if (runConfigNode == null)
                throw new ConfigurationException(
                        "Cannot find <runConfig> element.");
            definingClassName = xp.evaluate("@definition", runConfigNode);
        }

        String getDefiningClassName() throws Exception {
            return definingClassName;
        }

        RunInfo getRunInfo(BenchmarkDefinition benchDef)
                throws Exception {

            RunInfo runInfo = new RunInfo();
            String v = xp.evaluate("scale", runConfigNode);
            if (v != null && v.length() > 0)
                try {
                    runInfo.scale = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<scale> must be an integer.");
                }

            v = xp.evaluate("runControl/rampUp", runConfigNode);
            if (v == null || v.length() == 0)
                throw new ConfigurationException(
                        "Element <rampUp> not found.");
            try {
                runInfo.rampUp = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        "<rampUp> must be an integer.");
            }

            v = xp.evaluate("runControl/steadyState", runConfigNode);
            if (v == null || v.length() == 0)
                throw new ConfigurationException(
                        "Element <steadyState> not found.");
            try {
                runInfo.stdyState = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        "<steadyState> must be an integer.");
            }

            v = xp.evaluate("runControl/rampDown", runConfigNode);
            if (v == null || v.length() == 0)
                throw new ConfigurationException(
                        "Element <rampDown> not found.");
            try {
                runInfo.rampDown = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        "<rampDown> must be an integer.");
            }

            runInfo.resultsDir = xp.evaluate("outputDir", runConfigNode);
            if (runInfo.resultsDir == null || runInfo.resultsDir.length() == 0)
                throw new ConfigurationException(
                        "Element <resultsDir> not found.");

            v = xp.evaluate("audit", runConfigNode);
            if (v != null && v.length() > 0)
                try {
                    runInfo.audit = relaxedParseBoolean(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<audit> must be true or false.");
                }

            v = xp.evaluate("threadStart/delay", runConfigNode);
            if (v != null && v.length() > 0)
                try {
                    runInfo.msBetweenThreadStart = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<delay> must be an integer.");
                }

            v = xp.evaluate("threadStart/simultaneous", runConfigNode);
            if (v != null && v.length() > 0)
                try {
                    runInfo.simultaneousStart = relaxedParseBoolean(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<simultaneous> must be true or false.");
                }

            v = xp.evaluate("threadStart/parallel", runConfigNode);
            if (v != null && v.length() > 0)
                try {
                    runInfo.parallelAgentThreadStart = relaxedParseBoolean(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<parallel> must be true or false.");
                }

            v = xp.evaluate("stats/maxRunTime", runConfigNode);
            if (v!= null && v.length() > 0)
                try {
                    runInfo.maxRunTime = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<maxRunTime> must be an integer.");
                }

            v = xp.evaluate("stats/interval", runConfigNode);
            if (v!= null && v.length() > 0)
                try {
                    runInfo.graphInterval = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<interval> must be an integer.");
                }

            v = xp.evaluate("runtimeStats/@enabled", runConfigNode);
            if (v != null && v.length() > 0)
                try {
                    runInfo.runtimeStatsEnabled = relaxedParseBoolean(v);
                } catch (Exception e) {
                    throw new ConfigurationException(
                            "<runtimeStats enabled=[true|false]>");
                }

            v = xp.evaluate("runtimeStats/interval", runConfigNode);
            if (v != null && v.length() > 0)
                try {
                    runInfo.runtimeStatsInterval = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<interval> must be an integer.");
                }

            runInfo.driverConfigs = new DriverConfig[benchDef.drivers.length];
            for (int i = 0; i < benchDef.drivers.length; i++) {
                DriverConfig driverConfig =
                        new DriverConfig(benchDef.drivers[i]);
                Element driverConfigNode = (Element) xp.evaluate(
                        "driverConfig[@name=\"" + driverConfig.name + "\"][1]",
                        runConfigNode, XPathConstants.NODE);

                driverConfig.runControl = benchDef.runControl;
                if (driverConfigNode == null)
                    throw new ConfigurationException("Element <driverConfig " +
                            "name=\"" + driverConfig.name + "\"> not found.");

                v = xp.evaluate("agents", driverConfigNode);
                if (v != null && v.length() > 0)
                    try {
                        driverConfig.numAgents = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        throw new ConfigurationException(
                                "<agents> must be an integer.");
                    }

                v = xp.evaluate("threads", driverConfigNode);
                if (v != null && v.length() > 0)
                    try {
                        driverConfig.numThreads = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        throw new ConfigurationException(
                                "<threads> must be an integer.");
                    }

                v = xp.evaluate("stats/interval", driverConfigNode);
                if (v!= null && v.length() > 0)
                    try {
                        driverConfig.graphInterval = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        throw new ConfigurationException(
                                "<interval> must be an integer.");
                    }
                else driverConfig.graphInterval = runInfo.graphInterval;

                if (runInfo.runtimeStatsEnabled) {
                    driverConfig.runtimeStatsTarget = xp.evaluate(
                            "runtimeStats/@target", driverConfigNode);
                    if (driverConfig.runtimeStatsTarget == null ||
                            driverConfig.runtimeStatsTarget.length() == 0)
                        throw new ConfigurationException("Element " +
                                "<runtimeStats target=[port]> not found.");
                }
                driverConfig.rootElement = rootElement;
                driverConfig.properties = (Element) xp.evaluate("properties",
                        driverConfigNode, XPathConstants.NODE);
                driverConfig.mix[0].configure(driverConfigNode);
                driverConfig.mix[0].normalize();
                runInfo.driverConfigs[i] = driverConfig;
            }
            return runInfo;
        }

        static boolean relaxedParseBoolean(String str) {
            String newStr = str.toLowerCase();
            boolean retVal;
            if ("true".equals(newStr) || "t".equals(newStr)
                || "yes".equals(newStr) || "y".equals(newStr)
                || "1".equals(newStr))
                retVal = true;
            else if ("false".equals(newStr) || "f".equals(newStr)
                || "no".equals(newStr) || "n".equals(newStr)
                || "0".equals(newStr))
                retVal = false;
            else
                throw new NumberFormatException("Boolean " + str +
                        " not recognized!");
            return retVal;
        }
    }
}
