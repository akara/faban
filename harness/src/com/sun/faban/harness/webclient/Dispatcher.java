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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The dispatcher is introduced to reorganize the web interface using clean
 * MVC interfaces. Not all of Faban is using the dispatcher just yet but
 * will gradually over time (as it is impossible to re-write everything
 * at once. The dispatcher will dispatch requests to controller classes with
 * the following spec:<ul>
 * <li>The request http://site:9980/controller/my_controller/my_action will
 * map to a call to the controller class MyController in this package and the
 * method myAction. In the URL, we use the underbar character ('_') as the
 * word separator. When translated to Java classes or methods, this will be
 * mapped to capitalization of the word beginnings. The underbar is removed.
 * A class name is capitalized, while a method name will start with lower case.
 * </li>
 * <li>The controller class can be any plain old Java object (POJO) with
 * a public no-arg constructor.</li>
 * <li>The action methods must be public methods taking two arguments of type
 * HttpServletRequest and HttpServletResponse. There is no limit on the number
 * of exceptions the action methods can throw.</li>
 * <li>The action methods may be of return type void or of any other
 * return type.</li>
 * <li> The return value, if not null or not of void return type, is translated
 * to a string using the Object.toString() method. The return value determines
 * the view JSP to be invoked. Sensibly, any CharSequence type would make a
 * good return type.</li>
 * </ul>
 *
 * @author Akara Sucharitakul
 */
public class Dispatcher extends HttpServlet {

    static final ConcurrentHashMap<String, Class> CLASS_CACHE =
            new ConcurrentHashMap<String, Class>();

    static final ConcurrentHashMap<String, Method> METHOD_CACHE =
            new ConcurrentHashMap<String, Method>();

    String controllerPkg;

    @Override public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllerPkg = config.getInitParameter("controller.package");
    }

    @Override protected void doGet(HttpServletRequest request,
                                    HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override protected void doPost(HttpServletRequest request,
                                    HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getContextPath();
        String uri = request.getRequestURI();

        // Safety check making sure the resource is part of URI.
        if (!uri.startsWith(path)) {
            ServletOutputStream out = response.getOutputStream();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Could not identify requested resource.");
            out.flush();
            out.close();
            return;
        }

        // Now create a controller path with the context and servlet path
        // taken out.
        path = uri.substring(path.length());

        // Now we have a controller e.g. /controller/output/...
        // We need to know the resource type by extracting the next level.
        ArrayList<String> pathList = new ArrayList<String>();
        StringTokenizer t = new StringTokenizer(path, "/");
        while (t.hasMoreTokens())
            pathList.add(t.nextToken());

        String[] pathElements = new String[pathList.size()];
        pathElements = pathList.toArray(pathElements);

        // The controller will be pathElements[1] and the action will be
        // pathElements[2]. But sometimes the action is also passed by the
        // request parameter "action" in case multiple submit buttons are used.
        String action;
        if (pathElements.length < 3) {
            action = request.getParameter("action");
        } else {
            action = pathElements[2];
            if (pathElements.length > 3) {
                String[] restRequest = new String[pathElements.length - 3];
                for (int i = 3; i < pathElements.length; i++)
                    restRequest[i - 3] = pathElements[i];
                request.setAttribute("rest.request", restRequest);
            }
        }


        // For now, we use hard-coded dispatching. It is a little ugly and
        // should be changed to dynamic dispatching as soon as we get the
        // MVC framework in a better shape.
        //
        // First, see whether we have a controller in our session.
        // Remember, the controller instances stay with our session
        // so that the controllers can keep their state in their local
        // variable.
        String target = null;
        HttpSession session = request.getSession();
        Object controller = session.getAttribute(pathElements[1]);
        Class controllerClass = null;
        String controllerName = controllerPkg + '.' +
                                    toClassName(pathElements[1]);

        // If controller not found in session...
        if (controller == null ||
                !controllerName.equals(controller.getClass().getName())) {
            // Try to find controller in cache.
            controllerClass = CLASS_CACHE.get(pathElements[1]);
            try {
                if (controllerClass == null) {
                    // Otherwise, lookup the class.
                    controllerClass = Class.forName(controllerName);
                    CLASS_CACHE.put(pathElements[1], controllerClass);
                }
                // And create new instance.
                controller = controllerClass.newInstance();
                session.setAttribute(pathElements[1], controller);
            } catch (ClassNotFoundException e) {
                throw new ServletException("Cannot find controller class " +
                        controllerName, e);
            } catch (InstantiationException e) {
                throw new ServletException("Error creating controller class " +
                        controllerName, e);
            } catch (IllegalAccessException e) {
                throw new ServletException("No public noarg constructor for " +
                        "class " + controllerName, e);
            }
        }

        // Translate action to method.
        String fullAction = pathElements[1] + '/' + action;

        // Lookup from cache.
        Method method = METHOD_CACHE.get(fullAction);
        if (method == null) {
            String methodName = toMethodName(action);
            Class[] paramTypes = new Class[2];
            paramTypes[0] = HttpServletRequest.class;
            paramTypes[1] = HttpServletResponse.class;
            if (controllerClass == null)
                controllerClass = controller.getClass();
            try {
                method = controllerClass.getMethod(methodName, paramTypes);
                METHOD_CACHE.put(fullAction, method);
            } catch (NoSuchMethodException e) {
                throw new ServletException("Cannot find action " + fullAction,
                                                                            e);
            }
        }

        try {
            Object r = method.invoke(controller, request, response);
            if (!response.isCommitted()) {
                if (r == null)
                    target = "/" + action + ".jsp";
                else
                    target = r.toString(); // So we can support charsequence
                                           // return types, too.
                if (target.charAt(0) != '/')
                    target = '/' + target;
                render(target, request, response);
            }
        } catch (ClassCastException e) {
            throw new ServletException("Action " + fullAction +
                    "implementation has invalid return type", e);
        } catch (IllegalAccessException e) {
            throw new ServletException("Action " + fullAction +
                    "implementation not public", e);
        } catch (InvocationTargetException e) {
            Throwable t1 = e.getCause();
            if (t1 == null)
                throw new ServletException("Error invoking action " +
                        fullAction, e);
            while (t1 != null) {
                if (t1 instanceof ServletException) {
                    throw (ServletException) t1;
                } else if (t1 instanceof IOException) {
                    throw (IOException) t1;
                } else {
                    Throwable t2 = t1.getCause();
                    if (t2 == null) {
                        throw new ServletException("Error invoking action " +
                                fullAction, t1);
                    } else {
                        t1 = t2;
                    }
                }
            }
        }
    }

    private void render(String target, HttpServletRequest request,
                        HttpServletResponse response)
            throws IOException, ServletException {
        if (target != null) {
            RequestDispatcher rd = request.getRequestDispatcher(target);
            rd.forward(request, response);
        }
    }

    /**
     * Translates resource references from url reference to Java
     * class names. For example, foo_bar will translate to FooBar.
     * @param resource The resource name
     * @return The Java class name
     */
    private String toClassName(String resource) {
        StringTokenizer t = new StringTokenizer(resource, "_");
        StringBuilder b = new StringBuilder();
        while (t.hasMoreTokens()) {
            String part = t.nextToken();
            int idx = b.length();
            b.append(part);
            b.setCharAt(idx, Character.toUpperCase(b.charAt(idx)));
        }
        return b.toString();
    }

    /**
     * Translates action references from url to Java method names. For
     * example, foo_bar will translate to fooBar.
     * @param resource The resource name
     * @return The method name
     */
    private String toMethodName(String resource) {
        StringTokenizer t = new StringTokenizer(resource, "_");
        StringBuilder b = new StringBuilder();
        b.append(t.nextToken());
        while (t.hasMoreTokens()) {
            String part = t.nextToken();
            int idx = b.length();
            b.append(part);
            b.setCharAt(idx, Character.toUpperCase(b.charAt(idx)));
        }
        return b.toString();
    }
}
