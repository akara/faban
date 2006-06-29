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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FileServiceImpl.java,v 1.1 2006/06/29 18:51:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import com.sun.faban.harness.agent.FileAgent;
import com.sun.faban.harness.agent.FileService;
import com.sun.faban.harness.agent.FileServiceException;

import java.io.*;
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

    private BufferedReader in = null;
    private PrintWriter out = null;
    private BufferedInputStream inBytes = null;
    private BufferedOutputStream outBytes = null;
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
                in = new BufferedReader( new FileReader(file));
                inBytes = new BufferedInputStream(new FileInputStream(file));
            }
            else if (mode == FileAgent.WRITE) {
                out = new PrintWriter(new FileWriter(file));
                outBytes = new BufferedOutputStream(new FileOutputStream(file));
            }
            else if (mode == FileAgent.APPEND) {
                out = new PrintWriter(new FileWriter(file, true));
            }
            else {
                throw new FileServiceException(filehost + ": Invalid file open mode: " + mode);
            }
        } catch (IOException ie) {
            throw new FileServiceException (filehost + ": Error opening file " +
                    file + ie.getMessage());
        }
    }

    /**
     * This method is responsible for reading a file
     */
    public String read () throws FileServiceException {
        try {
            if (in == null) {
                throw new FileServiceException(filehost + ": File '"
                        + filename + "' not opened for read");
            }
            return(in.readLine());
        } catch (IOException ie) {
            throw new FileServiceException(filehost + ": I/O error during read of '"
                    + filename + "'");
        }
    }


    public byte[] readBytes(int count) throws FileServiceException {

        byte[] retVal;

        try {
            if (inBytes == null) {
                throw new FileServiceException(filehost + ": File '"
                        + filename + "' not opened for read");
            }
            if (inBytes.available() > count) {
                retVal = new byte[count];
                inBytes.read(retVal, 0, count);
                return retVal;
            }
            else {
                retVal = new byte[inBytes.available()];
                inBytes.read(retVal, 0, inBytes.available());
                return retVal;
            }
        }
        catch (IOException ie) {
            throw new
                    FileServiceException(filehost + ": I/O error during read of '"
                    + filename + "'");
        }
    }


    /**
     * This method is responsible for writing to a file
     * @param buffer to write
     */
    public void write (String buffer) throws FileServiceException {
        // try {
        if (out == null) {
            throw new FileServiceException(filehost + ": File not opened for write");
        }
        out.println(buffer);
        // } catch (IOException ie) {
        // throw new FileServiceException(filehost + ": I/O error during write");
        // }
    }


    public void writeBytes (byte[] buffer, int begin, int end) throws FileServiceException {
        try {
            if (outBytes == null) {
                throw new FileServiceException(filehost + ": File not opened for write");
            }
            outBytes.write(buffer, begin, end);
            outBytes.flush();
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
                in.close();
                in = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }
            if (inBytes != null) {
                inBytes.close();
                inBytes = null;
            }
            if (outBytes != null) {
                outBytes.close();
                outBytes = null;
            }
        } catch (IOException ie) {
            // We ignore any close errors, as it is a pain to handle them
            // during aborts
        }
    }
}
