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
import com.sun.faban.common.FileTransfer;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.io.*;

/**
 * This is the interface that is implemented by the FileAgent.
 * The FileAgent is used to access any file remotely.
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.agent.FileService
 */
public interface FileAgent extends Remote {
    /** Open for reading. */
    public static final int READ = 1;
    /** Open for writing. */
    public static final int WRITE = 2;
    /** Open for write in append mode. */
    public static final int APPEND = 3;	

    /**
     * Open a file for reading and/or writing.
     * @param file filename
     * @param mode file open mode - READ, WRITE, APPEND
     * @return FileService - reference to the FileService interface.
     * @throws RemoteException A communications error occurred.
     * @throws FileServiceException Error opening file
     */
    public FileService open(String file, int mode)
            throws RemoteException, FileServiceException;

    /**
     * Read a file from this machine and return the contents as a String.
     * 
     * @param file name of the file
     * @return String contents of the file
     * @throws IOException An I/O error occurred
     */
    public String readWholeFile(String file) throws IOException;

    /**
     * Write the given file to this machine.
     * 
     * @param file path
     * @param contents of the file.
     * @return boolean true if successful, false if not.
     * @throws RemoteException A communications error occurred.
     */
    public boolean writeWholeFile(String file, String contents)
            throws RemoteException;

    /**
     *
     * Remove a file.
     *
     * @param  fileName - The pathname for the file.
     * @return boolean - true if successful,
     *                   false if not successful or file does not exist.
     * @throws RemoteException A communications error occurred.
     */
    public boolean removeFile(String fileName) throws RemoteException;

    /**
     * Truncates a file to zero-length.
     * @param fileName The pathname for the file
     * @return Whether truncation succeeded
     * @throws RemoteException An error occurred.
     */
    public boolean truncateFile(String fileName) throws RemoteException;

    /**
     * Remove files from a directory matched by the filter.
     * @param dirName The directory path name
     * @param filter The filter
     * @return True if files are successfully removed. False otherwise
     * @throws RemoteException A communications error occurred.
     */
    boolean removeFiles(String dirName,
                               com.sun.faban.harness.FileFilter filter)
            throws RemoteException;
    /**
     * Gets a property from a given file.
     * @param configFile The config file name
     * @param propName The property key name
     * @return The property value
     * @throws IOException If there is an error accessing the config file
     */
    String getProperty(String configFile, String propName)
            throws IOException;

    /**
     * Checks whether the given remote file exists.
     * @param fileName The file name to test.
     * @return true if exists, false otherwise.
     * @throws RemoteException A communications error occurred.
     */
    boolean doesFileExist(String fileName) throws RemoteException;

    /**
     * Checks whether the given remote file exists and is a normal file.
     * @param fileName The file name to test.
     * @return true if file is a normal file, false otherwise.
     * @throws RemoteException A communications error occurred.
     */
    boolean isFile(String fileName) throws RemoteException;

    /**
     * Checks whether the given remote file exists and is a directory.
     * @param fileName The file name to test.
     * @return true if file is a directory, false otherwise.
     * @throws RemoteException A communications error occurred.
     */
    boolean isDirectory(String fileName) throws RemoteException;

    /**
     * Pushes a file, as encapsulated in the FileTransfer, from a the master
     * to this agent. The serialization of the FileTransfer causes the file
     * to be copied from src to dest over the wire.
     * @param transfer The file transfer description
     * @return The number of bytes transferred
     * @throws RemoteException If there is an error in the transfer
     */
    long push(FileTransfer transfer) throws RemoteException;

    /**
     * Gets a file from the local system to the master. The serialization of
     * the FileTransfer itself causes the file to be copied to the master.
     * @param srcFile The source file on the host the agent is running on
     * @param destFile The destination file on the master
     * @return The FileTransfer causing this transfer
     * @throws IOException If there is an error reading or transferring
     */
    FileTransfer get(String srcFile, String destFile)
            throws IOException;
}
