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
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Ant task for deployment of benchmarks. Usage is as follows:<p>
 * &lt;deploy url="${faban.url}/deploy" jar="${benchmark.name}.jar"/&gt;
 *
 * @author Akara Sucharitakul
 */
public class DeployTask extends Task {

    private String target;
    private File jarFile;
    private boolean clearConfig = false;
    private String user;
    private String password;

    /**
     * Sets the target URL to the Faban deployment servlet.
     * @param target The deployment servlet
     */
    public void setUrl(String target) {
        this.target = target;
    }

    /**
     * Sets the user name for deployment.
     * Note that the user/password is not checked if the Faban harness
     * security is turned off.
     * @param user The user name
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Sets the password for deployment.
     * Note that the user/password is not checked if the Faban harness
     * security is turned off.
     * @param password The user's password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the jar file to be deployed.
     * @param jarFile The benchmark jar
     */
    public void setJar(File jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * Sets whether to clear the configuration or not. Defaults to false.
     * @param clear Whether to clear old config files.
     */
    public void setClearConfig(boolean clear) {
        this.clearConfig = clear;
    }

    /**
     * Executes the Faban deployment task.
     * @throws BuildException If there is an
     */
    public void execute() throws BuildException {
        try {
            // First check the jar file and name
            if (jarFile == null)
                throw new BuildException(
                        "jar attribute missing for target deploy.");
            if (!jarFile.isFile())
                throw new BuildException("jar file not found.");
            String jarName = jarFile.getName();
            if (!jarName.endsWith(".jar"))
                throw new BuildException(
                        "Jar file name must end with \".jar\"");
            jarName = jarName.substring(0, jarName.length() - 4);
            if (jarName.indexOf('.') > -1)
                throw new BuildException("Jar file name must not have any " +
                        "dots except ending with \".jar\"");

            // Prepare the parts for the request.
            ArrayList<Part> params = new ArrayList<Part>(4);
            if (user != null)
                params.add(new StringPart("user", user));
            if (password != null)
                params.add(new StringPart("password", password));
            if (clearConfig)
                params.add(new StringPart("clearconfig", "true"));
            params.add(new FilePart("jarfile", jarFile));
            Part[] parts = new Part[params.size()];
            parts = params.toArray(parts);

            // Prepare the post method.
            PostMethod post = new PostMethod(target + "/deploy");

            // Ensure text/plain is the only accept header.
            post.removeRequestHeader("Accept");
            post.setRequestHeader("Accept", "text/plain");
            post.setRequestEntity(
                    new MultipartRequestEntity(parts, post.getParams()));

            // Execute the multi-part post method.
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().
                    setConnectionTimeout(5000);
            int status = client.executeMethod(post);
            StringBuilder b = new StringBuilder();
            InputStream respBody  = post.getResponseBodyAsStream();
            byte[] readBuffer = new byte[8192];
            for (;;) {
                int size = respBody.read(readBuffer);
                if (size == -1)
                    break;
                b.append(new String(readBuffer, 0, size));
            }
            String response = b.toString();

            // Check status.
            if (status == HttpStatus.SC_CONFLICT) {
                handleErrorFlush(response);
                throw new BuildException("Benchmark to deploy is currently " +
                        "run or queued to be run. Please clear run queue " +
                        "of this benchmark before deployment");
            } else if (status == HttpStatus.SC_NOT_ACCEPTABLE) {
                handleErrorFlush(response);
                throw new BuildException("Benchmark deploy name or deploy " +
                        "file invalid. Deploy file may contain errors. Name " +
                        "must have no '.' and file must have the " +
                        "'.jar' extensions.");
            } else if (status != HttpStatus.SC_CREATED) {
                handleOutput(response);
                throw new BuildException("Faban responded with status code " +
                        status + ". Status code 201 (SC_CREATED) expected.");
            }
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        } catch (HttpException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
}
