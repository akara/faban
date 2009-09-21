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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * The protocol handler interface defines the standard interface for
 * protocol handlers. No matter what protocol, all stream-based handlers
 * will have the followings called, in-order
 * <ol>
 * <li>doProcessRequest - one or more times</li>
 * <li>doProcessResponse</li>
 * <li>doContinueResponse - zero or more times</li>
 * <li>requestPending</li>
 * </ol>
 * This scheme allows for multiple read-writes by a protocol handler
 * before the whole interaction is done.
 *
 * @author Akara Sucharitakul
 */
public interface ProtocolHandler {

    /**
     * Sets the config object.
     * @param config The singleton config object
     */
    void setConfig(LogConfig config);

    /**
     * Sets the selection key for subsequent invocations.
     * @param key The nio selection key
     */
    void setKey(SelectionKey key);

    /**
     * Analyzes the buffer and return read active status.
     * @param buffer The buffer to look at
     * @param count The number of new bytes in the buffer
     * @return True if still active, false if done processing
     */
    boolean doProcessRequest(ByteBuffer buffer, int count);

    /**
     * Creates and writes the response to the channel. Returns write
     * active status. This method should return true if and only if
     * the non-blocking channel cannot take all the response to
     * be written.
     * @return True if still active, false if done processing
     * @exception IOException Cannot write response to channel
     */
    boolean doProcessResponse() throws IOException;

    /**
     * Continues writing the response to the channel. This method
     * is only called if the initial write did not manage to write
     * everything to the non-blocking channel.
     * @return True if still active, false if done processing
     * @exception IOException Cannot write response to channel
     */
    boolean doContinueResponse() throws IOException;

    /**
     * Tests the handler whether it needs to further process this request.
     * @return True if the handler still needs to proceed, false if it is done
     */
    boolean requestPending();
}
