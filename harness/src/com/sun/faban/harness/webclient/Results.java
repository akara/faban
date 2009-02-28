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
 * $Id: Results.java,v 1.2 2009/02/28 04:35:05 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.webclient.RunResult.FeedRecord;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author akara
 */
public class Results {
    private static Logger logger =
        Logger.getLogger(Results.class.getName());
    private static SimpleDateFormat formatOrig =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public String list(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        UserEnv usrEnv = getUserEnv(req);
        String tags = req.getParameter("inputtag");
        TableModel resultTable = null;
        if (tags != null) {
            resultTable = RunResult.getResultTable(usrEnv.getSubject(), tags);
        } else {
            resultTable = RunResult.getResultTable(usrEnv.getSubject());
    }
        req.setAttribute("table.model", resultTable);
        return "/resultlist.jsp";
    }

    public void feed(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        UserEnv usrEnv = getUserEnv(req);
        String[] restRequest = (String[]) req.getAttribute("rest.request");
        List<FeedRecord> itemList;
        if (restRequest != null) {
            itemList = RunResult.getFeeds(usrEnv.getSubject(), restRequest);
        } else {
            itemList = RunResult.getFeeds(usrEnv.getSubject());
        }

        // The first result is the most up-to-date.
        SimpleDateFormat format = (SimpleDateFormat) formatOrig.clone();
        for (FeedRecord feedItem : itemList) {
            feedItem.updated = format.format(new Date(feedItem.date));
        }
        req.setAttribute("feed.model", itemList);
        req.setAttribute("request.url", req.getRequestURL());

        String updated;
        if (itemList.size() > 0) {
            FeedRecord item0 = itemList.get(0);
            updated = item0.updated;
        } else {
            updated = format.format(new Date(0));
        }
        req.setAttribute("feed.updated", updated);
    }

    private UserEnv getUserEnv(HttpServletRequest req) {
        HttpSession session = req.getSession();
        UserEnv usrEnv = (UserEnv) session.getAttribute("usrEnv");
        if (usrEnv == null) {
            usrEnv = new UserEnv();
            session.setAttribute("usrEnv", usrEnv);
        }
        return usrEnv;
    }
}
