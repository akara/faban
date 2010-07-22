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
 * $Id$
 *
 * Copyright 2005-2010 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.engine;

import com.sun.faban.common.ParamReader;
import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.RunControl;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;
import java.util.logging.Handler;


/**
 * RunInfo
 * This class contains the run parameters used for the run.
 * They directly reflect the run configuration.
 *
 * sdo 10/17/08: Add support for https into driver template
 */
public class RunInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/** Namespace URI for Faban in general. */
	public static final String FABANURI = "http://faban.sunsource.net/ns/faban";

    /** Namespace URI for the Faban driver. */
    public static final String DRIVERURI =
                                "http://faban.sunsource.net/ns/fabandriver";
    /** The results directory. */
    public String resultsDir;
    
    /** Scale of the run. */
    public int scale = 1;
    
    /** Whether audit is on or not. This is not used. */
    public boolean audit = false;
    
    /** The run ID. */
    public String runId;
    
    /** Ramp Up, in seconds. */
    public int rampUp;
    
    /** Ramp Down, in seconds. */
    public int rampDown;
    
    /** Steady State, in seconds. */
    public int stdyState;

    /** Use varable load or not. */
    public boolean variableLoad = false;

    /** Variable load input file. */
    public String variableLoadFile;

    /** The load adjuster for variable load. */
    public VariableLoadHandler variableLoadHandler;

    /** Whether all threads start simultaneously. */
    public boolean simultaneousStart = false;
    
    /** Whether agents start in parallel. */
    public boolean parallelAgentThreadStart = false;
    
    /**
     * Milliseconds between thread start.
     */
    public int msBetweenThreadStart = 200;
  
    /** The benchmark start time, relative to the timer. */
    public int benchStartTime = Integer.MAX_VALUE;

    /** The actual millisec start time. */
    public long start = Long.MAX_VALUE;	// benchStartTime in ms actual time

    /** The maximum run time, used only for cycle runs. */
    public int maxRunTime = 6;  // 6 hrs

    /** The time interval for graphing. */
    public int graphInterval = 10; // 30 seconds

    /** Whether the runtime stats are enabled. */
    public boolean runtimeStatsEnabled = false;

    /** Interval for runtime stats. */
    public int runtimeStatsInterval = 10;

    /** The current driver config object. */
    public DriverConfig driverConfig;

    /** The current agent information. */
    public AgentInfo agentInfo;

    /** fhb bytes defining a class. */
    public byte[] defBytes;
            
    transient DriverConfig[] driverConfigs;

    private static transient ConfigurationReader reader;
    private static transient RunInfo instance;

    /** The log handler. */
    public transient Handler logHandler;

    private RunInfo() {
    	super();
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
        if (instance == null) {
			instance = this;
		}

        if (driverConfig.driverClass == null) {
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
        BenchmarkDefinition.refillMethod(driverConfig.driverClass,
                                         driverConfig.preRun);
        BenchmarkDefinition.refillMethod(driverConfig.driverClass,
                                         driverConfig.postRun);

        BenchmarkDefinition.refillOperations(
                driverConfig.driverClass, driverConfig.mix[0].operations);
        if (driverConfig.mix[1] != null) {
			BenchmarkDefinition.refillOperations(
			        driverConfig.driverClass, driverConfig.mix[1].operations);
		}

    }


    /**
     * Information passed to individual agents. This changes from
     * one to another agent.
     */
    public static class AgentInfo implements Serializable {
        
		private static final long serialVersionUID = 1L;
		/** Agent number. */
		public int agentNumber;
        /** Starting thread number. */
        public int startThreadNumber;
        /** Number of threads. */
        public int threads;
        /** Scale of the agent. */
        public double agentScale;
        /** Type of the agent. */
        public transient String agentType;
    }

    /**
     * The {@link DriverConfig} for a specific driver.
     */
    public static class DriverConfig extends BenchmarkDefinition.Driver {
        
		private static final long serialVersionUID = 1L;
		
		/**
		 * The reference to runControl.
		 */
		public RunControl runControl;
		
		/** Number of agents. */
        public int numAgents = -1; // Defaults to the actual number of agents.

        /** Number of threads. */
        public int numThreads = -1; // Overrides the threadPerScale

        /** Graph bucket interval. Overrides the bucket interval of the run. */
        int graphInterval;

        /** Target for runtime stats, currently unused. */
        public String runtimeStatsTarget;

        /** Root element of the configuration DOM tree. */
        public Element rootElement;

        /** Driver level variable load file. */
        public String variableLoadFile;

        /** Property element of the configuration DOM tree. */
        public Element properties;

        DriverConfig(BenchmarkDefinition.Driver driverDef) {
            name = driverDef.name;
            metric = driverDef.metric;
            opsUnit = driverDef.opsUnit;
            threadPerScale = driverDef.threadPerScale;
            responseTimeUnit = driverDef.responseTimeUnit;
            if (driverDef.preRun != null) {
				preRun = (BenchmarkDefinition.DriverMethod)
                        driverDef.preRun.clone();
			}
            if (driverDef.postRun != null) {
				postRun = (BenchmarkDefinition.DriverMethod)
                        driverDef.postRun.clone();
			}
            if (driverDef.mix[1] != null) {
				mix[1] = (Mix) driverDef.mix[1].clone();
			}
            mix[0] = (Mix) driverDef.mix[0].clone();

            if (driverDef.initialDelay[1] != null) {
				initialDelay[1] = (Cycle) driverDef.initialDelay[1].clone();
			}
            initialDelay[0] = (Cycle) driverDef.initialDelay[0].clone();

            // Copy operation references into a flat array.
            int totalOps = driverDef.operations.length;
            operations = new BenchmarkDefinition.Operation[totalOps];
            for (int j = 0; j < mix[0].operations.length; j++) {
				operations[j] = mix[0].operations[j];
			}
            if (mix[1] != null) {
				for (int j = 0; j < mix[1].operations.length; j++) {
					operations[j + mix[0].operations.length] =
                            mix[1].operations[j];
				}
			}

            className = driverDef.className;
            driverClass = driverDef.driverClass;

            percentiles = driverDef.percentiles;
            pctString = driverDef.pctString;
            pctSuffix = driverDef.pctSuffix;
            maxPercentile = driverDef.maxPercentile;
        }
    }

    /**
     * Obtains the run configuration by reading the configuration file.
     *
     * @author Akara Sucharitakul
     */
    static class ConfigurationReader {

        String configFileName;
        String definingClassName = null;
        Element rootElement;
        Object runConfigNode;
        XPath xp;
        

        ConfigurationReader() throws Exception {
            configFileName = System.getProperty("benchmark.config");
            if (configFileName == null) {
				throw new IOException("Property \"benchmark.config\" not set.");
			}

            ParamReader reader = new ParamReader(configFileName, true);
            Document doc = reader.getDocument();
            xp = reader.getXPath();
            rootElement = doc.getDocumentElement();
            String rootNS = rootElement.getNamespaceURI();
            String rootName = rootElement.getLocalName();

            if (FABANURI.equals(rootNS) && "runConfig".equals(rootName)) {
				runConfigNode = rootElement;
			} else {
				runConfigNode = xp.evaluate("fa:runConfig", rootElement,
                        XPathConstants.NODE);
			}
            if (runConfigNode == null) {
				throw new ConfigurationException(
                        "Cannot find <fa:runConfig> element.");
			}
            definingClassName = xp.evaluate("@definition", runConfigNode);
            
            //if the defining class is null, the benchmark definition needs 
            //to be created based on the information in the driverConfig node.
            if(definingClassName==null || "".equals(definingClassName.trim())){
                definingClassName = createDefinition(runConfigNode);
            }
        }
        
        private String getRequestLagTime(Object node) throws Exception {
            
            Element rltNode = (Element) xp.evaluate("fd:requestLagTime/*[1]",
                    node, XPathConstants.NODE);
            
            if(rltNode == null){
                return "";
            }
            String requestLagType = rltNode.getLocalName();
            String namespace = rltNode.getNamespaceURI();
            if (!DRIVERURI.equals(namespace)) {
				throw new ConfigurationException("Namespace " + namespace +
                            " unexpected under element fd:requestLagTime");
			}

            StringBuilder sb = new StringBuilder();
            
            if ("FixedTime".equalsIgnoreCase(requestLagType)) {
                sb.append("@FixedTime(");
                sb.append("cycleType=CycleType.").append(
                        xp.evaluate("fd:cycleType",
                        rltNode).toUpperCase()).append(",");
                sb.append("cycleTime=").append(xp.evaluate("fd:cycleTime",
                        rltNode)).append(",");
                sb.append("cycleDeviation=").append(
                        xp.evaluate("fd:cycleDeviation",rltNode));
                sb.append(")");
                
            } else if ("Uniform".equalsIgnoreCase(requestLagType)) {
                sb.append("@Uniform(");
                sb.append("cycleType=CycleType.").append(
                        xp.evaluate("fd:cycleType",
                        rltNode).toUpperCase()).append(",");
                sb.append("cycleMin=").append(xp.evaluate("fd:cycleMin",
                        rltNode)).append(",");
                sb.append("cycleMax=").append(xp.evaluate("fd:cycleMax",
                        rltNode)).append(",");
                sb.append("cycleDeviation=").append(
                        xp.evaluate("fd:cycleDeviation", rltNode));
                sb.append(")");
                
            } else if ("NegativeExponential".equalsIgnoreCase(requestLagType)) {
                sb.append("@NegativeExponential(");
                sb.append("cycleType=CycleType.").append(
                        xp.evaluate("fd:cycleType",
                        rltNode).toUpperCase()).append(",");
                sb.append("cycleMean=").append(
                        xp.evaluate("fd:cycleMean",rltNode)).append(",");
                sb.append("cycleMax=").append(
                        xp.evaluate("fd:cycleMax",rltNode)).append(",");
                sb.append("cycleDeviation=").append(
                        xp.evaluate("fd:cycleDeviation",rltNode));
                sb.append(")");
                
            }
            //At this point we have the requestLagTime annotation.
            return sb.toString(); 
           
        }

        private static enum TransportProvider {

            // We can add more providers here.
            SUN ("com.sun.faban.driver.transport.sunhttp.SunHttpTransport"),

            APACHE3 ("com.sun.faban.driver.transport.hc3.ApacheHC3Transport");


            final String providerClass;

            TransportProvider(String providerClass) {
                this.providerClass = providerClass;
            }

            static TransportProvider getProvider() {
                String providerStr = System.getProperty(
                        "fhb.http.provider", "apache3").toUpperCase();
                for (TransportProvider provider : TransportProvider.values()) {
                    if (provider.name().equals(providerStr))
                        return provider;
                }
                // Default is APACHE3
                return APACHE3;
            }
        }

        // Used to create the operation-specific information for various
        // types of requests
        private static abstract class RunInfoDefinition {
            boolean doSubst;
            boolean isBinary;
            String url;
            String data;
            String kbps = "-1";

            abstract String getURL(int opNum);

            String doTiming(int opNum, TransportProvider provider) {
                if (provider == TransportProvider.SUN &&
                        url.startsWith("https"))
                    return "ctx.recordTime();";
                return "";
            }

            String getKbps(int opNum) {
                if (kbps == null)
                    return "";
                if (Integer.parseInt(kbps) < 0)
                    return "";
                return "httpTransport.setDownloadSpeed(" + kbps + ");\n";
            }
            
            abstract String getPostRequest(int opNum) throws Exception;
            
            abstract String getStatics(int opNum) throws Exception;

            public void init(boolean doSubst, boolean isBinary,
                             String url, String data, String kbps) {
                this.doSubst = doSubst;
                this.isBinary = isBinary;
                this.url = url;
                this.data = data;
 				if (kbps != null)
 					this.kbps = kbps;
            }

            @SuppressWarnings("cast")
			protected String makeBinaryStaticBlock(byte[] b, int opNum) {
                StringBuilder sb = new StringBuilder();
                sb.append("private static byte[] post_data_");
                sb.append(opNum);
                sb.append(" = { ");
                for (int bi = 0; bi < b.length; bi++) {
                    sb.append((int) b[bi]);
                    if (bi != b.length - 1) {
						sb.append(", ");
					}
                }
                sb.append(" }; ");
                return sb.toString();
            }

            protected String makeCharStaticBlock(char[] c, int opNum) {
                StringBuilder sb = new StringBuilder();
                sb.append("private static char[] post_data_");
                sb.append(opNum);
                sb.append(" = { ");
                for (int ci = 0; ci < c.length; ci++) {
                    sb.append((int) c[ci]);
                    if (ci != c.length - 1) {
						sb.append(", ");
					}
                }
                sb.append(" };\n");
                sb.append("private static String post_string_");
                sb.append(opNum);
                sb.append(" = doEncode(new String(post_data_");
                sb.append(opNum);
                sb.append("));\n");
                return sb.toString();
            }

            protected String generateRandomData(String inputString) {
                StringBuilder output = new StringBuilder();
                String[] strArr = inputString.split("@@");
                for (int i = 0; i < strArr.length; i++) {
                    if (strArr[i].startsWith("faban.getRandom")) {
                        String tmp[] = strArr[i].split(",");
                        String low = tmp[0].split("\\(")[1];
                        String high = tmp[1].split("\\)")[0];

                        if (strArr[i].startsWith("faban.getRandomString")) {
                            strArr[i] = new StringBuilder("random.makeAString("
                            ).append(low).append(",").append(high).append(
                                    ")").toString();
                        } else if(strArr[i].startsWith("faban.getRandomInt")) {
                            strArr[i] = new StringBuilder("random.random(").append(
                                    low).append(",").append(high).append(")"
                                    ).toString();
                        }
                        output.append(".append(").append(strArr[i]).append(")");
                    } else {
                        output.append(".append(\"").
                                append(strArr[i]).append("\")");
                    }
                }
                return output.toString();
            }
        }

        private static class RunInfoPostFileDefinition
                extends RunInfoDefinition {

			String getStatics(int opNum) throws Exception {
                if (isBinary) {
                    FileInputStream fis = new FileInputStream(data);
                    byte[] b = new byte[fis.available()];
                    if (fis.read(b) != b.length) {
						throw new IOException("Short read of data file " + data);
					}
                    fis.close();
                    return makeBinaryStaticBlock(b, opNum);
                } else if (!doSubst) {
                    FileReader fr = new FileReader(data);
                    char[] c = new char[(int) new File(data).length()];
                    if (fr.read(c) != c.length) {
						throw new IOException("Short read of data file " + data);
					}
                    fr.close();
                    return makeCharStaticBlock(c, opNum);
                } else {
					return "";
				}
            }

			String getURL(int opNum) {
                return new StringBuilder("\"").append(url).
                        append("\"").toString();
            }

			String getPostRequest(int opNum) throws Exception {
                if (isBinary) {
                    return new StringBuilder(", post_data_").append(opNum).
                                                                    toString();
                }
                if (!doSubst) {
                    return new StringBuilder(", post_string_").append(opNum).
                                                                    toString();
                }
                BufferedReader br = new BufferedReader(new FileReader(data));
                String s = br.readLine();
                br.close();
                String requestString = generateRandomData(s);
                StringBuilder sb = new StringBuilder(
                                                ", new StringBuilder(\"\")");
                sb.append(requestString);
                sb.append(".toString()");
                return sb.toString();
            }
        }

        private static class RunInfoPostDataDefinition
                extends RunInfoPostFileDefinition {
            @Override
			String getStatics(int opNum) {
                if (isBinary) {
					return makeBinaryStaticBlock(data.getBytes(), opNum);
				} else if (!doSubst) {
					return makeCharStaticBlock(data.toCharArray(), opNum);
				} else {
					return "";
				}
            }

            @Override
			String getPostRequest(int opNum) throws Exception {
                if (isBinary || !doSubst) {
					return super.getPostRequest(opNum);
				}
                String requestString = generateRandomData(data);
                StringBuilder sb = new StringBuilder(
                                                ", new StringBuilder(\"\")");
                sb.append(requestString);
                sb.append(".toString()");
                return sb.toString();
            }
        }

        private static class RunInfoGetDefinition extends RunInfoDefinition {

			String getStatics(int opNum) {
                if (doSubst) {
					return "";
				}
                StringBuilder sb = new StringBuilder(
                        "private static String get_string_");
                sb.append(opNum);
                sb.append(" = \"");
                sb.append(url);
                sb.append(data);
                sb.append("\"");
                sb.append(";");
                return sb.toString();
            }

			String getURL(int opNum) {
                if (doSubst) {
                    String requestString = generateRandomData(data);
                    return new StringBuilder(
                            "new StringBuilder(\"").append(url).append("\")").
                            append(requestString).append(".toString()").toString();
                }  
                return new StringBuilder("get_string_").append(opNum).toString();
            }

			String getPostRequest(int opNum) {
                return "";
            }
        }

        private String createDefinition(Object runConfigNode) throws Exception {
            Element benchDefNode = (Element) xp.evaluate(
                                    "fd:benchmarkDefinition", runConfigNode,
                                    XPathConstants.NODE);

            if (benchDefNode == null) {
				return null;
			}

            String definingClassName= xp.evaluate("fd:driverConfig/@name",
                    runConfigNode);

            //Get the cycleTime annotation
            Element driverConfigNode = (Element)xp.evaluate("fd:driverConfig",
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

            // Obtain the provider.
            TransportProvider provider = TransportProvider.getProvider();

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
                    ("fd:driverConfig/fd:operation["+ i +"]"),
                    runConfigNode, XPathConstants.NODE)) != null){

                /*
                * There are many different options here. First, the operation
                * is either POST or GET. In either case, the subst element
                * indicates whether or not random strings are present in the
                * payload data (for backward compatibility, it is assumed
                * that random data is present).
                *
                * For POST, we can read the data from a file, and we can
                * encode the data as form-encoded or binary (octet-stream)
                */
                boolean isPost = false;
                boolean isBinary = false;
                boolean isFile = false;
                boolean doSubst = true;

                String requestLagTimeOverride = getRequestLagTime(operationNode);
                String operationName = xp.evaluate("fd:name", operationNode);
                String url = xp.evaluate("fd:url", operationNode);
                String max90th = xp.evaluate("fd:max90th", operationNode);
				String kbps = xp.evaluate("fd:kbps", operationNode);

                String requestString="";

                Element requestNode = (Element)xp.evaluate("fd:get",
                        operationNode, XPathConstants.NODE);

                if (requestNode == null) {
                    //Can't have both post & get either, but if both are there,
                    // will assume you meant GET.

                    requestNode = (Element) xp.evaluate("fd:post",
                            operationNode, XPathConstants.NODE);

                    if (requestNode != null) {
                        isPost = true;
                        if ("true".equalsIgnoreCase(
                                requestNode.getAttributeNS(null, "file"))) {
							isFile = true;
						}
                        if ("true".equalsIgnoreCase(
                                requestNode.getAttributeNS(null, "binary"))) {
							isBinary = true;
						}
                    } else {
                        throw new ConfigurationException("<operation> " +
                                            "must have a either a get/post");
                    }
                    //Can't have both post & get either, but if there,
                    // will assume you meant GET.
                }
                if ("false".equalsIgnoreCase(
                                    requestNode.getAttributeNS(null, "subst"))) {
					doSubst = false;
				}
                if (isBinary && doSubst) {
					throw new ConfigurationException("<operation> " +
                            "cannot be both binary and perform substitution");
				}
                requestString = requestNode.getNodeValue();
                if (requestString == null) {
                    CDATASection cDataNode =
                            (CDATASection)requestNode.getFirstChild();
                    if(cDataNode!=null){
                        requestString = cDataNode.getNodeValue();
                    }
                }
                if (requestString == null) {
					requestString = "";
				}

                if(requestLagTimeOverride==null) {
					requestLagTimeOverride="";
				}
                if(operationName==null) {
					throw new ConfigurationException(
                            "<operation> must have a <name> ");
				}
                if(url==null) {
					throw new ConfigurationException(
                            "<operation> must have a <url>");
				}


                RunInfoDefinition rid;
                if (isPost) {
                    if (isFile) {
                        rid = new RunInfoPostFileDefinition();
                    } else {
						rid = new RunInfoPostDataDefinition();
					}
                } else {
					rid = new RunInfoGetDefinition();
				}
                rid.init(doSubst, isBinary, url, requestString, kbps);

                //Create the benchmark Operation annotation
                StringBuilder bmop = new StringBuilder(
                        "@BenchmarkOperation(name = \"").append(operationName);
                bmop.append("\", max90th=").append(max90th).append(
                        ", ");
				if (provider == TransportProvider.SUN &&
                        url.startsWith("https"))
					bmop.append("timing = com.sun.faban.driver.Timing.MANUAL");
				else
                    bmop.append("timing = com.sun.faban.driver.Timing.AUTO");
                bmop.append(")");

                String opTemplateClone =  new String(opTemplate);
                //replace tokens with actual content
                opTemplateClone = opTemplateClone.replaceAll(
                        "@RequestLagTime@", requestLagTimeOverride);
                opTemplateClone = opTemplateClone.replaceAll(
                        "@Operations@", bmop.toString());
                opTemplateClone = opTemplateClone.replaceAll(
                        "@operationName@", operationName);
                opTemplateClone = opTemplateClone.replaceAll(
                        "@url@", rid.getURL(i));
                opTemplateClone = opTemplateClone.replaceAll(
                        "@postRequest@", rid.getPostRequest(i));
                opTemplateClone = opTemplateClone.replaceAll(
                        "@Statics@", rid.getStatics(i));
				opTemplateClone = opTemplateClone.replaceAll(
						"@doKbps@", rid.getKbps(i));
				opTemplateClone = opTemplateClone.replaceAll(
 						"@doTiming@", rid.doTiming(i, provider));
                
                operations.append(opTemplateClone);
                
                i++;
            }
            
            String benchmarkDef = getBenchmarkDefinition(benchDefNode);
            
            //replace tokens in template
            template = template.replaceFirst("#operation(.*\\n*)*operation#",
                    operations.toString());
            template = template.replaceFirst("@RequestLagTime@",
                    requestLagTime);
            template = template.replaceFirst("@BenchmarkDefinition@",
                    benchmarkDef);
            template = template.replaceAll("@DriverClassName@",
                    definingClassName);
            template = template.replaceFirst("@ProviderClass@",
                                             provider.providerClass);
            template = template.replaceFirst("@BenchmarkDriverName@",
                    definingClassName);
            
            String tmpDir = System.getProperty("faban.tmpdir");
            
            if(tmpDir==null){
                tmpDir = System.getProperty("java.io.tmpdir");
            }
            
            if(!tmpDir.endsWith(File.separator)){
                tmpDir = tmpDir+File.separator;
            }
                
            String className = new StringBuilder(tmpDir).
                    append(definingClassName).append(".java").toString();
            
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
            int errorCode = com.sun.tools.javac.Main.compile(arg);
            
            if(errorCode != 0){
                throw new ConfigurationException(
                        "unable to compile generated driver file. " +
                        "check output for errors"); 
            }

            
            return definingClassName;
            
        }

        private String getBenchmarkDefinition(Object benchDefNode)
                throws Exception{
            //to do error checking -- name is required?
            String defName      = xp.evaluate("fd:name", benchDefNode);
            String version      = xp.evaluate("fd:version", benchDefNode);
            String metric       = xp.evaluate("fd:metric", benchDefNode);
            String scaleName    = xp.evaluate("fd:scaleName", benchDefNode);
            String scaleUnit    = xp.evaluate("fd:scaleUnit", benchDefNode);
            
            
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
            String v = xp.evaluate("fa:scale", runConfigNode);
            if (v != null && v.length() > 0) {
				try {
                    runInfo.scale = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<scale> must be an integer.");
                }
			}

            v = xp.evaluate("fa:runControl/fa:rampUp", runConfigNode);
            if (v == null || v.length() == 0) {
				throw new ConfigurationException(
                        "Element <rampUp> not found.");
			}
            try {
                runInfo.rampUp = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        "<rampUp> must be an integer.");
            }

            v = xp.evaluate("fa:runControl/fa:steadyState", runConfigNode);
            if (v == null || v.length() == 0) {
				throw new ConfigurationException(
                        "Element <steadyState> not found.");
			}
            try {
                runInfo.stdyState = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        "<steadyState> must be an integer.");
            }

            v = xp.evaluate("fa:runControl/fa:rampDown", runConfigNode);
            if (v == null || v.length() == 0) {
				throw new ConfigurationException(
                        "Element <rampDown> not found.");
			}
            try {
                runInfo.rampDown = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(
                        "<rampDown> must be an integer.");
            }

            v = xp.evaluate("fa:runControl/fa:variableLoad", runConfigNode);
            if (v != null && v.length() > 0) {
              try {
                runInfo.variableLoad = relaxedParseBoolean(v);
              } catch (NumberFormatException e) {
                throw new ConfigurationException(
                    "<variableLoad> must be true or false.");
              }
            }

            if (runInfo.variableLoad) {
                runInfo.variableLoadFile = xp.evaluate(
                        "fa:runControl/fa:variableLoadFile", runConfigNode);
            }

            runInfo.resultsDir = xp.evaluate("fd:outputDir", runConfigNode);
            if (runInfo.resultsDir == null || runInfo.resultsDir.length() == 0) {
				throw new ConfigurationException(
                        "Element <outputDir> not found.");
			}

            v = xp.evaluate("fd:audit", runConfigNode);
            if (v != null && v.length() > 0) {
				try {
                    runInfo.audit = relaxedParseBoolean(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<audit> must be true or false.");
                }
			}

            v = xp.evaluate("fd:threadStart/fd:delay", runConfigNode);
            if (v != null && v.length() > 0) {
				try {
                    runInfo.msBetweenThreadStart = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<delay> must be an integer.");
                }
			}

            v = xp.evaluate("fd:threadStart/fd:simultaneous", runConfigNode);
            if (v != null && v.length() > 0) {
				try {
                    runInfo.simultaneousStart = relaxedParseBoolean(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<simultaneous> must be true or false.");
                }
			}

            v = xp.evaluate("fd:threadStart/fd:parallel", runConfigNode);
            if (v != null && v.length() > 0) {
				try {
                    runInfo.parallelAgentThreadStart = relaxedParseBoolean(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<parallel> must be true or false.");
                }
			}

            v = xp.evaluate("fd:stats/fd:maxRunTime", runConfigNode);
            if (v!= null && v.length() > 0) {
				try {
                    runInfo.maxRunTime = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<maxRunTime> must be an integer.");
                }
			}

            v = xp.evaluate("fd:stats/fd:interval", runConfigNode);
            if (v!= null && v.length() > 0) {
				try {
                    runInfo.graphInterval = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<interval> must be an integer.");
                }
			}

            v = xp.evaluate("fd:runtimeStats/@enabled", runConfigNode);
            if (v != null && v.length() > 0) {
				try {
                    runInfo.runtimeStatsEnabled = relaxedParseBoolean(v);
                } catch (Exception e) {
                    throw new ConfigurationException(
                            "<runtimeStats enabled=[true|false]>");
                }
			}

            v = xp.evaluate("fd:runtimeStats/fd:interval", runConfigNode);
            if (v != null && v.length() > 0) {
				try {
                    runInfo.runtimeStatsInterval = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(
                            "<interval> must be an integer.");
                }
			}

            runInfo.driverConfigs = new DriverConfig[benchDef.drivers.length];
            for (int i = 0; i < benchDef.drivers.length; i++) {
                DriverConfig driverConfig =
                        new DriverConfig(benchDef.drivers[i]);
                Element driverConfigNode = (Element) xp.evaluate(
                        "fd:driverConfig[@name=\"" + driverConfig.name + "\"][1]",
                        runConfigNode, XPathConstants.NODE);

                driverConfig.runControl = benchDef.runControl;
                if (driverConfigNode == null) {
					throw new ConfigurationException("Element " +
                            "<driverConfig name=\"" + driverConfig.name +
                            "\"> not found.");
				}

                v = xp.evaluate("fd:agents", driverConfigNode);

                // Note that the agents field has two valid formats:
                // 1. A single integer
                // 2. One or more host:count fields
                // The harness is interested in the host/count. What we need
                // is a simple count. Just add'em up.
                if (v != null && v.length() > 0) {
                    StringTokenizer t = new StringTokenizer(v, " ,");
                    driverConfig.numAgents = 0;
                    while (t.hasMoreTokens()) {
                        v = t.nextToken().trim();
                        if (v.length() == 0) {
							continue;
						}
                        int idx = v.indexOf(':');
                        if (++idx > 0) {
							v = v.substring(idx);
						}
                        if (v.length() == 0) {
							continue;
						}
                        try {
                            driverConfig.numAgents += Integer.parseInt(v);
                        } catch (NumberFormatException e) {

                            throw new ConfigurationException("<agents> " +
                                    "must be an integer or in the format " +
                                    "host:agents where agents is an integer. " +
                                    "Found: " + v);
                        }
                    }
                }

                v = xp.evaluate("fd:threads", driverConfigNode);
                if (v != null && v.length() > 0) {
					try {
                        driverConfig.numThreads = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        throw new ConfigurationException(
                                "<threads> must be an integer.");
                    }
				}

                v = xp.evaluate("fd:stats/fd:interval", driverConfigNode);
                if (v!= null && v.length() > 0) {
					try {
                        driverConfig.graphInterval = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        throw new ConfigurationException(
                                "<interval> must be an integer.");
                    }
				} else {
					driverConfig.graphInterval = runInfo.graphInterval;
				}

                if (runInfo.runtimeStatsEnabled) {
                    driverConfig.runtimeStatsTarget = xp.evaluate(
                            "fd:runtimeStats/@target", driverConfigNode);
                    if (driverConfig.runtimeStatsTarget != null) {
                        driverConfig.runtimeStatsTarget =
                                driverConfig.runtimeStatsTarget.trim();
                        if (driverConfig.runtimeStatsTarget.length() == 0)
                            driverConfig.runtimeStatsTarget = null;
                    }
                }

                if (runInfo.variableLoad) {
                    driverConfig.variableLoadFile = xp.evaluate(
                            "fd:variableLoadFile", driverConfigNode);
                    if (driverConfig.variableLoadFile == null ||
                        driverConfig.variableLoadFile.length() == 0) {
                        driverConfig.variableLoadFile = runInfo.variableLoadFile;
                    }
                    if (driverConfig.variableLoadFile == null ||
                         driverConfig.variableLoadFile.length() == 0) {
                         throw new ConfigurationException(
                                "Element <variableLoadFile> not found.");
                    }
                }


                driverConfig.rootElement = rootElement;
                driverConfig.properties = (Element) xp.evaluate("fd:properties",
                        driverConfigNode, XPathConstants.NODE);
                driverConfig.mix[0].configure(driverConfigNode);
                driverConfig.mix[0].normalize();
                runInfo.driverConfigs[i] = driverConfig;
                
                //need to store the class bytes for a remote agent
                //if the driver is an http driver
                
                InputStream is = null;
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
                        
                        loader.loadClass(defClassName);
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
                || "1".equals(newStr)) {
				retVal = true;
			} else if ("false".equals(newStr) || "f".equals(newStr)
                || "no".equals(newStr) || "n".equals(newStr)
                || "0".equals(newStr)) {
				retVal = false;
			} else {
				throw new NumberFormatException("Boolean " + str +
                        " not recognized!");
			}
            return retVal;
        }
    }
}
