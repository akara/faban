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
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.FileHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * The benchmark loader is called by the command agent to download benchmarks
 * to the agent machine in order to drive the workload without manually having
 * to cope with distributing benchmark code.
 *
 * @author Akara Sucharitakul
 */
public class Download {

    static Logger logger = Logger.getLogger(Download.class.getName());
    byte[] buffer = new byte[8192];

    /**
     * Downloads benchmark.
     * @param benchmarkName
     * @param context
     * @throws java.io.IOException
     */
    public void loadBenchmark(String benchmarkName, String context)
            throws IOException {
        download(Config.BENCHMARK_DOWNLOAD_PATH, benchmarkName, 
                 Config.BENCHMARK_DIR, context);
    }

    /**
     * Downloads service.
     * @param serviceLocation
     * @param context
     * @throws java.io.IOException
     */
    public void loadService(String serviceLocation, String context)
            throws IOException {
        download(Config.SERVICE_DOWNLOAD_PATH, serviceLocation,
                 Config.SERVICE_DIR, context);
    }

    private void download(String src, String name, String dest, String context)
            throws IOException {
        logger.fine("faban.download = " + context);
        if (context == null)
            return; // We are on the master. Just don't download
        if (!context.endsWith("/")) {
            context += '/';
        }


        File dir = new File(dest);
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                String msg = dir.getName() +
                        " is not a directory. Download terminated";
                logger.severe(msg);
                throw new IOException(msg);
            }
        } else {
            dir.mkdir();
        }
        dir = new File(dir, name);
        if (dir.exists()) {
            File runIdFile = new File(dir, "META-INF");
            runIdFile = new File(runIdFile, "RunID");
            if (runIdFile.exists() && (System.currentTimeMillis() -
                    runIdFile.lastModified()) <= 60000)
                return; // Recent RunID file means the directory is shared.
                        // Don't download.

            FileHelper.recursiveDelete(dir);
        }
        URL url = new URL(context + src + name + '/');
        downloadDir(url, dir);
    }

    private void downloadDir(URL url, File dir) throws IOException {
        String dirName = dir.getName();
        logger.finer("Creating directory " + dirName + " from "
                + url.toString());
        dir.mkdir();

        GetMethod get = new GetMethod(url.toString());
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().
                setConnectionTimeout(2000);
        int status = client.executeMethod(get);
        if (status != HttpStatus.SC_OK)
            throw new IOException("Download request for " + url + " returned " +
                                    HttpStatus.getStatusText(status) + '.');
        InputStream stream = get.getResponseBodyAsStream();
        DirectoryParser parser = new DirectoryParser();
        
        int length = 0;
        int bufEnd = 0;
        while (length != -1) {
            length = stream.read(buffer, bufEnd, buffer.length - bufEnd);
            if (length != -1)
                bufEnd += length;
            if (bufEnd > buffer.length / 4 * 3 || length == -1) {
                if (!parser.parse(buffer, bufEnd)) {
                    String msg = "URL " + url.toString() + " is not a directory!";
                    logger.severe(msg);
                    throw new IOException(msg);
                }
                bufEnd = 0;
            }
        }
        stream.close();

        String[] entries = parser.getEntries();
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            if ("META-INF/".equals(entry))
                continue;
            String name = entry.substring(0, entry.length() - 1);
            if (entry.endsWith("/")) { // Directory
                downloadDir(new URL(url, entry), new File(dir, name));
            } else {
                downloadFile(new URL(url, name), new File(dir, name));
            }
        }

    }

    private void downloadFile(URL url, File file) throws IOException {
        logger.finer("Downloading file " + url.toString());
        GetMethod get = new GetMethod(url.toString());
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().
                setConnectionTimeout(2000);
        int status = client.executeMethod(get);
        if (status != HttpStatus.SC_OK)
            throw new IOException("Download request for " + url + " returned " +
                                    HttpStatus.getStatusText(status) + '.');
        InputStream in = get.getResponseBodyAsStream();
        FileOutputStream out = new FileOutputStream(file);

        int length = in.read(buffer);
        while (length != -1) {
            out.write(buffer, 0, length);
            length = in.read(buffer);
        }
        out.flush();
        out.close();
        in.close();
    }

    static class DirectoryParser {
        static final byte[] HEADER = " Directory: ".getBytes();
        boolean headerRead = false;
        ArrayList entryList = new ArrayList();
        byte[] pending;
        int pendingLength = -1;

        boolean parse(byte[] buffer, int length) {
            int idx = 0;

            // Check the directory header
            if (!headerRead) {
                if (length <= HEADER.length)
                    return false;
                idx = 0;
                for (; idx < HEADER.length; idx++)
                    if (HEADER[idx] != buffer[idx])
                        return false;
                for (; buffer[idx] != '\n' && idx < length; idx++); // Advance
                headerRead = true;
            } else if (pendingLength > 0) {
                for (; buffer[idx] != '\n' && idx < length; idx++);
                System.arraycopy(buffer, 0, pending, pendingLength, idx);
                if (buffer[idx] == '\n') {
                    String entry = new String(pending, 0,
                            pendingLength + idx);
                    entryList.add(entry);
                    logger.finest("Adding pending entry: " + entry);
                    pendingLength = -1;
                } else {
                    return true;
                }
            }
            for (;;) {
                int oldIdx = ++idx;
                for (; buffer[idx] != '\n' && idx < length; idx++);
                if (buffer[idx] == '\n') {
                    if (idx == oldIdx)
                        break;
                    String entry = new String(buffer, oldIdx, idx - oldIdx);
                    entryList.add(entry);
                    logger.finest("Adding entry: " + entry);
                } else if (idx > oldIdx){
                    if (pending == null)
                        pending = new byte[512];
                    pendingLength = idx - oldIdx - 1;
                    System.arraycopy(buffer, oldIdx, pending, 0, pendingLength);
                    break;
                } else {
                    break;
                }
            }
            return true;
        }

        String[] getEntries() {
            String[] entries = new String[entryList.size()];
            return (String[]) entryList.toArray(entries);
        }
    }
}
