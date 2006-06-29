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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: StartPolicy.java,v 1.1 2006/06/29 18:51:32 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver;

/**
 * Provides a policy how to start the initial background operation.
 *
 * @author Akara Sucharitakul
 */
public enum StartPolicy {

    /**
     * Randomly selects a time from the background cycle to call initial
     * background operation. This policy most naturally simulates background
     * tasks in user clients.
     */
    RANDOM,

    /**
     * Selects a proportional time based on the client/thread id to start
     * the initial background operation. This method evenly spreads out
     * the calls so there will be no bursts of the background operation.
     */
    SEQUENCE,

    /**
     * All threads start the background operation at the start time. This
     * policy will provide the largest possible burstiness for background
     * operations.
     */
    SIMULTANEOUS
}
