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
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.engine.RunQ;
import com.sun.faban.harness.security.AccessController;
import com.sun.faban.harness.util.FileHelper;
import com.sun.faban.harness.util.XMLReader;
import com.sun.faban.harness.webclient.RunResult.FeedRecord;
import com.sun.faban.harness.webclient.RunResult.Target;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
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
     * Add/Edit the target to target.xml file
     * @param req
     * @param resp
     * @return String
     * @throws java.io.IOException
     */
    public String addEditTarget(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, Exception {
        String targetname = req.getParameter("targetname");
        String targetowner = req.getParameter("targetowner");
        String targetmetric = req.getParameter("targetmetric");
        String targetmetricunit = req.getParameter("targetmetricunit");
        String targetcolorred = req.getParameter("targetcolorred");
        String targetcolororange = req.getParameter("targetcolororange");
        String targetcoloryellow = req.getParameter("targetcoloryellow");
        String targettags = req.getParameter("targettags");
        String flag = req.getParameter("flag");
        String add = "<target>\n" +
                            "<name>" + targetname +"</name>\n" +
                            "<owner>" + targetowner +"</owner>\n" +
                            "<metric>" + targetmetric +"</metric>\n" +
                            "<metricunit>" + targetmetricunit +"</metricunit>\n" +
                            "<tags>" + targettags +"</tags>\n" +
                            "<red>" + targetcolorred +"</red>\n" +
                            "<orange>" + targetcolororange +"</orange>\n" +
                            "<yellow>" + targetcoloryellow +"</yellow>\n" +
                    "</target>\n</targets>";

        File targetFile = new File(Config.CONFIG_DIR, "targets.xml");

        if (!targetFile.exists() || targetFile.length() == 0) {
            FileHelper.writeStringToFile(
                    "<?xml version='1.0' encoding='UTF-8'?><targets></targets>",
                    targetFile);
        }

        XMLReader reader = new XMLReader(targetFile.getAbsolutePath());
        boolean found = false;
        if (reader != null) {
            List<String> list = reader.getValues("name");
            if(list.contains(targetname)){
                found = true;
            }
        }
        if (targetFile.exists() && targetFile.length() > 0) {
            if (flag.equals("add")) {
                if(found == false){
                    FileHelper.tokenReplace(targetFile.getAbsolutePath(),
                            "</targets>", add, null);
                    req.setAttribute("answer", "<span style=color:#00cc00; font-size: 14px>Target " + targetname +
                            " added Successfully!!</span>" );
                }else{
                    req.setAttribute("answer", "<span style=color:red; font-size: 14px>Sorry, target " + targetname +
                            " already exists, please try different name.</span>" );
                }
            }else if (flag.equals("edit") || flag.equals("delete")) {
                if (found == true) {
                    NodeList targetnodes = reader.getTopLevelElements();
                    for(int i = 0; i < targetnodes.getLength(); i++){
                        Node e = targetnodes.item(i);
                        if(targetnodes.item(i).getNodeType() ==
                                Node.ELEMENT_NODE ) {
                            NodeList targetchildnodes =
                                    targetnodes.item(i).getChildNodes();
                            for(int j = 0; j < targetchildnodes.getLength(); j++){
                                if(targetchildnodes.item(j).getNodeType() ==
                                        Node.ELEMENT_NODE) {
                                    //Node e = targetnodes.item(i);
                                    String nodename =
                                            targetchildnodes.item(j).getNodeName();
                                    String nodeval = reader.getValue("name", e);
                                    if(nodename == null || !nodename.equals("name")
                                       || nodeval == null || !nodeval.equals(targetname)){
                                        continue;
                                    }else if (nodename != null &&
                                            nodename.equals("name") &&
                                            nodeval != null &&
                                            nodeval.equals(targetname)) {
                                        if(flag.equals("edit")){
                                            editTargetNode(e, reader, targetowner,
                                                    targetmetric, targetmetricunit, targettags,
                                                    targetcolorred, targetcolororange,
                                                    targetcoloryellow);
                                            req.setAttribute("answer", "<span style=color:#00cc00; font-size: 14px>Target " +
                                                    targetname + " edited Successfully!!</span>" );
                                        }else if(flag.equals("delete")){
                                            deleteTargetNode(e, e.getParentNode(),
                                                    reader);
                                            req.setAttribute("answer", "<span style=color:#00cc00; font-size: 14px>Target " +
                                                    targetname + " deleted Successfully!!</span>" );
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }else{
                    req.setAttribute("answer", "<span style=color:red; font-size: 14px>Sorry, could not find target " +
                            targetname + " for editing.</span>" );
                }
            }
        }
        return targetlist(req,resp);
    }

    private void editTargetNode(Node target, XMLReader reader,
            String targetowner, String targetmetric, String targetmetricunit,
            String targettags, String targetcolorred, String targetcolororange,
            String targetcoloryellow ) throws Exception {
        //String paramFileName = Config.CONFIG_DIR + "targets.xml";
        //ParamRepository param = new ParamRepository(paramFileName, false);

        NodeList targetchildnodes = target.getChildNodes();
        for (int j = 0; j < targetchildnodes.getLength(); j++) {
            if (targetchildnodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) targetchildnodes.item(j);
                if (e.getNodeName().equals("owner")){
                    reader.replaceValue(e, targetowner);
                }else if (e.getNodeName().equals("metric")){
                    reader.replaceValue(e, targetmetric);
                }else if (e.getNodeName().equals("metricunit")){
                    reader.replaceValue(e, targetmetricunit);
                }else if (e.getNodeName().equals("tags")){
                    reader.replaceValue(e, targettags);
                }else if (e.getNodeName().equals("red")){
                    reader.replaceValue(e, targetcolorred);
                }else if (e.getNodeName().equals("orange")){
                    reader.replaceValue(e, targetcolororange);
                }else if (e.getNodeName().equals("yellow")){
                    reader.replaceValue(e, targetcoloryellow);
                }

            }
        }
        reader.save(null);
    }

    private void deleteTargetNode(Node target, Node parent, XMLReader reader ) throws Exception {
        reader.deleteNode(target, parent);
        reader.save(null);
    }

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
     * Obtains the targetlist table.
     * @param req
     * @param resp
     * @return String
     * @throws java.io.IOException
     */
    public String targetlist(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        ArrayList<Target> targetList = new ArrayList<Target>();
        int col = -1;
        UserEnv usrEnv = getUserEnv(req);
        String user = usrEnv.getUser();
        String target = req.getParameter("inputtarget");
        String sortColumn = req.getParameter("sortColumn");
        String sortDirection = req.getParameter("sortDirection");
        String viewAll = req.getParameter("viewAll");
        String viewMy = req.getParameter("viewMy");
        if (sortColumn != null && !"".equals(sortColumn)) {
            col = Integer.parseInt(sortColumn);
        }
        SortableTableModel targetTable = null;
        boolean targetSearch = false;
        if (target != null && !"".equals(target)) {
            target = target.trim();
            if (target.length() > 0) {
                targetSearch = true;
            }
        }

        if(targetSearch == true){
            targetList = RunResult.getTargetListForTarget(target);
            req.setAttribute("targetInSearch", target);
            if(targetList.isEmpty()) {
                req.setAttribute("answer", "<span style=color:red; font-size: 14px>There are no targets for search key " + target + "</span>" );
            }
            /*if(targetSearch == true && user == null){
                targetList = RunResult.getTargetListForTarget(target);
                req.setAttribute("targetInSearch", target);
            }else if(targetSearch == true && user != null){
            targetList = RunResult.getTargetListForUserWithTg(target, user);
            if(targetList.isEmpty()){
                viewAll = "null";
                targetList = RunResult.getTargetListForTarget(target);
            }
            req.setAttribute("targetInSearch", target);}*/
        }else if(targetSearch == false && user != null && !viewAll.equals("null")){
            targetList = RunResult.getTargetListForUser(user);
            if(targetList.isEmpty()){
                viewMy = "disable";
                viewAll = "null";
                targetList = RunResult.getTargetList();
                if(targetList.isEmpty()){
                    req.setAttribute("answer", "<span style=color:green; font-size: 14px>Targets " +
                    "are not defined. Currently, the list is empty.</span>" );
                }
            }
        }else{
            viewAll = "null";
            targetList = RunResult.getTargetList();
            if(targetList.isEmpty()){
                    req.setAttribute("answer", "<span style=color:green; font-size: 14px>Targets " +
                    "are not defined. Currently, the list is empty.</span>" );
            }
        }
        if(targetList.size() > 0) {
            if (col >= 0 && col < 5){
                    targetTable = RunResult.generateTargetTable(targetList, col, sortDirection.trim());
            }else{
                    targetTable = RunResult.generateTargetTable(targetList, 0, "DESCENDING");
            }
        }
        req.setAttribute("viewMy", viewMy);
        req.setAttribute("viewAll", viewAll);
        req.setAttribute("table.model", targetTable);
        return "/targetlist.jsp";
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
        } else if (result.logLink.length() > 0) {
            resp.sendRedirect(result.logLink);
        } else {
            resp.sendRedirect("/controller/results/list");
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
