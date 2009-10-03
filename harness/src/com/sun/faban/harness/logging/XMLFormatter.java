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
package com.sun.faban.harness.logging;

import java.util.logging.Handler;
// import java.util.logging.LogRecord;
import java.nio.charset.Charset;

/**
 * Overrides the XML header provided by the XMLFormatter in the JavaSE logging
 * package. The original/superclass puts an absolute reference to a DTD into
 * the XML header. This DTD never exists in the wanted places causing the
 * parser to bail out. On some versions of the XML parser we can set the
 * feature http://apache.org/xml/features/nonvalidating/load-external-dtd
 * to false and therefore ignore the DTD header altogether, but this causes
 * problems in parsers where the feature is not supported and the DTD is always
 * loaded.
 *
 * @author Akara Sucharitakul
 */
public class XMLFormatter extends java.util.logging.XMLFormatter {

    /**
     * Return the header string for a set of XML formatted records.
     *
     * @param h The target handler (can be null)
     * @return a valid XML string
     */
    public String getHead(Handler h) {
        StringBuilder sb = new StringBuilder();
        String encoding;
        sb.append("<?xml version=\"1.0\"");

        if (h != null) {
            encoding = h.getEncoding();
        } else {
            encoding = null;
        }

        if (encoding == null) {
            // Figure out the default encoding.
            encoding = Charset.defaultCharset().name();
        } else {
            // If we did not already got the encoding from the Charset,
            // try to map the encoding name to a canonical name.
            try {
                Charset cs = Charset.forName(encoding);
                encoding = cs.name();
            } catch (Exception ex) {
                // We hit problems finding a canonical name.
                // Just use the raw encoding name.
            }
        }

        sb.append(" encoding=\"");
        sb.append(encoding);
        sb.append("\"?>\n");
        sb.append("<log>\n");
        return sb.toString();
    }
}

