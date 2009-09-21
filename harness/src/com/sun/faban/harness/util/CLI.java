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
package com.sun.faban.harness.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import java.io.*;
import java.util.ArrayList;

/**
 * Command line interface for submitting runs, etc.<br/>
 * <br/>
 * CLI [-M master] [-U user [-P password]] action args<br/>
 * <br/>
 * Action and arguments:<ul>
 * <li>pending<br/>&nbsp;Lists pending runs.</li>
 * <li>status runId<br/>&nbsp;Provides the status for the given run id.</li>
 * <li>submit benchmark profile configfile.xml [configfile2.xml...]<br/>
 * &nbsp;Submits benchmark runs.</li>
 * </ul><br/>
 * The master is provided as a URL to the master's root context and defaults
 * to http://localhost:9980/.
 *
 * @author Akara Sucharitakul
 */
public class CLI {

    /**
     * The first argument to the CLI is the action. It can be:<ul>
     * <li>pending</li>
     * <li>status runId</li>
     * <li>submit benchmark profile configfile.xml</ul>
     * </ul>
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {

        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        ArrayList<String> argList = new ArrayList<String>();
        // Do the getopt thing.
        char opt = (char) -1;
        String master = null;
        String user = null;
        String password = null;

        for (String arg : args) {
            if (arg.startsWith("-M")) {
                String optArg = arg.substring(2);
                if (optArg.length() == 0) {
                    opt = 'M';
                    continue;
                }
                master = optArg;
            } else if (arg.startsWith("-U")) {
                String optArg = arg.substring(2);
                if (optArg.length() == 0) {
                    opt = 'U';
                    continue;
                }
                user = optArg;
            } else if (arg.startsWith("-P")) {
                String optArg = arg.substring(2);
                if (optArg.length() == 0) {
                    opt = 'P';
                    continue;
                }
                password = optArg;
            } else if (opt != (char) -1) {
                switch (opt) {
                    case 'M': master   = arg; opt = (char) -1; break;
                    case 'U': user     = arg; opt = (char) -1; break;
                    case 'P': password = arg; opt = (char) -1; break;
                }
            } else {
                argList.add(arg);
                opt = (char) -1;
            }
        }

        if (master == null)
            master = "http://localhost:9980/";
        else if (!master.endsWith("/"))
            master += '/';

        CLI cli = new CLI();
        String action = argList.get(0);

        try {
            if ("pending".equals(action)) {
                cli.doGet(master + "pending");
            } else if ("status".equals(action)) {
                if (argList.size() > 1)
                    cli.doGet(master + "status/" + argList.get(1));
                else
                    printUsage();
            } else if ("submit".equals(action)) {
                if (argList.size() > 3) {
                    cli.doPostSubmit(master, user, password, argList);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if ("kill".equals(action)) {
                if (argList.size() > 1) {
                    cli.doPostKill(master, user, password, argList);
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if ("wait".equals(action)) {
                if (argList.size() > 1) {
                    cli.pollStatus(master + "status/" + argList.get(1));
                } else {
                    printUsage();
                    System.exit(1);
                }
            } else if ("showlogs".equals(action)) {
                StringBuilder url = new StringBuilder();
                if (argList.size() > 1) {
                    url.append(master).append("logs/");
                    url.append(argList.get(1));
                } else {
                    printUsage();
                }
                for (int i = 2; i < argList.size(); i++) {
                    if ("-t".equals(argList.get(i)))
                        url.append("/tail");
                    if ("-f".equals(argList.get(i)))
                        url.append("/follow");
                    if ("-ft".equals(argList.get(i)))
                        url.append("/tail/follow");
                    if ("-tf".equals(argList.get(i)))
                        url.append("/tail/follow");
                }
                cli.doGet(url.toString());
            } else {
                printUsage();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        String cmd = System.getProperty("faban.cli.command");
        if (cmd == null)
            cmd = CLI.class.getName();
        cmd += " [-M masterURL] [-U user [-P password]] ";
        System.err.println(cmd + "pending");
        System.err.println(cmd + "status runId");
        System.err.println(cmd + "submit benchmark profile configfile");
        System.err.println(cmd + "kill runId");
        System.err.println(cmd + "wait runId");
        System.err.println(cmd + "showlogs runId [-ft]");
        System.exit(1);
    }

    private void doGet(String url) throws IOException {
        GetMethod get = new GetMethod(url);
        makeRequest(get);

    }

    private void pollStatus(String url) throws IOException {
        GetMethod get = new GetMethod(url);
        for (;;) {
            String status = makeStringRequest(get);
            if ( "COMPLETED".equals(status) ||
                    "FAILED".equals(status) ||
                    "KILLED".equals(status) ) {
                System.out.println(status);
            } else {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doPostSubmit(String master, String user, String password,
                       ArrayList<String> argList) throws IOException {
        String url = master + argList.get(0) + '/' + argList.get(1) + '/' +
                     argList.get(2);
        ArrayList<Part> params = new ArrayList<Part>();
        if (user != null)
            params.add(new StringPart("sun", user));
        if (password != null)
            params.add(new StringPart("sp", password));
        int submitCount = 0;
        for (int i = 3; i < argList.size(); i++) {
            File configFile = new File(argList.get(i));
            if (configFile.isFile()) {
                params.add(new FilePart("configfile", configFile));
                ++submitCount;
            } else {
                System.err.println("File " + argList.get(i) + " not found.");
            }
        }
        if (submitCount == 0) {
            throw new IOException("No run submitted!");
        }
        Part[] parts = new Part[params.size()];
        parts = params.toArray(parts);
        PostMethod post = new PostMethod(url);
        post.setRequestEntity(
                new MultipartRequestEntity(parts, post.getParams()));        
        makeRequest(post);
    }

    private void doPostKill(String master, String user, String password,
                            ArrayList<String> argList) throws IOException {
        String url = master + argList.get(0) + '/' + argList.get(1);
        PostMethod post = new PostMethod(url);
        if (user == null)
            user = "";
        if (password == null)
            password = "";
        post.addParameter("sun", user);
        post.addParameter("sp", password);
        makeRequest(post);
    }

    private void makeRequest(HttpMethodBase method) throws IOException {
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().
                setConnectionTimeout(5000);
        int status = client.executeMethod(method);
        String enc = method.getResponseCharSet();

        InputStream response = method.getResponseBodyAsStream();

        if (status == HttpStatus.SC_NOT_FOUND) {
            System.err.println("Not found!");
            return;
        } else if (status == HttpStatus.SC_NO_CONTENT) {
            System.err.println("Empty!");
            return;
        } else if (response != null) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response, enc));
            String line = null;
            while ((line = reader.readLine()) != null)
                System.out.println(line);
        } else if (status != HttpStatus.SC_OK)
            throw new IOException(HttpStatus.getStatusText(status));
    }

    private String makeStringRequest(HttpMethodBase method) throws IOException {
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().
                setConnectionTimeout(5000);
        int status = client.executeMethod(method);
        String enc = method.getResponseCharSet();

        InputStream response = method.getResponseBodyAsStream();
        StringBuilder buffer = new StringBuilder();
        if (status == HttpStatus.SC_NOT_FOUND) {
            System.err.println("Not found!");
            return buffer.toString();
        } else if (status == HttpStatus.SC_NO_CONTENT) {
            System.err.println("Empty!");
            return buffer.toString();
        } else if (response != null) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response, enc));
            String line = null;
            while ((line = reader.readLine()) != null)
                buffer.append(line).append('\n');
        }
        else if (status != HttpStatus.SC_OK)
            throw new IOException(HttpStatus.getStatusText(status));
        return buffer.toString();
    }
}
