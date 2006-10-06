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
 * $Id: RunUploader.java,v 1.1 2006/10/06 23:24:20 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.FileHelper;

/**
 * The RunUploader represents the upload servlet as well as an upload client
 * maintained in a single class.
 *
 * @author Akara Sucharitakul
 */
public class RunUploader extends HttpServlet {

    static Logger logger = Logger.getLogger(Deployer.class.getName());

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<String> uploadNames = new ArrayList<String>();

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

                // TODO: Check key

                if (!"jarfile".equals(fieldName))
                    continue;

                String fileName = item.getName();

                if (fileName == null) // We don't process files without names
                    continue;

                // The host, origin, key info must be here before we receive
                // any file.
                if (host == null || (origin && key == null)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
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

                File uploadFile = File.createTempFile("run", ".jar",
                        new File(Config.TMP_DIR));
                try {
                    item.write(uploadFile);
                } catch (Exception e) {
                    throw new ServletException(e);
                }
                File runTmp = FileHelper.unjarTmp(uploadFile);

                File metaInf = new File(runTmp, "META-INF");

                String runName = null;

                // Create origin file to know where this run came from.
                File originFile = new File(metaInf, "origin");
                if (origin && originFile.exists()) {
                    FileInputStream originIn = new FileInputStream(originFile);
                    byte[] buffer = new byte[128];
                    int length = originIn.read(buffer);
                    if (buffer[length - 1] == '\n')
                        --length;
                    // originSpec is in form of host.runName which is
                    // host.bench.runId.
                    String originSpec = new String(buffer, 0, length);
                    int idx = originSpec.lastIndexOf('.');
                    if (idx == -1) { // This is wrong, we do not accept this.
                        response.sendError(
                                HttpServletResponse.SC_NOT_ACCEPTABLE,
                                "Origin file error!");
                        return;
                    }
                    idx = originSpec.lastIndexOf('.', idx - 1);
                    if (idx == -1) {
                        response.sendError(
                                HttpServletResponse.SC_NOT_ACCEPTABLE,
                                "Origin file error!");
                        return;
                    }

                    runName = originSpec.substring(idx + 1);
                    String hostName = originSpec.substring(0, idx);
                    if (!hostName.equals(Config.FABAN_HOST)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    // TODO: Change the origin file to point to the executing rig
                }

                // TODO: Handle the case of the result server


            }
        } finally {

        }
    }
}
