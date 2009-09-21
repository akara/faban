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
package com.sun.faban.driver.util;

/**
 * Tool to choose the matching string out of a list of strings.
 * The current implementation sequentially matches the strings
 * but this can be reimplemented with a more efficient algorithm.
 * @author Akara Sucharitakul
 */
public class StringMatcher {

    String[] candidates;

    /**
     * Constructs a StringMatcher instance.
     * @param candidates the candidates to match
     */
    public StringMatcher(String[] candidates) {
        this.candidates = candidates;
    }

    /**
     * Matches the string to the candidates.
     * @param match object to match
     * @return index of the match in the candidate array or -1 if not found
     */
    public int match(Object match) {
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].equals(match))
                return i;
        }
        return -1;
    }
}
