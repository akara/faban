/*
* The contents of this file are subject to the terms
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
* Copyright 2009 Sun Microsystems Inc. All Rights Reserved
*/

package com.sun.hadoop.logrecords;


import java.util.Date;
import java.util.logging.Logger;

/**
 *
 * @author Damien Cooke
 */
public abstract class AbstractLogRecord
{
    static Logger logger = Logger.getLogger(AbstractLogRecord.class.getName());
    private Date timestamp;
    private String hostname;

    /**
     * method returns instance variable timestamp
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * method returns hostname from instance variable
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * method sets the hostname from the instance variable
     * @param hostname the hostname to set
     */
    void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    /**
     * Parses the detail part of a log record string into the target record
     * format.
     * @param detail The detail part of the log record
     * @return whether the parse succeeds
     */
    public abstract boolean parse(final String detail);
}
