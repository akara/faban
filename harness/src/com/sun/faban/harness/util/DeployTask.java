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
 * $Id: DeployTask.java,v 1.4 2006/09/27 23:25:00 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.util;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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
     * Sets the target URL to the Faban deployment servlet
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
            MultipartPostMethod post = new MultipartPostMethod(
                    target + "/deploy");
            if (user != null)
                post.addParameter("user", user);
            if (password != null)
                post.addParameter("password", password);
            if (clearConfig)
                post.addParameter("clearconfig", "true");
            post.addParameter(jarFile.getName(), jarFile);
            HttpClient client = new HttpClient();
            client.setConnectionTimeout(5000);
            int status = client.executeMethod(post);
            if (status == HttpStatus.SC_CONFLICT)
                throw new BuildException("Benchmark to deploy is currently " +
                        "run or queued to be run. Please clear run queue " +
                        "of this benchmark before deployment");
            else if (status != HttpStatus.SC_CREATED)
                throw new BuildException("Faban responded with status code " +
                        status + ". Status code 201 (SC_CREATED) expected.");
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        } catch (HttpException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
}
