/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
 * install_dir/license.txt
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
 * $Id: RunInfo.java,v 1.4 2006/10/31 20:33:58 rahulbiswas Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.RunControl;
import com.sun.faban.driver.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.CDATASection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
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

    public byte[] defBytes;
            
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
    public void postDeserialize() throws ClassNotFoundException{
        if (instance == null)
            instance = this;

        if (driverConfig.driverClass == null){
            try{
                //first try the default class loader
                ClassLoader cl = this.getClass().getClassLoader();
                driverConfig.driverClass=cl.loadClass(driverConfig.className);
                
            }catch(ClassNotFoundException cnfe){
                //do nothing, check defBytes
            }
        }
        //If we couldn't load the class, driverClass is still null
        //Try again, this time check defBytes
        if(driverConfig.driverClass == null){
            
            String tempDir = System.getProperty("faban.tmpdir");
            
            if(tempDir==null){
                tempDir = System.getProperty("java.io.tmpdir");
            }
            
            File classFileDir = new File(tempDir);
            
            String classFileName = new StringBuilder(tempDir)
            .append(driverConfig.className).append(".class").toString();
            
            FileOutputStream fos=null;
            
            try {
                
                File classFile = new File(classFileName);
                fos = new FileOutputStream(classFile);
                fos.write(this.defBytes);
                fos.flush();
                fos.close();
                
                URL url[]= new URL[1];
                
                url[0] = classFileDir.toURI().toURL();
                
                URLClassLoader loader = new URLClassLoader(url,
                        this.getClass().getClassLoader());
                
                driverConfig.driverClass = loader.loadClass(driverConfig.className);
                
            }catch (MalformedURLException ex) {
                throw new ClassNotFoundException(ex.getMessage());
            }catch(IOException ioex){
                throw new ClassNotFoundException(ioex.getMessage());
            }finally{
                try{
                    fos.close();
                }catch(Exception ex){
                    //if fos cannot be closed, leave it alone.
                }
            }
        }
        
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
            
            //if the defining class is null, the benchmark definition needs 
            //to be created based on the information in the driverConfig node.
            if(definingClassName==null || "".equals(definingClassName.trim())){
                definingClassName = createDefinition(runConfigNode);
            }
            
        }
        
        private String getRequestLagTime(Object node) throws Exception{
            
            Element rltNode = (Element) xp.evaluate("requestLagTime/*[1]", node,
                    XPathConstants.NODE);
            
            if(rltNode == null){
                return "";
            }
            String requestLagType = rltNode.getTagName();
            StringBuilder sb = new StringBuilder();
            
            
            if("FixedTime".equalsIgnoreCase(requestLagType)){
                sb.append("@FixedTime(");
                sb.append("cycleType=CycleType.").append(
                        xp.evaluate("cycleType",
                        rltNode).toUpperCase()).append(",");
                sb.append("cycleTime=").append(xp.evaluate("cycleTime",
                        rltNode)).append(",");
                sb.append("cycleDeviation=").append(
                        xp.evaluate("cycleDeviation",rltNode));
                sb.append(")");
                
            }else if ("Uniform".equalsIgnoreCase(requestLagType)){
                sb.append("@Uniform(");
                sb.append("cycleType=CycleType.").append(
                        xp.evaluate("cycleType",
                        rltNode).toUpperCase()).append(",");
                sb.append("cycleMin=").append(xp.evaluate("cycleMin",
                        rltNode)).append(",");
                sb.append("cycleMax=").append(xp.evaluate("cycleMax",
                        rltNode)).append(",");
                sb.append("cycleDeviation=").append(
                        xp.evaluate("cycleDeviation", rltNode));
                sb.append(")");
                
            }else if("NegativeExponential".equalsIgnoreCase(requestLagType)){
                sb.append("@NegativeExponential(");
                sb.append("cycleType=CycleType.").append(
                        xp.evaluate("cycleType", 
                        rltNode).toUpperCase()).append(",");
                sb.append("cycleMean=").append(
                        xp.evaluate("cycleMean",rltNode)).append(",");
                sb.append("cycleMax=").append(
                        xp.evaluate("cycleMax",rltNode)).append(",");
                sb.append("cycleDeviation=").append(
                        xp.evaluate("cycleDeviation",rltNode));
                sb.append(")");
                
            }
            //At this point we have the requestLagTime annotation.
            return sb.toString(); 
           
        }
        private String  createDefinition(Object runConfigNode) throws Exception {
            
            String definingClassName= xp.evaluate("driverConfig/@name", 
                    runConfigNode);
            
            //Get the cycleTime annotation
            Element driverConfigNode = (Element)xp.evaluate("driverConfig",
                    runConfigNode, XPathConstants.NODE);
            String requestLagTime = getRequestLagTime(driverConfigNode);
            
            /**Load the template from file 
             * Template file is small, but is there a better way to read this?
             **/
            String line = null;
            BufferedReader br = null; 
            InputStream is = null;
            
            try{
                ClassLoader cl = this.getClass().getClassLoader();
                is = cl.getResourceAsStream("driver_template");
                br = new BufferedReader(new InputStreamReader(is));
            }catch(Exception ex){
                //what to do?
                ex.printStackTrace();
                throw new ConfigurationException(ex);
            }
            
            StringBuilder sb = new StringBuilder();
            
            while((line=br.readLine())!=null){
                if(!line.equals("\n")){
                    sb.append(line).append("\n");
                }
            }

            String template = sb.toString();
            
            is.close();
            
            /**Get the operation token out of the template. 
             * Use this to create multiple operations/functions that will 
             * replace the token in the java file
             **/
            String opTemplate = template.substring(
                    (template.indexOf("#operation")+10),
                    template.indexOf("operation#"));
            StringBuilder operations = new StringBuilder();
            
            Element operationNode = null;
            int i=1;
            
            while( (operationNode=(Element)xp.evaluate(
                    ("driverConfig/operation["+ i +"]"), 
                    runConfigNode, XPathConstants.NODE)) != null){

                boolean isPost = false;
                
                String requestLagTimeOverride = getRequestLagTime(operationNode);
                String operationName = xp.evaluate("name", operationNode);
                String url = xp.evaluate("url", operationNode);
                String max90th = xp.evaluate("max90th", operationNode);

                String requestString="";
                Element requestNode = (Element)xp.evaluate("get",
                    operationNode, XPathConstants.NODE);
                
                if(requestNode == null){
                    
                    requestNode = (Element)xp.evaluate("post",
                    operationNode, XPathConstants.NODE);
                    
                    if(requestNode !=null){
                        isPost=true;
                    }else{
                        throw new ConfigurationException(
                            "<operation> must have a either a get/post");
                    }
                    //Can't have both post & get either, but if there, will assume you meant GET.
                }
                CDATASection cDataNode = (CDATASection)requestNode.getFirstChild();
                
                if(cDataNode!=null){
                    requestString = cDataNode.getNodeValue();
                }
                
                if(requestLagTimeOverride==null) 
                    requestLagTimeOverride="";
                if(operationName==null) 
                    throw new ConfigurationException(
                            "<operation> must have a <name> ");
                if(url==null) 
                    throw new ConfigurationException(
                            "<operation> must have a <url>");
                
                    
                requestString = generateRandomData(requestString);
                
                //Create the benchmark Operation annotation
                StringBuilder bmop = new StringBuilder(
                        "@BenchmarkOperation(name = \"").append(operationName);
                bmop.append("\", max90th=").append(max90th).append(
                        ", timing = com.sun.faban.driver.Timing.AUTO");
                bmop.append(")");
                
                String opTemplateClone =  new String(opTemplate);
                String reqUrl = null;
                String postRequest = "";
                
                if(isPost) {
                    reqUrl = new StringBuilder("\"").append(url).append(
                            "\"").toString();
                    postRequest = new StringBuilder(
                            ", new StringBuilder()").append(
                            requestString).append(".toString()").toString();
                    
                }else{
                    reqUrl = new StringBuilder(
                            "new StringBuilder(\"").append(url).append(
                            "\")").append(requestString).append(".toString()"
                            ).toString();
                }
                
                //replace tokens with actual content
                opTemplateClone = opTemplateClone.replaceAll(
                        "@RequestLagTime@", requestLagTimeOverride);
                opTemplateClone = opTemplateClone.replaceAll(
                        "@Operations@", bmop.toString());
                opTemplateClone = opTemplateClone.replaceAll(
                        "@operationName@", operationName);
                opTemplateClone = opTemplateClone.replaceAll(
                        "@url@", reqUrl);
                opTemplateClone = opTemplateClone.replaceAll(
                        "@postRequest@", postRequest);
                
                operations.append(opTemplateClone);
                
                i++;
            }
            
            String benchmarkDef = getBenchmarkDefinition(runConfigNode);
            
            //replace tokens in template
            template = template.replaceFirst("#operation(.*\\n*)*operation#",
                    operations.toString());
            template = template.replaceFirst("@RequestLagTime@",
                    requestLagTime);
            template = template.replaceFirst("@BenchmarkDefinition@",
                    benchmarkDef);
            template = template.replaceAll("@DriverClassName@",
                    definingClassName);
            template = template.replaceFirst("@BenchmarkDriverName@",
                    definingClassName);
            
            String tmpDir = System.getProperty("faban.tmpdir");
            
            if(tmpDir==null){
                tmpDir = System.getProperty("java.io.tmpdir");
            }
            
            if(!tmpDir.endsWith(File.separator)){
                tmpDir = tmpDir+File.separator;
            }
                
            String className = new StringBuilder(tmpDir).append(definingClassName).append(".java").toString();
            
            try{
                //convert class name to filename?
                PrintWriter pw = new PrintWriter(new BufferedWriter(
                        new FileWriter(className)));
                pw.print(template);
                pw.flush();
            }catch(Exception ex){
                ex.printStackTrace();
                throw new ConfigurationException(ex);
            }

            String classpath = System.getProperty("java.class.path");
            
            String arg[] = new String[] { "-classpath", classpath, className };
            com.sun.tools.javac.Main javac =  new com.sun.tools.javac.Main();
            int errorCode = javac.compile(arg);
            
            if(errorCode != 0){
                throw new ConfigurationException(
                        "unable to compile generated driver file. " +
                        "check output for errors"); 
            }

            
            return definingClassName;
            
        }
        private String generateRandomData(String inputString) 
        throws ConfigurationException{
            StringBuilder output = new StringBuilder();
            
            String[] strArr = inputString.split("@@");
            
            for(int i=0; i< strArr.length; i++){
                
                if(strArr[i].startsWith("faban.getRandom")){
                   
                    String tmp[] = strArr[i].split(",");
                    String low = tmp[0].split("\\(")[1];
                    String high = tmp[1].split("\\)")[0];
                    
                    int nLow = 0;
                    int nHi = 0;
                    try{
                        
                        nLow = Integer.parseInt(low.trim());
                        nHi = Integer.parseInt(high.trim());
                        
                    }catch(Exception nex){
                        throw new ConfigurationException(nex);
                    }
                    
                    if(strArr[i].startsWith("faban.getRandomString")){
                        strArr[i] = new StringBuilder("random.makeAString("
                                ).append(low).append(",").append(high).append(
                                ")").toString();
                    }else if(strArr[i].startsWith("faban.getRandomInt")){
                        strArr[i] = new StringBuilder("random.random(").append(
                                low).append(",").append(high).append(")"
                                ).toString();
                    }
                    
                    output.append(".append(").append(strArr[i]).append(")");
                }else{
                    output.append(".append(\"").append(strArr[i]).append("\")");
                }   
                
                
            }

            return output.toString();
        }
        private String getBenchmarkDefinition(Object runConfigNode)
        throws Exception{
            //to do error checking -- name is required?
            String defName      = xp.evaluate("benchmarkDefinition/name",
                    runConfigNode);
            String version      = xp.evaluate("benchmarkDefinition/version",
                    runConfigNode);
            String metric       = xp.evaluate("benchmarkDefinition/metric",
                    runConfigNode);
            String scaleName    = xp.evaluate("benchmarkDefinition/scaleName",
                    runConfigNode);
            String scaleUnit    = xp.evaluate("benchmarkDefinition/scaleUnit",
                    runConfigNode);
            
            
            StringBuilder sb = new StringBuilder("@BenchmarkDefinition (");
            sb.append("name = \"").append(defName).append("\",");
            sb.append("version = \"").append(version).append("\",");
            sb.append("metric = \"").append(metric).append("\",");
            sb.append("scaleName = \"").append(scaleName).append("\",");
            sb.append("scaleUnit = \"").append(scaleUnit).append("\",");
            sb.append("configPrecedence = true)");
            
            return sb.toString();
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
                
                //need to store the class bytes for a remote agent
                //if the driver is an http driver
                
                InputStream is = null;
                Class defClass=null;
                String defClassName = getDefiningClassName();
                try{
                    //check whether this class is defined in this class loader

                    ClassLoader cl = this.getClass().getClassLoader();
                    cl.loadClass(defClassName);
                    
                }catch(ClassNotFoundException cnfe){
                
                    String tempDir = System.getProperty("faban.tmpdir");
                    
                    if(tempDir==null){
                        tempDir = System.getProperty("java.io.tmpdir");
                    }
                    
                    File classFile = new File(tempDir);
                    
                    URL url[]= new URL[1];
                    
                    try {
                        url[0] = classFile.toURI().toURL();
                    } catch (MalformedURLException ex) {
                        throw new ConfigurationException(ex);
                    }
                    
                    URLClassLoader loader = new URLClassLoader(url, BenchmarkDefinition.class.getClassLoader());
                    
                    try{
                        
                        defClass=loader.loadClass(defClassName);
                        is = loader.getResourceAsStream(defClassName+".class");
                        
                        runInfo.defBytes = new byte[is.available()];
                        is.read(runInfo.defBytes);
                        is.close();
                        System.out.println("Bytes Read from class :"+runInfo.defBytes.length);
                        
                    }catch(ClassNotFoundException cnfex){
                        throw new ConfigurationException(cnfex);
                    }catch(IOException ioex){
                        throw new ConfigurationException(ioex);
                    }
                }
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
