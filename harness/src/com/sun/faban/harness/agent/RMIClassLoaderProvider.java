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
package com.sun.faban.harness.agent;

import java.rmi.server.RMIClassLoaderSpi;
import java.rmi.server.RMIClassLoader;
import java.net.MalformedURLException;
import java.io.File;

/**
 * The RMIClassLoaderProvider extends the RMIClassLoaderSpi and allows
 * the agent to find the benchmark classes in-process. This is needed to execute
 * remote commands from Java.
 */
public class RMIClassLoaderProvider extends RMIClassLoaderSpi {

    private RMIClassLoaderSpi delegate =
                            RMIClassLoader.getDefaultProviderInstance();

    /**
     * Prepends the benchmark jars to the code base.
     * @param codebase The original codebase
     * @return The new codebase with the benchmark jars
     */
    private String completeCodeBase(String codebase) {
        String[] classpath = null;
        if (AgentBootstrap.cmd != null) {
            if (AgentBootstrap.cmd.allClassPath != null)
                classpath = AgentBootstrap.cmd.allClassPath;
            else
                classpath = AgentBootstrap.cmd.baseClassPath;
        }
        StringBuilder buffer = new StringBuilder();
        if (classpath != null) {
            for (String element : classpath) {
                buffer.append(new File(element).toURI());
                buffer.append(' ');
            }
        }

        if (codebase != null)
            buffer.append(codebase);

        int lastIdx = buffer.length() - 1;
        if (lastIdx != -1 && buffer.charAt(lastIdx) == ' ')
            buffer.setLength(lastIdx);

        if (buffer.length() > 0)
            codebase = buffer.toString();

        return codebase;
    }

    /**
     * Provides the implementation for
     * {@link java.rmi.server.RMIClassLoader#loadClass(java.net.URL,String)},
     * {@link java.rmi.server.RMIClassLoader#loadClass(String,String)}, and
     * {@link java.rmi.server.RMIClassLoader#loadClass(String,String,ClassLoader)}.
     * <p/>
     * Loads a class from a codebase URL path, optionally using the
     * supplied loader.
     * <p/>
     * Typically, a provider implementation will attempt to
     * resolve the named class using the given <code>defaultLoader</code>,
     * if specified, before attempting to resolve the class from the
     * codebase URL path.
     * <p/>
     * <p>An implementation of this method must either return a class
     * with the given name or throw an exception.
     *
     * @param	codebase the list of URLs (separated by spaces) to load
     * the class from, or <code>null</code>
     * @param	name the name of the class to load
     * @param	defaultLoader additional contextual class loader
     * to use, or <code>null</code>
     * @return	the <code>Class</code> object representing the loaded class
     * @throws	java.net.MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to load classes is invalid
     * @throws	ClassNotFoundException if a definition for the class
     * could not be found at the specified location.
     */
    public Class<?> loadClass(String codebase, String name,
                              ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException {
        return delegate.loadClass(completeCodeBase(codebase),
                                    name, defaultLoader);
    }

    /**
     * Provides the implementation for
     * {@link java.rmi.server.RMIClassLoader#loadProxyClass(String,String[],ClassLoader)}.
     * <p/>
     * Loads a dynamic proxy class (see {@link java.lang.reflect.Proxy}
     * that implements a set of interfaces with the given names
     * from a codebase URL path, optionally using the supplied loader.
     * <p/>
     * <p>An implementation of this method must either return a proxy
     * class that implements the named interfaces or throw an exception.
     *
     * @param	codebase the list of URLs (space-separated) to load
     * classes from, or <code>null</code>
     * @param	interfaces the names of the interfaces for the proxy class
     * to implement
     * @return	a dynamic proxy class that implements the named interfaces
     * @param	defaultLoader additional contextual class loader
     * to use, or <code>null</code>
     * @throws	java.net.MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to load classes is invalid
     * @throws	ClassNotFoundException if a definition for one of
     * the named interfaces could not be found at the specified location,
     * or if creation of the dynamic proxy class failed (such as if
     * {@link java.lang.reflect.Proxy#getProxyClass(ClassLoader,Class[])}
     * would throw an <code>IllegalArgumentException</code> for the given
     * interface list)
     */
    public Class<?> loadProxyClass(String codebase, String[] interfaces,
                                   ClassLoader defaultLoader)
            throws MalformedURLException, ClassNotFoundException {
        return delegate.loadProxyClass(completeCodeBase(codebase), interfaces,
                                        defaultLoader);
    }

    /**
     * Provides the implementation for
     * {@link java.rmi.server.RMIClassLoader#getClassLoader(String)}.
     * <p/>
     * Returns a class loader that loads classes from the given codebase
     * URL path.
     * <p/>
     * <p>If there is a security manger, its <code>checkPermission</code>
     * method will be invoked with a
     * <code>RuntimePermission("getClassLoader")</code> permission;
     * this could result in a <code>SecurityException</code>.
     * The implementation of this method may also perform further security
     * checks to verify that the calling context has permission to connect
     * to all of the URLs in the codebase URL path.
     *
     * @return a class loader that loads classes from the given codebase URL
     *         path
     * @param	codebase the list of URLs (space-separated) from which
     * the returned class loader will load classes from, or <code>null</code>
     * @throws	java.net.MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to identify the class loader is invalid
     * @throws	SecurityException if there is a security manager and the
     * invocation of its <code>checkPermission</code> method fails, or
     * if the caller does not have permission to connect to all of the
     * URLs in the codebase URL path
     */
    public ClassLoader getClassLoader(String codebase)
            throws MalformedURLException { // SecurityException
        return delegate.getClassLoader(completeCodeBase(codebase));
    }

    /**
     * Provides the implementation for
     * {@link java.rmi.server.RMIClassLoader#getClassAnnotation(Class)}.
     * <p/>
     * Returns the annotation string (representing a location for
     * the class definition) that RMI will use to annotate the class
     * descriptor when marshalling objects of the given class.
     *
     * @param	cl the class to obtain the annotation for
     * @return	a string to be used to annotate the given class when
     * it gets marshalled, or <code>null</code>
     * @throws	NullPointerException if <code>cl</code> is <code>null</code>
     */
    public String getClassAnnotation(Class<?> cl) {
        return delegate.getClassAnnotation(cl);
    }
}
