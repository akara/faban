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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common invocation utilities used by wrappers.
 * @author Akara Sucharitakul
 */
public class Invoker {

    private static Logger logger = Logger.getLogger(Invoker.class.getName());

    private static final InheritableThreadLocal<String> location =
            new InheritableThreadLocal<String>();

    private static final Object[] NO_ARGS = new Object[0];

    /**
     * Throws the cause of an exception.
     * @param e The exception
     * @throws Exception The cause
     */
    public static void throwCauseException(Exception e)
            throws Exception {
        Throwable t = e.getCause();
        if (t == null) {
            throw e;
        } else if (t instanceof Exception) {
            throw (Exception) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            logger.log(Level.WARNING, e.getClass().getName() +
                    " with cause of unknown type.", t);
            throw e;
        }
    }

    /**
     * Checks a method whether it is a void, no-argument public method.
     * @param m The method to be checked
     * @return Whether the method conforms to invocation spec or not
     */
    public static boolean isVoidNoArg(Method m) {
        boolean retval= true;
        // Is it a noarg method?
        if (m.getParameterTypes().length > 0) {
            logger.warning(m.toString() + ": Method has arguments");
            retval = false;
        }
        // Is it a void method?
        if (!m.getReturnType().equals(Void.TYPE)) {
            logger.warning(m.toString() + ": Method is not of type Void");
            retval = false;
        }
        return retval;
    }

    /**
     * Invokes a method given a context location.
     * Skips any methods that are null.
     * @param o The object to invoke the method on
     * @param m The method
     * @param location The context location
     * @throws Exception Exceptions, if any
     */
    public static void invoke(Object o, Method m, String location)
            throws Exception {
        if (m != null) {
            setContextLocation(location);
            try {
                m.invoke(o, NO_ARGS);
            } catch (InvocationTargetException e) {
                throwCauseException(e);
            } finally {
                setContextLocation(null);
            }
        }
    }

    /**
     * Invokes a method with no context location.
     * Skips any methods that are null.
     * @param o The object to invoke the method on
     * @param m The method
     * @throws Exception Exceptions, if any
     */
    public static void invoke(Object o, Method m)
            throws Exception {
        if (m != null) {
            try {
                m.invoke(o, NO_ARGS);
            } catch (InvocationTargetException e) {
                throwCauseException(e);
            }
        }
    }

    /**
     * Sets a new context location.
     * @param newLocation The new context location
     */
    public static void setContextLocation(String newLocation) {
        location.set(newLocation);
    }

    /**
     * Obtains the current context location.
     * @return The current context location
     */
    public static String getContextLocation() {
        return location.get();
    }
}
