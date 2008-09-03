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
* $Id: ResultAction.java,v 1.1 2008/09/03 05:21:13 akara Exp $
*
* Copyright 2005 Sun Microsystems Inc. All Rights Reserved
*/
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.net.URL;
import java.net.URLConnection;

/**
 * Controller handling actions from the result list screen.
 */
public class ResultAction {

    private static Logger logger = Logger.getLogger(ResultAction.class.getName());

    public String takeAction(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        String process = request.getParameter("process");
        if ("Compare".equals(process))
            return editAnalysis(process, request, response);
        if ("Average".equals(process))
            return editAnalysis(process, request, response);
        if ("Archive".equals(process))
            return editArchive(request, response);
        return null;
    }

    public class EditArchiveModel implements Serializable {
        public String head;
        public String[] runIds;
        public Set<String> duplicates;
        public Result[] results;
    }

    String editArchive(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
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

        model.results = new Result[runIds.length];
        for (int i = 0; i < runIds.length; i++) {
            model.results[i] = Result.getInstance(new RunId(runIds[i]));
        }
        // We use request attributes as not to reflect session state.
        request.setAttribute("editarchive.model", model);
        return "/edit_archive.jsp";
    }

    private Set<String> checkArchivedRuns(String[] runIds) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append("/controller/uploader/check_runs");
        int endPath = b.length();
        for (String runId : runIds)
            b.append("&select=").append(runId);
        b.setCharAt(endPath, '?');
        HashSet<String> existingRuns = new HashSet<String>();
        for (URL repository : Config.repositoryURLs) {
            URL request = new URL(repository, b.toString());
            URLConnection c = request.openConnection();
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    c.getInputStream()));
            String runId = null;
            while ((runId = r.readLine()) != null) {
                existingRuns.add(runId);
            }
        }

        return existingRuns;
    }

    public static class EditAnalysisModel implements Serializable {
        public String head;
        public String type;
        public String runList;
        public String name;
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
                RunAnalyzer.analyze(type, model.runIds, model.name,
                                                    usrEnv.getUser());
            } catch (IOException e) {
                String msg = e.getMessage();
                response.getWriter().println(msg);
                logger.log(Level.SEVERE, msg, e);
                response.sendError(HttpServletResponse.SC_CONFLICT, msg);
                return null;
            }

        if (redirect)
            response.sendRedirect("/analysis/" + model.name + "/index.html");

        return null;
    }

    public void archive(HttpServletRequest request,
                        HttpServletResponse response) {
        // 1. Edit the xml
        // 2. Jar up the runs
        // 3. Send the runs
        // 4. Display results

    }
}

