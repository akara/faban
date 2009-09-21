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
package com.sun.faban.harness.webclient;

import java.io.*;
import java.util.ArrayList;

/**
 * The XML input stream rudimentary parses an xml input file for completion.
 * It checks that the outermost element is properly closed. Otherwise the
 * stream will not end.
 */
public class XMLInputStream extends FilterInputStream {

    /** Less-than sign. */
    private static final byte LT = (byte) '<';

    /** Greater-than sign. */
    private static final byte GT = (byte) '>';

    /** Question mark. */
    private static final byte Q  = (byte) '?';

    /** Slash sign. */
    private static final byte SL = (byte) '/';

    private Processor p = new Processor();

    private Object originator;

    private long position = 0;

    private long savedPosition = -1;

    private ArrayList<EOFListener> listeners;

    // SKIP_BUFFER_SIZE is used to determine the size of skipBuffer
    private static final int SKIP_BUFFER_SIZE = 8192;
    // skipBuffer is initialized in skip(long), if needed.
    private static byte[] skipBuffer;


    /**
     * Creates a <code>XMLInputStream</code> by
     * opening a connection to an actual file,
     * the file named by the path name <code>name</code>
     * in the file system.  A new <code>FileDescriptor</code>
     * object is created to represent this file
     * connection.
     * <p/>
     * First, if there is a security
     * manager, its <code>checkRead</code> method
     * is called with the <code>name</code> argument
     * as its argument.
     * <p/>
     * If the named file does not exist, is a directory rather than a regular
     * file, or for some other reason cannot be opened for reading then a
     * <code>FileNotFoundException</code> is thrown.
     *
     * @param name the system-dependent file name.
     * @throws java.io.FileNotFoundException if the file does not exist,
     *                                       is a directory rather than a regular file,
     *                                       or for some other reason cannot be opened for
     *                                       reading.
     * @throws SecurityException             if a security manager exists and its
     *                                       <code>checkRead</code> method denies read access
     *                                       to the file.
     * @see SecurityManager#checkRead(String)
     */
    public XMLInputStream(String name) throws FileNotFoundException {
        super(new FileInputStream(name));
        originator = name;
    }

    /**
     * Creates a <code>FileInputStream</code> by
     * opening a connection to an actual file,
     * the file named by the <code>File</code>
     * object <code>file</code> in the file system.
     * A new <code>FileDescriptor</code> object
     * is created to represent this file connection.
     * <p/>
     * First, if there is a security manager,
     * its <code>checkRead</code> method  is called
     * with the path represented by the <code>file</code>
     * argument as its argument.
     * <p/>
     * If the named file does not exist, is a directory rather than a regular
     * file, or for some other reason cannot be opened for reading then a
     * <code>FileNotFoundException</code> is thrown.
     *
     * @param file the file to be opened for reading.
     * @throws java.io.FileNotFoundException if the file does not exist,
     *                                       is a directory rather than a regular file,
     *                                       or for some other reason cannot be opened for
     *                                       reading.
     * @throws SecurityException             if a security manager exists and its
     *                                       <code>checkRead</code> method denies read access to the file.
     * @see java.io.File#getPath()
     * @see SecurityManager#checkRead(String)
     */
    public XMLInputStream(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
        originator = file;
    }

    /**
     * Creates a <code>FileInputStream</code> by using the file descriptor
     * <code>fdObj</code>, which represents an existing connection to an
     * actual file in the file system.
     * <p/>
     * If there is a security manager, its <code>checkRead</code> method is
     * called with the file descriptor <code>fdObj</code> as its argument to
     * see if it's ok to read the file descriptor. If read access is denied
     * to the file descriptor a <code>SecurityException</code> is thrown.
     * <p/>
     * If <code>fdObj</code> is null then a <code>NullPointerException</code>
     * is thrown.
     *
     * @param fdObj the file descriptor to be opened for reading.
     * @throws SecurityException if a security manager exists and its
     *                           <code>checkRead</code> method denies read access to the
     *                           file descriptor.
     * @see SecurityManager#checkRead(java.io.FileDescriptor)
     */
    public XMLInputStream(FileDescriptor fdObj) {
        super(new FileInputStream(fdObj));
        originator = fdObj;
    }

    private void reopen() throws IOException {
        eofEvent();
        in.close();
        FileInputStream fin = null;
        if (originator instanceof String)
            fin = new FileInputStream((String) originator);
        else if (originator instanceof File)
            fin = new FileInputStream((File) originator);
        else if (originator instanceof FileDescriptor)
            fin = new FileInputStream((FileDescriptor) originator);

        // Also re-positions.
        long skipped = 0;
        while (skipped < position)
            skipped += fin.skip(position - skipped);
        in = fin;
    }

    /**
     * Adds the EOF listener.
     * @param listener The listener to add
     */
    public void addEOFListener(EOFListener listener) {

        if (listener == null)
            throw new NullPointerException("Null listener");

        if (listeners == null)
            listeners = new ArrayList<EOFListener>();

        listeners.add(listener);
    }

    /**
     * Fires off an eofEvent if EOF is hit. Only one event is fired, not
     * after retries.
     */
    private void eofEvent() {
        if (savedPosition == position)
            return;
        savedPosition = position;
        for (EOFListener listener : listeners)
            listener.eof();
    }

    /**
     * Reads a byte of data from this input stream. This method blocks
     * if no input is yet available.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     *         file is reached.
     * @throws java.io.IOException if an I/O error occurs.
     */
    @Override public int read() throws IOException {
        int r = in.read();
        if (r != -1) {
            ++position;
            p.process((byte) r);
        } else if (p.stackDepth > 0) {
            do {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Do nothing, just keep trying
                }
                reopen();
                r = in.read();
            } while (r == -1);
            p.process((byte) r);
            ++position;
        } else {
            eofEvent();
        }
        return r;
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
    @Override public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes. If <code>len</code> is not zero, the method
     * blocks until some input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     *         <code>-1</code> if there is no more data because the end of
     *         the file has been reached.
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws java.io.IOException       if an I/O error occurs.
     */
    @Override public int read(byte b[], int off, int len) throws IOException {
        int size = in.read(b, off, len);
        if (size != -1) {
            for (int i = off; i < size; i++)
                p.process(b[i]);
            position += size;
        } else if (p.stackDepth > 0) {
            do {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Do nothing, just keep trying
                }
                reopen();
                size = in.read(b, off, len);
            } while (size == -1);

            for (int i = off; i < size; i++)
                p.process(b[i]);
            position += size;
        } else {
            eofEvent();
        }
        return size;
    }

    /**
     * Tests if this input stream supports the <code>mark</code>
     * and <code>reset</code> methods.
     * This method always returns false. Mark is not supported in
     * XMLInputStream.
     *
     * @return <code>true</code> if this stream type supports the
     *         <code>mark</code> and <code>reset</code> method;
     *         <code>false</code> otherwise.
     * @see java.io.FilterInputStream#in
     * @see java.io.InputStream#mark(int)
     * @see java.io.InputStream#reset()
     */
    @Override public boolean markSupported() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method actually reads the skipped bytes from the stream.
     */
    @Override public long skip(long n) throws IOException {
        if (skipBuffer == null)
            skipBuffer = new byte[SKIP_BUFFER_SIZE];
        long remaining = n;
        while (remaining > 0) {
            int readSize = SKIP_BUFFER_SIZE;
            if (remaining < SKIP_BUFFER_SIZE)
                readSize = (int) remaining;
            readSize = read(skipBuffer, 0, readSize);
            if (readSize != -1)
                remaining -= readSize;
            else
                break;
        }
        return n - remaining;
    }

    /**
     * Marks the current position in this input stream. A subsequent
     * call to the <code>reset</code> method repositions this stream at
     * the last marked position so that subsequent reads re-read the same bytes.
     * <p/>
     * The <code>readlimit</code> argument tells this input stream to
     * allow that many bytes to be read before the mark position gets
     * invalidated.
     * <p/>
     * Since mark is not supported in XMLInputStream, this method is a NOOP.
     *
     * @param readlimit the maximum limit of bytes that can be read before
     *                  the mark position becomes invalid.
     * @see java.io.FilterInputStream#in
     * @see java.io.FilterInputStream#reset()
     */
    @Override public void mark(int readlimit) {
        // Do nothing on mark.
    }

    /**
     * Repositions this stream to the position at the time the
     * <code>mark</code> method was last called on this input stream.
     * <p/>
     * This method
     * simply performs <code>in.reset()</code>.
     * <p/>
     * Stream marks are intended to be used in
     * situations where you need to read ahead a little to see what's in
     * the stream. Often this is most easily done by invoking some
     * general parser. If the stream is of the type handled by the
     * parse, it just chugs along happily. If the stream is not of
     * that type, the parser should toss an exception when it fails.
     * If this happens within readlimit bytes, it allows the outer
     * code to reset the stream and try another parser.
     *
     * @throws java.io.IOException if the stream has not been marked or if the
     *                             mark has been invalidated.
     * @see java.io.FilterInputStream#in
     * @see java.io.FilterInputStream#mark(int)
     */
    @Override public void reset() throws IOException {
        throw new IOException("Mark not supported!");
    }

    private static class Processor {

        byte[] elBuffer = new byte[16];
        int idx = 0;
        int stackDepth = 0;

        private void process(byte b) throws IOException {
            switch (b) {
                case LT :
                    if (idx == 0) {
                        elBuffer[idx++] = b;
                        ++stackDepth;
                    } else {
                        throw new IOException("Found '<' inside element");
                    }
                    break;
                case GT :
                    if (idx > 0) {
                        if (elBuffer[1] == SL)          // Found '</'
                            stackDepth -= 2;
                        else if (elBuffer[idx - 1] == Q)    // Found '?>'
                            if (elBuffer[1] == Q)
                                --stackDepth;
                            else
                                throw new IOException(
                                        "Illegal '?>' inside element");

                        else if (elBuffer[idx - 1] == SL)   // Found '/>'
                            --stackDepth;
                        idx = 0;
                    } else {
                        throw new IOException(
                                "Found '>' without element start.");
                    }
                    break;
                default :
                    if (idx > 0) {
                        if (b == SL || b == Q)
                            elBuffer[idx++] = b;
                        else if (idx == 1 || elBuffer[idx - 1] == SL ||
                                 elBuffer[idx - 1] == Q)
                            elBuffer[idx++] = b;
                    }
            }
        }
    }

    /**
     * A listener to listen to end-of-file events of XMLInputStream.
     * The XMLInputStream does not return -1 on end-of-file like other
     * input streams if the XML is not properly closed. Instead, it will
     * wait for more data being written to the file. Some applications
     * want to know if the EOF is hit anyway and this can be achieved by
     * implementing this interface and register the listener with
     * the input stream.
     */
    public static interface EOFListener {

        /**
         * Gets called if and when eof is hit.
         */
        public void eof();
    }
}
