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
 * $Id: Config.java,v 1.9 2006/08/12 06:54:23 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
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
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

public class Config {

    // This field is a legal requirement and serves no other purpose.
    static final String COPYRIGHT =
            "Copyright \251 2006 Sun Microsystems, Inc., 4150 Network Circle, " +
            "Santa Clara, California 95054, U.S.A. All rights reserved.\n" +
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
            "Copyright \251 2006 Sun Microsystems, Inc., 4150 Network Circle, " +
            "Santa Clara, California 95054, Etats-Unis. Tous droits " +
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

    public static final String HARNESS_NAME = "Faban";
    public static final String HARNESS_VERSION = "0.8";

    public static final String LOG_FILE = "log.xml";

    // The default logging port
    public static int LOGGING_PORT = 9999;

    // The default RMI port
    public static int RMI_PORT = 9998;

    public static final String PROFILE_SIGNAL = "PROF";

    // resultinfo contains a single line summary result
    public static final String RESULT_INFO = "resultinfo";

    // Config of packages
    public static final String ENGINE_PKG = "com.sun.faban.harness.engine.";
    public static final String TOOLS_PKG = "com.sun.faban.harness.tools.";
    public static final String BENCH_PKG = "com.sun.faban.harness.benchmarks.";

    // RMI related
    public static final String CMD_AGENT = "CmdAgent";
    public static final String FILE_AGENT = "FileAgent";
    public static final String TOOL_AGENT = "ToolAgent";

    // Resource downloads
    public static final String DOWNLOAD_PATH = "bench_downloads/";

	// Universal Faban constants
    public static final int DEFAULT_PRIORITY = 1;
    public static final int HIGHER_PRIORITY = 2;

    public static String TMP_DIR;
    // File related
    public static String FABAN_HOME; // This is where Faban is installed
    public static String FABAN_ROOT; // This is the context root for Faban
    public static String FABAN_URL; // The URL to to access Faban
    public static String CONFIG_DIR;
    public static String RUNQ_DIR;
    public static String OUT_DIR;
    public static String SEQUENCE_FILE;

    // Constants used by UserEnv
    public static String BENCHMARK_DIR;
    public static String BENCH_FILE;
    public static String PROFILES_DIR;

    // The URL to the context, initialized only on master process.
    public static String CONTEXT_URL;

    // space before and after string are required in next line
    // Constant used by engine.CmdService
    public static String BIN_DIR;
    public static String CMD_SCRIPT;
    public static String LIB_DIR;

    public static String CONFIG_FILE;

    public static String DEFAULT_LOG_FILE;


    // Configuration from the file
    public static boolean SECURITY_ENABLED = false;
    public static LoginConfiguration LOGIN_CONFIG = null;

    public static URL[] replicationURLs = null;


    static {
        deriveConfig();
        configLogger();
        readConfig();
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

            // Move back to the fourth File.separator from right.
            int idx = FABAN_ROOT.length() - 1;
            for (int i = 0; i < 4; i++) {
                idx = FABAN_ROOT.lastIndexOf(File.separator, idx);
                --idx;
            }

            // Then take the substring including the separator.
            FABAN_HOME = FABAN_ROOT.substring(0, idx + 2);


            // Only for the server, we need to set URL
            FABAN_URL = System.getProperty("faban.url");
        } else {
            String fabanHome = System.getProperty("faban.home", userHome +
                    "faban" + File.separator);
            // Make sure it ends with '/'
            if (fabanHome.endsWith(File.separator))
                FABAN_HOME = fabanHome;
            else
                FABAN_HOME = fabanHome + File.separator;

            CONFIG_DIR = FABAN_HOME + "config" + File.separator;
        }


        // space before and after string are required in next line
        // Constant used by engine.CmdService
        BIN_DIR = " " + FABAN_HOME + "bin" + File.separator;
        CMD_SCRIPT = BIN_DIR + "faban ";
        LIB_DIR = FABAN_HOME + "lib" + File.separator;
        OUT_DIR = FABAN_HOME + "output" + File.separator;
        CONFIG_DIR = FABAN_HOME + "config" + File.separator;
        RUNQ_DIR = CONFIG_DIR  + "runq" + File.separator;
        SEQUENCE_FILE = CONFIG_DIR  + "sequence";
        CONFIG_FILE = CONFIG_DIR + "harness.xml";

        // Constants used UserEnv
        BENCHMARK_DIR = FABAN_HOME + "benchmarks" + File.separator;
        BENCH_FILE = CONFIG_DIR + "benchmarks.list";
        PROFILES_DIR = CONFIG_DIR + "profiles" + File.separator;
    }

    private static void configLogger() {
        String path = FABAN_HOME + "logs";

        // Redirect log to faban.log.xml
        if(path == null)
            path = "%t";

        path = path + File.separator + "faban.log.xml";

        StringBuffer sb = new StringBuffer();
	    sb.append("\nhandlers = java.util.logging.FileHandler\n");
        sb.append("java.util.logging.FileHandler.pattern = " + path + "\n");
        sb.append("java.util.logging.FileHandler.append = true\n");
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
                }

                // Reading replication config
                NodeList servers = (NodeList) xPath.evaluate(
                        "replication/server[@enabled='true']", root,
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
                    replicationURLs = new URL[serverList.size()];
                    replicationURLs = serverList.toArray(replicationURLs);
                }

                // Note: The logServer config is read by LogConfig, not here.

                v = xPath.evaluate("rmiPort", root);
                if (v != null) {
                    v = v.trim();
                    if (v.length() > 0) {
                        RMI_PORT = Integer.parseInt(v);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Error reading Faban harness configuration.", e);
            }
        else
            logger.warning("Configuration file " + Config.CONFIG_FILE +
                    " does not exist. Using default settings.");
    }
}
