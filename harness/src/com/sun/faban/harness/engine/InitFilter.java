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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Filter to ensure proper initialization of the Faban system on first request
 * to any Faban resource.
 *
 * @author Akara Sucharitakul
 */
public class InitFilter implements Filter {

    ServletContext ctx;

    public void init(FilterConfig filterConfig) throws ServletException {
        ctx = filterConfig.getServletContext();
    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = null;
        try {
            resp = (HttpServletResponse) response;
            HttpServletRequest req = (HttpServletRequest) request;
            Engine.initIfNotInited(ctx, req);
        } catch (Throwable e) {
            Logger.getLogger(this.getClass().getName()).
                                        log(Level.SEVERE, e.getMessage(), e);
            if (resp != null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                e.getMessage());
                resp.flushBuffer();
            }
            return;
        }
        chain.doFilter(request, response);
    }

    public void destroy() {
        Engine.destroy();
    }
}
