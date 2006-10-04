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
 * $Id: RunRetriever.java,v 1.1 2006/10/04 23:55:07 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.common.Command;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.engine.RunEntryException;
import com.sun.faban.harness.engine.RunQ;
import com.sun.faban.harness.util.DeployUtil;
import com.sun.faban.harness.util.NameValuePair;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the servlet that allows retrieving runs from the run queue.
 *
 * @author Akara Sucharitakul
 */
public class RunRetriever extends HttpServlet {

    private static Logger logger = Logger.getLogger(
            RunRetriever.class.getName());
    private static String tmpDir = System.getProperty("java.io.tmpdir");

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // TODO: 1. Check access rights
        // TODO: 2. Check that local run queue is disabled
        String v = request.getParameter("minage");
        if (v != null) {
            nextRunAge(Long.parseLong(v), response);
            return;
        }
        v = request.getParameter("name");
        if (v != null) {
            fetchNextRun(v, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void nextRunAge(long minAge, HttpServletResponse response)
            throws IOException {
        NameValuePair<Long> runAge = RunQ.getHandle().nextRunAge(minAge);

        if (runAge == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        StringBuilder output = new StringBuilder(128);
        output.append(runAge.name).append('\t').append(runAge.value);

        response.setContentType("txt/plain");
        response.setStatus(HttpServletResponse.SC_OK);
        OutputStream out = response.getOutputStream();
        out.write(output.toString().getBytes());
        out.flush();
        out.close();
    }

    private void fetchNextRun(String runName, HttpServletResponse response)
            throws ServletException, IOException {

        Run nextRun = null;
        for (;;)
            try {
                nextRun = RunQ.getHandle().fetchNextRun();
                break;
            } catch (RunEntryException e) {
            }

        if (nextRun == null) { // Queue empty
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        // Jar up the run.
        File jarFile = null;
        try {
            jarFile = jar(nextRun);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Run preparation interrupted.", e);
            throw new ServletException(e);
        }

        // Send the run jar to the output stream
        long length = jarFile.length();
        int bufferSize = 1024 * 1024 * 128; // 128MB buffer limit
        if (length < bufferSize)
            bufferSize = (int) length;

        byte[] buffer = new byte[bufferSize];

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/java-archive");
        OutputStream out = response.getOutputStream();
        FileInputStream jarIn = new FileInputStream(jarFile);
        int readSize = 0;
        while ((readSize = jarIn.read(buffer)) != -1)
            out.write(buffer, 0, readSize);

        out.flush();
        out.close();

        // Update status locally
        nextRun.updateStatus("RECEIVED");

        // Clear tmp file
        jarFile.delete();
    }

    /**
     * Jars up the run directory.
     * @param run The run to jar up
     */
    public static File jar(Run run) throws IOException, InterruptedException {

        logger.info("Preparing run " + run.getRunName() + " for download.");

        String runName = run.getRunName();

        String jarName = runName + ".jar";
        File jar = new File(tmpDir, jarName);

        if (jar.exists())
            jar.delete();

        String jarCmd = DeployUtil.getJavaHome() + File.separator + "bin" +
                File.separator + "jar";
        Command cmd = new Command(jarCmd + " cf " + jar.getAbsolutePath() +
                ' ' + runName);
        cmd.setWorkingDirectory(Config.OUT_DIR);
        cmd.execute();
        return jar;
    }
}
