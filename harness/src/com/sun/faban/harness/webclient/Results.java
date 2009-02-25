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
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.webclient.Result.ResultField;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author akara
 */
public class Results {
    private static Logger logger =
        Logger.getLogger(Results.class.getName());

    public String list(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        UserEnv usrEnv = (UserEnv) req.getSession().getAttribute("usrEnv");
        String tags = req.getParameter("inputtag");
        String[] tagsArray = null;
        if (tags != null || "".equals(tags)) {
            StringTokenizer tok = new StringTokenizer(tags, " ,:;");
            tagsArray = new String[tok.countTokens()];
            int count = tok.countTokens();
            int i = 0;
            while (i < count) {
                String nextT = tok.nextToken().trim();
                tagsArray[i] = nextT;
                i++;
            }
        }
        TableModel resultTable = null;
        if(tagsArray != null){
            try {
                TagEngine tagEngine = TagEngine.getInstance();
                Set<String> answer = tagEngine.search(tagsArray);
                resultTable = Result.getTagSearchResultTable(answer, usrEnv.getSubject());
            } catch (ClassNotFoundException ex) {
                throw new IOException("Error obtaining tag engine.", ex);
            }
        }else {
            resultTable = Result.getResultTable(usrEnv.getSubject());
        }
        req.setAttribute("table.model", resultTable);
        return "/resultlist.jsp";
    }

    public String feed(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        UserEnv usrEnv = (UserEnv) req.getSession().getAttribute("usrEnv");
        String[] restRequest = (String[]) req.getAttribute("rest.request");
        TableModel resultTable = null;
        if (restRequest != null) {
            try {
                TagEngine tagEngine = TagEngine.getInstance();
                Set<String> answer = tagEngine.search(restRequest);
                resultTable = Result.getTagSearchResultTable(answer, usrEnv.getSubject());
            } catch (ClassNotFoundException ex) {
                throw new IOException("Error obtaining tag engine.", ex);
            }
        } else {
            resultTable = Result.getResultTable(usrEnv.getSubject());
        }
        // The first result is the most up-to-date.
        ResultField<Long> updatedField = (ResultField<Long>)
                resultTable.getField(0, 6);
        long updated = updatedField.value;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ssZ");
        req.setAttribute("table.model", resultTable);
        req.setAttribute("request.url", req.getRequestURL());
        req.setAttribute("feed.updated", new Date(updated));
        req.setAttribute("feed.dateformat", format);

        return "/feed.jsp";

    }
}
