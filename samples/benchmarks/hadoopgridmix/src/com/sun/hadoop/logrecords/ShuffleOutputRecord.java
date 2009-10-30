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

/**
 *
 * @author Damien Cooke
 * Container class to store a parsed ShuffleOutputRecord
 */
public class ShuffleOutputRecord extends AbstractLogRecord
{
    private String sessionId;
    private String shuffle_failed_outputs;
    private String shuffle_handler_busy_percent;
    private String shuffle_output_bytes;
    private String shuffle_success_outputs;

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @return the shuffle_failed_outputs
     */
    public String getShuffle_failed_outputs() {
        return shuffle_failed_outputs;
    }

    /**
     * @return the shuffle_handler_busy_percent
     */
    public String getShuffle_handler_busy_percent() {
        return shuffle_handler_busy_percent;
    }

    /**
     * @return the shuffle_output_bytes
     */
    public String getShuffle_output_bytes() {
        return shuffle_output_bytes;
    }

    /**
     * @return the shuffle_success_outputs
     */
    public String getShuffle_success_outputs() {
        return shuffle_success_outputs;
    }

    /*
     * method is a specilised parse method to parse a detail from a log of ShuffleOutputRecord type
     * @param detail, detail of log file to parse
     * @return success or failure of the parse process
     */
    public boolean parse(final String detail)
    {
        /*
         * 2009-06-05 17:05:06,724 mapred.shuffleOutput: hostName=pae2250-9h, sessionId=, shuffle_failed_outputs=0, shuffle_handler_busy_percent=0.0, shuffle_output_bytes=540551799, shuffle_success_outputs=32
         */

        // we get the detail section, after the last ':'
        if(detail != null && detail.length() > 0)
        {
            //all appears to be ok we have 4 records to deal with.
            String[] logMessages = detail.split(",");

            String[] hostname = logMessages[0].split("=");
            setHostname(hostname[1]);

            String[] sessionId;
            try
            {
                sessionId = logMessages[1].split("=");
                this.sessionId = sessionId[1];
            }catch(ArrayIndexOutOfBoundsException aob)
            {
                this.sessionId = "none";
            }

            String[] shuffle_failed_outputs = logMessages[2].split("=");
            this.shuffle_failed_outputs = shuffle_failed_outputs[1];

            String[] shuffle_handler_busy_percent = logMessages[3].split("=");
            this.shuffle_handler_busy_percent = shuffle_handler_busy_percent[1];

            String[] shuffle_output_bytes = logMessages[4].split("=");
            this.shuffle_output_bytes = shuffle_output_bytes[1];

            String[] shuffle_success_outputs = logMessages[5].split("=");
            this.shuffle_success_outputs = shuffle_success_outputs[1];


        }else
        {
            //all bad
            logger.warning("Line passed into ShuffleOutputRecord.parse was not in the format expected noticed while retrieving the elements");
            return false;
        }

        return true;
    }
}
