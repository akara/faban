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
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;

import com.sun.faban.harness.util.FileHelper;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import static com.sun.faban.harness.util.FileHelper.*;

/**
 * This is a controller class.
 *
 * @author Sheetal Patil
 */

public class Uploader {
  private static Logger logger = Logger.getLogger(ResultAction.class.getName());

  /**
   * Checks the existence of the runs on the repository.
   * @param request
   * @param response
   * @return String
   * @throws java.io.IOException
   * @throws javax.servlet.ServletException
   */
  public String checkRuns(HttpServletRequest request, HttpServletResponse
                                response) throws IOException, ServletException {
            HashSet<String> duplicateSet = new HashSet<String>();
            String host = request.getParameter("host");
            String[] runIds = request.getParameterValues("runId");
            String[] ts = request.getParameterValues("ts");
            for (int r=0; r< runIds.length ; r++){
                String runId = runIds[r];
                if (checkIfArchived(host+"."+runId)){
                    //check for runId timestamp
                    String reposTs =
                          getRunIdTimestamp(host + "." + runId, Config.OUT_DIR);
                    if (ts[r].equals(reposTs)) {
                        duplicateSet.add(runId);
                    }
                }
                response.setStatus(HttpServletResponse.SC_OK);
            }
            request.setAttribute("duplicates", duplicateSet);
            return "/duplicates.jsp";
        }

        private String getRunIdTimestamp(String runId,  String dir) {
            char[] cBuf = null;
            String[] status = new String[2];
            int length = -1;
            try {
               FileReader reader = new FileReader(dir + runId + '/'
                                                         + Config.RESULT_INFO);
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
            String seq = current.getRunSeq(); // Say "1A1"
            int i = 0;
            for (; i < seq.length(); i++) {
                if (Character.isLetter(seq.charAt(i)))
                    break;
            } // i now points to 'A'
            String origSeq = seq.substring(0, i + 1); // origSeq = "1A"
            String cDup = seq.substring(i + 1);       // cDup = "1"
            String nDup = null;
            if (cDup.length() == 0) {
                nDup = "0";
            } else {
                int x = Integer.parseInt(cDup, 16);  // x = (int) 1
                nDup = Integer.toHexString(++x).toUpperCase(); // nDup - "2"
            }
            RunId next = new RunId(current.getHostName(),
                    current.getBenchName(), origSeq + nDup);
            return next.toString();
        }
        
        private boolean checkIfArchived(String runId) {
            boolean found = false;
            File file = new File(Config.OUT_DIR + runId + '/' +
                                                            Config.RESULT_INFO);
            found = file.exists();
            return found;
        }

        /**
         * Updates the tags file.
         * @param req
         * @param resp
         * @throws java.io.IOException
         */
        public void updateTagsFile(HttpServletRequest req,
            HttpServletResponse resp) throws IOException{
            String tags = req.getParameter("tags");
            String runId = req.getParameter("runId");
            RunResult result = RunResult.getInstance(new RunId(runId));
            StringBuilder formattedTags = new StringBuilder();
            File runTagFile = new File(Config.OUT_DIR + runId + "/META-INF/tags");
            if (tags != null && !"".equals(tags)) {
                StringTokenizer t = new StringTokenizer(tags," \n,");
                ArrayList<String> tagList = new ArrayList<String>(t.countTokens());
                while (t.hasMoreTokens()) {
                    String nextT = t.nextToken().trim();
                    if( nextT != null && !"".equals(nextT) ){
                        formattedTags.append(nextT + "\n");
                        tagList.add(nextT);
                    }
                }
                FileHelper.writeContentToFile(formattedTags.toString(), runTagFile);
                result.tags = tagList.toArray(new String[tagList.size()]);
            }
            try {
                uploadTags(runId);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Uploader.class.getName()).log(Level.SEVERE, null, ex);
            }
            Writer w = resp.getWriter();
            w.write("Tags updating completed");
            w.flush();
            w.close();
        }

        /**
         * Updates the run description.
         * @param req
         * @param resp
         * @throws java.io.IOException
         */
        public void updateRunDesc(HttpServletRequest req,
            HttpServletResponse resp) throws IOException{
            String runId = req.getParameter("runId");
            RunResult result = RunResult.getInstance(new RunId(runId));
            result.description = req.getParameter("desc");
            ResultAction.editXML(result);
            Writer w = resp.getWriter();
            w.write("Tags updating completed");
            w.flush();
            w.close();
        }


        private void uploadTags(String runId) throws IOException, ClassNotFoundException {
            File file = new File(Config.OUT_DIR + runId + "/META-INF/tags");
            String tags = FileHelper.readContentFromFile(file);
            TagEngine te = TagEngine.getInstance();
            String[] tagsArray;
            if (tags != null && !"".equals(tags)) {
                StringTokenizer tok = new StringTokenizer(tags," ");
                tagsArray = new String[tok.countTokens()];
                int count = tok.countTokens();
                int i=0;
                while(i < count){
                    String nextT = tok.nextToken().trim();
                    tagsArray[i] = nextT;
                    i++;
                }
                te.add(runId, tagsArray);
            }else{
                te.add(runId, new String[0]);
            }
            te.save();
        }

        /**
         * Responsible for uploading the runs.
         * @param request
         * @param response
         * @return String
         * @throws java.io.IOException
         * @throws javax.servlet.ServletException
         * @throws java.lang.ClassNotFoundException
         */
        public String uploadRuns(HttpServletRequest request, HttpServletResponse
                                response) throws IOException, ServletException, ClassNotFoundException {
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
            // the location for saving data that is larger than
            // getSizeThreshold()
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
                File uploadFile = new File(Config.TMP_DIR, host + '.' +
                                                                     fileName);
                try {
                    item.write(uploadFile);
                } catch (Exception e) {
                    throw new ServletException(e);
                }
                int runIdx = fileName.lastIndexOf(".");
                String runName = host + '.' + fileName.substring(0, runIdx);
                File runTmp = unjarTmp(uploadFile);
                //Check if archived recently
                if (checkIfArchived(runName) &&
                       !(replaceSet.contains(fileName.substring(0, runIdx)))) {
                    //Now check if timestamps are same
                    //Get the timestamp of run being uploaded at this point
                    //ts is timestamp of run being uploaded
                    String ts = getRunIdTimestamp(runName,Config.TMP_DIR);
                    l1: while (true) {                      
                        //reposTs is timestamp of run being compared in the
                        //repository
                        String reposTs =
                                getRunIdTimestamp(runName,Config.OUT_DIR);
                        if (reposTs.equals(ts)){
                            duplicateSet.add(fileName.substring(0, runIdx));
                        }else{
                            runName = getNextRunId(runName);
                            if (checkIfArchived(runName))
                                continue l1;
                            File newRunNameFile = new File(Config.OUT_DIR,
                                    runName);
                            if (newRunNameFile.exists()) {
                                recursiveDelete(newRunNameFile);
                            }
                            if (recursiveCopy(runTmp, newRunNameFile)) {
                                newRunNameFile.setLastModified(runTmp.lastModified());
                                uploadTags(runName);
                                uploadFile.delete();
                                recursiveDelete(runTmp);
                            } else {
                                logger.warning("Origin upload requested. " +
                                                                "Copy error!");
                                response.sendError(
                                        HttpServletResponse.SC_NOT_ACCEPTABLE);
                                break;
                            }
                            response.setStatus(HttpServletResponse.SC_CREATED);
                        }
                        break;
                    }
                }else{
                    //File runTmp = unjarTmp(uploadFile);

                    String runId = null;

                    if (origin) {
                        // Change origin file to know where this run came from.
                        File metaInf = new File(runTmp, "META-INF");
                        File originFile = new File(metaInf, "origin");
                        if (!originFile.exists()) {
                            logger.warning("Origin upload requested. " +
                                           "Origin file does not exist!");
                            response.sendError(
                                    HttpServletResponse.SC_NOT_ACCEPTABLE,
                                    "Origin file does not exist!");
                            break;
                        }

                        RunId origRun;
                        try {
                            origRun = new RunId(
                                    readStringFromFile(originFile).trim());
                        } catch (IndexOutOfBoundsException e) {
                            response.sendError(
                                    HttpServletResponse.SC_NOT_ACCEPTABLE,
                                    "Origin file error. " + e.getMessage());
                            break;
                        }

                        runId = origRun.getBenchName() + '.' +
                                                           origRun.getRunSeq();
                        String localHost = origRun.getHostName();
                        if (!localHost.equals(Config.FABAN_HOST)) {
                            logger.warning("Origin upload requested. Origin " +
                            "host" + localHost + " does not match this host " +
                                           Config.FABAN_HOST + '!');
                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN);
                            break;
                        }
                        writeStringToFile(runTmp.getName(), originFile);
                    }  else {
                        runId = runTmp.getName();
                    }
                    File newRunFile = new File(Config.OUT_DIR, runId);
                    if(newRunFile.exists()){
                        recursiveDelete(newRunFile);
                    }
                    if (recursiveCopy(runTmp, newRunFile)){
                        newRunFile.setLastModified(runTmp.lastModified());
                        uploadFile.delete();
                        uploadTags(runId);
                        recursiveDelete(runTmp);
                    } else {
                        logger.warning("Origin upload requested. Copy error!");
                        response.sendError(
                                HttpServletResponse.SC_NOT_ACCEPTABLE);
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
