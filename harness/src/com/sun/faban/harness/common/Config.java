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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.common;

import com.sun.faban.harness.engine.LoginConfiguration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Central configuration object for the Faban harness. Fields are initialized
 * as the harness is brought up and should never be changed.
 */
public class Config {

    // This field is a legal requirement and serves no other purpose.
    static final String COPYRIGHT =
            "Copyright \251 2006-2009 Sun Microsystems, Inc., 4150 Network " +
            "Circle, Santa Clara, California 95054, U.S.A. All rights" +
            "reserved.\n" +
            "U.S. Government Rights - Commercial software.  Government users " +
            "are subject to the Sun Microsystems, Inc. standard license " +
            "agreement and applicable provisions of the FAR and its " +
            "supplements.\n" +
            "Use is subject to license terms.\n" +
            "This distribution may include materials developed by third " +
            "parties.\n" +
            "Sun,  Sun Microsystems,  the Sun logo and  Java are trademarks " +
            "or registered trademarks of Sun Microsystems, Inc. in the U.S. " +
            "and other countries.\n" +
            "Apache is a trademark of The Apache Software Foundation, and is " +
            "used with permission.\n" +
            "This product is covered and controlled by U.S. Export Control " +
            "laws and may be subject to the export or import laws in other " +
            "countries.  Nuclear, missile, chemical biological weapons or " +
            "nuclear maritime end uses or end users, whether direct or " +
            "indirect, are strictly prohibited.  Export or reexport to " +
            "countries subject to U.S. embargo or to entities identified on " +
            "U.S. export exclusion lists, including, but not limited to, the " +
            "denied persons and specially designated nationals lists is " +
            "strictly prohibited.\n" +
            "\n" +
            "Copyright \251 2006-2009 Sun Microsystems, Inc., 4150 Network " +
            "Circle, Santa Clara, California 95054, Etats-Unis. Tous droits " +
            "r\351serv\351s.\n" +
            "L'utilisation est soumise aux termes de la Licence.\n" +
            "Cette distribution peut comprendre des composants " +
            "d\351velopp\351s par des tierces parties.\n" +
            "Sun,  Sun Microsystems,  le logo Sun et  Java sont des marques " +
            "de fabrique ou des marques d\351pos\351es de " +
            "Sun Microsystems, Inc. aux Etats-Unis et dans d'autres pays.\n" +
            "Apache est une marque d\264Apache Software Foundation, utilis\351e " +
            "avec leur permission.\n" +
            "Ce produit est soumis \340 la l\351gislation am\351ricaine " +
            "en mati\350re de contr\364le des exportations et peut \352tre " +
            "soumis \340 la r\350glementation en vigueur dans d'autres pays " +
            "dans le domaine des exportations et importations. Les " +
            "utilisations, ou utilisateurs finaux, pour des armes " +
            "nucl\351aires, des missiles, des armes biologiques et chimiques " +
            "ou du nucl\351aire maritime, directement ou indirectement, sont " +
            "strictement interdites. Les exportations ou r\351exportations " +
            "vers les pays sous embargo am\351ricain, ou vers des entit\351s " +
            "figurant sur les listes d'exclusion d'exportation " +
            "am\351ricaines, y compris, mais de mani\350re non exhaustive, " +
            "la liste de personnes qui font objet d'un ordre de ne pas " +
            "participer, d'une fa\347on directe ou indirecte, aux " +
            "exportations des produits ou des services qui sont r\351gis par " +
            "la l\351gislation am\351ricaine en mati\350re de contr\364le " +
            "des exportations et la liste de ressortissants sp\351cifiquement " +
            "d\351sign\351s, sont rigoureusement interdites.\n";

    /** The harness name. */
    public static final String HARNESS_NAME = "Faban";

    /** The harness version. */
    public static final String HARNESS_VERSION = "1.0.1";

    /** The log file name. */
    public static final String LOG_FILE = "log.xml";

    /** The logging port. */
    public static int LOGGING_PORT = 9999;

    /** The RMI registry port. */
    public static int RMI_PORT = 9998;

    /** The agent daemon port. */
    public static int AGENT_PORT = 9981;

    /** resultinfo contains a single line summary result. */
    public static final String RESULT_INFO = "resultinfo";

    // RMI related
    /** Command agent name in registry. */
    public static final String CMD_AGENT = "CmdAgent";

    /** File agent name in registry. */
    public static final String FILE_AGENT = "FileAgent";

    /** Tool agent name in registry. */
    public static final String TOOL_AGENT = "ToolAgent";

    // Resource downloads
    /** Context path for benchmark downloads. */
    public static final String BENCHMARK_DOWNLOAD_PATH = "bench_downloads/";

    /** Context path for service downloads. */
    public static final String SERVICE_DOWNLOAD_PATH = "service_downloads/";

	// Universal Faban constants
    /** The default priority. */
    public static final int DEFAULT_PRIORITY = 1;

    /** The higher priority. */
    public static final int HIGHER_PRIORITY = 2;

    /** Path under result to store postprocessing results. */
    public static final String POST_DIR = "post/";

    /** Directory name containing architecture-specific files. */
    public static String ARCH_DIR;

    /** Directory name containing OS-specific files. */
    public static String OS_DIR;

    /** Temporary directory name. */
    public static String TMP_DIR;

    // File related
    /** The current host name. */
    public static String FABAN_HOST;

    /** The faban installation directory. */
    public static String FABAN_HOME; // This is where Faban is installed

    /** The web root context to access Faban. */
    public static String FABAN_ROOT; // This is the context root for Faban

    /** The full URL used for accessing Faban from inside the rig. */
    public static String FABAN_URL; // The URL to access Faban

    /** The Faban configuration directory. */
    public static String CONFIG_DIR;

    /** The directory containing the run queue. */
    public static String RUNQ_DIR;

    /** The output directory. */
    public static String OUT_DIR;

    /** The directory containing analysis output. */
    public static String ANALYSIS_DIR;

    /** The name of the sequence file. */
    public static String SEQUENCE_FILE;

    // Constants used by UserEnv
    /** The directory containing the benchmark. */
    public static String BENCHMARK_DIR;

    /** The directory containing services. */
    public static String SERVICE_DIR;

    /** The directory containing user profiles. */
    public static String PROFILES_DIR;

    // space before and after string are required in next line
    // Constant used by engine.CmdService

    /** The directory containing the binary files. */
    public static String BIN_DIR;

    /** Directory containing jars and libraries. */
    public static String LIB_DIR;

    /** The Faban configuration file. */
    public static String CONFIG_FILE;

    /** The default log file. */
    public static String DEFAULT_LOG_FILE;

    /** The size of the log view, in entries. */
    public static int LOG_VIEW_BUFFER_SIZE = 20;

    /** Generic thread pool. */
    public static ExecutorService THREADPOOL;

    // Configuration from the file
    /** Whether or not security is enabled. */
    public static boolean SECURITY_ENABLED = false;

    /** The login prompt. */
    public static String loginPrompt;

    /** Tooltip help message for the login prompt. */
    public static String loginHint;

    /** The password prompt. */
    public static String passwordPrompt;

    /** Tooltip help message for the password prompt. */
    public static String passwordHint;

    /**
     * The login configuration. This includes information such as the provider.
     */
    public static LoginConfiguration LOGIN_CONFIG = null;

    /** The login principals. */
    public static Set<String> PRINCIPALS;

    /** User name used for deploying benchmarks and services. */
    public static String DEPLOY_USER;

    /** Password used for deploying benchmarks and services. */
    public static String DEPLOY_PASSWORD;

    /** Submitter used when submitting runs through the FabanCLI. */
    public static String CLI_SUBMITTER;

    /** Submitter password for the FabanCLI. */
    public static String SUBMIT_PASSWORD;

    /** The mode of the run daemon in this configuration. */
    public static DaemonModes daemonMode;

    /** Host names of the pollers. */
    public static HostInfo[] pollHosts;

    /** URLs for Faban repositories, if enabled. */
    public static URL[] repositoryURLs = null;
    
    /** Faban repositories, targeting feature disabled by default. */
    public static boolean targetting = false;

    static {
        deriveConfig();
        configLogger();
        readConfig();
    }

    /**
     * Enumeration for daemon modes.
     */
    public enum DaemonModes {
        /** Poller mode. Also makes runs. */
        POLLER,

        /** Pollee proxy. Does not make runs. */
        POLLEE,

        /** Standard Faban master. Make runs, but no polling. */
        LOCAL,

        /** Repository. Runs are disabled. */
        DISABLED
    }

    /**
     * Host information structure.
     */
    public static class HostInfo {

        /** Host name. */
        public String name;

        /** URL to access the host. */
        public URL url;

        /** The key used for accessing this host. */
        public String key;

        /** Proxy, if any, for accesing the host. */
        public String proxyHost;

        /** The proxy port. */
        public int proxyPort;
    }


    /**
     * Sets the derived configuration variables. These are derived from
     * system properties and previously set variables.
     */
    private static void deriveConfig() {
        String userHome  = System.getProperty("user.home");
        if(!userHome.endsWith(File.separator))
            userHome += File.separator;

        TMP_DIR = System.getProperty("java.io.tmpdir");
        if(!TMP_DIR.endsWith(File.separator))
            TMP_DIR += File.separator;

        FABAN_ROOT = System.getProperty("faban.root");
        if (FABAN_ROOT != null) { // If FABAN_ROOT != null, we're in the server
            String fabanHome = System.getProperty("faban.home");
            if (fabanHome != null) {
                File fabanHomePath = new File(fabanHome);
                if (!fabanHomePath.isAbsolute()) {
                    fabanHomePath = new File(FABAN_ROOT);
                    // Derive absolute path from relative faban.home.
                    StringTokenizer t = new StringTokenizer(fabanHome, "/");
                    while (t.hasMoreTokens()) {
                        String pathElement = t.nextToken();
                        if ("..".equals(pathElement))
                            fabanHomePath = fabanHomePath.getParentFile();
                        else
                            fabanHomePath = new File(fabanHomePath, pathElement);
                    }
                    fabanHome = fabanHomePath.getAbsolutePath();
                }
                if (fabanHome.endsWith(File.separator))
                    FABAN_HOME = fabanHome;
                else
                    FABAN_HOME = fabanHome + File.separator;
            } else {
                // Move back to the fourth File.separator from right.
                int idx = FABAN_ROOT.length() - 1;
                for (int i = 0; i < 4; i++) {
                    idx = FABAN_ROOT.lastIndexOf(File.separator, idx);
                    --idx;
                }

                // Then take the substring including the separator.
                FABAN_HOME = FABAN_ROOT.substring(0, idx + 2);
            }

            // Only for the server, we need to set URL
            FABAN_URL = System.getProperty("faban.url");
        } else {
            String fabanHome = System.getProperty("faban.home");
            if (fabanHome == null)
                fabanHome = System.getenv("FABAN_HOME");
            // Make sure it ends with '/'
            if (fabanHome.endsWith(File.separator))
                FABAN_HOME = fabanHome;
            else
                FABAN_HOME = fabanHome + File.separator;

            CONFIG_DIR = FABAN_HOME + "config" + File.separator;
        }

        try {
            FABAN_HOST = InetAddress.getLocalHost().getHostName();
            int dotIdx = FABAN_HOST.indexOf('.');
            if (dotIdx > 0) // Sometimes we get host.domain, we want only host.
                FABAN_HOST = FABAN_HOST.substring(0, dotIdx);
        } catch (UnknownHostException e) {
            FABAN_HOST = "";
        }

        // OS name is only the part before the space.
        String osName = System.getProperty("os.name");
        StringTokenizer st = new StringTokenizer(osName);
        OS_DIR = st.nextToken() + File.separator;

        ARCH_DIR = OS_DIR + System.getProperty("os.arch") + File.separator;

        // space before and after string are required in next line
        // Constant used by engine.CmdService
        BIN_DIR = " " + FABAN_HOME + "bin" + File.separator;
        LIB_DIR = FABAN_HOME + "lib" + File.separator;
        OUT_DIR = FABAN_HOME + "output" + File.separator;
        ANALYSIS_DIR = OUT_DIR + "analysis" + File.separator;
        CONFIG_DIR = FABAN_HOME + "config" + File.separator;
        RUNQ_DIR = CONFIG_DIR  + "runq" + File.separator;
        SEQUENCE_FILE = CONFIG_DIR  + "sequence";
        CONFIG_FILE = CONFIG_DIR + "harness.xml";

        // Constants used UserEnv
        BENCHMARK_DIR = FABAN_HOME + "benchmarks" + File.separator;
        SERVICE_DIR = FABAN_HOME + "services" + File.separator;
        PROFILES_DIR = CONFIG_DIR + "profiles" + File.separator;

        String[] emptyDirs = { BENCHMARK_DIR, SERVICE_DIR, OUT_DIR, RUNQ_DIR, PROFILES_DIR,
                              FABAN_HOME + "logs",
                              FABAN_HOME + "master" + File.separator + "logs" };
        ensureDirs(emptyDirs);
    }

    private static void ensureDirs(String[] dirNames) {
        File dir = null;
        for (String dirName : dirNames) {
            dir = new File(dirName);
            if (!dir.exists() && !dir.mkdirs())
                // We do not have a logger yet. Just dump it
                // to the Tomcat logs directly
                System.err.println("Cannot create directory " + dirName);
        }
    }

    private static void configLogger() {
        String path = FABAN_HOME + "logs" + File.separator + "faban.log.xml";
        
        // If it's windows, we need to make sure the format is
        // C:\\faban\\logs\\faban.log.xml
        path = path.replace("\\", "\\\\");

        StringBuffer sb = new StringBuffer();
        sb.append("\nhandlers = java.util.logging.FileHandler\n");
        sb.append("java.util.logging.FileHandler.pattern = ").append(path);
        sb.append("\njava.util.logging.FileHandler.append = true\n");
        sb.append("java.util.logging.FileHandler.limit = 102400\n");
        sb.append("java.util.logging.FileHandler.formatter = " +
                "com.sun.faban.harness.logging.XMLFormatter\n");

        DEFAULT_LOG_FILE = path;

        try {
            LogManager.getLogManager().readConfiguration(
                    new ByteArrayInputStream(sb.toString().getBytes()));
            
        } catch(IOException e) {
            Logger logger = Logger.getLogger(Config.class.getName());
            logger.log(Level.SEVERE, "Error configuring log manager.", e);
        }

    }

    /**
     * Reads the Faban harness configuration file FABAN_HOME/config/harness.xml
     * at server startup.
     */
    private static void readConfig() {
        Logger logger = Logger.getLogger(Config.class.getName());
        File harnessXml = new File(CONFIG_FILE);
        if (harnessXml.exists())
            try {
                logger.fine("Reading configuration file.");
                DocumentBuilder parser = DocumentBuilderFactory.newInstance().
                                            newDocumentBuilder();
                XPath xPath = XPathFactory.newInstance().newXPath();

                Node root = parser.parse(harnessXml).getDocumentElement();

                // Reading security config
                String v = xPath.evaluate("security/@enabled", root);
                logger.fine("Security enabled: " + v);
                if ("true".equals(v)) {
                    SECURITY_ENABLED = true;
                    LOGIN_CONFIG = new LoginConfiguration();
                    LOGIN_CONFIG.readConfig(root, xPath);

                    loginPrompt = xPath.evaluate("security/loginPrompt", root);
                    loginHint = xPath.evaluate("security/loginHint", root);
                    passwordPrompt = xPath.evaluate("security/passwordPrompt",
                                                    root);
                    passwordHint = xPath.evaluate("security/passwordHint",
                                                  root);

                    // Obtain PRINCIPALS with rig-wide manage rights.
                    NodeList managePrincipals = (NodeList) xPath.evaluate(
                            "security/managePrincipals/name", root,
                            XPathConstants.NODESET);
                    int principalCount;
                    if (managePrincipals != null && (principalCount =
                            managePrincipals.getLength()) > 0) {
                        PRINCIPALS = new HashSet<String>(principalCount);
                        for (int i = 0; i < principalCount; i++) {
                            Node nameNode = managePrincipals.item(i).
                                            getFirstChild();
                            if (nameNode != null) {
                                String name = nameNode.getNodeValue();
                                if (name.length() > 0)
                                    PRINCIPALS.add(name.trim().toLowerCase());
                            }
                        }
                    }
                    DEPLOY_USER = xPath.evaluate("security/deployUser", root);
                    DEPLOY_PASSWORD = xPath.evaluate("security/deployPassword",
                                                     root);
                    CLI_SUBMITTER = xPath.evaluate("security/cliSubmitter",
                                                   root);
                    SUBMIT_PASSWORD = xPath.evaluate("security/submitPassword",
                                                     root);
                }

                v = xPath.evaluate("runDaemon/@mode", root);
                if ("poller".equalsIgnoreCase(v))
                    daemonMode = DaemonModes.POLLER;
                else if ("pollee".equalsIgnoreCase(v))
                    daemonMode = DaemonModes.POLLEE;
                else if ("disabled".equalsIgnoreCase(v))
                    daemonMode = DaemonModes.DISABLED;
                else // default is local
                    daemonMode = DaemonModes.LOCAL;

                if (daemonMode == DaemonModes.POLLER ||
                        daemonMode == DaemonModes.POLLEE ) {
                    NodeList hosts = (NodeList) xPath.evaluate(
                            "runDaemon/host[@enabled='true']", root,
                            XPathConstants.NODESET);
                    int hostCount = hosts.getLength();
                    if (hostCount < 1) {
                        if (daemonMode == DaemonModes.POLLER)
                            daemonMode = DaemonModes.LOCAL;
                        else
                            daemonMode = DaemonModes.DISABLED;
                    }
                    pollHosts = new HostInfo[hostCount];
                    for (int i = 0; i < hostCount; i++) {
                        Node hostNode = hosts.item(i);
                        pollHosts[i] = new HostInfo();
                        pollHosts[i].name = xPath.evaluate("name", hostNode);
                        v = xPath.evaluate("url", hostNode);
                        if (v != null && v.length() > 0)
                            pollHosts[i].url = new URL(v);
                        pollHosts[i].key = xPath.evaluate("key", hostNode);
                        pollHosts[i].proxyHost =
                                xPath.evaluate("proxyHost",hostNode);
                        if (pollHosts[i].proxyHost != null &&
                                pollHosts[i].proxyHost.length() == 0)
                            pollHosts[i].proxyHost = null;
                        v = xPath.evaluate("proxyPort", hostNode);
                        if (v != null && v.length() > 0)
                            pollHosts[i].proxyPort = Integer.parseInt(v);
                        else
                            pollHosts[i].proxyHost = null;
                    }
                }

                // Reading repository config
                NodeList servers = (NodeList) xPath.evaluate(
                        "repository/server[@enabled='true']", root,
                        XPathConstants.NODESET);

                int serverCount;

                if (servers != null && (serverCount = servers.getLength()) > 0) {
                    ArrayList<URL> serverList = new ArrayList<URL>(serverCount);
                    for (int i = 0; i < serverCount; i++) {
                        // Fetch the text node underneath.
                        Node urlNode = servers.item(i).getFirstChild();
                        if (urlNode != null) {
                            // Then get the value.
                            String serverURL = urlNode.getNodeValue();
                            logger.fine("Upload to:" + serverURL);
                            try {
                                serverList.add(new URL(serverURL));
                            } catch (MalformedURLException e) {
                                logger.log(Level.WARNING, "Invalid URL " +
                                        serverURL, e);
                            }
                        }
                    }
                    repositoryURLs = new URL[serverList.size()];
                    repositoryURLs = serverList.toArray(repositoryURLs);
                }

                // Reading targetting config
                Node t = (Node) xPath.evaluate(
                        "repository/targetting[@enabled='true']", root,
                        XPathConstants.NODE);
                if (t != null) {
                   targetting = true;
                }

                // Note: The logServer config is read by LogConfig, not here.
                
                v = xPath.evaluate("logView/bufferSize", root).trim();
                if (v != null && v.length() > 0) {
                    LOG_VIEW_BUFFER_SIZE = Integer.parseInt(v.trim());
                }

                v = xPath.evaluate("rmiPort", root);
                if (v != null) {
                    v = v.trim();
                    if (v.length() > 0) {
                        RMI_PORT = Integer.parseInt(v);
                    }
                }
                v = xPath.evaluate("agentPort", root);
                if (v != null) {
                    v = v.trim();
                    if (v.length() > 0) {
                        AGENT_PORT = Integer.parseInt(v);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Error reading Faban harness configuration.", e);
            }
        else
            logger.fine("Configuration file " + Config.CONFIG_FILE +
                    " does not exist. Probably on client. " +
                    "Using default settings.");
    }
}
