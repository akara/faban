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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.common.NameValuePair;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.engine.RunEntryException;
import com.sun.faban.harness.engine.RunQ;
import com.sun.faban.harness.util.FileHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents both the servlet that allows fetching runs from
 * the run queue and the client side utility pollRun used to retrieve
 * remote runs.
 *
 * @author Akara Sucharitakul
 */
public class RunRetriever extends HttpServlet {

    static final String SERVLET_PATH = "pollrun";

    private static Logger logger = Logger.getLogger(
            RunRetriever.class.getName());

    /**
     * Post method to retrieve a run for a remote queue. Used only by pollees.
     * @param request The servlet request
     * @param response The servlet response
     * @throws ServletException If there is an error in the servlet
     * @throws IOException If the servlet has an I/O error
     */
    public void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // Check that we are a pollee
        if (Config.daemonMode != Config.DaemonModes.POLLEE) {
            logger.warning("Being polled for runs, not pollee!");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Check access rights
        String hostName = request.getParameter("host");
        String key = request.getParameter("key");

        if (hostName == null || key == null) {
            logger.warning("Being polled for runs, no hostname or key!");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!authenticate(hostName, key)) {
            logger.warning("Polling authentication from host " + hostName +
                    " denied!");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Is this an age probe?
        String v = request.getParameter("minage");
        if (v != null) {
            nextRunAge(Long.parseLong(v), response);
            return;
        }
        v = request.getParameter("runid");
        if (v != null) {
            try {
                fetchNextRun(v, response);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(RunRetriever.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /**
     * Authenticates the host and key against the stored host/key pair.
     * @param host The name of the communicating host
     * @param key The host's key
     * @return True if the authentication succeeds, false otherwise
     */
    static boolean authenticate(String host, String key) {
        boolean authenticated = false;

        // We do not expect too many hosts polling, so we use sequential
        // search. If this turns out wrong, we can always go for alternatives.
        for (int i = 0; i < Config.pollHosts.length; i++)
            if (host.equals(Config.pollHosts[i].name) &&
                    key.equals(Config.pollHosts[i].key)) {
                authenticated = true;
                break;
            }
        return authenticated;
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

    private void fetchNextRun(String runId, HttpServletResponse response)
            throws IOException, ClassNotFoundException {

        Run nextRun = null;
        for (;;)
            try {
                nextRun = RunQ.getHandle().fetchNextRun(runId);
                break;
            } catch (RunEntryException e) {
            }

        if (nextRun == null) { // Queue empty
            logger.warning("Fetching run " + runId + ": No longer available!");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        // Jar up the run.
        File jarFile = null;
        jarFile = jar(nextRun);

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
        nextRun.updateStatus(Run.RECEIVED);

        // Clear tmp file
        jarFile.delete();
    }

    /**
     * Jars up the run directory.
     * @param run The run to jar up
     * @return The resulting jar file
     * @throws IOException Error creating the jar
     */
    public static File jar(Run run) throws IOException {

        logger.info("Preparing run " + run.getRunId() + " for download.");

        String runId = run.getRunId();

        String jarName = runId + ".jar";
        File jar = new File(Config.TMP_DIR, jarName);

        String[] files = new File(Config.OUT_DIR, runId).list();        
        if (jar.exists())
            jar.delete();

        FileHelper.jar(Config.OUT_DIR + runId, files, jar.getAbsolutePath());
        return jar;
    }

    /**
     * Client side method to poll for the oldest run which must be older than
     * localAge. If found, the run will be downloaded into the temp space.
     * and the run name will be returned.
     * @param localAge The age of the oldest local run in the queue
     * @return The file reference to the local run in the directory
     */
    public static File pollRun(long localAge) {
        Config.HostInfo selectedHost = null;
        NameValuePair<Long> selectedRun = null;
        for (int i = 0; i < Config.pollHosts.length; i++) {
            Config.HostInfo pollHost = Config.pollHosts[i];
            NameValuePair<Long> run = null;
            try {
                run = poll(pollHost, localAge);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error polling " + pollHost.url +
                            '.', e);
            }
            if (run != null &&
                    (selectedRun == null || run.value > selectedRun.value)) {
                selectedRun = run;
                selectedHost = pollHost;
            }
        }
        File tmpDir = null;
        if (selectedRun != null) {
            try {
                // Download and unjar the run.
                File tmpJar = download(selectedHost, selectedRun);
                if (tmpJar == null) {
                    logger.warning("Download null jar file.");
                    return null;
                }
                tmpDir = FileHelper.unjarTmp(tmpJar);
                File metaInf = new File(tmpDir, "META-INF");
                if (!metaInf.isDirectory())
                    metaInf.mkdir();

                // Create origin file to know where this run came from.
                FileHelper.writeStringToFile(selectedHost.name + '.' +
                        selectedRun.name, new File(metaInf, "origin"));
                tmpJar.delete();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error downloading run " +
                           selectedRun.name + " from " + selectedHost.url + '.',
                           e);
            }
        }
        return tmpDir;
    }

    private static NameValuePair<Long> poll(Config.HostInfo host, long minAge)
            throws IOException {

        NameValuePair<Long> run = null;
        URL target = new URL(host.url, SERVLET_PATH);

        HttpURLConnection c;
        if (host.proxyHost != null)
            c = (HttpURLConnection) target.openConnection(
                    new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(host.proxyHost, host.proxyPort)));
        else
            c = (HttpURLConnection) target.openConnection();

        try {
            c.setRequestMethod("POST");
            c.setConnectTimeout(2000);
            c.setDoOutput(true);
            c.setDoInput(true);
            PrintWriter out = new PrintWriter(c.getOutputStream());
            out.write("host=" + Config.FABAN_HOST + "&key=" + host.key +
                      "&minage=" + minAge);
            out.flush();
            out.close();
        } catch (SocketTimeoutException e) {
            logger.log(Level.WARNING, "Timeout trying to connect to " +
                    target + '.', e);
            throw new IOException("Socket connect timeout");
        }

        int responseCode = c.getResponseCode();

        if (responseCode == HttpServletResponse.SC_OK) {
            InputStream is = c.getInputStream();

            // The input is a one liner in the form runId\tAge
            byte[] buffer = new byte[256]; // Very little cost for this new/GC.
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
        } else if (responseCode != HttpServletResponse.SC_NO_CONTENT) {
            logger.warning("Polling " + target + " got response code " +
                           responseCode);
        }
        return run;
    }

    private static File download(Config.HostInfo host, NameValuePair<Long> run)
            throws IOException {

        File jarFile = null;
        FileOutputStream jarOut = null;
        URL target = new URL(host.url, SERVLET_PATH);

        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) target.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(2000);
            c.setDoOutput(true);
            c.setDoInput(true);
            PrintWriter out = new PrintWriter(c.getOutputStream());
            out.write("host=" + Config.FABAN_HOST + "&key=" + host.key +
                      "&runid=" + run.name);
            out.flush();
            out.close();
        } catch (SocketTimeoutException e) {
            logger.log(Level.WARNING, "Timeout trying to connect to " +
                    target + '.', e);
            throw new IOException("Socket connect timeout");
        }

        int responseCode = c.getResponseCode();
        if (responseCode == HttpServletResponse.SC_OK) {
            InputStream is = c.getInputStream();
            // We allocate in every method instead of a central buffer
            // to allow concurrent downloads. This can be expanded to use
            // buffer pools to avoid GC, if necessary.
            byte[] buffer = new byte[8192];
            int size;
            while ((size = is.read(buffer)) != -1) {
                if (size > 0 && jarFile == null) {
                    jarFile = new File(Config.TMP_DIR, host.name + '.' +
                                       run.name + ".jar");
                    jarOut = new FileOutputStream(jarFile);
                }
                jarOut.write(buffer, 0, size);
            }
            is.close();
            jarOut.flush();
            jarOut.close();
        } else {
            logger.warning("Downloading run " + run.name + " from " + target +
                           " got response code " + responseCode);
        }
        return jarFile;
    }
}
