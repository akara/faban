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
	public static enum Direction { UP, DOWN };

	private DriverContext ctx;

	/* Time it should take to process 1 byte */
	private double desiredUpNanoPerByte;
	private double desiredDownNanoPerByte;

	/* Last timing value user */
	private int lastUpSpeed;
	private int lastDownSpeed;

	/* Amount of time we need to sleep but haven't yet (because we
	 * can't sleep for really short nano-second measured periods of time)
	 */
	private long pendingNanoSleep;

	public Throttle(DriverContext ctx) {
		this.ctx = ctx;
		checkForChange(Direction.UP);
		checkForChange(Direction.DOWN);
	}

	public boolean isThrottled(Direction direction) {
		return (direction == Direction.UP) ?
			(lastUpSpeed > 0) : (lastDownSpeed > 0);
	}

	public void throttle(int bytes, long elapsedTime, Direction direction) {
		checkForChange(direction);
		double desiredNanoPerByte = (direction == Direction.UP) ?
				desiredUpNanoPerByte : desiredDownNanoPerByte;
		double expectedTime = bytes * desiredNanoPerByte;
		double sleepTime = (expectedTime - elapsedTime);
		if (sleepTime > 0) {
			pendingNanoSleep += sleepTime;
			// Can't sleep for less than 50 ms
			long msToSleep = 0;
			if (pendingNanoSleep > (1000000L * 50)) {
				msToSleep = pendingNanoSleep / 1000000L;
				pendingNanoSleep -= (msToSleep * 1000000L);
			}
			if (msToSleep > 0) {
				try {
					Thread.currentThread().sleep(msToSleep);
				} catch (InterruptedException ie) {
				    Thread.currentThread().interrupt();
			    }
			}
		}
	}

	private void checkForChange(Direction d) {
	    if (d == Direction.UP) {
			int last = ctx.getUploadSpeed();
			if (last != lastUpSpeed) {
				desiredUpNanoPerByte = calcNanoPerByte(last);
				lastUpSpeed = last;
			}
		} else {
			int last = ctx.getDownloadSpeed();
			if (last != lastDownSpeed) {
				desiredDownNanoPerByte = calcNanoPerByte(last);
				lastDownSpeed = last;
			}
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
