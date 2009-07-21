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
 * $Id: FileService.java,v 1.4 2009/07/21 22:54:46 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;
import java.rmi.RemoteException;
import java.rmi.Remote;

/**
 * The methods in this interface are the public face of FileService
 * They can be used by any remote object to access remote files.
 * For example, the ToolAgent uses this class to copy the tool output
 * files to the master machine.
 * Objects of this class are created via the FileAgent.
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.agent.FileAgent
 */
public interface FileService extends Remote {

    /**
     * This method is responsible for reading a file.
     * @return String read, null if end-of-file
     */
    public byte[] read() throws RemoteException, FileServiceException;
    
    
    /**
     * This method is responsible for writing to a file.
     * @param count no. of bytes to read
     */
    public byte[] readBytes(int count) throws RemoteException, FileServiceException;

    /**
     * This method is responsible for writing to a file.
     * @param buffer to write
     */
    public void write (byte[] buffer) throws RemoteException, FileServiceException;
    
    
    /**
     * This method is responsible for writing to a file.
     * @param buffer to write
     * @param begin staring index
     * @param end ending index
     */
    public void writeBytes (byte[] buffer, int begin, int end) throws RemoteException, FileServiceException;

    /**
     * Close the current file.
     */
    public void close() throws RemoteException;
}
    
