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
package com.sun.faban.common;

import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The FileTransfer class represents a file to be transferred via RMI from
 * one to the other system. We override the serialization mechanism
 * to ensure we don't need to suck the file's content into memory causing
 * memory bloat. This is useful for transferring large files over RMI.
 * As this object gets serialized, deserialized, the file transfer happens
 * internally and the destination file gets created as a result of the
 * deserialization. The file size limit is Long.MAX_VALUE (64bit).
 *
 * @author Akara Sucharitakul
 */
public class FileTransfer implements Externalizable {

    private static final long serialVersionUID = 20090812L;    

    private static final int MAX_BUFFER_SIZE = 8192;
    private static final Logger logger =
                            Logger.getLogger(FileTransfer.class.getName());

    private String src;
    private String dest;
    private long size; // Size only gets populated once file transfer happens.

    private transient long transferSize;
    private transient byte[] buffer;
    private transient FileInputStream dataIn;

    /**
     * Creates a file transfer object.
     * @param src The source file name
     * @param dest The destination file name
     * @exception IOException Error reading the file to be transferred
     */
    public FileTransfer(String src, String dest) throws IOException {
        this.src = src;
        this.dest = dest;

        // Ensure the file is really there and readable.
        File srcFile = new File(src);
        if (!srcFile.exists())
            throw new FileNotFoundException("File " +
                    srcFile.getAbsolutePath() + " does not exist.");
        transferSize = srcFile.length();
        if (transferSize < 0)
            throw new IOException(srcFile.getAbsolutePath() +
                    ": Invalid file size of " + transferSize);

        dataIn = new FileInputStream(srcFile);
        buffer = new byte[transferSize < MAX_BUFFER_SIZE ?
                                (int) transferSize : MAX_BUFFER_SIZE];

        // Fill the first full buffer now, in order to detect I/O issues
        // now and not during serialization.
        int idx = 0;
        while (idx < buffer.length) {
            int readCount = dataIn.read(buffer, idx, buffer.length - idx);
            if (readCount < 0)
                throw new IOException("Error reading file " +
                        srcFile.getAbsolutePath() + ". Size: " + transferSize +
                        ", Read: " + idx);
            idx += readCount;
        }

        if (buffer.length == transferSize) { // We have read everything now.
            dataIn.close();
        }
    }

    /**
     * Creates a file transfer object from a byte buffer.
     * @param buffer The buffer
     * @param offset The starting offset to use
     * @param length The length of data to use, in bytes
     * @param dest The destination file
     */
    public FileTransfer(byte[] buffer, int offset, int length, String dest) {
        this.src = "";
        this.dest = dest;
        transferSize = length;
        this.buffer = new byte[length];
        System.arraycopy(buffer, offset, this.buffer, 0, length);
    }

    /**
     * The noarg constructore is used for deserializing.
     */
    public FileTransfer() {

    }

    /**
     * Obtains the sources file name.
     * @return The source file name, or an empty string if the transfer happens
     *         from a buffer.
     */
    public String getSource() {
        return src;
    }

    /**
     * Obtains the destination file name.     *
     * @return The destination file name
     */
    public String getDest() {
        return dest;
    }

    /**
     * Obtains the size of the file transferred, or 0 if the file transfer
     * has not yet happen.
     * @return The size of the file transferred, or 0
     */
    public long getSize() {
        return size;
    }

    /**
     * Obtains the size to be transferred on the sending side, or the size
     * really transferred on the receiving side.
     * @return The transfer size
     */
    public long getTransferSize() {
        return transferSize;
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        // Flush headers and the first chunk.
        size = transferSize;
        out.writeObject(src);
        out.writeObject(dest);
        out.writeLong(size);
        out.write(buffer);

        // Then stream the rest, if any
        if (size > buffer.length) {
            // Write the rest of the data stream, one buffer at a time.
            long remainder = size - buffer.length;
            while (remainder > 0) {
                int chunkSize = remainder < buffer.length ?
                        (int) remainder : buffer.length;
                if (dataIn != null) {
                    // Even if we have or had errors, we need to transfer
                    // the whole agreed upon bytes, valid or not.
                    // This is not to corrupt the stream.
                    try {
                        chunkSize = dataIn.read(buffer, 0, chunkSize);
                    } catch (IOException e) {
                        logger.log(Level.WARNING,
                                    "Error reading from file " + src, e);
                        dataIn = null;
                    }
                }
                if (chunkSize < 0) {
                    break;
                } else if (chunkSize == 0) {
                    continue;
                }
                out.write(buffer, 0, chunkSize);
                remainder -= chunkSize;
            }
            if (dataIn != null)
                try {
                    dataIn.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error closing file " +
                                                src, e);
                }
            buffer = null;
        }
    }

    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {

        // Read the headers
        src = (String) in.readObject();
        dest = (String) in.readObject();
        size = in.readLong();

        // Convert destination file name to OS path name
        dest = Utilities.convertPath(dest);

        // We need to ensure we read everything out in order not to
        // cause an rmi stream corruption, even if our file write bails.
        // Create the file
        FileOutputStream dataOut = null;
        try {
            dataOut = new FileOutputStream(dest);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error opening file " + dest, e);
        }

        // Then, read the data stream and save to file, one buffer at a time.
        if (buffer == null)
            buffer = new byte[size < MAX_BUFFER_SIZE ? (int) size :
                                                        MAX_BUFFER_SIZE];
        long remainder = size;
        while (remainder > 0) {
            int chunkSize =
                    remainder < buffer.length ? (int) remainder : buffer.length;
            chunkSize = in.read(buffer, 0, chunkSize);
            if (chunkSize < 0) {
                break;
            } else if (chunkSize == 0) {
                continue;
            }
            if (dataOut != null)
                // We still have to clear the stream,
                // even if we cannot write it to file.
                try {
                    dataOut.write(buffer, 0, chunkSize);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error writing to file " +
                                            dest, e);
                    dataOut = null;
                }
            remainder -= chunkSize;
        }
        transferSize = size - remainder;
        if (dataOut != null) {
            try {
                dataOut.flush();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error flushing data to file " +
                                            dest, e);
            }
            try {
                dataOut.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing file " +
                                            dest, e);
            }
        }
        buffer = null;
    }
}
