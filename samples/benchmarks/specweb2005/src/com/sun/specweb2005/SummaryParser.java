/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
 * install_dir/license.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.specweb2005;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The result parser is responsible for parsing the results into a Faban
 * summary and detail XML file.
 *
 * @author Sreekanth Setty
 */
public class SummaryParser {

    String runId;
    Calendar startTime;
    Calendar endTime;
    Logger logger;

    /**
     * Parses the SPECweb summary report.
     * @param runId The run id
     * @param startTime The start time
     * @param endTime The end time
     * @param logger The logger
     */
    public SummaryParser(String runId, Calendar startTime, Calendar endTime, Logger logger) {
        this.runId = runId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.logger = logger;
    }

    /**
     * Generates the Faban summary report for this benchmark.
     * @param in The result input
     * @param out The summary report output
     * @throws IOException Error reading or writing
     */
    public void convert(BufferedReader in, Writer out) throws IOException {
        String a;
        String status;
        String respTime=null;
        Matcher m1, m2, m3;
        float good, ok, bad;
        int total_req, validated_errors;

        // Pattern p1 = Pattern.compile("\\|\\s*([.\\d]+?)\\%\\s*([.\\d]+?)\\%\\s*([.\\d]+?)\\%");
        Pattern p1 = Pattern.compile("\\|\\s*([.\\d]+?)\\%\\s*([.\\d]+?)\\%\\s*([.\\d]+?)\\%\\s*\\|\\s*(\\d+)\\s*\\|");

        // Pattern p2 = Pattern.compile("\\|(.*)\\|(.*)\\|(.*)\\|(.*)\\|");
        Pattern p2 = Pattern.compile("\\|      TOTAL       \\|(.*)\\|(.*)\\|(.*)\\|");

        // Pattern p3 = Pattern.compile("\\|      TOTAL       \\|(.*)\\|[\\d\\s]+");
        Pattern p3 = Pattern.compile("\\|      TOTAL       \\|\\s*(\\d+)\\s*\\|[\\d\\s]+");

        good = ok = bad = total_req = validated_errors = 0;
        a = in.readLine(); // read first line

        boolean done1, done2, done3, b1, b2, b3;
        done1 = false;
        done2 = false;
        done3 = false;
        b1 = false;
        b2 = false;
        b3 = false;
        while (a != null) {
            m1 = p1.matcher(a);
            b1 = m1.find();
            m2 = p2.matcher(a);
            b2 = m2.find();
            m3 = p3.matcher(a);
            b3 = m3.find();

            if (b1) {
               good = Float.parseFloat(m1.group(1));
               ok = Float.parseFloat(m1.group(2));
               bad = Float.parseFloat(m1.group(3));
               validated_errors = Integer.parseInt(m1.group(4));
               done1 = true;
               logger.fine("good:" + good + " ok:" + ok + " bad:" + bad + " errors:" + validated_errors);
            }

            if (b2) {
               // respTime = a.substring(51, 61);
               respTime = m2.group(3);
               logger.fine("resp time" + respTime);
               done2 = true;
            }

            if (b3) {
               total_req = Integer.parseInt(m3.group(1));
               logger.fine("total req" + total_req);
               done3 = true;
            }

            if ((done1 == true) && (done2 == true) && (done3 == true))
                break;
            else
                a = in.readLine();
        }

        if ( ( (good >= 95.0) && (ok >= 99.0) && (bad <= 5.0)) &&  (total_req/200 >= validated_errors))
            status = "true";
        else  status = "false";

        DateFormat dateFormat = DateFormat.getDateTimeInstance();

        StringBuilder buffer = new StringBuilder(8192);
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append("<?xml-stylesheet type=\"text/xsl\" ");
        buffer.append("href=\"../../xslt/summary_report.xsl\"?>\n");
        buffer.append("<benchResults>\n");
        buffer.append("    <benchSummary name=\"SpecWeb2005\" version=\"0.5\">\n");
        buffer.append("        <runId>");
        buffer.append(runId);
        buffer.append("</runId>\n");
        buffer.append("        <startTime>");
        buffer.append(dateFormat.format(this.startTime.getTime()));
        buffer.append("</startTime>\n");
        buffer.append("        <endTime>");
        buffer.append(dateFormat.format(this.endTime.getTime()));
        buffer.append("</endTime>\n");
        buffer.append("        <metric unit=\"");
        buffer.append("Avg Resp Time");
        buffer.append("\">");
        buffer.append(respTime);
        buffer.append("</metric>\n");
        buffer.append("        <passed>");
        buffer.append(status);
        buffer.append("</passed>\n");
        buffer.append("    </benchSummary>\n");
        buffer.append("</benchResults>\n");

        out.write(buffer.toString());
    }
}
