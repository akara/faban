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
import com.sun.faban.harness.util.Invoker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
     * Constructs a service wrapper.
     * @param serviceClass The service class
     * @param ctx The service context
     * @throws Exception Error creating the service wrapper
     */
    ServiceWrapper(Class serviceClass, ServiceContext ctx) throws Exception {
        this.ctx = ctx;
        Method[] methods = serviceClass.getMethods();
        for (Method method : methods) {
            // Check annotation.
            if (method.getAnnotation(ClearLogs.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (clearLogsMethod == null) {
                    clearLogsMethod = method;
                } else {
                    logger.severe("Error: Multiple @ClearLogs methods.");
                    //throw new Error ("Multiple @Validate methods.");
                }
            }
            if (method.getAnnotation(Configure.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (configureMethod == null) {
                    configureMethod = method;
                } else {
                    logger.severe("Error: Multiple @Configure methods.");
                    //throw new Error ("Multiple @Configure methods.");
                }
            }
            if (method.getAnnotation(GetConfig.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (getConfigMethod == null) {
                    getConfigMethod = method;
                } else {
                    logger.severe("Error: Multiple @Start methods.");
                    //throw new Error ("Multiple @Start methods.");
                }
            }
            if (method.getAnnotation(GetLogs.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (getLogsMethod == null) {
                    getLogsMethod = method;
                } else {
                    logger.severe("Error: Multiple @End methods.");
                    //throw new Error ("Multiple @End methods.");
                }
            }
            if (method.getAnnotation(Start.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (startupMethod == null) {
                    startupMethod = method;
                } else {
                    logger.severe("Error: Multiple @PostRun methods.");
                    //throw new Error ("Multiple @PostRun methods.");
                }
            }
            if (method.getAnnotation(Stop.class) != null) {
                if (!Invoker.isVoidNoArg(method))
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
        Invoker.setContextLocation(ctx.servicePath);
        try {
            service = serviceClass.newInstance();
            if (ctxField != null)
                ctxField.set(service, ctx);
        } catch (InstantiationException e) {
            Invoker.throwCauseException(e);
        } finally {
            Invoker.setContextLocation(null);
        }
    }

    /**
     * Invokes service's method annotated by @ClearLogs.
     * @throws java.lang.Exception
     */
    void clearLogs() throws Exception {
        if (configured)
            Invoker.invoke(service, clearLogsMethod, ctx.servicePath);
    }

    /**
     * Invokes service's method annotated by @Configure.
     * @throws java.lang.Exception
     */
    void configure() throws Exception {
        Invoker.invoke(service, configureMethod, ctx.servicePath);
        configured = true;
    }

    /**
     * Invokes service's method annotated by @GetConfig.
     * @throws java.lang.Exception
     */
    void getConfig() throws Exception {
        if (configured)
            Invoker.invoke(service, getConfigMethod, ctx.servicePath);
    }

   /**
     * Invokes service's method annotated by @GetLogs.
     * @throws java.lang.Exception
     */
    void getLogs() throws Exception {
        if (configured)
            Invoker.invoke(service, getLogsMethod, ctx.servicePath);
    }

    /**
     * Invokes service's method annotated by @Startup.
     * @throws java.lang.Exception
     */
    void startup() throws Exception {
        if (configured)
            Invoker.invoke(service, startupMethod, ctx.servicePath);
    }

    /**
     * Invokes service's method annotated by @Shutdown.
     * @throws java.lang.Exception
     */
    void shutdown() throws Exception {
        if (configured)
            Invoker.invoke(service, shutdownMethod, ctx.servicePath);
    }
}
