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
package com.sun.faban.harness.logging;

import com.sun.faban.harness.common.Config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Protocol handler for log requests.
 * @author Ramesh Ramachandran
 */
public class LogHandler implements ProtocolHandler {

    private FlexBuffer xBuffer;
    private byte[] writeBuffer;

    /**
     * Sets the config object.
     * @param config The singleton config object
     */
    public void setConfig(LogConfig config) {
        xBuffer = new FlexBuffer(config.readBufferSize);
   }

    /**
     * Sets the selection key for subsequent invocations.
     * @param key The nio selection key
     */
    public void setKey(SelectionKey key) {

    }

    /**
     * Analyzes the buffer and return read active status.
     * @param buffer The buffer to look at
     * @param count The number of new bytes in the buffer
     * @return True if still active, false if done processing
     */
    public boolean doProcessRequest(ByteBuffer buffer, int count){
        if (count > 0) {
            // Buffer has been written to. Prepare the buffer for reading.
            buffer.flip();

            // Get the data from the direct buffer.
            xBuffer.appendDirect(buffer, count);

            /*
            PrintWriter debug = null;

            try {
                debug = new PrintWriter(new FileWriter(Config.TMP_DIR +
                                        "debug.log", true));
            } catch (IOException e) {
            }
            */
            if (!(xBuffer.endsWith("</record>\n")
                || xBuffer.endsWith("</log>\n"))) { // Log record not closed
                //debug.println(xBuffer.toString() + " {end of partial read}");
                return true;
            }
            try {
                /*
                debug.println(xBuffer.toString() + " {end of read}");
                debug.flush();
                debug.close();
                */

                // Copy out the whole record in one piece
                int writeSize = xBuffer.size();
                if (writeBuffer == null || writeBuffer.length < writeSize)
                    writeBuffer = new byte[(int) (writeSize * 1.25)];

                xBuffer.getBytes(0, writeBuffer, 0, writeSize);

                //Open the log file and dump the record/s
                String logFile = System.getProperty("faban.log.file");
                if(logFile == null)
                    logFile = Config.TMP_DIR + "log.xml";
                FileOutputStream logStream = new FileOutputStream(logFile, true);
                logStream.write(writeBuffer, 0, writeSize);
                logStream.flush();
                logStream.close();
            } catch(IOException e) {
                // if (debug != null)
                //    e.printStackTrace(debug);
            }
            xBuffer.clear();
        }
        // Return false as we don't care which instance of Handler writes the
        // rest of the log record.
        return false;
    }

    /**
     * Creates and writes the response to the channel. Returns write
     * active status. This method should return true if and only if
     * the non-blocking channel cannot take all the response to
     * be written.
     * @return True if still active, false if done processing
     * @exception java.io.IOException Cannot write response to channel
     */
    public boolean doProcessResponse() throws IOException {
        return false;
    }

    /**
     * Continues writing the response to the channel. This method
     * is only called if the initial write did not manage to write
     * everything to the non-blocking channel.
     * @return True if still active, false if done processing
     * @exception java.io.IOException Cannot write response to channel
     */
    public boolean doContinueResponse() throws IOException {
        return false;
    }

    /**
     * Tests the handler whether it needs to further process this request.
     * @return True if the handler still needs to proceed, false if it is done
     */
    public boolean requestPending() {
        return false;
    }
}
