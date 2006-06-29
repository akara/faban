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
 * $Id: WebDriver.java,v 1.2 2006/06/29 19:38:45 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package sample.driver;

import com.sun.faban.driver.*;
import com.sun.faban.driver.util.Random;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.logging.Logger;

@BenchmarkDefinition (
    name    = "Sample Web Workload 1",
    version = "0.2"
)
@BenchmarkDriver (
    name           = "WebDriver",
    threadPerScale    = 1
)
@MatrixMix (
    operations = {"MyOperation1", "MyOperation2", "MyOperation3"},
    mix = { @Row({  0, 70, 30 }),
            @Row({ 60,  0, 40 }),
            @Row({ 50, 50,  0 }) },
    deviation = 2
)

@NegativeExponential (
    cycleType = CycleType.CYCLETIME,
    cycleMean = 5000,
    cycleDeviation = 2
)
public class WebDriver {

    /** The driver context for this instance */
    private DriverContext ctx;
    private HttpTransport http;
    private String url1, url2, url3;
    Logger logger;
    Random random;

    public WebDriver() throws XPathExpressionException {
        ctx = DriverContext.getContext();
        http = new HttpTransport();
        logger = ctx.getLogger();
        random = ctx.getRandom();
        String host = ctx.getXPathValue("/webBenchmark/serverConfig/host");
        String port = ctx.getXPathValue("/webBenchmark/serverConfig/port");
        String path1 = ctx.getProperty("path1");
        String path2 = ctx.getProperty("path2");
        String path3 = ctx.getProperty("path3");
        url1 = "http://" + host + ':' + port + '/' + path1;
        url2 = "http://" + host + ':' + port + '/' + path2;
        url3 = "http://" + host + ':' + port + '/' + path3;
    }

    @BenchmarkOperation (
        name    = "MyOperation1",
        max90th = 2,
        timing  = Timing.AUTO
    )
    public void doMyOperation1() throws IOException {
        logger.finest("Accessing " + url1);
        http.fetchURL(url1);
    }

    @BenchmarkOperation (
        name    = "MyOperation2",
        max90th = 2,
        timing  = Timing.AUTO
    )
    public void doMyOperation2() throws IOException {
        logger.finest("Accessing " + url2);
        http.fetchURL(url2);
    }

    @BenchmarkOperation (
        name    = "MyOperation3",
        max90th = 2,
        timing  = Timing.AUTO
    )
    public void doMyOperation3() throws IOException {
        logger.finest("Accessing " + url3);
        http.fetchURL(url3);
    }
}
