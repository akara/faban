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
 * $Id: Permission.java,v 1.1 2006/08/17 06:29:52 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

/**
 * Permission enumeration used for the Faban access controller.
 */
public enum Permission {

    /**
     * The manage permission is allowed all manage actions
     * on the benchmark or the rig.
     */
    MANAGE,

    /**
     * The submit permission is allowed to submit runs
     * and delete submitted runs.
     */
    SUBMIT,

    /**
     * The view permission is allowed to view run results.
     */
    VIEW,

    /**
     * The write permission is allowed to add comments to runs. Usually
     * this acl should not exist. All people allowed to view are allowed
     * to comment, unless one really misbehaves.
     */
    WRITE
}
