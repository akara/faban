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

import com.sun.faban.common.SortDirection;
import com.sun.faban.common.SortableTableModel;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.engine.RunQ;
import com.sun.faban.harness.security.AccessController;
import com.sun.faban.harness.webclient.RunResult.FeedRecord;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controller handling actions from the result list screen.
 * @author akara
 */
public class Results {
    private static SimpleDateFormat formatOrig =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * Obtains the resultlist table.
     * @param req
     * @param resp
     * @return String
     * @throws java.io.IOException
     */
    public String list(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        int col = -1;
        UserEnv usrEnv = getUserEnv(req);
        String tag = req.getParameter("inputtag");
        String sortColumn = req.getParameter("sortColumn");
        String sortDirection = req.getParameter("sortDirection");
        if (sortColumn != null && !"".equals(sortColumn)) {
            col = Integer.parseInt(sortColumn);
        }
        SortableTableModel resultTable = null;
        boolean tagSearch = false;
        String feedURL = "/controller/results/feed";
        if (tag != null && !"".equals(tag)) {
            tag = tag.trim();
            if (tag.length() > 0) {
                tagSearch = true;
            }
        }
        if (tagSearch) {
            if (col >= 0 && col < 8)
                resultTable = RunResult.getResultTable(usrEnv.getSubject(), tag, col, sortDirection.trim());
            else     
                resultTable = RunResult.getResultTable(usrEnv.getSubject(), tag, 5, "DESCENDING");
            StringTokenizer t = new StringTokenizer(tag, " ,;:");
            StringBuilder b = new StringBuilder(tag.length());
            b.append(feedURL);
            while (t.hasMoreTokens()) {
                b.append('/');
                String tagName = t.nextToken();
                tagName = tagName.replace("/", "+");
                b.append(tagName);
            }
            feedURL = b.toString();
            req.setAttribute("tagInSearch", tag);
        } else if (col >= 0 && col < 8) {
            resultTable = RunResult.getResultTable(usrEnv.getSubject(), col, sortDirection.trim());
        } else {
            resultTable = RunResult.getResultTable(usrEnv.getSubject(), 5, "DESCENDING");
        }

        req.setAttribute("feedURL", feedURL );
        req.setAttribute("table.model", resultTable);
        return "/resultlist.jsp";
    }

    /**
     * Obtains the feed list.
     * @param req
     * @param resp
     * @throws java.io.IOException
     */
    public void feed(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        UserEnv usrEnv = getUserEnv(req);
        String[] restRequest = (String[]) req.getAttribute("rest.request");
        List<FeedRecord> itemList;
        if (restRequest != null) {
            itemList = RunResult.getFeeds(usrEnv.getSubject(), restRequest);
        } else {
            itemList = RunResult.getFeeds(usrEnv.getSubject());
        }

        // The first result is the most up-to-date.
        SimpleDateFormat format = (SimpleDateFormat) formatOrig.clone();
        for (FeedRecord feedItem : itemList) {
            feedItem.updated = format.format(new Date(feedItem.date));
        }
        req.setAttribute("feed.model", itemList);
        req.setAttribute("request.url", req.getRequestURL());

        String updated;
        if (itemList.size() > 0) {
            FeedRecord item0 = itemList.get(0);
            updated = item0.updated;
        } else {
            updated = format.format(new Date(0));
        }
        req.setAttribute("feed.updated", updated);
    }

    /**
     * Obtains the run information.
     * @param req
     * @param resp
     * @return String
     * @throws java.io.IOException
     */
    public String getRunInfo(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        UserEnv usrEnv = getUserEnv(req);
        String runId = req.getParameter("runId");
        RunResult result = RunResult.getInstance(new RunId(runId));
        String[] header = {"RunId","Description","Result","Scale","Metric","Status","Date/Time","Submitter","Tags"};
        String[] runInfo = new String[9];
        runInfo[0] = result.runId.toString();
        if (result.description == null || result.description.length() == 0)
                runInfo[1] = "UNAVAILABLE";
        else
                runInfo[1] = result.description;
        if (result.result != null) {              
                runInfo[2] = result.result;
        } else {                
                runInfo[2] = "N/A";
        }

            if (result.scale == null || result.scale.length() < 1) {
                runInfo[3] = "N/A";
            } else {
                runInfo[3] = result.scale;
            } 

            if (result.metric.text == null) {
                runInfo[4] = "N/A";
            } else {
                runInfo[4] = result.metric.text;
            } 
            if (result.status != null) {
                runInfo[5] = result.status;
            } else {               
                runInfo[5] = "UNKNOWN";
            }

            if (result.dateTime != null) {
                runInfo[6] = formatOrig.format(result.dateTime);
            } else {
                runInfo[6] = "N/A";
            }
            
            if (result.submitter != null)
                runInfo[7] = result.submitter;
            else
                runInfo[7] = "&nbsp;";


            if (result.tags != null && result.tags.length > 0) {
                StringBuilder b = new StringBuilder();
                for (String tag : result.tags) {
                    b.append(tag).append(' ');
                }
                b.setLength(b.length() - 1);
                runInfo[8] = b.toString();
                b.setLength(0);
            } else {
                runInfo[8] = "&nbsp;";
            }
        req.setAttribute("header", header);
        req.setAttribute("runinfo", runInfo);
        if (!AccessController.isWriteAllowed(usrEnv.getSubject(), runId))
            return "/runinfo_readonly.jsp";
        else
            return "/runinfo.jsp";
    }

    /**
     * .
     * @param req
     * @param resp
     * @throws java.io.IOException
     */
    public void location(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String[] restRequest = (String[]) req.getAttribute("rest.request");
        String runId = restRequest[0];
        UserEnv usrEnv = getUserEnv(req);

        RunResult result = RunResult.getInstance(new RunId(runId));
        if (result == null) {
            // Perhaps the runId is still in the pending queue.
            boolean found = false;
            String[] pending = RunQ.listPending();
            for (String run : pending) {
                if (run.equals(runId)) {
                    resp.sendRedirect("/pending-runs.jsp");
                    found = true;
                    break;
                }
            }
            if (!found) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "Run " + runId + " not found");
            }
        } else if (!AccessController.isViewAllowed(usrEnv.getSubject(), runId)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                           "Run " + runId + " not found");
        } else if ("COMPLETED".equals(result.status)){
            resp.sendRedirect(result.resultLink);
        } else {
            resp.sendRedirect(result.logLink);
        }
    }

    private UserEnv getUserEnv(HttpServletRequest req) {
        HttpSession session = req.getSession();
        UserEnv usrEnv = (UserEnv) session.getAttribute("usrEnv");
        if (usrEnv == null) {
            usrEnv = new UserEnv();
            session.setAttribute("usrEnv", usrEnv);
        }
        return usrEnv;
    }
}
