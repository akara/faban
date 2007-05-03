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
 * $Id: Utilities.java,v 1.1 2007/05/03 23:04:06 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

/**
 * Common utilities, usually accessible via static import.
 */
public class Utilities {

    /**
     * Parses a string escaped with \n, \t, \020, etc. Returns the
     * properly escaped string.
     * @param s
     * @return
     */
    public static String parseEscapedString(String s) {
        char[] cArray = s.toCharArray();
        boolean escape = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < cArray.length; ++i) {
            if (escape) {
                if (cArray[i] == 'b') {
                    buf.append('\b');
                } else if (cArray[i] == 't') {
                    buf.append('\t');
                } else if (cArray[i] == 'n') {
                    buf.append('\n');
                } else if (cArray[i] == 'r') {
                    buf.append('\r');
                } else if (cArray[i] == 'f') {
                    buf.append('\f');
                } else if (cArray[i] == 'u') {
                    // Unicode escape
                    int utf = Integer.parseInt(s.substring(i + 1, i + 5), 16);
                    buf.append((char)utf);
                    i += 4;
                } else if (Character.isDigit(cArray[i])) {
                    // Octal escape
                    int j = 0;
                    for (j = 1; (j < 2) && (i + j < cArray.length); ++j) {
                        if (!Character.isDigit(cArray[i+j]))
                            break;
                    }
                    int octal = Integer.parseInt(s.substring(i, i + j), 8);
                    buf.append((char)octal);
                    i += j;
                } else {
                    buf.append(cArray[i]);
                }
                escape = false;
            } else if (cArray[i] == '\\') {
                escape = true;
            } else {
                buf.append(cArray[i]);
            }
        }
        return buf.toString();
    }

}
