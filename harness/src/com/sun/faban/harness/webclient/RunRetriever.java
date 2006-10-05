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
 * $Id: RunRetriever.java,v 1.2 2006/10/05 16:17:19 akara Exp $
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
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;
import java.net.HttpURLConnection;

/**
 * This class represents both the servlet that allows fetching runs from
 * the run queue and the client side utility pollRun used to retrieve
 * remote runs.
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

        // Check that we are a pollee
        if (Config.daemonMode != Config.DaemonModes.POLLEE) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Check access rights
        String hostName = request.getParameter("host");
        String key = request.getParameter("key");

        if (hostName == null || key == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean authenticated = false;

        // We do not expect too many hosts polling, so we use sequential
        // search. If this turns out wrong, we can always go for alternatives.
        for (int i = 0; i < Config.pollHosts.length; i++) {
            if (hostName.equals(Config.pollHosts[i].name) &&
                    key.equals(Config.pollHosts[i].key)) {
                authenticated = true;
                break;
            }
        }

        if (!authenticated) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Is this an age probe?
        String v = request.getParameter("minage");
        if (v != null) {
            nextRunAge(Long.parseLong(v), response);
            return;
        }
        v = request.getParameter("runname");
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
                nextRun = RunQ.getHandle().fetchNextRun(runName);
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

    /**
     * Client side method to poll for the oldest run which must be older than
     * localAge. If found, the run will be downloaded into the temp space.
     * and the run name will be returned.
     * @param localAge The age of the oldest local run in the queue
     * @return The file reference to the local run in the directory
     */
    public static File pollRun(long localAge) throws IOException {
        Config.HostInfo selectedHost = null;
        NameValuePair<Long> selectedRun = null;
        for (int i = 0; i < Config.pollHosts.length; i++) {
            Config.HostInfo pollHost = Config.pollHosts[i];
            NameValuePair<Long> run = poll(pollHost, localAge);
            if (run != null &&
                    (selectedRun == null || run.value > selectedRun.value)) {
                selectedRun = run;
                selectedHost = pollHost;
            }
        }
        File tmpJar = null;
        if (selectedRun != null) {
            tmpJar = download(selectedHost, selectedRun);
        }
        return tmpJar;
    }

    private static NameValuePair<Long> poll(Config.HostInfo host, long minAge)
            throws IOException {

        HttpURLConnection c = (HttpURLConnection) host.url.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setDoInput(true);
        PrintWriter out = new PrintWriter(c.getOutputStream());
        out.write("host=" + host.name + "&key=" + host.key +
                  "&minage=" + minAge);
        out.flush();
        out.close();

        NameValuePair<Long> run = null;
        if (c.getResponseCode() == HttpServletResponse.SC_OK) {
            InputStream is = c.getInputStream();

            // The input is a one liner in the form runName\tAge
            byte[] buffer = new byte[256];
            int size = is.read(buffer);

            // We have to close the input stream in order to return it to
            // the cache, so we get it for all content, even if we don't
            // use it. It's (I believe) a bug that the content handlers used
            // by getContent() don't close the input stream, but the JDK team
            // has marked those bugs as "will not fix."
            is.close();

            StringTokenizer t = new StringTokenizer(
                                        new String(buffer, 0, size), "\t\n");
            run = new NameValuePair<Long>();
            run.name = t.nextToken();
            run.value = Long.parseLong(t.nextToken());
        }
        return run;
    }

    private static File download(Config.HostInfo host, NameValuePair<Long> run)
            throws IOException {

        HttpURLConnection c = (HttpURLConnection) host.url.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setDoInput(true);
        PrintWriter out = new PrintWriter(c.getOutputStream());
        out.write("host=" + host.name + "&key=" + host.key +
                  "&runname=" + run.name);
        out.flush();
        out.close();

        File jarFile = null;
        FileOutputStream jarOut = null;
        if (c.getResponseCode() == HttpServletResponse.SC_OK) {
            InputStream is = c.getInputStream();
            byte[] buffer = new byte[8192];
            int size;
            while ((size = is.read(buffer)) != -1) {
                if (size > 0 && jarFile == null) {
                    jarFile = new File(Config.TMP_DIR, "remote_run.jar");
                    jarOut = new FileOutputStream(jarFile);
                }
                jarOut.write(buffer, 0, size);
            }
            is.close();
            jarOut.flush();
            jarOut.close();
        }
        return jarFile;
    }
}
