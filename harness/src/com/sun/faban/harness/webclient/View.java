/*
 * Copyright(c) 2010-2011 Shanti Subramanyam. All Rights Reserved.
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class View {

    static final String TITLE = "Title:";
    static final String SECTION = "Section:";
    static final String DISPLAY = "Display:";
    static final String TIME = "Time"; // Used to detect the start of a data range in CSV file
    static final String RUNID = "RunId"; // Used to find the first instance of usable Section Name

    static Logger logger = Logger.getLogger(View.class.getName());

    /**
     * Method to parse xan file and create JSON object for javascript graphing by xan_view.jsp
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void xanView(HttpServletRequest request, HttpServletResponse response) throws IOException {
        File xanFile;
        Xan xan;

        String[] restReq = (String[]) request.getAttribute("rest.request");
        HttpSession session = request.getSession();
        StringBuilder pathBuilder = new StringBuilder();
        for (String pathElement : restReq) {
            pathBuilder.append(pathElement).append(File.separator);
        }
        logger.fine("Parsing xan file: " + Config.OUT_DIR + File.separator + pathBuilder.toString());
        xanFile = new File(Config.OUT_DIR, pathBuilder.toString());

        try {
            xan = parseXan(xanFile);
        } catch (IOException e) {
            String msg = "Error reading file " + xanFile;
            logger.log(Level.WARNING, msg, e);
            throw e;
        } catch (ParseException e) {
            String msg = "Error parsing file " + xanFile;
            logger.log(Level.WARNING, msg, e);
            throw new IOException(msg, e);
        }
        createJson(xan);
        request.setAttribute("model", xan);
        request.setAttribute("tblOnly", false);
    }


    /**
     * Parse xan file and generate Xan object to pass to jsp
     *
     * @param xanFile
     * @return
     * @throws IOException
     * @throws ParseException
     */
    static Xan parseXan(File xanFile)
            throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new FileReader(xanFile));
        Xan xan = new Xan();
        Section currentSection = null;
        boolean freshSection = false;
        int lineNo = 0;
        for (; ; ) {
            String line = reader.readLine();
            ++lineNo;
            if (line == null)
                break;
            if (line.startsWith(TITLE)) {
                xan.title = line.substring(TITLE.length()).trim();
            } else if (line.startsWith(SECTION)) {
                currentSection = new Section();
                currentSection.name = line.substring(SECTION.length()).trim();
                freshSection = true;
                xan.sections.add(currentSection);
            } else if (line.startsWith(DISPLAY) && freshSection) {
                currentSection.display = line.substring(DISPLAY.length()).trim();
            } else {
                line = line.trim();
                if (line.length() == 0)
                    continue;
                // Scan whether it is just a divider line (---- ---- ----)
                int idx = 0;
                for (; idx < line.length(); idx++) {
                    char c = line.charAt(idx);
                    if (c != '-' && c != ' ')
                        break;
                }
                if (idx == line.length())
                    continue;

                if (freshSection) {
                    freshSection = false;
                    currentSection.headers = Arrays.asList(line.split("\t| {2,}"));
                } else {
                    currentSection.rows.add(Arrays.asList(line.split("\t| {2,}")));
                }
            }
        }
        return xan;
    }

    static void createJson(Xan xan) {
        StringBuilder xanBuffer = new StringBuilder(2048);
        StringBuilder lineBuffer = new StringBuilder(120);
        int sectionId = 1;
        for (Section section : xan.sections) {
            if (!"line".equalsIgnoreCase(section.display)) {
                sectionId++;
                continue;
            }
            int columns = section.headers.size();
            section.json = new ArrayList<String>();
            boolean xIsTime = false;

            for (int i = 1; i < columns; i++) {
                if (i == 1)
                    section.dataName.append("data" + sectionId + i); // data11, data12, etc.
                else
                    section.dataName.append(", data" + sectionId + i);
                boolean firstRow = true;
                for (List<String> row : section.rows) {
                    if (firstRow) {
                        section.min = format(row.get(0));
                        firstRow = false;
                        lineBuffer.append("[[");
                        if (i == 1)
                            // Determine if x-axis is time
                            xIsTime = isTime(row.get(0));
                    } else if (lineBuffer.length() > 70) {
                        // Flush buffer and newline shortly after 70 columns.
                        lineBuffer.append(",\n");
                        xanBuffer.append(lineBuffer);
                        lineBuffer.setLength(0);
                        lineBuffer.append('[');
                    } else {
                        lineBuffer.append(",[");
                    }
                    lineBuffer.append(format(row.get(0))).append(',');
                    lineBuffer.append(format(row.get(i))).append(']');
                }
                // Get the last entry of the x-axis column
                section.max = format(section.rows.get(section.rows.size()-1).get(0));
                lineBuffer.append("]");
                xanBuffer.append(lineBuffer);
                logger.fine("section.json=" + xanBuffer);
                section.json.add(xanBuffer.toString());
                lineBuffer.setLength(0);
                xanBuffer.setLength(0);
            }
            section.xIsTime = xIsTime ? 1 : 0;
            sectionId++;
        }
    }

    private static String format(String s) {

        if ("-".equals(s))
            return "null";
        if (isTime(s)) {
            StringBuilder sb = new StringBuilder();
            // Put quotes around time to create json string value
            sb.append("'");
            sb.append(s);
            sb.append("'");
            return sb.toString();
        }
        BigDecimal bd;
        try {
            bd = new BigDecimal(s);
        } catch (NumberFormatException e) {
            return s;
        }
        bd = bd.stripTrailingZeros();
        String s1 = bd.toPlainString();
        String s2 = bd.toString();
        if (s1.length() > s2.length())
            return s2;
        else
            return s1;
    }

    private static boolean isTime(String s) {
        String t[] = s.split(":");
        if (t != null && t.length > 1)
            // This looks like time
            return (true);
        else
            return (false);
    }


    public static void main(String[] args) throws IOException, ParseException {
        File xanFile = new File(args[0]);
        FileWriter writer = new FileWriter(args[1]);
        Xan xan = parseXan(xanFile);
        createJson(xan);
        writer.flush();
        writer.close();
    }

    public static class Xan {
        public String title;
        public List<Section> sections = new ArrayList<Section>();
    }

    public static class Section {
        public String name;
        public String link = null;
        public String display = "table";
        public List<String> headers;
        public List<List<String>> rows = new ArrayList<List<String>>();
        public List<String> json = null;    //holds the json data objects, one for each column
        public StringBuilder dataName = new StringBuilder(); //holds the series data variables
        public int xIsTime = 0;
        public String min, max; // min, max values of x-axis
    }
}
