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
 * $Id: FileTransfer.java,v 1.2 2008/04/18 07:09:39 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.io.*;

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
public class FileTransfer implements Serializable {

    private static final long serialVersionUID = 3122008L;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private String src;
    private String dest;
    private long size;

    private transient byte[] buffer;

    /**
     * Creates a file transfer object.
     * @param src The source file name
     * @param dest The destination file name
     */
    public FileTransfer(String src, String dest) throws FileNotFoundException {
        this.src = src;
        File srcFile = new File(src);
        if (!srcFile.isFile())
            throw new FileNotFoundException("File " + src + " does not exist.");
        this.dest = dest;
    }

    public FileTransfer(byte[] buffer, int offset, int length, String dest) {
        this.dest = dest;
        this.buffer = new byte[length];
        System.arraycopy(buffer, offset, this.buffer, 0, length);
    }

    /**
     * Obtains the sources file name.
     * @return The source file name
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (src != null)
            writeFile(out);
        else if (buffer != null)
            writeBuffer(out);
        else
            throw new IOException("Data source not provided");
    }

    private void writeFile(ObjectOutputStream out) throws IOException {

        File srcFile = new File(src);
        size = srcFile.length();
        if (size <= 0)
            throw new IOException(srcFile.getAbsolutePath() +
                    ": Invalid file size of " + size);

        // Ensure we can read from the input file.
        FileInputStream dataIn = new FileInputStream(src);

        // Write the headers.
        out.writeObject(src);
        out.writeObject(dest);
        out.writeLong(size);

        // Then, write the data stream, one buffer at a time.
        if (buffer == null)
            buffer = new byte[size < DEFAULT_BUFFER_SIZE ? (int) size :
                                                        DEFAULT_BUFFER_SIZE];

        long remainder = size;
        while (remainder > 0) {
            int chunkSize =
                    remainder < buffer.length ? (int) remainder : buffer.length;
            chunkSize = dataIn.read(buffer, 0, chunkSize);
            out.write(buffer, 0, chunkSize);
            remainder -= chunkSize;
        }
        dataIn.close();
        buffer = null;
    }

    private void writeBuffer(ObjectOutputStream out) throws IOException {
        size = buffer.length;
        out.writeObject("");
        out.writeObject(dest);
        out.writeLong(size);
        out.write(buffer, 0, buffer.length);
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {

        // Read the headers
        src = (String) in.readObject();
        dest = (String) in.readObject();
        size = in.readLong();

        // Create the file
        FileOutputStream dataOut = new FileOutputStream(dest);

        // Then, read the data stream and save to file, one buffer at a time.
        if (buffer == null)
            buffer = new byte[size < DEFAULT_BUFFER_SIZE ? (int) size :
                                                        DEFAULT_BUFFER_SIZE];
        long remainder = size;
        while (remainder > 0) {
            int chunkSize =
                    remainder < buffer.length ? (int) remainder : buffer.length;
            chunkSize = in.read(buffer, 0, chunkSize);
            dataOut.write(buffer, 0, chunkSize);
            remainder -= chunkSize;
        }
        dataOut.flush();
        dataOut.close();
        buffer = null;
    }

    private void readObjectNoData()
            throws ObjectStreamException {
        throw new StreamCorruptedException("FileTransfer did not receive data");
    }
}
