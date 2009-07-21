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
 * $Id: RemoteLogFormatter.java,v 1.3 2009/07/21 22:54:46 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;

public class RemoteLogFormatter extends XMLFormatter {
    private String ident;

    public RemoteLogFormatter() {
        super();
        StringBuffer sb = new StringBuffer("<record>\n  <host>");
        try {
            sb.append((InetAddress.getLocalHost()).getHostName());
        }
        catch (java.net.UnknownHostException uhe) {
            sb.append("unknown");
        }
        sb.append("</host>");
        ident = sb.toString();

    }

    /**
     *
     * @param record.
     * @return return with the host added to the log record.
     */
    public String format(LogRecord record) {
        String log = super.format(record);

        log = log.replaceFirst("<record>", ident);
        return log;
    }


    /**
     *
     * @param h
     * @return return an empty string
     */
    public String getHead(Handler h) {
        return "";
    }

    /**
     *
     * @param h
     * @return return an empty string
     */
    public String getTail(Handler h) {
        return "";
    }
}
