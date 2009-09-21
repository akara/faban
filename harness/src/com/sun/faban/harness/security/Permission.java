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
package com.sun.faban.harness.security;

/**
 * Permission enumeration used for the Faban access controller.
 */
public enum Permission {

    /**
     * The rigmanage acl is listed in the harness configuration file
     * harness.xml at the elements security/managePrincipal and is only read
     * at startup. By default, a user having manage permission on a benchmark
     * will have the rigmanage permission on rig-wide resources, i.e. the
     * run queue. If the list in harness.xml is not empty, only the listed
     * principals will have rigmanage permission on rig-wide resources.
     */
    RIGMANAGE,

    /**
     * The manage permission is allowed all manage actions
     * on the benchmark. By default, users having submit
     * permissions will have manage permissions unless overridden by
     * manage.acl
     */
    MANAGE,

    /**
     * The submit permission is allowed to submit runs
     * and delete submitted runs. By default, all logged in users
     * have submit permission on all deployed benchmarks unless
     * overridden by submit.acl
     */
    SUBMIT,

    /**
     * The view permission is allowed to view run results. By default,
     * everybody is allowed to view results, logged in or not. If
     * META-INF/view.acl exists and has entries, the user needs to be logged
     * in and listed in view.acl to view the particular result.
     */
    VIEW,

    /**
     * The write permission is allowed to add comments to runs. Usually
     * this acl should not exist. All people allowed to view are allowed
     * to comment, unless one really misbehaves. The user needs to be logged in
     * and have view permissions on the benchmark to write. If
     * META-INF/write.acl exists and has entries, only users listed in
     * write.acl are allowed to add comments.
     */
    WRITE;

    /**
     * Converts string to lowercase.
     * @return String
     */
    public String toString() {
        return name().toLowerCase();
    }
}
