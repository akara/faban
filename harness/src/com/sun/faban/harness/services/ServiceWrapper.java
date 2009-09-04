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

import com.sun.faban.harness.Context;

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

    static final int NOT_STARTED = 0;
    static final int STARTED = 1;
    static final int STOPPED = 2;

    int serviceStatus = NOT_STARTED;

    /**
     * Constructor.
     * @param serviceClass
     * @param ctx
     * @throws java.lang.Exception
     */
    ServiceWrapper(Class serviceClass, ServiceContext ctx) throws Exception {
        service = serviceClass.newInstance();
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
            if (method.getAnnotation(Startup.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (startupMethod == null) {
                    startupMethod = method;
                } else {
                    logger.severe("Error: Multiple @PostRun methods.");
                    //throw new Error ("Multiple @PostRun methods.");
                }
            }
            if (method.getAnnotation(Shutdown.class) != null) {
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
                        logger.warning("More than one valid @Context annotation.");
            }
        }
        if (ctxField != null)
            ctxField.set(service, ctx);
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
    private void clearLogs() throws Exception {
        if (configured && clearLogsMethod != null) {
            try {
                clearLogsMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
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
                configureMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
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
                getConfigMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
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
                getLogsMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
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
                startupMethod.invoke(service,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
        serviceStatus = STARTED;
    }

    /**
     * Invokes service's method annotated by @Shutdown.
     * @throws java.lang.Exception
     */
    void shutdown() throws Exception {
        if (serviceStatus == STARTED && shutdownMethod != null) {
            try {
                shutdownMethod.invoke(service, new Object[]{});
                serviceStatus = STOPPED;
                logger.fine(ctx.desc.id + " Stopped ");
                clearLogs();
                logger.fine("Cleared Logs");
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        } 
    }
}
