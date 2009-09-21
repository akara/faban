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
package com.sun.faban.harness.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * FileServiceImpl is the class that reads/writes remote files and
 * can be used by anyone to access remote files.
 * <ul>
 * <li> It implements the FileService interface; see the
 *      FileService.java file for its description.
 * </ul>
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.agent.FileService
 */
class FileServiceImpl extends UnicastRemoteObject
        implements FileService {

    private long inSize;
    private long offset = 0l;
    private FileInputStream in = null;
    private FileOutputStream out = null;
    private String filename;
    private String filehost;

    /**
     * Open a file for reading or writing.
     * @param file to access
     * @param mode open mode - READ or WRITE/APPEND
     * @throws RemoteException A communications error occurred.
     * @throws FileServiceException Error opening file
     */
    public FileServiceImpl(String file, int mode) throws
            RemoteException, FileServiceException {
        this.filename = file;
        try {
            filehost = "FileService@" + (InetAddress.getLocalHost()).getHostName();
            if (mode == FileAgent.READ) {
                File inFile = new File(file);
                inSize = inFile.length();
                in = new FileInputStream(inFile);
            }
            else if (mode == FileAgent.WRITE) {
                out = new FileOutputStream(file);
            }
            else if (mode == FileAgent.APPEND) {
                out = new FileOutputStream(file, true);
            }
            else {
                throw new FileServiceException(filehost +
                        ": Invalid file open mode: " + mode);
            }
        } catch (IOException ie) {
            throw new FileServiceException (filehost + ": Error opening file " +
                    file + ie.getMessage());
        }
    }

    /**
     * This method is responsible for reading a whole file or a whole remainder
     * of the file. Up to 2GB are read at a time.
     * @return The byte array represeting the read output
     * @throws FileServiceException Error reading the file
     */
    public byte[] read() throws FileServiceException {
        long remainder = inSize - offset;
        int readSize;
        if (remainder > Integer.MAX_VALUE)
            readSize = Integer.MAX_VALUE;
        else
            readSize = (int) remainder;

        return verifiedReadBytes(readSize);
    }

    /**
     * This method is responsible for reading a whole remainder
     * of the file up to a given size.
     * @param readSize The size to read
     * @return The read result, up to the specified size
     * @throws FileServiceException
     */
    public byte[] readBytes(int readSize) throws FileServiceException {
        long remainder = inSize - offset;
        if (readSize > remainder) {
            readSize = (int) remainder;
        }

        return verifiedReadBytes(readSize);
    }

    private byte[] verifiedReadBytes(int readSize) throws FileServiceException {

        byte[] content = new byte[readSize];
        try {
            int position = 0;
            while (position < readSize) {
                int c = in.read(content, position, readSize - position);
                if (c >= 0) {
                    position += c;
                    offset += c;
                } else { // Prematurely ran into eof
                    throw new FileServiceException(
                                                filehost + ": Unexpected EOF!");
                }
            }
        } catch (IOException e) {
            throw new FileServiceException(filehost + ": Error reading file.");
        }
        return content;
    }


    /**
     * This method is responsible for writing to a file.
     * @param buffer to write
     *
     * @throws FileServiceException
     */
    public void write (byte[] buffer) throws FileServiceException {
        writeBytes(buffer, 0, buffer.length);
    }

    /**
     * This method is responsible for writing bytes to a file.
     * @param buffer
     * @param begin
     * @param end
     * @throws com.sun.faban.harness.agent.FileServiceException
     */
    public void writeBytes (byte[] buffer, int begin, int end) throws FileServiceException {
        try {
            if (out == null) {
                throw new FileServiceException(filehost + ": File not opened for write");
            }
            out.write(buffer, begin, end);
            out.flush();
        }
        catch (IOException ie) {
            throw new
                    FileServiceException(filehost + ": I/O error during read of '"
                    + filename + "'");
        }
    }


    /**
     * Close the file opened previously.
     */
    public void close() {
        try {
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (IOException ie) {
            // We ignore any close errors, as it is a pain to handle them
            // during aborts
        }
    }
}
