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
 * $Id: UserEnv.java,v 1.2 2006/06/29 19:38:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.FileHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserEnv {

    Logger logger;

    public UserEnv() {
        logger = Logger.getLogger(this.getClass().getName());
    }

    public String[] getBenchmarks() {
        String[] benchmarks = null;
        String fileName = Config.BENCH_FILE;
        logger.fine("Looking for list of Benchmarks in " + fileName);
        try {
            File bm = new File(fileName);
            if(bm.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(bm));
                String line = reader.readLine();
                ArrayList benchList = new ArrayList();
                while(line != null) {
                    line = line.trim();
                    if (!line.startsWith("#"))
                        benchList.add(line);
                    line = reader.readLine();
                }
                benchmarks = new String[benchList.size()];
                benchmarks = (String[]) benchList.toArray(benchmarks);
            }
        }catch(Exception e) {
            logger.severe("Error reading " + fileName);
            logger.log(Level.INFO, "Exception", e);
        }
        return benchmarks;
    }

    public String[] getUsers() {
        String[] users = null;
        String fileName = Config.USERS_DIR;
        logger.fine("Looking for list of Benchmarks in " + fileName);
        try {
            File f = new File(fileName);
            if(f.exists() && f.isDirectory()) {
                File [] dir = f.listFiles();
                ArrayList userList = new ArrayList();
                for(int i = 0; i < dir.length; i++)
                    if(dir[i].isDirectory() && !(dir[i].getName().equals("default")))
                        userList.add(dir[i].getName());

                if(userList.size() > 0) {
                    users = new String[userList.size()];
                    users = (String[])userList.toArray(users);
                }
            }
            else
                logger.severe("Unable to locate users in " + fileName);
        }catch(Exception e) {
            logger.severe("Error reading " + fileName);
            logger.log(Level.INFO, "Exception", e);
        }
        return users;
    }

    public void copyParamRepository(String user, BenchmarkDescription desc) {
        String srcFile = Config.USERS_DIR + user + File.separator +
                         desc.configFileName + "." + desc.shortName;
        File f = new File(srcFile);

        String dstFile = "/tmp/" + desc.configFileName;
        if(!f.exists()) // Use the default config file
            srcFile = Config.BENCHMARK_DIR + File.separator + desc.shortName +
                      File.separator + "META-INF" + File.separator +
                      desc.configFileName;

        FileHelper.copyFile(srcFile, dstFile, false);
    }

    public void saveParamRepository(String user, BenchmarkDescription desc,
                                    char[] buf) {

        String srcFile = "/tmp/" + desc.configFileName;

        // Save it into the /tmp/run.xml
        try {
            FileWriter writer = new FileWriter(srcFile);
            writer.write(buf);
            writer.close();


            String usrDir = Config.USERS_DIR + user;
            File dir = new File(usrDir);
            if(dir.exists()) {
                if(!dir.isDirectory()) {
                     logger.severe(usrDir + " should be a directory");
                    dir.delete();
                    logger.fine(dir + " deleted");
                }
                else
                    logger.fine("Saving parameter file to" + usrDir);
            }
            else {
                logger.fine("Creating new user directory for " + user);
                if(dir.mkdirs())
                    logger.fine("Created new user directory " + usrDir);
                else
                    logger.severe("Failed to create user directory " + usrDir);
            }

            // Save the latest config file into the users directory
            String dstFile = Config.USERS_DIR + user + File.separator +
                    desc.configFileName + "." + desc.shortName;

            FileHelper.copyFile(srcFile, dstFile, false);
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Unable to write " + srcFile + '.', e);
        }
    }
}
