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
package com.sun.faban.driver;

/**
 * Sets the timing mode for the operation. If AUTO timing is specified, the
 * operation does not need to implement any time registration for the
 * output metric. The operation will need to make time registrations if
 * MANUAL timing is set.
 *
 * @author Akara Sucharitakul
 */
public enum Timing {
    /** Configures AUTO timing for the operation. */
    AUTO,

    /** Configures MANUAL timing for the operation. */
    MANUAL
}
