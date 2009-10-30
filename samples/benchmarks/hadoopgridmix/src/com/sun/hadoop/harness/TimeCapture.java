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

package com.sun.hadoop.harness;

import java.util.Calendar;

/**
 *
 * @author damien
 */
public class TimeCapture {
    private Calendar firstRecord;
    private long totalAcumulatedTime;
    private String tag;

    public TimeCapture()
    {
        firstRecord = Calendar.getInstance();
        totalAcumulatedTime = 0;
    }
    /**
     * @return the firstRecord
     */
    public Calendar getFirstRecord() {
        return firstRecord;
    }

    /**
     * @param firstRecord the firstRecord to set
     */
    public void setFirstRecord(final Calendar firstRecord) {
        this.firstRecord = firstRecord;
    }

    /**
     * @return the totalAcumulatedTime
     */
    public long getTotalAcumulatedTime() {
        return totalAcumulatedTime;
    }

    /**
     * @param totalAcumulatedTime the totalAcumulatedTime to set
     */
    public void setTotalAcumulatedTime(final long totalAcumulatedTime) {
        this.totalAcumulatedTime = this.totalAcumulatedTime + totalAcumulatedTime;
    }

    /**
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(final String tag) {
        this.tag = tag;
    }

}
