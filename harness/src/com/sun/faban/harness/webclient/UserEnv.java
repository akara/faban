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
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The user environemnt is kept in the user's session at all times. It contains
 * many of Faban's necessary states.
 */
public class UserEnv {

    Logger logger = Logger.getLogger(this.getClass().getName());
    Authenticator auth;

    /**
     * Obtains the authenticator used for user authentication.
     * This call returns null only if security is disabled.
     * @return The active authenticator for this session
     */
    public Authenticator getAuthenticator() {
        if (Config.SECURITY_ENABLED && auth == null)
            auth = new Authenticator();
        return auth;
    }

    /**
     * Obtains the login name or id of the current user.
     * @return The login name or id, or null if not logged in.
     */
    public String getUser() {
        String user = null;
        if (auth != null)
            user = auth.getLogin();
        return user;
    }

    /**
     * Obtains the subject of the current user.
     * @return The user's subject, or null if not logged in.
     */
    public Subject getSubject() {
        Subject user = null;
        if (auth != null)
            user = auth.getSubject();
        return user;
    }

    /**
     * Obtains a list of current profiles set up on this Faban harness instance.
     * @return The list of profiles.
     */
    public String[] getProfiles() {
        String[] profiles = null;
        String fileName = Config.PROFILES_DIR;
        logger.fine("Looking for list of Benchmarks in " + fileName);
        try {
            File f = new File(fileName);
            if(f.exists() && f.isDirectory()) {
                File [] dir = f.listFiles();
                ArrayList profileList = new ArrayList();
                for(int i = 0; i < dir.length; i++)
                    if(dir[i].isDirectory() && !(dir[i].getName().equals("default")))
                        profileList.add(dir[i].getName());

                if(profileList.size() > 0) {
                    profiles = new String[profileList.size()];
                    profiles = (String[])profileList.toArray(profiles);
                }
            }
            else
                logger.warning("Unable to locate profiles in " + fileName);
        }catch(Exception e) {
            logger.severe("Error reading " + fileName);
            logger.log(Level.INFO, "Exception", e);
        }
        return profiles;
    }

    /**
     * Saves the config file to the given profile.
     * @param profile The profile to save to
     * @param desc The benchmark description
     * @param buf The buffer containing the run config file
     */
    public void saveParamRepository(String profile, BenchmarkDescription desc,
                                    char[] buf) {

        String destFile = null;

        try {

            String usrDir = Config.PROFILES_DIR + profile;
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
                logger.fine("Creating new profile directory for " + profile);
                if(dir.mkdirs())
                    logger.fine("Created new profile directory " + usrDir);
                else
                    logger.severe("Failed to create profile directory " + usrDir);
            }

            // Save the latest config file into the profile directory
            String dstFile = Config.PROFILES_DIR + profile + File.separator +
                    desc.configFileName + "." + desc.shortName;

            FileWriter writer = new FileWriter(dstFile);
            writer.write(buf);
            writer.close();

        } catch(Exception e) {
            logger.log(Level.SEVERE, "Unable to write " + destFile + '.', e);
        }
    }
}
