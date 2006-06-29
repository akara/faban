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
 * $Id: Config.java,v 1.2 2006/06/29 19:38:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.common;

import java.io.File;

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
            "Apache est une marque d’Apache Software Foundation, utilis\351e " +
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
    public static final String HARNESS_VERSION = "0.7";

    public static final String LOG_FILE = "log.xml";

    // This is not final as it may be changed if there is a conflict
    public static int LOGGING_PORT = 9999;

    // TODO : The client agents should be able to reconfigure this port
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
    public static String USERS_DIR;

    // The URL to the context, initialized only on master process.
    public static String CONTEXT_URL;

    // space before and after string are required in next line
    // Constant used by engine.CmdService
    public static String BIN_DIR;
    public static String CMD_SCRIPT;
    public static String LIB_DIR;


    static {
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

        // Constants used UserEnv
        BENCHMARK_DIR = FABAN_HOME + "benchmarks" + File.separator;
        BENCH_FILE = CONFIG_DIR + "benchmarks.list";
        USERS_DIR = CONFIG_DIR + "users" + File.separator;
    }
}
