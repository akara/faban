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
 * $Id: Result.java,v 1.7 2006/08/19 03:06:12 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.security.AccessController;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.XMLReader;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Result class scans through the output directory and compiles a list of run
 * results. Enhance this to enable the user to sort it using different options.
 */
public class Result {

    public String runId;
    public String description;
    public String result;
    private String scaleName;
    public String scale;
    private String scaleUnit;
    public String metric;
    private String metricUnit;
    public String status;
    public String time;

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

            // The output format
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                                      "EEE'&nbsp;'MM/dd/yy HH:mm:ss'&nbsp;'z");
            // The format in the result file
            SimpleDateFormat parseFormat = new SimpleDateFormat(
                                      "EEE MMM dd HH:mm:ss z yyyy");

            // Sort the result list by time, descending. Newest run first.
            TreeMap<String, File> dirMap = new TreeMap<String, File>(descend);
            for (int i = 0; i < list.length; i++) {
                String runId = list[i].getName();
                if (!AccessController.isViewAllowed(user, runId))
                    continue;
                runId = runId.substring(runId.lastIndexOf('.') + 1);
                dirMap.put(runId, list[i]);
            }

            // First entry in the list is the header.
            header = new Result();
            header.runId = "Run ID";
            header.description = "Description";
            header.scale = "Scale";
            header.metric = "Metric";
            header.result = "Result";
            header.status = "Status";
            header.time = "Date/Time";
            resultList.add(header);


            // Benchmark descriptions
            Map<String, BenchmarkDescription> benchMap = null;

            // Now iterate through the sorted map.
            for (Map.Entry<String, File> entry : dirMap.entrySet()) {
                File resultDir = entry.getValue();
                // If we're not dealing with a dir, it's not a run result.
                File[] files = resultDir.listFiles();
                if (files == null)
                    continue;

                // run id
                Result result = new Result();
                result.runId = resultDir.getName();

                // First, check whether the results contain meta info.
                String shortName = result.runId.substring(0,
                                    result.runId.lastIndexOf('.'));
                BenchmarkDescription desc = BenchmarkDescription.
                        readDescription(shortName, resultDir.getAbsolutePath());
                if (desc == null) {

                    // If not, we fetch it from the benchmark meta info.
                    if (benchMap == null)
                        benchMap = BenchmarkDescription.getBenchDirMap();

                    desc = (BenchmarkDescription) benchMap.get(shortName);
                }


                String href = null;

                // run result and HREF to the summary or log file.
                for(int j = 0; j < files.length; j++) {
                    // assuming the result file name will be "summary.xml"
                    if(files[j].getName().equalsIgnoreCase("summary.xml") &&
                            files[j].length() > 0) {
                        result.result = "PASSED";
                        href = "<a href=\"resultframe.jsp?runId=" +
                                result.runId + "&result=" +
                                desc.resultFilePath + "\">";

                        //Use the XMLReader and locate the <passed> elements
                        XMLReader reader = new XMLReader(files[j].
                                           getAbsolutePath());

                        // Obtain the metric before we break pass/fail.
                        result.metric = reader.getValue("benchSummary/metric");
                        try {
                            Date runTime = parseFormat.parse(
                                    reader.getValue("benchSummary/endTime"));
                            result.time = dateFormat.format(runTime);
                        } catch (ParseException e) {
                            // Do nothing. result.time will be null and
                            // later we'll use the param file's mod time
                            // for this field instead.
                        }

                        List v = reader.getValues("passed");
                        for(int k = 0; k < v.size(); k++) {
                            if(((String)v.get(k)).toUpperCase().
                                    indexOf("FALSE") != -1) {
                                result.result = "FAILED";
                                break;
                            }
                        }

                        // if any "result" with a 'false' in <passed> element,
                        // flags the final result.
                        if("FAILED".equals(result.result))
                            break;

                    }
                }

                if(href != null)
                    result.result = href + result.result + "</a>";

                StringBuilder b = new StringBuilder(
					"<a href=\"resultframe.jsp?runId=");
                b.append(result.runId);
                b.append("&result=");
                b.append(desc.resultFilePath);
                b.append("&show=logs\">");
                b.append(getStatus(result.runId));
                b.append("</a>");
                result.status = b.toString();

                String paramFileName = resultDir.getAbsolutePath() +
                        File.separator + desc.configFileName;
                File paramFile = new File(paramFileName);
                if (paramFile.isFile()) {
                    if (result.time == null)
                        result.time = dateFormat.format(new Date(
                                                  paramFile.lastModified()));
                    ParamRepository par = new ParamRepository(paramFileName);
                    result.description = par.getParameter("runConfig/description");
                    result.scale = par.getParameter("runConfig/scale");

                    if (desc.scaleName == null)
                        desc.scaleName = "";
                    if (desc.scaleUnit == null)
                        desc.scaleUnit = "";
                    if (desc.metric == null)
                        desc.metric = "";
                    result.scaleName = desc.scaleName;
                    result.scaleUnit = desc.scaleUnit;
                    result.metricUnit = desc.metric;
                    scaleNames.add(result.scaleName);
                    scaleUnits.add(result.scaleUnit);
                    metricUnits.add(result.metricUnit);
                }

                // Now we need to fix up all the nulls.

                // First, if we're dealing with totally blank results or just
                // a directory, we just ignore this directory altogether.
                if (result.result == null && result.status == null &&
                        result.time == null)
                    continue;

                // Then if individual pieces are missing
                if (result.result == null)
                    result.result = "N/A";

                if (result.status == null)
                    result.status = "N/A";

                if (result.time == null)
                    result.time = "N/A";

                if (result.description == null ||
                    result.description.length() == 0)
                    result.description = "UNAVAILABLE";

                resultList.add(result);
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
     * @param runId The runId of the run in question
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

        return status;
    }
}
