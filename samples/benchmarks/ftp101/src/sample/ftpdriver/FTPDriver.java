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
package sample.ftpdriver;

import com.sun.faban.driver.*;
import com.sun.faban.driver.util.Random;
import sun.net.ftp.FtpClient;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.logging.Logger;

/**
 * FTP Driver. Please note that we're using the sun.net.ftp.FtpClient api.
 * While this api has been relatively stable, it is not a public api and
 * may change without notice.
 */
@BenchmarkDefinition (
    name    = "Sample FTP Workload 101",
    version = "0.2",
    configPrecedence = true
)
@BenchmarkDriver (
    name           = "FTPDriver",
    threadPerScale    = 1
)
@FlatMix (
    operations = {"GET", "PUT", "Connect"},
    mix = { 80, 15, 5 },
    deviation = 2
)
@NegativeExponential (
    cycleType = CycleType.CYCLETIME,
    cycleMean = 2000,
    cycleDeviation = 2
)
public class FTPDriver {

    /** The driver context for this instance. */
    private DriverContext ctx;
    Logger logger;
    Random random;
    FtpClient ftpClient;
    int fileCount;
    String host;
    int port = -1;
    int threadId;
    int putSequence = 1;
    String localFileName;
    String uploadPrefix;
    String user;
    String password;
    byte[] buffer = new byte[8192];

    /**
     * Constructs the FTP driver instance.
     * @throws XPathExpressionException An XPath error occurred
     * @throws IOException I/O error creating the driver instance
     */
    public FTPDriver() throws XPathExpressionException, IOException {
        ctx = DriverContext.getContext();

        logger = ctx.getLogger();

        random = ctx.getRandom();
        threadId = ctx.getThreadId();
        uploadPrefix = "up" + threadId + '_';
        localFileName = "/tmp/ftp" + threadId;
        host =
                ctx.getXPathValue("/ftpBenchmark/serverConfig/host").trim();
        String port =
                ctx.getXPathValue("/ftpBenchmark/serverConfig/port").trim();
        fileCount = Integer.parseInt(ctx.getXPathValue(
                "/ftpBenchmark/serverConfig/fileCount").trim());
        user = ctx.getProperty("user");
        password = ctx.getProperty("password");

        // Connect ftp client.
        ftpClient = new FtpClient();
        if (port == null || port.length() == 0) {
            ftpClient.openServer(host);
        } else {
            this.port = Integer.parseInt(port);
            ftpClient.openServer(host, this.port);
        }
        ftpClient.login(user, password);
        ftpClient.binary();
        ftpClient.cd("pub");

        // Download initial file.
        int fileNo = random.random(1, fileCount);
        String fileName = "File" + fileNo;
        FileOutputStream download = new FileOutputStream(localFileName);
        int count;
        int size = 0;
        InputStream ftpIn = ftpClient.get(fileName);
        while ((count = ftpIn.read(buffer)) != -1) {
            download.write(buffer, 0, count);
            size += count;
        }
        if (size == 0)
            throw new FatalException("Cannot get file :" + fileName);
        ftpIn.close();
        download.close();
    }

    /**
     * Operation to test FTP get.
     * @throws IOException Error doing the get
     */
    @BenchmarkOperation (
        name    = "GET",
        max90th = 2,
        timing  = Timing.MANUAL
    )
    public void doGet() throws IOException {
        int fileNo = random.random(1, fileCount);
        String fileName = "File" + fileNo;
        logger.finest("Getting " + fileName);
        FileOutputStream download = new FileOutputStream(localFileName);
        int count;
        ctx.recordTime();
        InputStream ftpIn = ftpClient.get(fileName);
        while ((count = ftpIn.read(buffer)) != -1)
            download.write(buffer, 0, count);
        ftpIn.close();
        ctx.recordTime();
        download.close();
    }

    /**
     * Operation to test FTP put.
     * @throws IOException Error doing the put
     */
    @NegativeExponential (
        cycleType = CycleType.CYCLETIME,
        cycleMean = 4000,
        cycleDeviation = 2
    )
    @BenchmarkOperation (
        name    = "PUT",
        max90th = 2,
        timing  = Timing.MANUAL
    )
    public void doPut() throws IOException {
        String fileName = uploadPrefix + putSequence++;
        logger.finest("Putting " + fileName);
        FileInputStream upload = new FileInputStream(localFileName);
        int count;
        ctx.recordTime();
        OutputStream ftpOut = ftpClient.put(fileName);
        while ((count = upload.read(buffer)) != -1)
            ftpOut.write(buffer, 0, count);
        ftpOut.close();
        ctx.recordTime();
        upload.close();
    }

    /**
     * Operation to test ftp connect to server.
     * @throws IOException Error connecting to ftp server
     */
    @BenchmarkOperation (
        name    = "Connect",
        max90th = 2,
        timing  = Timing.MANUAL
    )
    public void doConnect() throws IOException {
        ftpClient.closeServer();

        ctx.recordTime();
        if (port == -1) {
            ftpClient.openServer(host);
        } else {
            ftpClient.openServer(host, this.port);
        }
        ftpClient.login(user, password);
        ftpClient.binary();
        ftpClient.cd("pub");
        ctx.recordTime();
    }
}
