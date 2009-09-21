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
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Config;

import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClassLoader for benchmarks, services, and possible future deploy images.
 *
 * @author Akara Sucharitakul
 */
public class DeployImageClassLoader extends URLClassLoader {

    private static Logger logger = Logger.getLogger(
                                        DeployImageClassLoader.class.getName());

    // We do not expect getInstance to be executed concurrently. So no
    // synchronization is provided.
    private static final HashMap DEPLOY_LOADERS = new HashMap();

    long timestamp;

    /**
     * Obtains an instance of the classloader for a certain deploy image
     * (benchmark or service).
     * @param type The type of the deploy image, benchmark or service
     * @param dir The directory containing the deploy image
     * @param parent The parent class loader
     * @return The class loader to load the benchmarks or resources
     */
    public static DeployImageClassLoader getInstance(String type,
                                    String dir, ClassLoader parent) {
        File resourceDir = new File(Config.FABAN_HOME + File.separator + type,
                                    dir);

        DeployImageClassLoader loader = (DeployImageClassLoader)
                DEPLOY_LOADERS.get(type + '/' + dir);

        if (loader != null) {
            if (!resourceDir.isDirectory())
                return null;
            if (resourceDir.lastModified() < loader.timestamp)
                return loader;
        }

        File libDir = new File(resourceDir, "lib");

        if (libDir.isDirectory()) {
            File[] jars = libDir.listFiles();
            ArrayList list = new ArrayList(jars.length);
            for (int i = 0; i < jars.length; i++)
                if (jars[i].getName().endsWith(".jar"))
                    try {
                        list.add(jars[i].toURI().toURL());
                    } catch (MalformedURLException e) {
                        logger.log(Level.SEVERE, "Bad file URL!", e);
                        return null;
                    }
            if (list.size() > 0) {
                URL[] jarUrls = new URL[list.size()];
                jarUrls = (URL[]) list.toArray(jarUrls);
                loader = new DeployImageClassLoader(jarUrls, parent);
                DEPLOY_LOADERS.put(type + '/' + dir, loader);
                return loader;
            }
        }
        return null;
    }

    private DeployImageClassLoader(URL[] jars, ClassLoader parent) {
        super(jars, parent);
        timestamp = System.currentTimeMillis();
    }
}
