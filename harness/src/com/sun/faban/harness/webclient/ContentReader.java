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
 * $Id: ContentReader.java,v 1.1 2006/06/29 18:51:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

/**
 * The content reader reads requests for output files from the actual file
 * location which is not a subpath of the context and sends it back to the
 * requestor.
 *
 * @author Akara Sucharitakul
 */
public class ContentReader extends HttpServlet {
    ObjectPool bufferPool = new ObjectPool();

    public void doGet(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getContextPath();
        String uri = request.getRequestURI();
        ServletOutputStream out = response.getOutputStream();

        // Safety check making sure the resource is part of URI.
        if (!uri.startsWith(path)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Could not identify requested resource.");
            out.flush();
            out.close();
            return;
        }

        // Now create a resource path with the context and servlet path
        // taken out.
        path = uri.substring(path.length());

        // Now we have a resource i.e. /output/xmark.1A/summary.xml
        // We need to know the resource type by extracting the next level.
        int idx = path.indexOf('/', 1);

        // Then we break down the resource further to the actual file we wanted
        String resource = "";
        if (idx != -1) {
            resource = path.substring(idx + 1);
            path = path.substring(0, idx);
        }

        // Now the resource is actually the resource resource we want.
        // We have to append the real output dir to it.
        if ("/output".equals(path)) {
            resource = Config.OUT_DIR + resource;
        } else if ("/bench_downloads".equals(path)) {
            resource = Config.BENCHMARK_DIR + resource;
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            out.println("Routing of path " + path + " not implemented.");
            out.flush();
            out.close();
            return;
        }

        // Need to check that this file exists and is not a directory.
        File f = new File(resource);
        if (!f.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("Resource " + resource + " not found.");
            out.flush();
            out.close();
            return;
        }

        // For directory, we send a text list of entries in the directory.
        // This is even easier to parse than XML.
        if (f.isDirectory()) {
            response.setContentType("text/plain");
            // Note: wWe use '\n' instead of println so we can be sure it is
            // always '\n' and not '\r\n', etc.
            out.print(" Directory: " + f.getName() + '\n');
            File[] entries = f.listFiles();
            for (int i = 0; i < entries.length; i++) {
                char postfix;
                if (!entries[i].exists())
                    continue;
                if (entries[i].isDirectory())
                    postfix = '/';
                else
                    postfix = ' ';
                out.print(entries[i].getName() + postfix + '\n');
            }
            out.flush();
            out.close();
            return;
        }

        // We don't want to keep allocating 8K all the time, so we keep
        // our buffers in a pool.
        byte[] buffer = (byte[]) bufferPool.get();
        if (buffer == null)
            buffer = new byte[8192];

        // Then we just transfer the file to the servlet output.
        FileInputStream fIn = new FileInputStream(f);
        for (;;) {
            int length = fIn.read(buffer);
            if (length > 0)
                out.write(buffer, 0, length);
            else if (length == -1)
                break;
        }

        // Now return the buffer to the pool.
        bufferPool.put(buffer);

        // All done. Now we just close everything.
        out.flush();
        out.close();
    }

    static class ObjectPool {

        ArrayList backingList = new ArrayList();

        public synchronized Object get() {
            if (backingList.size() == 0)
                return null;
            return backingList.remove(backingList.size() - 1);
        }

        public synchronized void put(Object o) {
            backingList.add(o);
        }
    }
}
