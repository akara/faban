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
 * Copyright 2005-2010 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.faban.driver.transport.util;

import com.sun.faban.driver.engine.DriverContext;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

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
 * Copyright 2005-2010 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * A pass-through input stream that records the time of the input.
 * Note that the client-side time recording for reads is
 * always after the read happens.
 *
 * @author Akara Sucharitakul
 */
public class TimedInputStream extends FilterInputStream {

    DriverContext ctx;
    private Throttle throttle;

    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public TimedInputStream(InputStream in) {
        super(in);
        ctx = DriverContext.getContext();
        if (ctx != null)
            throttle = new Throttle(ctx);
    }

    /**
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>. If no byte is available
     * because the end of the stream has been reached, the value
     * <code>-1</code> is returned. This method blocks until input data
     * is available, the end of the stream is detected, or an exception
     * is thrown.
     * <p/>
     * This method
     * simply performs <code>in.read()</code> and returns the result.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     *         stream is reached.
     * @throws java.io.IOException if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    @Override
	public int read() throws IOException {
        long startReadAt = 0L;
        boolean isThrottled = false;
        if (ctx != null) {
            isThrottled = throttle.isThrottled(Throttle.DOWN);
            if (isThrottled)
	    	    startReadAt = ctx.getNanoTime();
        }
        int b = super.read();
        if (ctx != null && b != -1) {
            ctx.recordEndTime();
			if (isThrottled)
	    		throttle.throttle(1, startReadAt, Throttle.DOWN);
        }
        return b;
    }

    /**
     * Reads up to <code>byte.length</code> bytes of data from this
     * input stream into an array of bytes. This method blocks until some
     * input is available.
     * <p/>
     * This method simply performs the call
     * <code>read(b, 0, b.length)</code> and returns
     * the  result. It is important that it does
     * <i>not</i> do <code>in.read(b)</code> instead;
     * certain subclasses of  <code>FilterInputStream</code>
     * depend on the implementation strategy actually
     * used.
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or
     *         <code>-1</code> if there is no more data because the end of
     *         the stream has been reached.
     * @throws java.io.IOException if an I/O error occurs.
     * @see java.io.FilterInputStream#read(byte[], int, int)
     */
    @Override
	public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);

    }

    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes. This method blocks until some input is
     * available.
     * <p/>
     * This method simply performs <code>in.read(b, off, len)</code>
     * and returns the result.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     *         <code>-1</code> if there is no more data because the end of
     *         the stream has been reached.
     * @throws java.io.IOException if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    @Override
	public int read(byte b[], int off, int len) throws IOException {
        long startReadAt = 0L;
        boolean isThrottled = false;
        if (ctx != null) {
            isThrottled = throttle.isThrottled(Throttle.DOWN);
            if (isThrottled) {
                startReadAt = System.nanoTime();
            }
        }
        int bytes = super.read(b, off, len);
        if (ctx != null && bytes > 0) {
            ctx.recordEndTime();
            if (isThrottled)
                throttle.throttle(bytes, startReadAt, Throttle.DOWN);
        }
        return bytes;
    }
}
