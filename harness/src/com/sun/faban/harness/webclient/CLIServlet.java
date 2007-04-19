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
 * $Id: CLIServlet.java,v 1.2 2007/04/19 06:56:22 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.engine.RunQ;
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
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Submitter servlet is used to submit a benchmark run from the CLI.
 *
 * @author Akara Sucharitakul
 */
public class CLIServlet extends HttpServlet {

    static Logger logger = Logger.getLogger(CLIServlet.class.getName());

    String[] getPathComponents(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();

        StringTokenizer pathTokens = null;
        int tokenCount = 0;
        if (pathInfo != null) {
            pathTokens = new StringTokenizer(pathInfo, "/");
            tokenCount = pathTokens.countTokens();
        }
        String[] comps = new String[tokenCount + 1];
        comps[0] = request.getServletPath();
        int i = 1;
        while (pathTokens != null && pathTokens.hasMoreTokens()) {
            comps[i] = pathTokens.nextToken();
            if (comps[i] != null && comps[i].length() > 0)
                ++i;
        }

        if (i != comps.length) {
            String[] comps0 = new String[i];
            System.arraycopy(comps, 0, comps0, 0, i);
            comps = comps0;
        }
        return comps;
    }

    /**
     * Lists pending runs or obtans status of a particular run.<ol>
     * <li>Status:  http://..../status/${runid}</li>
     * <li>Pending: http://..../pending/</li>
     * </ol>
     * @param request The request object
     * @param response The response object
     * @throws ServletException Error executing servlet
     * @throws IOException I/O error
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String[] reqC = getPathComponents(request);
        if ("/status".equals(reqC[0])) {
            if (reqC.length < 2) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                            "Missing RunId.");
                return;
            }
            String status = null;
            RunId runId = new RunId(reqC[1]);
            Result result = Result.getInstance(runId);
            if (result == null) {
                // Perhaps the runId is still in the pending queue.
                String[] pending = listPending();
                for (String run : pending)
                    if (run.equals(runId.toString())) {
                        status = "QUEUED";
                        break;
                    }
                if (status == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                            "No such runId: " + runId);
                    return;
                }
            } else {
                status = result.status.value;
            }
            Writer w = response.getWriter();
            w.write(status + '\n');
            w.flush();
            w.close();
        } else if ("/pending".equals(reqC[0])) {
            String[] pending = listPending();
            if (pending == null) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT,
                        "No pending runs");
            } else {
                Writer w = response.getWriter();
                for (int i = 0; i < pending.length; i++)
                    w.write(pending[i] + '\n');
                w.flush();
                w.close();
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Request string " + reqC[0] + " not understood!");
        }
    }

    private String[] listPending() {
        String[][] pendingA = RunQ.getHandle().listRunQ();
        String[]  pendingL = null;
        if (pendingA != null) {
            pendingL = new String[pendingA.length];
            for (int i = 0; i < pendingA.length; i++)
                pendingL[i] = pendingA[i][1] + '.' + pendingA[i][0];
        }
        return pendingL;
    }

    /**
     * Submits new runs. The POST request must be a multi-part POST. The first
     * parts contain user name and password information (if security is
     * enabled). Each subsequent part contains the run configuration file.
     * <br><br>
     * Path to call this servlet is http://.../submit/${benchmark}/${profile}
     *
     * @param request The mime multi-part post request
     * @param response The response object
     * @throws ServletException Error executing servlet
     * @throws IOException I/O error
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Path to call this servlet is http://...../${benchmark}/${profile}
        // And it is a post request with optional user, password, and
        // all the config files.
        String[] reqC = getPathComponents(request);
        if (reqC.length < 3) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Benchmark and profile not provided in request.");
            return;
        }
        BenchmarkDescription desc =                 // first is the bench name
                BenchmarkDescription.getDescription(reqC[1]);
        if (desc == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Benchmark " + reqC[1] + " not deployed.");
            return;
        }
        try {
            String user = null;
            String password = null;
            boolean hasPermission = true;
            ArrayList<String> runIdList = new ArrayList<String>();

            DiskFileUpload fu = new DiskFileUpload();
            // No maximum size
            fu.setSizeMax(-1);
            // maximum size that will be stored in memory
            fu.setSizeThreshold(8192);
            // the location for saving data larger than getSizeThreshold()
            fu.setRepositoryPath(Config.TMP_DIR);

            List fileItems = null;
            try {
                fileItems = fu.parseRequest(request);
            } catch (FileUploadException e) {
                throw new ServletException(e);
            }

            for (Iterator i = fileItems.iterator(); i.hasNext();) {
                FileItem item = (FileItem) i.next();
                String fieldName = item.getFieldName();
                if (item.isFormField()) {
                    if ("sun".equals(fieldName)) {
                        user = item.getString();
                    } else if ("sp".equals(fieldName)) {
                        password = item.getString();
                    }
                    continue;
                }
                if (reqC[2] == null) // No profile
                    break;

                if (desc == null)
                    break;

                if (!"configfile".equals(fieldName))
                    continue;

                if (Config.SECURITY_ENABLED) {
                    if (Config.CLI_SUBMITTER == null ||
                            Config.CLI_SUBMITTER.length() == 0 ||
                            !Config.CLI_SUBMITTER.equals(user)) {
                        hasPermission = false;
                        break;
                    }
                    if (Config.SUBMIT_PASSWORD == null ||
                            Config.SUBMIT_PASSWORD.length() == 0 ||
                            !Config.SUBMIT_PASSWORD.equals(password)) {
                        hasPermission = false;
                        break;
                    }
                }

                String usrDir = Config.PROFILES_DIR + reqC[2];
                File dir = new File(usrDir);
                if(dir.exists()) {
                    if(!dir.isDirectory()) {
                         logger.severe(usrDir +
                                        " should be a directory");
                        dir.delete();
                        logger.fine(dir + " deleted");
                    }
                    else
                        logger.fine("Saving parameter file to" +
                                    usrDir);
                }
                else {
                    logger.fine("Creating new profile directory for " +
                                reqC[2]);
                    if(dir.mkdirs())
                        logger.fine("Created new profile directory " +
                                    usrDir);
                    else
                        logger.severe("Failed to create profile " +
                                      "directory " + usrDir);
                }


                // Save the latest config file into the profile directory
                String dstFile = Config.PROFILES_DIR + reqC[2] +
                        File.separator + desc.configFileName + "." +
                        desc.shortName;

                item.write(new File(dstFile));

                String runId = RunQ.getHandle().addRun(user, reqC[2], desc);
                runIdList.add(runId);
            }

            response.setContentType("text/plain");
            Writer writer = response.getWriter();

            if (!hasPermission) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                writer.write("Permission denied!\n");
            }

            if (runIdList.size() == 0)
                writer.write("No runs submitted.\n");
            for (String runId : runIdList) {
                writer.write(runId);
            }

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
}
