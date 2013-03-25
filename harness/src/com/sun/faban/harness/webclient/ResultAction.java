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

import com.sun.faban.common.SortableTableModel;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.util.FileHelper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.faban.harness.util.FileHelper.*;

/**
 * Controller handling actions from the result list screen.
 */
public class ResultAction {

    private static Logger logger =
            Logger.getLogger(ResultAction.class.getName());
    private SimpleDateFormat dateFormat = new SimpleDateFormat(
                              "EEE MM/dd/yy HH:mm:ss z");

    /**
     * Checks for actions the user asked to perform and take the appropriate
     * action.
     * @param request The request
     * @param response The response
     * @return The name of the jsp view to forward this request
     * @throws IOException If there are problems reading or writing data
     * @throws ParseException Error parsing input
     */
    public String takeAction(HttpServletRequest request,
                           HttpServletResponse response)
            throws IOException, ParseException {
        String process = request.getParameter("process");
        if ("Compare".equals(process))
            return editAnalysis(process, request, response);
        if ("Average".equals(process))
            return editAnalysis(process, request, response);
        if ("Archive".equals(process))
            return editArchive(request, response);
        if ("Delete".equals(process))
            return deleteResults(request, response);
        return null;
    }

    /**
     * The model object for the EditArchive screen view.
     * This is according to the MVC pattern.
     */
    public class EditArchiveModel implements Serializable {

        /** The display header. */
        public String head;

        /** The run id of the runs to archive. */
        public String[] runIds;

        /** The set of duplicate runs. */
        public Set<String> duplicates;

        /** The list of run result objects. */
        public RunResult[] results;
    }

    String editArchive(HttpServletRequest request,
                              HttpServletResponse response) throws IOException,
                              FileNotFoundException, ParseException {
        String[] runIds = request.getParameterValues("select");

        if (runIds == null || runIds.length < 1) {
            String msg;
            msg = "Select at least one runs to archive.";
            response.getOutputStream().println(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return null;
        }
        
        EditArchiveModel model = new EditArchiveModel();
        model.runIds = runIds;
        model.duplicates = checkArchivedRuns(runIds);

        if (Config.repositoryURLs != null &&
                Config.repositoryURLs.length > 1)
            model.head = "Repositories";
        else
            model.head = "Repository";

        model.results = new RunResult[runIds.length];
        for (int i = 0; i < runIds.length; i++) {
            model.results[i] = RunResult.getInstance(new RunId(runIds[i]));
        }
        // We use request attributes as not to reflect session state.
        request.setAttribute("editarchive.model", model);
        return "/edit_archive.jsp";
    }

    /**
     * Obtains the tags list for each profile.
     * @param req
     * @param resp
     * @throws java.io.IOException
     */
    public void profileTagList (HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        String profile = req.getParameter("profileselected");
        File tagsFile = new File(Config.PROFILES_DIR + profile + "/tags");
        String tagsForProfile = "";
        if(tagsFile.exists() && tagsFile.length()>0){
            tagsForProfile = FileHelper.readContentFromFile(tagsFile).trim();
        }
        Writer w = resp.getWriter();
        w.write(tagsForProfile);
        w.flush();
        w.close();
    }

    /*private Set<String> checkArchivedRuns(String[] runIds) throws IOException{
        StringBuilder b = new StringBuilder();
        b.append("/controller/uploader/check_runs");
        int endPath = b.length();      
        for (String runId : runIds)
            b.append("&select=").append(runId);
        b.setCharAt(endPath, '?');
        HttpURLConnection c = (HttpURLConnection) request.openConnection();
                if (c.getResponseCode() != 404){
                    existingRuns.add(runId);
         }

        HashSet<String> existingRuns = new HashSet<String>();
        for (String runId : runIds){
            for (URL repository : Config.repositoryURLs) {
                URL request = new URL(repository, "/output/"+
                                                 Config.FABAN_HOST+"."+runId);
                URLConnection c = request.openConnection();
                int len = c.getContentLength();
                if (len < 0){
                        existingRuns.add(runId);
                }
            }
        }
        return existingRuns;
    }*/

    private String editResultInfo(String runID)
            throws FileNotFoundException, IOException {
        RunId runId = new RunId(runID);
        String ts = null;
        String[] status = new String[2];
        File file = new File(Config.OUT_DIR + runID + '/' + Config.RESULT_INFO);
        RandomAccessFile rf = new RandomAccessFile(file, "rwd");
        long size = rf.length();
        byte[] buffer = new byte[(int) size];
        rf.readFully(buffer);
        String content = new String(buffer, 0, (int) size );
        int idx = content.indexOf('\t');
        if (idx != -1) {
                status[0] = content.substring(0, idx).trim();
                status[1] = content.substring(++idx).trim();
        } else {
                status[0] = content.trim();
                int lastIdxln = status[0].lastIndexOf("\n");
                if(lastIdxln != -1)
                    status[0] = status[0].substring(0, lastIdxln-1);
        }
        if (status[1] != null) {
            ts = status[1];
        }else{
            String paramFileName = runId.getResultDir().getAbsolutePath() +
                    File.separator + "run.xml";
            File paramFile = new File(paramFileName);
            long dt = paramFile.lastModified();
            ts = dateFormat.format(new Date(dt));
            rf.seek(rf.length());
            rf.writeBytes('\t' + ts.trim());
        }
        rf.close();
        return ts;
    }

    private Set<String> checkArchivedRuns(String[] runIds) throws
                            FileNotFoundException, IOException {
        HashSet<String> existingRuns = new HashSet<String>();
        String[] runIdTimeStamps = new  String[runIds.length];
        for (int r=0; r< runIds.length ; r++){
            String runId = runIds[r];
            runIdTimeStamps[r] = editResultInfo(runId);
        }
        for (URL repository : Config.repositoryURLs) {
            URL repos = new URL(repository, "/controller/uploader/check_runs");
            PostMethod post = new PostMethod(repos.toString());
            post.addParameter("host",Config.FABAN_HOST);
            for (String runId : runIds){
                post.addParameter("runId",runId);
            }
            for (String ts : runIdTimeStamps){
                post.addParameter("ts",ts);
            }
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().
                    setConnectionTimeout(5000);
            int status = client.executeMethod(post);
            if (status != HttpStatus.SC_OK)
                logger.info("SC_OK not ok");

            String response = post.getResponseBodyAsString();
            StringTokenizer t = new StringTokenizer(response.trim(),"\n");
            while (t.hasMoreTokens()) {
                existingRuns.add(t.nextToken().trim());
            }
        }
        return existingRuns;
    }

    /**
     * The model object for the EditAnalysis screen view.
     * This is according to the MVC pattern.
     */
    public static class EditAnalysisModel implements Serializable {
        /** The header, usually the process type. */
        public String head;

        /** The type string in all lowercase. */
        public String type;

        /** The string representation of the list of runs to analyze. */
        public String runList;

        /** The analysis name. */
        public String name;

        /** The run ids to analyze. */
        public String[] runIds;
    }

    String editAnalysis(String process, HttpServletRequest request,
                              HttpServletResponse response)
            throws IOException {

        EditAnalysisModel model = new EditAnalysisModel();
        model.head = process;
        model.type = process.toLowerCase();

        model.runIds = request.getParameterValues("select");
        if (model.runIds == null || model.runIds.length < 2) {
            String msg;
            msg = "Select at least 2 runs to " + model.type + ".";
            response.getOutputStream().println(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return null;
        }

        StringBuilder runList = new StringBuilder();

        for (String runId : model.runIds)
            runList.append(runId).append(", ");

        runList.setLength(runList.length() - 2); //Strip off the last comma
        model.runList = runList.toString();

        model.name = RunAnalyzer.suggestName(RunAnalyzer.Type.COMPARE,
                model.runIds);
        request.setAttribute("editanalysis.model", model);

        return "/edit_analysis.jsp";
    }

    /**
     * This method is responsible for analyzing the runs.
     * @param request
     * @param response
     * @return string
     * @throws java.io.IOException
     */
    public String analyze(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

        EditAnalysisModel model = new EditAnalysisModel();
        model.name = request.getParameter("output");
        model.type = request.getParameter("type");
        RunAnalyzer.Type type;
        if ("compare".equals(model.type)) {
            type = RunAnalyzer.Type.COMPARE;
        } else if ("average".equals(model.type)) {
            type = RunAnalyzer.Type.AVERAGE;
        } else {
            String msg = "Invalid analysis: " + model.name;
            response.getWriter().println(msg);
            logger.severe(msg);
            response.sendError(HttpServletResponse.SC_CONFLICT, msg);
            return null;
        }

        model.runIds = request.getParameterValues("select");
        boolean analyze = false;
        boolean redirect = false;
        if (RunAnalyzer.exists(model.name)) {
            String replace = request.getParameter("replace");
            if (replace == null) {
                request.setAttribute("editanalysis.model", model);
                return "/confirm_analysis.jsp";
            } else if ("Replace".equals(replace)) {
                analyze = true;
                redirect = true;
            } else {
                redirect = true;
            }
        } else {
            analyze = true;
            redirect = true;
        }
        if (analyze)
            try {
                RunAnalyzer.clear(model.name);
                UserEnv usrEnv = (UserEnv) request.getSession().
                                                getAttribute("usrEnv");
                new RunAnalyzer().analyze(type, model.runIds, model.name,
                                                    usrEnv.getUser());
            } catch (IOException e) {
                String msg = e.getMessage();
                response.getWriter().println(msg);
                logger.log(Level.SEVERE, msg, e);
                response.sendError(HttpServletResponse.SC_CONFLICT, msg);
                return null;
            }

        if (redirect)
            //response.sendRedirect("/analysis/" + model.name + "/index.html");
            response.sendRedirect("/controller/view/xan_view/analysis/" +
                    model.name + "/" + model.type + ".xan");

        return null;
    }

    /**
     * This method is responsible for archiving the runs to the repository.
     * @param request
     * @param response
     * @return String
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     * @throws java.text.ParseException
     * @throws java.lang.ClassNotFoundException
     */
    public String archive(HttpServletRequest request,
                        HttpServletResponse response) throws IOException,
                        FileNotFoundException, ParseException, ClassNotFoundException {
        //Reading values from request
        String[] duplicateIds = request.getParameterValues("duplicates");
        String[] replaceIds = request.getParameterValues("replace");
        String[] runIds = request.getParameterValues("select");
        String submitAction = request.getParameter("process");

        HashSet<String> modelDuplicates = new HashSet<String>();
        HashSet<String> replaceSet = new HashSet<String>();
        HashSet<File> uploadSet = new HashSet<File>();
        HashSet<String> uploadedRuns = new HashSet<String>();
        HashSet<String> duplicateSet = new HashSet<String>();
        if(replaceIds != null) {
            for(String replaceId : replaceIds){
                replaceSet.add(replaceId);
            }
        }
        
        EditArchiveModel model = new EditArchiveModel();
        model.runIds = runIds;
        if (duplicateIds != null) {
            for (String duplicateId : duplicateIds){
                modelDuplicates.add(duplicateId);
            }
        }
        model.duplicates = modelDuplicates;
        if (Config.repositoryURLs != null &&
                Config.repositoryURLs.length > 1)
            model.head = "Repositories";
        else
            model.head = "Repository";

        model.results = new RunResult[runIds.length];
        for (int i = 0; i < runIds.length; i++) {
            model.results[i] = RunResult.getInstance(new RunId(runIds[i]));
        }

        if (submitAction.equals("Archive")) {
            for (int i = 0; i < model.runIds.length; i++) {
                String runId = model.runIds[i];
                if (model.duplicates.contains(runId)) {
                    if (replaceIds != null) {
                        if (replaceSet.contains(runId)) {
                            prepareUpload(request, model.results[i],
                                    uploadedRuns, uploadSet);
                        } else { // Description or tags got changed, replace anyway...
                            if (!model.results[i].description.equals(request.
                                    getParameter(runId + "_description")) ||
                                !model.results[i].tags.toString().equals(request.
                                    getParameter(runId + "_tags"))) {
                                replaceSet.add(runId);
                                prepareUpload(request, model.results[i],
                                        uploadedRuns, uploadSet);
                            }
                        }
                    } else { // Single run, description changed, replace anyway.
                        if (!model.results[i].description.equals(
                                request.getParameter(runId + "_description")) ||
                                !model.results[i].tags.toString().equals(request.
                                    getParameter(runId + "_tags"))) {
                            replaceSet.add(runId);
                            prepareUpload(request, model.results[i],
                                    uploadedRuns, uploadSet);
                        }
                    }
                } else {
                    prepareUpload(request, model.results[i],
                            uploadedRuns, uploadSet);
                }
            }
        }
        duplicateSet = uploadRuns(runIds, uploadSet,replaceSet);
        request.setAttribute("archive.model", model);
        request.setAttribute("uploadedRuns", uploadedRuns);
        request.setAttribute("duplicateRuns", duplicateSet);
        return "/archive_results.jsp";
    }

    public String deleteResults(HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        String[] runIds = request.getParameterValues("select");
        if (runIds != null) {
            TagEngine tagEngine;
            try {
                tagEngine = TagEngine.getInstance();
            } catch (ClassNotFoundException ex) {
                logger.log(Level.SEVERE, "Cannot find tag engine class", ex);
                throw new IOException("Cannot find tag engine class", ex);
            }
            for (String r : runIds) {
                RunResult runResult = RunResult.getInstance(new RunId(r));
                runResult.delete(r);
                tagEngine.removeRun(r);
                tagEngine.save();
            }
        }
        HttpSession session = request.getSession();
        UserEnv usrEnv = (UserEnv) session.getAttribute("usrEnv");
        if (usrEnv == null) {
            usrEnv = new UserEnv();
            session.setAttribute("usrEnv", usrEnv);
        }
        SortableTableModel resultTable = RunResult.getResultTable(usrEnv.getSubject(), 5, "DESCENDING");
        String feedURL = "/controller/results/feed";
        request.setAttribute("feedURL", feedURL );
        request.setAttribute("table.model", resultTable);
        return "/resultlist.jsp";

    }

    @SuppressWarnings("empty-statement")
    private void prepareUpload(HttpServletRequest request, RunResult result,
                    HashSet<String> uploadedRuns, HashSet<File> uploadSet)
            throws IOException, ClassNotFoundException {
        String runId = result.runId.toString();
        StringBuilder formattedTags = new StringBuilder();
        File runTagFile = new File(Config.OUT_DIR + runId + "/META-INF/tags");
        String tags = request.getParameter(runId + "_tags").trim();
        TagEngine te = TagEngine.getInstance();
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
            
            boolean tagsChanged = true;
            if (result.tags == null && result.tags.length == 0) {
                if (tags == null || "".equals(tags)) {
                    tagsChanged = false;
                }
            }

            if (tagsChanged) {
                te.add(runId, result.tags);
                te.save();
            }
        } else {
            runTagFile.delete();
            result.tags = new String[1];
            result.tags[0] = "&nbsp;";
            te.add(runId, new String[0]);
            te.save();
        }
        result.description = request.getParameter(runId + "_description");
        editXML(result);
        uploadedRuns.add(runId);
        uploadSet.add(jarUpRun(runId));
    }

    /**
     * Edit run.xml file.
     * @param result The run result object to edit
     */
    public static void editXML(RunResult result){
        try {
            File resultDir = result.runId.getResultDir();
            String shortName = result.runId.getBenchName();
            BenchmarkDescription desc = BenchmarkDescription.readDescription(
                                        shortName, resultDir.getAbsolutePath());
            String paramFileName = resultDir.getAbsolutePath() + File.separator
                                                          + desc.configFileName;
            ParamRepository param = new ParamRepository(paramFileName, false);
            param.setParameter("fa:runConfig/fh:description",
                                                            result.description);
            param.save();
        } catch (Exception ex) {
            Logger.getLogger(ResultAction.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Jar up the run by runId.
     * @param runId
     * @return File
     * @throws IOException 
     */
    private File jarUpRun(String runId) throws IOException{
    	String[] files = new File(Config.OUT_DIR, runId).list();
        File jarFile = new File(Config.TMP_DIR, runId + ".jar");
        jar(Config.OUT_DIR + runId, files, jarFile.getAbsolutePath());
        return jarFile;      
        //return new File(Config.TMP_DIR, "test.jar");
    }   

    /**
     * This method is responsible for uploading the runs to repository.
     * @param uploadSet
     * @param replaceSet
     * @return HashSet
     * @throws java.io.IOException
     */
    public static HashSet<String> uploadRuns(String[] runIds,
                                             HashSet<File> uploadSet,
                                             HashSet<String> replaceSet)
            throws IOException {
        // 3. Upload the run
        HashSet<String> duplicates = new HashSet<String>();

        // Prepare run id set for cross checking.
        HashSet<String> runIdSet = new HashSet<String>(runIds.length);
        for (String runId : runIds) {
            runIdSet.add(runId);
        }

        // Prepare the parts for the request.
        ArrayList<Part> params = new ArrayList<Part>();
        params.add(new StringPart("host", Config.FABAN_HOST));
        for (String replaceId : replaceSet) {
            params.add(new StringPart("replace", replaceId));
        }
        for (File jarFile : uploadSet) {
            params.add(new FilePart("jarfile", jarFile));
        }
        Part[] parts = new Part[params.size()];
        parts = params.toArray(parts);

        // Send the request for each reposotory.
        for (URL repository : Config.repositoryURLs) {
            URL repos = new URL(repository, "/controller/uploader/upload_runs");
            PostMethod post = new PostMethod(repos.toString());
            post.setRequestEntity(
                    new MultipartRequestEntity(parts, post.getParams()));

            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().
                    setConnectionTimeout(5000);
            int status = client.executeMethod(post);

            if (status == HttpStatus.SC_FORBIDDEN)
                logger.warning("Server denied permission to upload run !");
            else if (status == HttpStatus.SC_NOT_ACCEPTABLE)
                logger.warning("Run origin error!");
            else if (status != HttpStatus.SC_CREATED)
                logger.warning("Server responded with status code " +
                        status + ". Status code 201 (SC_CREATED) expected.");
            for (File jarFile : uploadSet) {
                jarFile.delete();
            }

            String response = post.getResponseBodyAsString();

            if (status == HttpStatus.SC_CREATED) {

                StringTokenizer t = new StringTokenizer(response.trim(),"\n");
                while (t.hasMoreTokens()) {
                    String duplicateRun = t.nextToken().trim();
                    if (duplicateRun.length() > 0)
                        duplicates.add(duplicateRun.trim());
                }

                for (Iterator<String> iter = duplicates.iterator(); iter.hasNext();) {
                    String runId = iter.next();
                    if (!runIdSet.contains(runId)) {
                        logger.warning("Unexpected archive response from " +
                                repos + ": "  + runId);
                        iter.remove();
                    }
                }
            } else {
                logger.warning("Message from repository: " + response);
            }
        }
        return duplicates;
    }
}
