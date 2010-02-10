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
 * $Id
 *
 * Copyright 2010 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.transport.util;

import com.sun.faban.driver.engine.DriverContext;

/**
 * A helper class to provide throttling on sockets
 *
 * @author Scott Oaks
 */
public class Throttle {
    public static final int UP = 0;
    public static final int DOWN = 1;

	private DriverContext ctx;

	/* Time it should take to process 1 byte */
    private double[] desiredNanoPerByte = new double[2];

	/* Last timing value user */
    private int[] lastSpeed = new int[2];

	/* Amount of time we need to sleep but haven't yet (because we
	 * can't sleep for really short nano-second measured periods of time)
	 */
	private long[] pendingNanoSleep = new long[2];

    /**
     * Constructs a throttle.
     * @param ctx The driver context
     */
	public Throttle(DriverContext ctx) {
		this.ctx = ctx;
		checkForChange(UP);
		checkForChange(DOWN);
	}

    /**
     * Checks whether the bandwidth is throttled for the given direction
     * @param direction The direction to check
     * @return Whether the bandwidth is throttled
     */
	public boolean isThrottled(int direction) {
		return lastSpeed[direction] > 0;
	}

    /**
     * The throttle sleeps until the calculated time for the request has
     * expired, before continuing with subsequent I/O.
     * @param bytes The size of the data sent/received
     * @param startTime The start time of the send/receive
     * @param direction The direction, up or down
     */
	public void throttle(int bytes, long startTime, int direction) {
		checkForChange(direction);
		double expectedTime = bytes * desiredNanoPerByte[direction];
        long wakeupTime = startTime + (long) expectedTime +
                        pendingNanoSleep[direction];
        ctx.wakeupAt(wakeupTime);
        // If slept too long, the pendingNanoSleep will go negative.
        // If it wakes up too early, the pendingNanoSleep will go positive
        // needing to add a little more delay to the subsequent sleep.
        pendingNanoSleep[direction] = wakeupTime - System.nanoTime();
	}

	private void checkForChange(int direction) {
        int last;
	    if (direction == UP)
			last = ctx.getUploadSpeed();
        else
            last = ctx.getDownloadSpeed();

        if (last != lastSpeed[direction]) {
            desiredNanoPerByte[direction] = calcNanoPerByte(last);
            lastSpeed[direction] = last;
        }
	}

	private double calcNanoPerByte(int kbps) {
        /*  
         * Convert the given unit (kilobytes per second) to
         * the amount of time (in nanoseconds) it takes to
         * read/write one byte
         */  
        if (kbps < 0)
			return -1;
        else {
            double bytesPerNano = kbps * 1024   // bytes/second
                        / 1000000000.;          // bytes/nano
            return 1. / bytesPerNano;
        }               
	}
}
