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
package com.sun.faban.common;

import java.util.Locale;

public class Tools {

    public static final int JAVAC = 0; //position
    private static final String[] EXECUTABLES = new String[1]; // tool
    private static final Tools INSTANCE = new Tools();

    static {
        String platform = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if ( platform.contains( "windows" )) {
            EXECUTABLES[JAVAC] = "javac.exe";
        } else  {
            EXECUTABLES[JAVAC] = "javac";
        }
    }
    
    public String executable( int tool )
    {
        String ls = System.getProperty("line.separator");
        return String.format( "%1$s%2$sbin%2$s%3$s", FabanShell.javaHome, ls, EXECUTABLES[tool]);
    }

    public static Tools getToolsInstance() {
        return INSTANCE;
    }
}
