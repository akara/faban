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
 * $Id: RunResult.java,v 1.7 2009/07/21 22:54:48 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.common.SortDirection;
import com.sun.faban.common.SortableTableModel;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.engine.RunQ;
import com.sun.faban.harness.security.AccessController;
import com.sun.faban.harness.util.FileHelper;
import com.sun.faban.harness.util.XMLReader;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Result class scans through the output directory and compiles a list of run
 * results. Enhance this to enable the user to sort it using different options.
 */
public class RunResult {

    /**
     * Status and result values when they are not available. The
     * 6 character string with all lower case 'z' is after any other
     * possible status string or result string in the collation sequence.
     */
    private static final String NOT_AVAILABLE = "zzzzzz";
    private static final String[] EMPTY_ARRAY = new String[0];
    private static final int FEED_LIMIT = 25;

    private static Logger logger = Logger.getLogger(RunResult.class.getName());
    private static ConcurrentHashMap<String, RunResult> resultCache =
            new ConcurrentHashMap<String, RunResult>(1024);

    // The format in the result file
    private SimpleDateFormat parseFormat = new SimpleDateFormat(
                              "EEE MMM dd HH:mm:ss z yyyy");

    private static SimpleDateFormat dateFormatOrig = new SimpleDateFormat(
                                    "MM/dd/yy EEE'&#160;'HH:mm:ss'&#160;'z");

    private long modTime = 0;
    public RunId runId;
    public String description;
    public String result;
    public String resultLink;
    private String scaleName;
    public String scale;
    private String scaleUnit;
    public ResultField<Double> metric = new ResultField<Double>();
    private String metricUnit;
    public String status;
    public String logLink;
    public Date dateTime;
    public String submitter;
    public String[] tags;

    /**
     * This getInstance0 assumes you've checked the run id is found. It will
     * return an instance. If the run id is not there, the behavior is not
     * defined. This is why it is a private interface.
     * @param runId The run id of the run
     * @return A result instance.
     */
    private static RunResult getInstance0(RunId runId) {
        RunResult result = new RunResult(runId);
        RunResult oldResult = resultCache.putIfAbsent(runId.toString(), result);
        if (oldResult != null)
            result = oldResult;
        try {
            result.refresh();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, runId.toString() +
                    ": Error reading run.", e);
            result.description = "Error reading this result";
        }
        return result;
    }

    /**
     * Obtains the result of a certain run id.
     * @param runId The run id to query
     * @return An instance of the result, or null if such runId is not found
     */
    public static RunResult getInstance(RunId runId) {
        if (runId.getResultDir().isDirectory())
            return getInstance0(runId);
        return null;
    }

    private RunResult(RunId runId) {
        this.runId = runId;
    }

    private synchronized void refresh() {

        File resultDir = runId.getResultDir();

        long modTime = resultDir.lastModified();
        if (modTime <= this.modTime) {
            logger.finer("Run " + runId + " already cached.");
            return;
        }
        logger.finer("Fetching run " + runId + " from disk.");
        this.modTime = modTime;

        String shortName = runId.getBenchName();        

        String resultFilePath;
        String configFileName;
        BenchmarkDescription desc = BenchmarkDescription.
                readDescription(shortName, resultDir.getAbsolutePath());
        if (desc == null) {
            Map<String, BenchmarkDescription> benchMap =
                    BenchmarkDescription.getBenchDirMap();
            desc = (BenchmarkDescription) benchMap.get(shortName);
        }

        if (desc == null) {
            logger.warning(runId.toString() + ": Cannot find benchmark " +
                    "description in result and benchmark not deployed, " +
                    "trying default values");
            
            // Assigning default values;
            resultFilePath = "summary.xml";
            configFileName = "run.xml";
        } else {
            resultFilePath = desc.resultFilePath;
            configFileName = desc.configFileName;
            scaleName = desc.scaleName;
            scaleUnit = desc.scaleUnit;
            metricUnit = desc.metric;
        }

        String[] statusFileContent = readStatus(runId.toString());

        // run result and HREF to the summary or log file.
        File resultFile = new File(resultDir, "summary.xml");
        if (resultFile.exists() && resultFile.length() > 0) {
            result = "PASSED";
            resultLink = "/resultframe.jsp?runId=" +
                    this.runId + "&result=" +
                    resultFilePath;

            //Use the XMLReader and locate the <passed> elements
            XMLReader reader = new XMLReader(resultFile.
                    getAbsolutePath());

            // Obtain the metric before we break pass/fail.
            metric.text = reader.getValue("benchSummary/metric");
            if (metric.text != null && metric.text.length() > 0)
                metric.value = new Double(metric.text);
            try {
                dateTime = parseFormat.parse(
                        reader.getValue("benchSummary/endTime"));
            } catch (ParseException e) {
                // Do nothing. result.dateTime will be null and
                // later we'll use the param file's mod dateTime
                // for this field instead.
            }

            List<String> passedList = reader.getValues("passed");
            for(String passed : passedList) {
                if(passed.toUpperCase().indexOf("FALSE") != -1) {
                    result = "FAILED";
                    break;
                }
            }
        }

        status = statusFileContent[0];
        StringBuilder b = new StringBuilder(
            "/resultframe.jsp?runId=");
        b.append(this.runId);
        b.append("&result=");
        b.append(resultFilePath);
        b.append("&show=logs");
        logLink = b.toString();

        if (dateTime == null && statusFileContent[1] != null) {
            try {
                dateTime = parseFormat.parse(statusFileContent[1]);
            } catch (ParseException e) {
                // Do nothing. result.dateTime will be null and
                // later we'll use the param file's mod dateTime
                // for this field instead.
            }
        }

        String paramFileName = resultDir.getAbsolutePath() +
                File.separator + configFileName;
        File paramFile = new File(paramFileName);

        if (paramFile.isFile()) {

            // Compatible with old versions of Config.RESULT_INFO
            // Old version does not have timestamp in RESULT_INFO
            // So we need to establish it from the paramFile timestamp.
            // This block may be removed in future.
            if (dateTime == null) {
                dateTime = new Date(paramFile.lastModified());
            }
            // End compatibility block

            ParamRepository par = new ParamRepository(paramFileName, false);
            description = par.getParameter("fa:runConfig/fh:description");
            scale = par.getParameter("fa:runConfig/fa:scale");
        } else {
            logger.warning(runId.toString() +
                    ": Parameter file invalid or non-existent.");
        }
            
        File submitterFile = new File(resultDir, "META-INF" + File.separator +
                                      "submitter");
        if (submitterFile.exists())
            try {
                submitter = FileHelper.readStringFromFile(submitterFile).trim();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error reading submitter file for " +
                        "run " + runId, e);
            }

        File tagsFile = new File(resultDir, "META-INF" + File.separator +
                                            "tags");
        if (tagsFile.exists()) {
            try {
                tags = FileHelper.readArrayContentFromFile(tagsFile);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error reading tags file for " +
                        "run " + runId, e);
            }
        }else{
            tags = new String[1];
            tags[0] = "&nbsp";
        }
    }

    /**
     * Obtains the current status of a run.
     * @param runId The id of the run in question
     * @return The current status string or "UNKNOWN" in error cases
     */
    public static String[] readStatus(String runId) {
        char[] cBuf = null;
        String[] status = new String[2];
        int length = -1;
        try {
            FileReader reader = new FileReader(Config.OUT_DIR + runId +
                    '/' + Config.RESULT_INFO);
            cBuf = new char[128];
            length = reader.read(cBuf);
            reader.close();
        } catch (IOException e) {
            // Do nothing, length = -1.
        }

        if (length == -1) {
            status[0] = "UNKNOWN";
        } else {
            String content = new String(cBuf, 0, length);
            int idx = content.indexOf('\t');
            if (idx != -1) {
                status[0] = content.substring(0, idx).trim();
                status[1] = content.substring(++idx);
            } else {
                status[0] = content.trim();
            }
        }
        return status;
    }

    /**
     * Obtains the status of a given run. If it is queued, returns "QUEUED"
     * @param runId The run id to obtain status
     * @return The status string, or null if run cannot be found.
     */
    public static String getStatus(RunId runId) {
        String status = null;
        RunResult result = getInstance(runId);
        if (result == null) {
            // Perhaps the runId is still in the pending queue.
            String[] pending = RunQ.listPending();
            for (String run : pending)
                if (run.equals(runId.toString())) {
                    status = "QUEUED";
                    break;
                }
            if (status == null) {
                return null;
            }
        } else {
            status = result.status;
        }
        if (status == null) // Worse come to worse, the run dir is in bad shape.
            status = "UNKNOWN";
        return status;
    }


    /**
     * Returns the SortableTableModel with tag search.
     * @param user
     * @param tags
     * @param column
     * @param sortDirection
     * @return SortableTableModel
     * @throws java.io.IOException
     */
    public static SortableTableModel getResultTable(Subject user, String tags,
            int column, String sortDirection)
            throws IOException {
        TagEngine tagEngine;
        try {
            tagEngine = TagEngine.getInstance();
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "Cannot find tag engine class", ex);
            throw new IOException("Cannot find tag engine class", ex);
        }
        Set<String> runIds = tagEngine.search(tags);
        ArrayList<RunResult> resultList =
                new ArrayList<RunResult>(runIds.size());
        for (String runid : runIds) {
            try {
                RunId runId = new RunId(runid);
                if (!AccessController.isViewAllowed(user, runid))
                    continue;
                RunResult res = getInstance(runId);
                if (res == null){
                    try{                        
                        tagEngine.removeRun(runid);
                        tagEngine.save();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Cannot remove run " + runid, e);
                    }
                }
                resultList.add(res);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot read result dir " + runid, e);
            }
        }
        return generateTable(resultList, column, sortDirection);
    }

    /**
     * Returns the SortableTableModel.
     * @param user
     * @param column
     * @param sortDirection
     * @return SortableTableModel
     */
    public static SortableTableModel getResultTable(Subject user, int column, String sortDirection) {

        File[] dirs = new File(Config.OUT_DIR).listFiles();
        ArrayList<RunResult> runs = new ArrayList<RunResult>(dirs.length);
        for (File runDir : dirs) {
            if (!runDir.isDirectory()) {
                continue;
            }
            String runIdS = runDir.getName();
            try {
                if (!AccessController.isViewAllowed(user, runIdS)) {
                    continue;
                }
                RunId runId = new RunId(runIdS);
                RunResult result = getInstance0(runId);
                if (result == null) {
                    continue;
                }
                runs.add(result);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot read result dir " + runIdS, 
                                                                            e);
            }
        }
        return generateTable(runs, column, sortDirection);
    }

    /**
     * Generates the table.
     * @param runs
     * @param column
     * @param sortDirection
     * @return SortableTableModel
     */
    static SortableTableModel generateTable(List<RunResult> runs, int column,
            String sortDirection) {

        HashSet<String> scaleNames = new HashSet<String>();
        HashSet<String> scaleUnits = new HashSet<String>();
        HashSet<String> metricUnits = new HashSet<String>();

        // 1. Scan result for scale names, units and metric units
        RunResult result0 = null;
        for (RunResult result : runs) {
                if (result.scaleName != null)
                    scaleNames.add(result.scaleName);
                else
                    scaleNames.add("Scale");
                if (result.scaleUnit != null)
                    scaleUnits.add(result.scaleUnit);

                if (result.metricUnit != null)
                    metricUnits.add(result.metricUnit);
                else
                    metricUnits.add("Metric");

                if (result0 == null)
                    result0 = result;
        }
        
        if (result0 == null) // No rows!
            return null;

        // 2. Generate table header
        SortableTableModel table = new SortableTableModel(8);
        String sort = "<img src=/img/sort_asc.gif></img>";
        if(sortDirection.equals("DESCENDING")){
             sort = "<img src=\"/img/sort_asc.gif\" border=\"0\"></img>";
        }else if(sortDirection.equals("ASCENDING")){
             sort = "<img src=\"/img/sort_desc.gif\" border=\"0\"></img>";
        }
        if(column == 0)
            table.setHeader(0, "RunID " + sort);
        else
            table.setHeader(0, "RunID");

        if(column == 1)
            table.setHeader(1, "Description " + sort);
        else
            table.setHeader(1, "Description");

        if(column == 2)
            table.setHeader(2, "Result " + sort);
        else
            table.setHeader(2, "Result");

        boolean singleScale = false;
        if (scaleNames.size() == 1 && scaleUnits.size() == 1) {
            singleScale = true;
            if (result0.scaleName.length() > 0 &&
                    result0.scaleUnit.length() > 0){
                table.setHeader(3, result0.scaleName + " (" +
                        result0.scaleUnit + ')');
            }else if (result0.scaleName.length() > 0){
                table.setHeader(3, result0.scaleName);
            }else if (result0.scaleUnit.length() > 0){
                table.setHeader(3, result0.scaleUnit);
            }else{
                if(column == 3)
                    table.setHeader(3, "Scale " + sort);
                else
                    table.setHeader(3, "Scale");
            }

        } else {
            if(column == 3)
                table.setHeader(3, "Scale " + sort);
            else
                table.setHeader(3, "Scale");
        }

        boolean singleMetric = false;
        if (metricUnits.size() == 1) {
            singleMetric = true;
            if (result0.metricUnit.length() > 0){
                table.setHeader(4, result0.metricUnit);
            }else{
                if(column == 4)
                    table.setHeader(4, "Metric " + sort);
                else
                    table.setHeader(4, "Metric");
            }
        } else {
            if(column == 4)
                table.setHeader(4, "Metric " + sort);
            else
                table.setHeader(4, "Metric");
        }

        //table.setHeader(5, "Status");
        if(column == 5)
            table.setHeader(5, "Date/Time " + sort);
        else
            table.setHeader(5, "Date/Time");

        if(column == 6)
            table.setHeader(6, "Submitter " + sort);
        else
            table.setHeader(6, "Submitter");

        if(column == 7)
            table.setHeader(7, "Tags " + sort);
        else
            table.setHeader(7, "Tags");

        // 3. Generate table rows.
        StringBuilder b = new StringBuilder();
        // The output format.
        SimpleDateFormat dateFormat = (SimpleDateFormat) dateFormatOrig.clone();
        for (RunResult result : runs) {
            //int idx = table.newRow();
            Comparable[] row = table.newRow();
            row[0] = result.runId;
            if (result.description == null || result.description.length() == 0)
                row[1] = "UNAVAILABLE";
            else
                row[1] = result.description;

            ResultField<String> r = new ResultField<String>();
            if (result.result != null) {
                r.value = result.result;
                if (result.resultLink != null){
                    if(result.result.equals("PASSED"))
                        r.text = "<a href=\""+ result.resultLink + "\"><img onmouseover=\"showtip('" + result.result.toString() + "')\" onmouseout=\"hideddrivetip()\" class=\"icon\"; src='/img/passed.png'></img></a>";
                    else if(result.result.equals("FAILED"))
                        r.text = "<a href=\""+ result.resultLink + "\"><img onmouseover=\"showtip('" + result.result.toString() + "')\" onmouseout=\"hideddrivetip()\" class=\"icon\"; src='/img/failed.png'></img></a>";
                    
                }
            }else if(result.status != null){
                r.value = result.status;
                if (result.logLink != null){
                    if(result.status.equals("FAILED"))
                        r.text = "<a href=\""+ result.logLink + "\"><img onmouseover=\"showtip('" + result.status.toString() + "')\" onmouseout=\"hideddrivetip()\" class=\"icon\"; src='/img/incomplete.png'></img></a>";
                    else if(result.status.equals("KILLED"))
                        r.text = "<a href=\""+ result.logLink + "\"><img onmouseover=\"showtip('" + result.status.toString() + "')\" onmouseout=\"hideddrivetip()\" class=\"icon\"; src='/img/killed.png'></img></a>";
                    else if(result.status.equals("RECEIVED"))
                        r.text = "<a href=\""+ result.logLink + "\"><img onmouseover=\"showtip('" + result.status.toString() + "')\" onmouseout=\"hideddrivetip()\" class=\"icon\"; src='/img/received.png'></img></a>";
                    else if(result.status.equals("STARTED"))
                        r.text = "<a href=\""+ result.logLink + "\"><img onmouseover=\"showtip('" + result.status.toString() + "')\" onmouseout=\"hideddrivetip()\" class=\"icon\"; src='/img/running.png'></img></a>";
                    else if(result.status.equals("COMPLETED")){
                        r.text = "<a href=\""+ result.logLink + "\"><img onmouseover=\"showtip('" + result.status.toString() + "')\" onmouseout=\"hideddrivetip()\" class=\"icon\"; src='/img/failed.png'></img></a>";
                    }
                }
            } else {
                r.value = NOT_AVAILABLE;
                r.text = "<a href=\"/controller/results/list\"><img onmouseover=\"showtip('unknown')\" onmouseout=\"hideddrivetip()\" class=\"icon\"; src='/img/unknown.png'></img></a>";
            }
            row[2] = r;

            ResultField<Integer> scale = new ResultField<Integer>();
            if (result.scale == null || result.scale.length() < 1) {
                scale.text = "N/A";
                scale.value = Integer.MIN_VALUE;
            } else if (singleScale) {
                scale.text = result.scale;
                scale.value = new Integer(result.scale);
            } else {
                b.append(result.scale);
                if (result.scaleName.length() > 0)
                    b.append(' ').append(result.scaleName).append(' ');
                if (result.scaleUnit.length() > 0)
                    b.append(' ').append(result.scaleUnit);
                scale.text = b.toString();
                scale.value = new Integer(result.scale);
                b.setLength(0);
            }
            row[3] = scale;


            ResultField<Double> metric = new ResultField<Double>();
            if (result.metric.text == null) {
                metric.text = "N/A";
                metric.value = -1d;
            } else if (singleMetric) {
                metric.text = result.metric.text;
                metric.value = result.metric.value;
            } else {
                b.append(result.metric);
                if (result.metricUnit.length() > 0)
                    b.append(' ').append(result.metricUnit);
                metric.text = b.toString();
                metric.value = result.metric.value;
                b.setLength(0);
            }
            row[4] = metric;

            /*ResultField<String> status = new ResultField<String>();
            if (result.status != null) {
                status.value = result.status;
                if (result.logLink != null)
                    status.text = "<a href=\""+ result.logLink + "\">" +
                            result.status + "</a>";
                else
                    status.text = result.status;
            } else {
                status.value = NOT_AVAILABLE;
                status.text = "UNKNOWN";
            }
            row[5] = status;*/

            ResultField<Long> dateTime = new ResultField<Long>();
            if (result.dateTime != null) {
                dateTime.text = dateFormat.format(result.dateTime);
                dateTime.value = result.dateTime.getTime();
            } else {
                dateTime.text = "N/A";
                dateTime.value = 0l;
            }
            row[5] = dateTime;

            if (result.submitter != null)
                row[6] = result.submitter;
            else
                row[6] = "&nbsp;";


            if (result.tags != null && result.tags.length > 0) {
                for (String tag : result.tags) {
                    b.append(tag).append(' ');
                }
                b.setLength(b.length() - 1);
                row[7] = b.toString();
                b.setLength(0);
            } else {
                row[7] = "&nbsp;";
            }
        }

        SortDirection enumValForDirection = table.getSortDirection().valueOf(sortDirection);
        table.sort(column, enumValForDirection);
        return table;
    }

    public static class ResultField<T extends Comparable>
            implements Comparable {

        String text;
        T value;

        public int compareTo(Object o) {
            ResultField other = (ResultField) o;
            return value.compareTo(other.value);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static class FeedRecord {
        public String title;
        public String summary;
        public String id;
        long date;
        public String updated; // Formatted string value of updated.
        public String link;
        public String[] tags;

        FeedRecord(RunId runId, RunResult result) {
            StringBuilder b = new StringBuilder();
            b.append(runId).append(" [").append(result.scale);
            if (result.metric.value != null)
                b.append(", ").append(result.metric.text);

            // If result.result is valid, use it. Otherwise, use result.status.
            if (result.result != null)
                b.append(", ").append(result.result);
            else if (result.status != null)
                b.append(", ").append(result.status);
            
            b.append(']');
            title = b.toString();
            summary = result.description;
            id = runId.toString();

            link = "/controller/results/location/" + runId;

            date = result.dateTime.getTime();
            if (result.tags == null)
                tags = EMPTY_ARRAY;
            else
                tags = result.tags;
        }
    }

    /**
     * Obtains the list of feeds.
     * @param user
     * @return List<FeedRecord>.
     */
    public static List<FeedRecord> getFeeds(Subject user) {

        File[] dirs = new File(Config.OUT_DIR).listFiles();
        ArrayList<FeedRecord> feedList = new ArrayList<FeedRecord>(dirs.length);
        RunResult result0 = null;
        for (File runDir : dirs) {
            if (!runDir.isDirectory())
                continue;
            String runIdS = runDir.getName();
            try {
                RunId runId = new RunId(runIdS);
                if (!AccessController.isViewAllowed(user, runIdS))
                    continue;
                result0 = getInstance0(runId);
                FeedRecord feedRecord = new FeedRecord(runId, result0);
                feedList.add(feedRecord);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot read result dir " + runIdS,
                                                                            e);
            }
        }
        return sortAndLimit(feedList);
    }

    /**
     * Obtains the list of feeds based on tags.
     * @param user
     * @param tags
     * @return List<FeedRecord>
     * @throws java.io.IOException
     */
    public static List<FeedRecord> getFeeds(Subject user, String[] tags) 
            throws IOException {

        TagEngine tagEngine;
        try {
            tagEngine = TagEngine.getInstance();
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "Cannot find tag engine class", ex);
            throw new IOException("Cannot find tag engine class", ex);
        }
        Set<String> runIds = tagEngine.search(tags);
        ArrayList<FeedRecord> feedList =
                                new ArrayList<FeedRecord>(runIds.size());
        RunResult res = null;
        boolean runRemoved = false;
        for (String runid : runIds) {
            try {
                RunId runId = new RunId(runid);
                if (!AccessController.isViewAllowed(user, runid))
                    continue;
                res = getInstance(runId);
                if (res == null){
                    tagEngine.removeRun(runid);
                    runRemoved = true;
                }
                FeedRecord feedRecord = new FeedRecord(runId, res);
                feedList.add(feedRecord);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot read result dir " + runid, e);
            }
        }
        if (runRemoved)
            tagEngine.save();
        return sortAndLimit(feedList);
    }

    private static List<FeedRecord> sortAndLimit(List<FeedRecord> feedList) {

        // Sort the list, newest first.
        Collections.sort(feedList, new Comparator<FeedRecord>() {
            public int compare(FeedRecord a, FeedRecord b) {
                if (a.date > b.date)
                    return -1;
                else if (a.date < b.date)
                    return 1;
                else
                    return 0;
            }
        });
        
        // Then limit to FEED_LIMIT (25) items.
        if (feedList.size() > FEED_LIMIT)
            return feedList.subList(0, FEED_LIMIT);
        return feedList;
    }
}
