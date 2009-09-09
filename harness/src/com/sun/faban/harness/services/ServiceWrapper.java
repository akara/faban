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
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.faban.harness.services;

import com.sun.faban.harness.Configure;
import com.sun.faban.harness.Context;

import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
import com.sun.faban.harness.util.ContextLocation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a wrapper class for service.
 *
 * @author Sheetal Patil
 */
public class ServiceWrapper {

    private static Logger logger =
            Logger.getLogger(ServiceWrapper.class.getName());

    Object service;
    ServiceContext ctx;
    boolean configured = false;
    Method clearLogsMethod;
    Method configureMethod;
    Method getConfigMethod;
    Method getLogsMethod;
    Method startupMethod;
    Method shutdownMethod;

    /**
     * Constructor.
     * @param serviceClass
     * @param ctx
     * @throws java.lang.Exception
     */
    ServiceWrapper(Class serviceClass, ServiceContext ctx) throws Exception {
        this.ctx = ctx;
        Method[] methods = serviceClass.getMethods();
        for (Method method : methods) {
            // Check annotation.
            if (method.getAnnotation(ClearLogs.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (clearLogsMethod == null) {
                    clearLogsMethod = method;
                } else {
                    logger.severe("Error: Multiple @ClearLogs methods.");
                    //throw new Error ("Multiple @Validate methods.");
                }
            }
            if (method.getAnnotation(Configure.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (configureMethod == null) {
                    configureMethod = method;
                } else {
                    logger.severe("Error: Multiple @Configure methods.");
                    //throw new Error ("Multiple @Configure methods.");
                }
            }
            if (method.getAnnotation(GetConfig.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (getConfigMethod == null) {
                    getConfigMethod = method;
                } else {
                    logger.severe("Error: Multiple @Start methods.");
                    //throw new Error ("Multiple @Start methods.");
                }
            }
            if (method.getAnnotation(GetLogs.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (getLogsMethod == null) {
                    getLogsMethod = method;
                } else {
                    logger.severe("Error: Multiple @End methods.");
                    //throw new Error ("Multiple @End methods.");
                }
            }
            if (method.getAnnotation(Start.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (startupMethod == null) {
                    startupMethod = method;
                } else {
                    logger.severe("Error: Multiple @PostRun methods.");
                    //throw new Error ("Multiple @PostRun methods.");
                }
            }
            if (method.getAnnotation(Stop.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (shutdownMethod == null) {
                    shutdownMethod = method;
                } else {
                    logger.severe("Error: Multiple @Kill methods.");
                    //throw new Error ("Multiple @Kill methods.");
                }
            }
        }
        Field ctxField = null;
        Field[] fields = serviceClass.getFields();
        for (Field field : fields) {
            if (field.getType().equals(ServiceContext.class) &&
                    (field.getAnnotation(Context.class) != null)) {
                if (ctxField == null)
                    ctxField = field;
                else
                    logger.warning(
                            "More than one valid @Context annotation.");
            }
        }
        ContextLocation.set(ctx.servicePath);
        try {
            service = serviceClass.newInstance();
            if (ctxField != null)
                ctxField.set(service, ctx);
        } finally {
            ContextLocation.set(null);
        }
    }

    private boolean conformsToSpec(Method method) {
            boolean retval= true;
            // Is it a noarg method?
            if (method.getParameterTypes().length > 0) {
                logger.warning("Method has arguments");
                retval = false;
            }
            // Is it a void method?
            if (!method.getReturnType().equals(Void.TYPE)) {
                logger.warning("Method is not of type Void");
                retval = false;
            }
            return retval;
    }

    private void throwSourceException(InvocationTargetException e)
                throws Exception {
            Throwable t = e.getCause();
            if (t instanceof Exception) {
                logger.log(Level.WARNING, t.getMessage(), t);
                throw (Exception) t;
            } else {
                throw e;
            }
    }

    /**
     * Invokes service's method annotated by @ClearLogs.
     * @throws java.lang.Exception
     */
    void clearLogs() throws Exception {
        if (configured && clearLogsMethod != null) {
            try {
                ContextLocation.set(ctx.servicePath);
                clearLogsMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            } finally {
                ContextLocation.set(null);
            }
        }
    }

    /**
     * Invokes service's method annotated by @Configure.
     * @throws java.lang.Exception
     */
    void configure() throws Exception {
        if (configureMethod != null) {
            try {
                ContextLocation.set(ctx.servicePath);
                configureMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            } finally {
                ContextLocation.set(null);
            }
        }
        configured = true;
    }

    /**
     * Invokes service's method annotated by @GetConfig.
     * @throws java.lang.Exception
     */
    void getConfig() throws Exception {
        if (configured && getConfigMethod != null) {
            try {
                ContextLocation.set(ctx.servicePath);
                getConfigMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            } finally {
                ContextLocation.set(null);
            }
        }
    }

   /**
     * Invokes service's method annotated by @GetLogs.
     * @throws java.lang.Exception
     */
    void getLogs() throws Exception {
        if (configured && getLogsMethod != null) {
            try {
                ContextLocation.set(ctx.servicePath);
                getLogsMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            } finally {
                ContextLocation.set(null);
            }
        }
    }

    /**
     * Invokes service's method annotated by @Startup.
     * @throws java.lang.Exception
     */
    void startup() throws Exception {
        if (configured && startupMethod != null) {
            try {
                ContextLocation.set(ctx.servicePath);
                startupMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            } finally {
                ContextLocation.set(null);
            }
        }
    }

    /**
     * Invokes service's method annotated by @Shutdown.
     * @throws java.lang.Exception
     */
    void shutdown() throws Exception {
        if (configured && shutdownMethod != null) {
            try {
                ContextLocation.set(ctx.servicePath);
                shutdownMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            } finally {
                ContextLocation.set(null);
            }
        }
    }
}
