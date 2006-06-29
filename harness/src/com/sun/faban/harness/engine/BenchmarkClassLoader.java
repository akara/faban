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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: BenchmarkClassLoader.java,v 1.1 2006/06/29 18:51:42 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
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
 * ClassLoader for benchmarks.
 *
 * @author Akara Sucharitakul
 */
public class BenchmarkClassLoader extends URLClassLoader {

    private static Logger logger = Logger.getLogger(
                                        BenchmarkClassLoader.class.getName());

    // We do not expect getInstance to be executed concurrently. So no
    // synchronization is provided.
    private static final HashMap BENCH_LOADERS = new HashMap();

    public long timestamp;

    public static BenchmarkClassLoader getInstance(String shortName,
                                                   ClassLoader parent) {
        BenchmarkClassLoader loader = (BenchmarkClassLoader)
                BENCH_LOADERS.get(shortName);

        if (loader != null) {
            File benchDir = new File(Config.BENCHMARK_DIR, shortName);
            if (!benchDir.isDirectory())
                return null;
            if (benchDir.lastModified() < loader.timestamp)
                return loader;
        }

        File libDir = new File(Config.BENCHMARK_DIR + shortName +
                File.separator + "lib");
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
                loader = new BenchmarkClassLoader(jarUrls, parent);
                BENCH_LOADERS.put(shortName, loader);
                return loader;
            }
        }
        return null;
    }

    private BenchmarkClassLoader(URL[] jars, ClassLoader parent) {
        super(jars, parent);
        timestamp = System.currentTimeMillis();
    }
}
