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
 * $Id: Deployer.java,v 1.2 2006/06/29 19:38:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.DeployUtil;
import com.sun.faban.harness.util.FileHelper;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Deployer servlet is used to deploy a benchmark from a remote system.
 *
 * @author Akara Sucharitakul
 */
public class Deployer extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Writer writer = response.getWriter();
        writeHeader(request, writer);
        writer.write("        <form NAME=\"Standard\" ACTION=\"" +
                     request.getRequestURI() +
                     "\" METHOD=\"POST\" ENCTYPE=\"multipart/form-data\">\n");
        writer.write("    	    <p>Benchmark JAR File:</p>\n");
        writer.write("    	    <p><input TYPE=FILE NAME=\"jarfile\" SIZE=64>" +
                     "</p>\n");
        writer.write("          <p><input TYPE=SUBMIT NAME=\"Submit\" " +
                     "VALUE=\"Deploy\"></p>\n");
        writer.write("        </form>\n");
        writeTrailer(writer);
        writer.flush();
        writer.close();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List deployNames = new ArrayList();
        List cantDeployNames = new ArrayList();

        DiskFileUpload fu = new DiskFileUpload();
        // No maximum size
        fu.setSizeMax(-1);
        // maximum size that will be stored in memory
        fu.setSizeThreshold(4096);
        // the location for saving data that is larger than getSizeThreshold()
        fu.setRepositoryPath(System.getProperty("java.io.tmpdir"));

        List fileItems = null;
        try {
            fileItems = fu.parseRequest(request);
        } catch (FileUploadException e) {
            throw new ServletException(e);
        }
        // assume we know there are two files. The first file is a small
        // text file, the second is unknown and is written to a file on
        // the server
        for (Iterator i = fileItems.iterator(); i.hasNext();) {
            FileItem item = (FileItem) i.next();
            String fileName = item.getName();

            if (fileName == null) // We don't process files without names
                continue;

            // Now, this name may have a path attached, dependent on the
            // source browser. We meed to cover all possible clients...

            char[] pathSeparators = {'/', '\\'};
            // Well, if there is another separator we did not account for,
            // just add it above.

            pathCheck:
            for (int j = 0; j < pathSeparators.length; j++) {
                int idx = fileName.lastIndexOf(pathSeparators[j]);
                if (idx != -1) {
                    fileName = fileName.substring(idx + 1);
                    break pathCheck;
                }
            }

            // Ignore all non-jarfiles.
            if (!fileName.toLowerCase().endsWith(".jar"))
                continue;

            // Check whether we can deploy or not. If running or queued,
            // we won't deploy.
            String benchName = fileName.substring(0, fileName.length() - 4);
            if (!DeployUtil.canDeploy(benchName)) {
                cantDeployNames.add(benchName);
                continue;
            }

            File uploadFile = new File(Config.BENCHMARK_DIR, fileName);
            if (uploadFile.exists())
                FileHelper.recursiveDelete(uploadFile);

            try {
                item.write(uploadFile);
            } catch (Exception e) {
                throw new ServletException(e);
            }

            try {
                DeployUtil.unjar(benchName);
                DeployUtil.generateDD(benchName);
            } catch (Exception e) {
                throw new ServletException(e);
            }
            deployNames.add(benchName);
        }

        if (cantDeployNames.size() > 0)
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        else if (deployNames.size() > 0)
            response.setStatus(HttpServletResponse.SC_CREATED);
        else
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        Writer writer = response.getWriter();
        writeHeader(request, writer);

        if (deployNames.size() > 1)
            writer.write("Benchmarks ");
        else if (deployNames.size() > 0)
            writer.write("Benchmark ");

        for (int i = 0; i < deployNames.size(); i++) {
            if (i > 0)
                writer.write(", ");
            writer.write((String) deployNames.get(i));
        }
        writer.write(" deployed.<br>\n");

        if (cantDeployNames.size() > 0) {
            if (cantDeployNames.size() > 1)
                writer.write("Cannot deploy benchmarks ");
            else
                writer.write("Cannot deploy benchmark ");
            for (int i = 0; i < cantDeployNames.size(); i++) {
                if (i > 0)
                    writer.write(", ");
                writer.write((String) cantDeployNames.get(i));
            }
            writer.write(". Benchmark being run or queued up for run.<br>\n");
        }
        writeTrailer(writer);
        writer.flush();
        writer.close();
    }

    private static void writeHeader(HttpServletRequest request, Writer w) 
            throws IOException {
        w.write("<!DOCTYPE html\n");
        w.write("    PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n");
        w.write("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-" +
                "transitional.dtd\">\n");
        w.write("<html>\n");
        w.write("    <head>\n");
        w.write("        <title>" + Config.HARNESS_NAME +
                " Benchmark Deployment</title>\n");
        w.write("<link rel=\"icon\" type=\"image/gif\" href=\"" +
                    request.getContextPath() + "/img/faban.gif\">");

        w.write("    </head>\n");
        w.write("    <body>\n");
        w.write("        <table BORDER=\"0\" CELLSPACING=\"5\" CELL" +
                "PADDING=\"10\" WIDTH=\"100%\" BGCOLOR=\"#FFFFFF\" >\n");
        w.write("            <tr>\n");
        w.write("                <td ALIGN=\"CENTER\" WIDTH=\"33%\" " +
                "BGCOLOR=\"#5382A1\"> Sun Microsystems </td>\n");
        w.write("                <td ALIGN=\"CENTER\" WIDTH=\"34%\" " +
                "BGCOLOR=\"#E76F00\"><b>" + Config.HARNESS_NAME +
                " Benchmark Deployment</b></td>\n");
        w.write("                <td ALIGN=\"CENTER\" WIDTH=\"33%\" " +
                "BGCOLOR=\"#B2BC00\"> Version " + Config.HARNESS_VERSION +
                " </td>\n");
        w.write("            </tr>\n");
        w.write("        </table>\n");
        w.write("        <br><center><b>");
    }

    private static void writeTrailer(Writer w) throws IOException {
        w.write("        </b></center>");
        w.write("    </body>\n");
        w.write("</html>\n");
    }
}
