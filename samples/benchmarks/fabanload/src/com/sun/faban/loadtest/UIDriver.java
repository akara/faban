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
package com.sun.faban.loadtest;

import com.sun.faban.driver.*;
import com.sun.faban.driver.util.Random;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/** The driver to drive load against the Faban harness. */
@BenchmarkDefinition (
    name    = "Faban Harness Workload",
    version = "0.1"
)
@BenchmarkDriver (
    name           = "UIDriver",
    threadPerScale    = 1
)
@MatrixMix (
    operations = {"Home Page", "List Results", "View Result",
                  "Submit Run", "Pending Runs"},
    mix = { @Row({  0, 80,  0,  5, 15 }),
            @Row({  5, 10, 70,  5, 10 }),
            @Row({  5, 80,  0,  5, 10 }),
            @Row({  0, 80,  0,  0, 20 }),
            @Row({ 20, 60,  0, 20,  0 }) },
    deviation = 2
)
@NegativeExponential (
    cycleType = CycleType.CYCLETIME,
    cycleMean = 5000,
    cycleDeviation = 2
)
public class UIDriver {

    /** The driver context for this instance. */
    private DriverContext ctx;
    private HttpTransport http;
    boolean realSubmit;
    Logger logger;
    Random random;
    String baseURL;
    String[] homePageGets;
    String resultList;
    String[] submitRuns;
    String pendingList;
    String deleteRuns;
    String[] runIds;
    String runId;

    /**
     * Constructs a UIDriver instance.
     * @throws XPathExpressionException Invalid XPath
     */
    public UIDriver() throws XPathExpressionException {
        ctx = DriverContext.getContext();
        http = HttpTransport.newInstance();
        logger = ctx.getLogger();
        random = ctx.getRandom();
        String host = ctx.getXPathValue("/fabanLoad/serverConfig/fa:hostConfig/fa:host");
        String port = ctx.getXPathValue("/fabanLoad/serverConfig/port");
        realSubmit = Boolean.parseBoolean(ctx.getProperty("realSubmit"));
        baseURL = "http://" + host + ':' + port + '/';
        homePageGets = new String[4];
        homePageGets[0] = baseURL;
        homePageGets[1] = baseURL + "banner.jsp";
        homePageGets[2] = baseURL + "welcome.jsp";
        homePageGets[3] = baseURL + "menu.jsp";
        resultList = baseURL + "resultlist.jsp";
        submitRuns = new String[3];
        submitRuns[0] = baseURL + "selectprofile.jsp";
        submitRuns[1] = baseURL + "new-run.jsp";
        submitRuns[2] = baseURL + "benchmarks/fabanload/config.xhtml";
        pendingList = baseURL + "pending-runs.jsp";
        deleteRuns = baseURL + "delete-runs.jsp";
    }

    /**
     * Clears the pending runs.
     * @throws IOException Error clearing the pending runs.
     */
    @OnceAfter public void cleanPendingRuns() throws IOException {
        pendingRuns();
    }

    /**
     * Access the home page.
     * @throws IOException Error accessing the home page.
     */
    @BenchmarkOperation (
        name    = "Home Page",
        max90th = 2,
        timing  = Timing.AUTO
    )
    public void homePage() throws IOException {
        for (String get : homePageGets) {
            logger.finest("Accessing " + get);
            http.readURL(get);
        }
    }

    /**
     * Tests listing the results.
     * @throws IOException Error obtaining the result list.
     */
    @BenchmarkOperation (
        name    = "List Results",
        max90th = 2,
        timing  = Timing.AUTO
    )
    public void listResults() throws IOException {

        logger.finest("Accessing " + resultList);
        if (runIds == null) { // Just parse the runIds once
            http.fetchURL(resultList);
            StringBuilder response = http.getResponseBuffer();
            int tableIdx = response.indexOf("<table");
            if (tableIdx != -1)
                runIds = parseResultTable(response, tableIdx);
        } else { // RunIds won't change much over the run.
            http.readURL(resultList);
        }
        if (runIds != null) {
            int idx = random.random(0, runIds.length - 1);
            runId = runIds[idx];
        }
    }

    /**
     * Test viewing a result.
     * @throws IOException Error viewing the result
     */
    @BenchmarkOperation (
        name    = "View Result",
        max90th = 2,
        timing  = Timing.AUTO
    )
    public void viewResult() throws IOException {
        
        if (runId == null) {
            logger.warning("No results just yet. Try again next run.");
            return;
        }

        http.readURL(baseURL + "resultframe.jsp?runId=" + runId +
                "&result=summary.xml&show=logs");
        http.readURL(baseURL + "LogReader?runId=" + runId);
        http.fetchURL(baseURL + "resultnavigator.jsp?runId=" + runId);
        StringBuilder b = http.getResponseBuffer();

        // Search for summary file
        final String href = "href=\"";
        String summaryPath = null;
        String detailPath = null;

        int endIdx = b.indexOf("\" target=\"display\">Summary&nbsp;Result</a>");
        if (endIdx != -1) {
            int begIdx = b.lastIndexOf(href, endIdx) + href.length();
            summaryPath = b.substring(begIdx, endIdx);
        }

        // Search for detail file
        endIdx = b.indexOf("\" target=\"display\">Detailed&nbsp;Results</a>");
        if (endIdx != -1) {
            int begIdx = b.lastIndexOf(href, endIdx) + href.length();
            detailPath = b.substring(begIdx, endIdx);
        }

        if (summaryPath != null)
            http.readURL(baseURL + summaryPath);

        if (detailPath != null)
            http.readURL(baseURL + detailPath);

        http.readURL(baseURL + "output/" + runId + "/run.xml");
        http.readURL(baseURL + "statsnavigator.jsp?runId=" + runId);        
    }

    /**
     * Test submitting a run.
     * @throws IOException Error submitting run
     */
    @BenchmarkOperation (
        name    = "Submit Run",
        max90th = 2,
        timing  = Timing.AUTO
    )
    public void submitRun() throws IOException {
        http.fetchURL(submitRuns[0]);
        StringBuilder b = http.getResponseBuffer();
        if (-1 == b.indexOf( // Selection required
                "<meta HTTP-EQUIV=REFRESH CONTENT=\"0;URL=new-run.jsp\">"))
            http.readURL(submitRuns[1], "profile=test&profilelist=test&" +
                                        "benchmark=Faban+Harness+Workload");
        else
            http.readURL(submitRuns[1]);
        http.readURL(submitRuns[2]);
        http.readURL(submitRuns[2], "t_trigger-runConfig=Driver&" +
                "d_input-javaHome=%2Fopt%2Fjdk1.6.0&d_input-jvmOptions=" +
                "-Xmx256m+-Xms64m+-XX%3A%2BDisableExplicitGC");
        http.readURL(submitRuns[2], "t_trigger-uiDriver=" +
                "Harness%C2%A0UI%C2%A0Driver&d_input-description=" +
                "Sample+benchmark+run&d_input-agent-host=sucharitakul&" +
                "d_input-scale=100&d_input-agent-tools=vmstat+10&" +
                "d_input-rampUp=60&d_input-steadyState=300&" +
                "d_input-rampDown=30");
        if (realSubmit)
            http.readURL(submitRuns[2], "d_input-driver-agents=1&" +
                    "d_input-driver-statsInterval=30&" +
                    "d_select1-driver-realSubmit=false&" +
                    "d_select1-driver-realSubmit=&" +
                    "d_input-server-host=sucharitakul&" +
                    "d_input-server-port=9980&t_trigger-ok=Ok");
    }

    /**
     * Tests viewing pending runs.
     * @throws IOException Error viewing pending runs
     */
    @BenchmarkOperation (
        name    = "Pending Runs",
        max90th = 2,
        timing  = Timing.AUTO
    )
    public void pendingRuns() throws IOException {
        if (realSubmit) {
            http.fetchURL(pendingList);
            StringBuilder response = http.getResponseBuffer();
            String postList = parsePendingTable(response);
            if (postList != null)
                http.readURL(deleteRuns, postList);
        } else {
            http.readURL(pendingList);
        }

    }

    private String[] parseResultTable(StringBuilder content, int start) {
        ArrayList<String> runIdList = new ArrayList<String>();
        int tableEnd = content.indexOf("</table>", start);

        rowLoop:
        for (;;) {
            int rowIdx = content.indexOf("<tr>", start);
            if (rowIdx > tableEnd || rowIdx == -1)
                break;
            rowIdx += 4;
            int rowEnd = content.indexOf("</tr>", rowIdx);
            start = rowEnd + 5;

            for (int col = 0;;col++) {
                rowIdx = content.indexOf("<td>", rowIdx);
                if (rowIdx > rowEnd)
                    continue rowLoop;
                rowIdx += 4;
                int fieldEnd = content.indexOf("</td>", rowIdx);
                if (col == 1) {
                    String runId = content.substring(rowIdx, fieldEnd);
                    logger.finest("Found runId: " + runId);
                    runIdList.add(runId);
                    continue rowLoop;
                }
            }
        }
        String[] runIds = new String[runIdList.size()];
        return runIdList.toArray(runIds);
    }

    private String parsePendingTable(StringBuilder response) {
        final String search = "<input type=\"checkbox\" " +
                              "name=\"selected-runs\" value=\"";
        final String searchEnd = "\">";
        StringBuilder post = new StringBuilder();
        int idx = 0;
        for (;;) {
            int runIdStart = response.indexOf(search, idx);
            if (runIdStart == -1)
                break;
            else
                runIdStart += search.length();
            int runIdEnd = response.indexOf(searchEnd, runIdStart);
            if (post.length() > 0)
                post.append('&');
            post.append("selected-runs=");
            post.append(response.substring(runIdStart, runIdEnd));
            idx = runIdEnd + searchEnd.length();
        }

        String postString = null;
        if (post.length() > 0)
            postString = post.toString();
        return postString;
    }
}
