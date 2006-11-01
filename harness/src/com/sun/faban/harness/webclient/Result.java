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
 * $Id: Result.java,v 1.10 2006/11/01 18:42:15 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.security.AccessController;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.util.XMLReader;
import com.sun.faban.harness.util.FileHelper;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Result class scans through the output directory and compiles a list of run
 * results. Enhance this to enable the user to sort it using different options.
 */
public class Result {

    private static Logger logger = Logger.getLogger(Result.class.getName());
    private static ConcurrentHashMap<String, Result> resultCache =
            new ConcurrentHashMap<String, Result>(1024);

    // The output format
    private SimpleDateFormat dateFormat = new SimpleDateFormat(
                              "EEE'&nbsp;'MM/dd/yy HH:mm:ss'&nbsp;'z");
    // The format in the result file
    private SimpleDateFormat parseFormat = new SimpleDateFormat(
                              "EEE MMM dd HH:mm:ss z yyyy");

    private long modTime = 0;
    public RunId runId;
    public String description;
    public String result;
    private String scaleName;
    public String scale;
    private String scaleUnit;
    public String metric;
    private String metricUnit;
    public String status;
    private long date;
    public String dateTime;
    public String submitter;

    public static Result getInstance(RunId runId) {
        Result result = new Result(runId);
        Result oldResult = resultCache.putIfAbsent(runId.toString(), result);
        if (oldResult != null)
            result = oldResult;
        result.refresh();
        return result;
    }

    private Result() {
    }

    private Result(File resultDir) throws IOException {
        refresh(resultDir);
    }

    private Result(RunId runId) {
        this.runId = runId;
    }

    private void refresh(File resultDir) throws IOException {
        long modTime = resultDir.lastModified();
        if (modTime <= this.modTime) // older than what we have in ram
            return;                  // use the cached version.

        this.modTime = modTime;

        try {
            this.runId = new RunId(resultDir.getName());
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Invalid result directory " + this.runId);
        }
        refresh();
    }

    private synchronized void refresh() {

        String shortName = runId.getBenchName();
        File resultDir = runId.getResultDir();
        BenchmarkDescription desc = BenchmarkDescription.
                readDescription(shortName, resultDir.getAbsolutePath());
        if (desc == null) {
            Map<String, BenchmarkDescription> benchMap =
                    BenchmarkDescription.getBenchDirMap();
            desc = (BenchmarkDescription) benchMap.get(shortName);
        }
        String href = null;

        // run result and HREF to the summary or log file.
        File resultFile = new File(resultDir, "summary.xml");
        if (resultFile.exists() && resultFile.length() > 0) {
            result = "PASSED";
            href = "<a href=\"resultframe.jsp?runId=" +
                    this.runId + "&result=" +
                    desc.resultFilePath + "\">";

            //Use the XMLReader and locate the <passed> elements
            XMLReader reader = new XMLReader(resultFile.
                    getAbsolutePath());

            // Obtain the metric before we break pass/fail.
            metric = reader.getValue("benchSummary/metric");
            try {
                Date runTime = parseFormat.parse(
                        reader.getValue("benchSummary/endTime"));
                dateTime = dateFormat.format(runTime);
                date = runTime.getTime();
            } catch (ParseException e) {
                // Do nothing. result.dateTime will be null and
                // later we'll use the param file's mod dateTime
                // for this field instead.
            }

            List passedList = reader.getValues("passed");
            for(Object passed : passedList) {
                if(((String) passed).toUpperCase().indexOf("FALSE") != -1) {
                    result = "FAILED";
                    break;
                }
            }
        }

        // Put the hyperlink to the results
        if(href != null)
            result = href + result + "</a>";

        StringBuilder b = new StringBuilder(
            "<a href=\"resultframe.jsp?runId=");
        b.append(this.runId);
        b.append("&result=");
        b.append(desc.resultFilePath);
        b.append("&show=logs\">");
        b.append(getStatus(runId.toString()));
        b.append("</a>");
        status = b.toString();

        String paramFileName = resultDir.getAbsolutePath() +
                File.separator + desc.configFileName;
        File paramFile = new File(paramFileName);
        if (paramFile.isFile()) {
            if (dateTime == null)
                dateTime = dateFormat.format(new Date(
                                          paramFile.lastModified()));
            ParamRepository par = new ParamRepository(paramFileName);
            description = par.getParameter("runConfig/description");
            scale = par.getParameter("runConfig/scale");

            if (desc.scaleName == null)
                desc.scaleName = "";
            if (desc.scaleUnit == null)
                desc.scaleUnit = "";
            if (desc.metric == null)
                desc.metric = "";
            scaleName = desc.scaleName;
            scaleUnit = desc.scaleUnit;
            metricUnit = desc.metric;
            // Now we need to fix up all the nulls.

            // First, if we're dealing with totally blank results or just
            // a directory, we just ignore this directory altogether.
            if (result == null && status == null && dateTime == null)
                return;

            // Then if individual pieces are missing
            if (result == null)
                result = "N/A";

            if (status == null)
                status = "N/A";

            if (dateTime == null)
                dateTime = "N/A";

            if (description == null ||
                description.length() == 0)
                description = "UNAVAILABLE";
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

        if (submitter == null)
            submitter = "N/A";
    }

    public static Result[] getResults(Subject user) {

        ArrayList<Result> resultList = null;
        Result header = null;
        HashSet<String> scaleNames = null;
        HashSet<String> scaleUnits = null;
        HashSet<String> metricUnits = null;
        File[] list = (new File(Config.OUT_DIR)).listFiles();
        if((list != null) && (list.length > 0)) {

            // Allocate supporting structures.
            resultList = new ArrayList<Result>(list.length);
            scaleNames = new HashSet<String>();
            scaleUnits = new HashSet<String>();
            metricUnits = new HashSet<String>();

            // Create a comparator that orders Strings in descending order.
            Comparator<String> descend = new Comparator<String>() {

                public int compare(String s1, String s2) {
                    return s2.compareTo(s1);
                }
            };

            // Sort the result list by dateTime, descending. Newest run first.
            TreeMap<String, File> dirMap = new TreeMap<String, File>(descend);
            for (int i = 0; i < list.length; i++) {
                String runId = list[i].getName();
                if (!AccessController.isViewAllowed(user, runId))
                    continue;
                dirMap.put(new RunId(runId).getRunSeq(), list[i]);
            }

            // First entry in the list is the header.
            header = new Result();
            header.runId = new RunId("Run","ID");
            header.description = "Description";
            header.scale = "Scale";
            header.metric = "Metric";
            header.result = "Result";
            header.status = "Status";
            header.dateTime = "Date/Time";
            resultList.add(header);

            // Now iterate through the sorted map.
            for (Map.Entry<String, File> entry : dirMap.entrySet()) {
                try {
                    Result result = new Result(entry.getValue());
                    scaleNames.add(result.scaleName);
                    scaleUnits.add(result.scaleUnit);
                    metricUnits.add(result.metricUnit);
                    resultList.add(result);
                } catch (IOException e) {
                }
            }

            // Now check whether the results are using single metric unit,
            // scale name, or scale unit. In that case, just put it into the
            // header instead of in all records.
            if (scaleNames.size() == 1 && scaleUnits.size() == 1) {
                Result result = resultList.get(1);
                if (result.scaleName.length() > 0 &&
                    result.scaleUnit.length() > 0)
                    header.scale = result.scaleName + " (" +
                                   result.scaleUnit + ')';
                else if (result.scaleName.length() > 0)
                    header.scale = result.scaleName;
                else if (result.scaleUnit.length() > 0)
                    header.scale = result.scaleUnit;
                for (int i = 1; i < resultList.size(); i++) {
                    result = resultList.get(i);
                    result.scaleName = null;
                    result.scaleUnit = null;
                }
            } else {
                // Construct scale field.
                StringBuilder b = new StringBuilder();
                for (int i = 1; i < resultList.size(); i++) {
                    Result result = resultList.get(i);
                    if (result.scaleName.length() > 0) {
                        b.append(result.scaleName);
                        b.append(' ');
                    }
                    b.append(result.scale);
                    if (result.scaleUnit.length() > 0) {
                        b.append(' ');
                        b.append(result.scaleUnit);
                    }
                    result.scale = b.toString();
                    b.setLength(0);
                }
            }

            // Do the same for the metric
            if (metricUnits.size() == 1) {
                Result result = resultList.get(1);
                if (result.metricUnit.length() > 0)
                    header.metric = result.metricUnit;
                for (int i = 1; i < resultList.size(); i++) {
                    result = resultList.get(i);
                    if (result.metric == null)
                        result.metric = "N/A";
                    result.metricUnit = null;
                }
            } else {
                StringBuilder b = new StringBuilder();
                for (int i = 1; i < resultList.size(); i++) {
                    Result result = resultList.get(i);
                    if (result.metric == null) {
                        result.metric = "N/A";
                        continue;
                    }
                    b.append(result.metric);
                    if (result.metricUnit.length() > 0) {
                        b.append(' ');
                        b.append(result.metricUnit);
                    }
                    result.metric = b.toString();
                    result.metricUnit = null;
                    b.setLength(0);
                }
            }

            // Finally, convert it to an array.
            Result[] results = new Result[resultList.size()];
            return resultList.toArray(results);
        }
        return null;
    }

    /**
     * Obtains the current status of a run.
     * @param runId The id of the run in question
     * @return The current status string or "UNKNOWN" in error cases
     */
    public static String getStatus(String runId) {
        char[] cBuf = null;
        int length = -1;
        try {
            FileReader reader = new FileReader(Config.OUT_DIR + runId +
                    "/resultinfo");
            cBuf = new char[64];
            length = reader.read(cBuf);
            reader.close();
        } catch (IOException e) {
            // Do nothing, length = -1.
        }

        String status;
        if (length == -1)
            status = "UNKNOWN";
        else
            status = new String(cBuf, 0, length);

        return status.trim();
    }

    public static Result[] getResults(int offset, int records,
                                      String sortColumn, boolean ascending,
                                      Subject subject) {
        // TODO: Implement the getResults with condition, including result caching.
        TreeMap<Long, Result> sortMap = new TreeMap<Long, Result>();
        String dirs[] = new File(Config.OUT_DIR).list();
        for (String runId : dirs) {
            try {
                Result result = getInstance(new RunId(runId));
                sortMap.put(result.date, result);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot read result dir " + runId);
            }
        }
        Collection<Result> resultCol = sortMap.values();
        Result[] results = new Result[resultCol.size()];
        results = resultCol.toArray(results);
        return results;
    }
}
