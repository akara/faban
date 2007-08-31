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
 * $Id: FabanShell.java,v 1.1 2007/08/31 22:18:59 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A platform-independent command execution tool that parses the command
 * line and assignes all the parameters correctly. This is needed as the
 * capacity of scripting interpreters on different operating systems may
 * be limited.
 */
public class FabanShell extends Thread {

    public static String JAVA_HOME = System.getenv("JAVA_HOME");
    public static final String FABAN_HOME = System.getenv("FABAN_HOME");

    private InputStream in;
    private OutputStream out;

    /**
     * The FabanShell instances handle the streaming.
     * @param in The input stream
     * @param out The output stream
     */
    private FabanShell(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        start();
    }

    public void run() {
        byte[] buffer = new byte[8192];
        try {
            for (;;) {
                int length = in.read(buffer);
                if (length == -1)
                    break;
                if (length > 0)
                    out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {

        // Check the environment settings.
        boolean javaHomeEnv = true;
        if (JAVA_HOME == null || JAVA_HOME.length() == 0) {
            JAVA_HOME = Utilities.getJavaHome();
            javaHomeEnv = false;
        }

        if (FABAN_HOME == null || FABAN_HOME.length() == 0) {
            System.err.println("FABAN_HOME not specified.");
            System.exit(1);
        }

        StringBuilder classPath = new StringBuilder();

        // Check whether we have a JDK, if necessary.
        String javaLibs = JAVA_HOME + File.separator + "lib";
        String needJDK = System.getProperty("fabanshell.needJDK");
        if ("true".equalsIgnoreCase(needJDK)) {
            File toolsJar = new File(javaLibs, "tools.jar");
            if (!toolsJar.isFile()) {
                System.err.println("Could not find a JDK at " + JAVA_HOME +
                        ". Please make sure the JDK is installed and set " +
                        "JAVA_HOME or PATH accordingly.");
                System.exit(1);
            }
            classPath.append(toolsJar.getAbsolutePath());
        }

        if (!javaHomeEnv)
            System.err.println("JAVA_HOME not set. Using " + JAVA_HOME + ".");

        String java = JAVA_HOME + File.separator + "bin" +
                                                    File.separator + "java";
        String fabanLibs = FABAN_HOME + File.separator + "lib" + File.separator;
        File fabanLibDir = new File(fabanLibs);
        String[] jars = fabanLibDir.list();
        for (String jar : jars) {
            if (classPath.length() > 0)
                classPath.append(File.pathSeparator);
            classPath.append(fabanLibs).append(jar);
        }

        StringBuilder jvmArgs = new StringBuilder();
        StringBuilder toolArgs = new StringBuilder();

        boolean jArgsFollow = false;
        for (String arg : args) {
            if (arg.startsWith("-J")) {
                String argVal = arg.substring(2);
                if ("".equals(argVal))
                    jArgsFollow = true;
                else
                    jvmArgs.append(' ').append(argVal);
            } else if (jArgsFollow) {
                jvmArgs.append(' ').append(arg);
                jArgsFollow = false;
            } else {
                toolArgs.append(' ').append(arg);
            }
        }

        String cmdLine = System.getProperty("faban.cli.command");
        String execClass = System.getProperty("fabanshell.exec");

        StringBuilder javaCmd = new StringBuilder();
        javaCmd.append(java).append(jvmArgs).append(" -cp ").append(classPath).
                append(" -Dfaban.cli.command=").append(cmdLine).append(' ').
                append(execClass).append(toolArgs);

        try {
            Process p = Runtime.getRuntime().exec(javaCmd.toString());
            new FabanShell(p.getInputStream(), System.out);
            new FabanShell(p.getErrorStream(), System.err);
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}