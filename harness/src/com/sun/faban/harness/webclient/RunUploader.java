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

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import static com.sun.faban.harness.util.FileHelper.*;

/**
 * The RunUploader represents the upload servlet as well as an upload client
 * maintained in a single class.
 *
 * @author Akara Sucharitakul
 */
public class RunUploader extends HttpServlet {

    static Logger logger = Logger.getLogger(Deployer.class.getName());

    /**
     * Post method to upload the run.
     * @param request The servlet request
     * @param response The servlet response
     * @throws ServletException If the servlet fails
     * @throws IOException If there is an I/O error
     */
    public void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String host = null;
        String key = null;
        boolean origin = false; // Whether the upload is to the original
        // run requestor. If so, key is needed.

        DiskFileUpload fu = new DiskFileUpload();
        // No maximum size
        fu.setSizeMax(-1);
        // maximum size that will be stored in memory
        fu.setSizeThreshold(4096);
        // the location for saving data that is larger than getSizeThreshold()
        fu.setRepositoryPath(Config.TMP_DIR);

        List fileItems = null;
        try {
            fileItems = fu.parseRequest(request);
        } catch (FileUploadException e) {
            throw new ServletException(e);
        }
        // assume we know there are two files. The first file is a small
        // text file, the second is unknown and is written to a file on
        // the server
        for (Iterator i = fileItems.iterator(); i.hasNext();) {
            FileItem item = (FileItem) i.next();
            String fieldName = item.getFieldName();
            if (item.isFormField()) {
                if ("host".equals(fieldName)) {
                    host = item.getString();
                } else if ("key".equals(fieldName)) {
                    key = item.getString();
                } else if ("origin".equals(fieldName)) {
                    String value = item.getString();
                    origin = Boolean.parseBoolean(value);
                }
                continue;
            }

            if (host == null) {
                logger.warning("Host not received on upload request!");
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                break;
            }

            // The host, origin, key info must be here before we receive
            // any file.
            if (origin) {
                if (Config.daemonMode != Config.DaemonModes.POLLEE) {
                    logger.warning("Origin upload requested. Not pollee!");
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    break;
                }
                if (key == null) {
                    logger.warning("Origin upload requested. No key!");
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    break;
                }
                if (!RunRetriever.authenticate(host, key)) {
                    logger.warning("Origin upload requested. " +
                            "Host/key mismatch: " +host + '/' + key + "!");
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    break;
                }
            }

            if (!"jarfile".equals(fieldName)) // ignore
                continue;

            String fileName = item.getName();

            if (fileName == null) // We don't process files without names
                continue;

            // Now, this name may have a path attached, dependent on the
            // source browser. We need to cover all possible clients...
            char[] pathSeparators = {'/', '\\'};
            // Well, if there is another separator we did not account for,
            // just add it above.

            for (int j = 0; j < pathSeparators.length; j++) {
                int idx = fileName.lastIndexOf(pathSeparators[j]);
                if (idx != -1) {
                    fileName = fileName.substring(idx + 1);
                    break;
                }
            }

            // Ignore all non-jarfiles.
            if (!fileName.toLowerCase().endsWith(".jar"))
                continue;
            File uploadFile = new File(Config.TMP_DIR, host + '.' + fileName);
            try {
                item.write(uploadFile);
            } catch (Exception e) {
                throw new ServletException(e);
            }
            File runTmp = unjarTmp(uploadFile);

            String runId = null;

            if (origin) {
                // Change origin file to know where this run came from.
                File metaInf = new File(runTmp, "META-INF");
                File originFile = new File(metaInf, "origin");
                if (!originFile.exists()) {
                    logger.warning("Origin upload requested. Origin file" +
                                   "does not exist!");
                    response.sendError(
                            HttpServletResponse.SC_NOT_ACCEPTABLE,
                            "Origin file does not exist!");
                    break;
                }

                RunId origRun;
                try {
                    origRun = new RunId(readStringFromFile(originFile).trim());
                } catch (IndexOutOfBoundsException e) {
                    response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,
                            "Origin file error. " + e.getMessage());
                    break;
                }

                runId = origRun.getBenchName() + '.' + origRun.getRunSeq();
                String localHost = origRun.getHostName();
                if (!localHost.equals(Config.FABAN_HOST)) {
                    logger.warning("Origin upload requested. Origin host " +
                                   localHost + " does not match this host " +
                                   Config.FABAN_HOST + '!');
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    break;
                }
                writeStringToFile(runTmp.getName(), originFile);
            }  else {
                runId = runTmp.getName();
            }

            if (recursiveCopy(runTmp, new File(Config.OUT_DIR, runId))) {
                uploadFile.delete();
                recursiveDelete(runTmp);
            } else {
                logger.warning("Origin upload requested. Copy error!");
                response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                break;
            }

            response.setStatus(HttpServletResponse.SC_CREATED);
            break;
        }
    }


    /**
     * Client call to upload the run back to the originating server.
     * This method does nothing if the run is local.
     * @param runId The id of the run
     * @throws IOException If the upload fails
     */
    public static void uploadIfOrigin(String runId) throws IOException {

        // 1. Check origin
        File originFile = new File(Config.OUT_DIR + File.separator + runId +
                    File.separator + "META-INF" + File.separator + "origin");

        if (!originFile.isFile())
            return; // Is local run, do nothing.

        String originSpec = readStringFromFile(originFile);
        int idx = originSpec.lastIndexOf('.');
        if (idx == -1) { // This is wrong, we do not accept this.
            logger.severe("Bad origin spec.");
            return;
        }
        idx = originSpec.lastIndexOf('.', idx - 1);
        if (idx == -1) {
            logger.severe("Bad origin spec.");
            return;
        }

        String host = originSpec.substring(0, idx);
        String key = null;
        URL target = null;
        String proxyHost = null;
        int proxyPort = -1;

        // Search the poll hosts for this origin.
        for (int i = 0; i < Config.pollHosts.length; i++) {
            Config.HostInfo pollHost = Config.pollHosts[i];
            if (host.equals(pollHost.name)) {
                key = pollHost.key;
                target = new URL(pollHost.url, "upload");
                proxyHost = pollHost.proxyHost;
                proxyPort = pollHost.proxyPort;
                break;
            }
        }

        if (key == null) {
            logger.severe("Origin host/url/key not found!");
            return;
        }

        // 2. Jar up the run
        String[] files = new File(Config.OUT_DIR, runId).list();
        File jarFile = new File(Config.TMP_DIR, runId + ".jar");
        jar(Config.OUT_DIR + runId, files, jarFile.getAbsolutePath());

        // 3. Upload the run
        ArrayList<Part> params = new ArrayList<Part>();
        //MultipartPostMethod post = new MultipartPostMethod(target.toString());
        params.add(new StringPart("host", Config.FABAN_HOST));
        params.add(new StringPart("key", key));
        params.add(new StringPart("origin", "true"));
        params.add(new FilePart("jarfile", jarFile));
        Part[] parts = new Part[params.size()];
        parts = params.toArray(parts);
        PostMethod post = new PostMethod(target.toString());
        post.setRequestEntity(
                new MultipartRequestEntity(parts, post.getParams()));
        HttpClient client = new HttpClient();
        if (proxyHost != null)
            client.getHostConfiguration().setProxy(proxyHost, proxyPort);
        client.getHttpConnectionManager().getParams().
                setConnectionTimeout(5000);
        int status = client.executeMethod(post);
        if (status == HttpStatus.SC_FORBIDDEN)
            logger.severe("Server " + host + " denied permission to upload run "
                            + runId + '!');
        else if (status == HttpStatus.SC_NOT_ACCEPTABLE)
            logger.severe("Run " + runId + " origin error!");
        else if (status != HttpStatus.SC_CREATED)
            logger.severe("Server responded with status code " +
                    status + ". Status code 201 (SC_CREATED) expected.");
        jarFile.delete();
    }
}
