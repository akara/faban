/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
 * install_dir/license.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Designates a method to be called only once, just before the start of the
 * benchmark run. It is always called from global thread 0 for the given driver.
 * No other thread will be started until this method finishes. Note that only
 * one method is allowed for the OnceBefore designation. If Faban finds more
 * than one method with the OnceBefore annotation, it will throw a
 * DefinitionException at startup.<p/>
 * The OnceBefore and OnceAfter annotations are used mainly for auditing
 * the state of the SUT before and after the benchmark run. A CustomMetrics
 * shall be used to report the audit results.
 *
 * @author Akara Sucharitakul
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnceBefore {
	// marker attribute
}
