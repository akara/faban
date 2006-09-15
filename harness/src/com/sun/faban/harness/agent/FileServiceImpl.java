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
 * $Id: FileServiceImpl.java,v 1.3 2006/09/15 18:51:28 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

    private ByteBuffer inBuffer;
    private long inSize;
    private FileInputStream in = null;
    private FileOutputStream out = null;
    private String filename;
    private String filehost;

    /**
     * Open a file for reading or writing
     * @param file to access
     * @param file open mode - READ or WRITE/APPEND
     */
    public FileServiceImpl(String file, int mode) throws
            RemoteException, FileServiceException {
        this.filename = file;
        try {
            filehost = "FileService@" + (InetAddress.getLocalHost()).getHostName();
            if (mode == FileAgent.READ) {
                in = new FileInputStream(file);
                FileChannel channel = in.getChannel();
                inSize = channel.size();
                if (inSize >= Integer.MAX_VALUE)
                    throw new FileServiceException("Cannot handle file size >= 2GB");
                inBuffer = channel.map(FileChannel.MapMode.READ_ONLY,
                                0, inSize);

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
     * This method is responsible for reading a file
     */
    public byte[] read() throws FileServiceException {
        byte[] content = new byte[(int) inSize];
        inBuffer.position(0);
        inBuffer.get(content);
        return content;
    }


    public byte[] readBytes(int count) throws FileServiceException {
        int remain = inBuffer.remaining();
        if (count > remain)
            count = remain;
        byte[] content = new byte[count];
        inBuffer.get(content);
        return content;
    }


    /**
     * This method is responsible for writing to a file
     * @param buffer to write
     */
    public void write (byte[] buffer) throws FileServiceException {
        writeBytes(buffer, 0, buffer.length);
    }


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
     * Close the file opened previously
     */
    public void close() {
        try {
            if (in != null) {
                inBuffer = null;
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
