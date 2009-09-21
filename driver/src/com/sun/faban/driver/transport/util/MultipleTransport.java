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
package com.sun.faban.driver.transport.util;

import com.sun.faban.driver.HttpTransport;
import com.sun.faban.driver.engine.DriverContext;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used to load multiple HTTP requests, emulating
 * a browser. It is configured with a number of threads, and requests are
 * loaded simultaneously by those threads.
 *
 * Typical usage of this class:
 * <pre>
 *    MultipleTransport mt = new MultipleTransport(2);
 *    mt.addURL("http://myhost/index.html");
 *    mt.addURL("http://myhost/image1.gif");
 *    mt.addURL("http://myhost/image2.gif");
 *    mt.addURL("http://myhost/image3.gif");
 *    mt.addURL("http://myhost/image4.gif");
 *    boolean success = mt.waitForAll();
 * </pre>
 *
 * Nothing is actually loaded until the waitForAll() method is called. When
 * that method is called, the first URL is loaded (hence, the first URL is
 * typically the page URL) in the calling thread. Subsequently, the remaining
 * URLs (typically images and other resources) are loaded by the calling
 * thread and the threads created by this class. The total number of
 * simultaneous threads is specified by the constructor of this class, but
 * that includes the calling thread: that creates a more efficient thread
 * usage.
 *
 * After calling the waitForAll() method, the MultipleTransport is reset and
 * can be reused to load a new set of requests; if the requests are loaded
 * from the same host, then the keep-alive semantics of the HTTP transport
 * will be used.
 *
 * When finished with this object, you should call its close() method to
 * terminate its threads; otherwise the threads will leak. I have chosen
 * not to add a finalizer to this class to close the threads, since that
 * particular operation is known to cause issues with the garbage collector.
 *
 * TODO: Need a way to retrieve individual URL status
 */

public class MultipleTransport implements Runnable {

    private enum Status {
        FAILED,
        SUCCEEDED,
        PENDING
    }
    
    private static int myGlobalId = 0;
    private int myId;
    private static synchronized int getId() {
        return myGlobalId++;
    }

    private static class Request {
        private String url;
        private String postData;
        private Status status;
    }

    Thread[] threads;
    private volatile boolean done = false;
    private LinkedList<Request> pendingQueue;
    private LinkedList<Request> completedList;

    private ReentrantLock lock;
    private Condition workAvailable, workDone;
    private int pending = 0;
    private volatile boolean runHelper;

    private HttpTransport globalTransport;

    /**
     * Create an MultipleTransport that can load n requests simultaneously.
     *
     * @param n Number of threads to use to load requests. That includes
     * the thread that calls waitForAll(), so we create n-1 threads.
     */
    public MultipleTransport(int n) {
        myId = getId();
        n--;
        threads = new Thread[n];
        pendingQueue = new LinkedList<Request>();
        completedList = new LinkedList<Request>();
        lock = new ReentrantLock();
        workAvailable = lock.newCondition();
        workDone = lock.newCondition();
        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(this);
            threads[i].setDaemon(true);
            threads[i].start();
        }
        globalTransport = new HttpTransport();
        pending = 0;
        runHelper = false;
    }

    /**
     * Runs the parallel threads fetching the other URLs.
     * @see java.lang.Runnable#run()
     */
    public void run() {
        HttpTransport http = new HttpTransport();
        while (!done) {
            Request ir = null;
            try {
                lock.lock();
                while (!runHelper) {
                    try {
                        workAvailable.await();
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
                ir = pendingQueue.poll();
                if (ir != null)
                    pending++;
                else runHelper = false;
            } finally {
                lock.unlock();
            }

            if (ir != null) {
                process(http, ir);
                try {
                    lock.lock();
                    completedList.add(ir);
                    pending--;
                    if (pending == 0)
                        workDone.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
        // TODO: should http transport have a close method?
        // http.close();
    }

    /**
     * Add a request to the list of things to be retrieved. Retrieval 
     * does not start until the waitForAll() method is called.
     *
     * @param url The URL to load
     */
    public void addURL(String url) {
        addURL(url, null);
    }

    /**
     * Add a request to the list of things to be retrieved. Retrieval 
     * does not start until the waitForAll() method is called.
     *
     * @param url The URL to load
     * @param postData Data for a POST URL. If data is null, URL is
     * assumed to be a GET URL.
     */
    public void addURL(String url, String postData) {
        Request ir = new Request();
        ir.url = url;
        ir.postData = postData;
        try {
            lock.lock();
            pendingQueue.add(ir);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Start loading requests and wait for them all to be loaded. This also
     * clears the object so that it
     * may be reused.
     *
     * @return boolean Whether or not all images were loaded successfully.
     */
    public boolean waitForAll() {
        DriverContext.getContext().recordTime();
        try {
            lock.lock();
            // Take the first page and load it synchronously. That emulates
            // the steps a browser would take.
            Request ir = pendingQueue.removeFirst();
            process(globalTransport, ir);
            completedList.add(ir);

            // Now wake up the helper threads
            runHelper = true;
            workAvailable.signalAll();
        } finally {
            lock.unlock();
        }

        boolean done = false;
        while (!done) {
            // Now do as much work as possble in this thread too
            // This thread and the helper threads will compete for work
            // until the queue is drained
            Request ir = null;
            try {
                lock.lock();
                ir = pendingQueue.poll();
            } finally {
                lock.unlock();
            }
            if (ir != null) {
                process(globalTransport, ir);
                try {
                    lock.lock();
                    completedList.add(ir);
                } finally {
                    lock.unlock();
                }
            }
            else done = true;
        }

        try {
            lock.lock();
            // Now wait for the helper threads to finish
            while (pendingQueue.size() != 0 || pending != 0) {
                try {
                    workDone.await();
                } catch (InterruptedException ie) {
                    return false;
                }
            }

            // Everyone is done. Figure out the results and send it back.
            Iterator<Request> it = completedList.iterator();
            while (it.hasNext()) {
                Request ir = it.next();
                if (ir.status != Status.SUCCEEDED)
                    return false;
            }
            return true;
        } finally {
            pendingQueue = new LinkedList<Request>();
            completedList = new LinkedList<Request>();
            lock.unlock();
            runHelper = false;
            DriverContext.getContext().recordTime();
        }
    }

    /**
     * Close down the loader, stopping all its threads.
     */
    public void close() {
        done = true;
        for (int i = 0; i < threads.length; i++)
            threads[i].interrupt();
        threads = null;
        pendingQueue = null;
        completedList = null;
        // TODO: close globalTransport
    }

    /**
     * Provides a string representation of this transport.
     * @return The string representation
     * @see java.lang.Object#toString()
     */
    @Override
	public String toString() {
        return "MultipleTransport[" + myId + "]";
    }

    private void process(HttpTransport http, Request ir) {
        try {
            if (ir.postData == null)
                http.readURL(ir.url);
            else http.readURL(ir.url, ir.postData);
            ir.status = MultipleTransport.Status.SUCCEEDED;
        } catch (Exception e) {
            e.printStackTrace();
            ir.status = MultipleTransport.Status.FAILED;
        }
    }
}
