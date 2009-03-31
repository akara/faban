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
 * $Id: Deployer.java,v 1.10 2009/03/31 00:23:56 sheetalpatil Exp $
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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The Deployer servlet is used to deploy a benchmark from a remote system.
 *
 * @author Akara Sucharitakul
 */
public class Deployer extends HttpServlet {

    static Logger logger = Logger.getLogger(Deployer.class.getName());

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Writer writer = response.getWriter();
        writeHeader(request, writer);
        writer.write("        <form NAME=\"Standard\" ACTION=\"" +
                     request.getRequestURI() +
                     "\" METHOD=\"POST\" ENCTYPE=\"multipart/form-data\">\n");
        writer.write("<table cellpadding=\"0\" cellspacing=\"2\" " +
                     "border=\"0\" align=\"center\">\n");
        writer.write("  <tbody>\n");
        writer.write("    <tr>\n");
        writer.write("        <td style=\"text-align: right;\">" +
                     "Login ID:</td>\n");
        writer.write("        <td>\n");
        writer.write("            <input type=\"text\" name=\"user\" " +
                     "size=\"10\"/>\n");
        writer.write("       </td>\n");
        writer.write("    </tr>\n");
        writer.write("    <tr>\n");
        writer.write("        <td style=\"text-align: right;\">" +
                     "Password:</td>\n");
        writer.write("        <td>\n");
        writer.write("            <input type=\"password\" name=\"password\" " +
                     "size=\"10\"/>\n");
        writer.write("        </td>\n");
        writer.write("    </tr>\n");
        writer.write("    <tr>\n");
        writer.write("        <td colspan=\"2\">Benchmark JAR File:<br>\n");
        writer.write("    	      <input type=\"file\" name=\"jarfile\" " +
                     "size=\"64\"/>\n");
        writer.write("        </td>\n");
        writer.write("    </tr>\n");
        writer.write("    <tr>\n");
        writer.write("        <td style=\"text-align: right;\">\n");
        writer.write("            <input type=\"checkbox\" " +
                     "name=\"clearconfig\" value=\"true\"/>\n");
        writer.write("        </td>\n");
        writer.write("        <td>Clear previous benchmark configuration" +
                     "</td>\n");
        writer.write("    </tr>\n");
        writer.write("  </tbody>\n");
        writer.write("</table>\n");
        writer.write("<input type=\"submit\" name=\"Submit\" " +
                     "value=\"Deploy\">\n");
        writer.write("</form>\n");
        writeTrailer(writer);
        writer.flush();
        writer.close();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            List<String> deployNames = new ArrayList<String>();
            List<String> cantDeployNames = new ArrayList<String>();
            List<String> invalidNames = new ArrayList<String>();

            String user = null;
            String password = null;
            boolean clearConfig = false;
            boolean hasPermission = true;

            DiskFileUpload fu = new DiskFileUpload();
            // No maximum size
            fu.setSizeMax(-1);
            // maximum size that will be stored in memory
            fu.setSizeThreshold(4096);
            // the location for saving data that is larger than getSizeThreshold()
            fu.setRepositoryPath(Config.TMP_DIR);

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
                String fieldName = item.getFieldName();
                if (item.isFormField()) {
                    if ("user".equals(fieldName)) {
                        user = item.getString();
                    } else if ("password".equals(fieldName)) {
                        password = item.getString();
                    } else if ("clearconfig".equals(fieldName)) {
                        String value = item.getString();
                        clearConfig = Boolean.parseBoolean(value);
                    }
                    continue;
                }

                if (!"jarfile".equals(fieldName))
                    continue;

                String fileName = item.getName();

                if (fileName == null) // We don't process files without names
                    continue;

                if (Config.SECURITY_ENABLED) {
                    if (Config.DEPLOY_USER == null ||
                            Config.DEPLOY_USER.length() == 0 ||
                            !Config.DEPLOY_USER.equals(user)) {
                        hasPermission = false;
                        break;
                    }
                    if (Config.DEPLOY_PASSWORD == null ||
                            Config.DEPLOY_PASSWORD.length() == 0 ||
                            !Config.DEPLOY_PASSWORD.equals(password)) {
                        hasPermission = false;
                        break;
                    }
                }
                // Now, this name may have a path attached, dependent on the
                // source browser. We need to cover all possible clients...

                char[] pathSeparators = {'/', '\\'};
                // Well, if there is another separator we did not account for,
                // just add it above.

                for (int j = 0; j < pathSeparators.length; j++) {
                    int idx = fileName.lastIndexOf(pathSeparators[j]);
                    if (idx != -1) {
                        fileName = fileName.substring(idx + 1);
                        break;
                    }
                }

                // Ignore all non-jarfiles.
                if (!fileName.toLowerCase().endsWith(".jar")) {
                    invalidNames.add(fileName);
                    continue;
                }

                String benchName = fileName.substring(0, fileName.length() - 4);

                if (benchName.indexOf('.') >= 0) {
                    invalidNames.add(benchName);
                    continue;
                }

                // Check whether we can deploy or not. If running or queued,
                // we won't deploy.
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
                    DeployUtil.generateXform(benchName);
                } catch (Exception e) {
                    throw new ServletException(e);
                }
                deployNames.add(benchName);
            }

            if (clearConfig)
                for (String benchName: deployNames)
                    DeployUtil.clearConfig(benchName);

            if (!hasPermission)
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            else if (cantDeployNames.size() > 0)
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            else if (invalidNames.size() > 0)
                response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
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

            if (deployNames.size() > 0)
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

            if (!hasPermission)
                writer.write("Permission denied!");

            writeTrailer(writer);
            writer.flush();
            writer.close();
        } catch (ServletException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ServletException(e);
        }
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
