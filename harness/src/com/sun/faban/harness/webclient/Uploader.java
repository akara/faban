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
* $Id: Uploader.java,v 1.1 2008/12/05 22:07:59 sheetalpatil Exp $
*
* Copyright 2005 Sun Microsystems Inc. All Rights Reserved
*/
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import static com.sun.faban.harness.util.FileHelper.*;

/**
 * @author Sheetal Patil
 */

public class Uploader {
        private static Logger logger = Logger.getLogger(ResultAction.class.getName());

        public String checkRuns(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            HashSet<String> duplicateSet = new HashSet<String>();
            String host = request.getParameter("host");
            String[] runIds = request.getParameterValues("runId");
            String[] ts = request.getParameterValues("ts");
            for (int r=0; r< runIds.length ; r++){
                String runId = runIds[r];
                if (checkIfArchived(host+"."+runId)){
                        //check for runId timestamp
                        String reposTs = getRunIdTimestamp(host+"."+runId);
                        if (ts[r].equals(reposTs)) {
                            duplicateSet.add(runId);
                        }
                }
                response.setStatus(HttpServletResponse.SC_OK);
            }
            request.setAttribute("duplicates", duplicateSet);
            return "/duplicates.jsp";
        }

        private String getRunIdTimestamp(String runId) {
            char[] cBuf = null;
            String[] status = new String[2];
            int length = -1;
            try {
               FileReader reader = new FileReader(Config.OUT_DIR + runId + '/' + Config.RESULT_INFO);
               cBuf = new char[128];
               length = reader.read(cBuf);
               reader.close();
            } catch (IOException e) {
               // Do nothing, length = -1.
            }
            String content = new String(cBuf, 0, length);
            int idx = content.indexOf('\t');
            if (idx != -1) {
               status[0] = content.substring(0, idx).trim();
               status[1] = content.substring(++idx).trim();
            } else {
               status[0] = content.trim();
            }
            return status[1];
        }

        private String getNextRunId(String runId) {
            RunId current = new RunId(runId);
            String seq = current.getRunSeq();
            int i = 0;
            for (; i < seq.length(); i++) {
                if (Character.isLetter(seq.charAt(i)))
                    break;
            }
            String cDup = seq.substring(i + 1);
            String nDup = null;
            if (cDup.length() == 0) {
                nDup = "0";
            } else {
                int x = Integer.parseInt(cDup, 16);
                nDup = Integer.toHexString(++x).toUpperCase();
            }
            RunId next = new RunId(current.getHostName(),
                    current.getBenchName(), seq + nDup);
            return next.toString();
        }
        
        private boolean checkIfArchived(String runId) throws IOException {
            boolean found = false;
            File file = new File(Config.OUT_DIR + runId + '/' + Config.RESULT_INFO);
            found = file.exists();
            return found;
        }

        public String uploadRuns(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            // 3. Upload the run
            HashSet<String> duplicateSet = new HashSet<String>();
            HashSet<String> replaceSet = new HashSet<String>();
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
                    } else if ("replace".equals(fieldName)) {
                        replaceSet.add(item.getString());
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
                int runIdx = fileName.lastIndexOf(".");
                String runName = host + '.' + fileName.substring(0, runIdx);
                File runTmp = unjarTmp(uploadFile);
                if ( checkIfArchived(runName) && !(replaceSet.contains(fileName.substring(0, runIdx))) ) {
                    char[] cBuf = null;
                    int length = -1;
                    try {
                       FileReader reader = new FileReader(Config.TMP_DIR + runName + '/' + Config.RESULT_INFO);
                       cBuf = new char[128];
                       length = reader.read(cBuf);
                       reader.close();
                    } catch (IOException e) {
                       // Do nothing, length = -1.
                    }
                    String content = new String(cBuf, 0, length);
                    int idx = content.indexOf('\t');
                    String ts = content.substring(++idx);
                    if (ts.equals(getRunIdTimestamp(runName))){
                        duplicateSet.add(fileName.substring(0, runIdx));
                    }else{
                        String runId = getNextRunId(runName);
                        if (recursiveCopy(runTmp, new File(Config.OUT_DIR, runId))) {
                            uploadFile.delete();
                            recursiveDelete(runTmp);
                        } else {
                            logger.warning("Origin upload requested. Copy error!");
                            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                            break;
                        }
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    }
                }else{
                    //File runTmp = unjarTmp(uploadFile);

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
                }
                response.setStatus(HttpServletResponse.SC_CREATED);
                //break;
            }
            request.setAttribute("duplicates", duplicateSet);
            return "/duplicates.jsp";
        }
}
