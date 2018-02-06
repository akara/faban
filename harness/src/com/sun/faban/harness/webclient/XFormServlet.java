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
package com.sun.faban.harness.webclient;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.chiba.adapter.AbstractChibaAdapter;
import org.chiba.adapter.ChibaAdapter;
import org.chiba.adapter.InteractionHandler;
import org.chiba.tools.xslt.StylesheetLoader;
import org.chiba.tools.xslt.UIGenerator;
import org.chiba.tools.xslt.XSLTGenerator;
import org.chiba.xml.xforms.config.Config;
import org.chiba.xml.xforms.events.EventFactory;
import org.chiba.xml.xforms.exception.XFormsException;
import org.chiba.xml.xforms.ui.Repeat;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The XFormServlet is the glue code between Faban and Chiba XForms.
 * It serves get requests to initially access the XForm and post requests for
 * subsequent interactions.
 */
public class XFormServlet extends HttpServlet {

    private static Logger logger = Logger.getLogger(
            "com.sun.faban.harness.webclient.XFormServlet");
    private static final String XML_PARSER_FACTORY = "javax.xml.parsers.DocumentBuilderFactory";
    private static final String XSLT_TRANSFORMER_FACTORY = "javax.xml.transform.TransformerFactory";

    private String configFile;
    private String ctxRoot;
    private String uploadDir;
    private String xsltDir;
    private String errPage;
    ServletContext ctx;
    private static String internalXercesImpl, xercesImpl;
    private static String internalXalanImpl, xalanImpl;

    /**
     * Initializes the servlet.
     *
     * @throws javax.servlet.ServletException
     */
    public void init() throws ServletException {

        ctx = getServletContext();
        ctxRoot = ctx.getRealPath("");
        if (ctxRoot == null)
            ctxRoot = ctx.getRealPath(".");

        ServletConfig cfg = getServletConfig();

        // Location of the config file.
        String path = cfg.getInitParameter("configFile");
        if (path != null)
            configFile = ctx.getRealPath(path);

        // Directory to store uploaded files, default to java.io.tmpDir
        uploadDir = cfg.getInitParameter("uploadDir");
        if (uploadDir == null)
            uploadDir = System.getProperty("java.io.tmpdir");

        // Make sure nobody uploads to WEB-INF - security breach!
        if (uploadDir != null && uploadDir.equalsIgnoreCase("WEB-INF")) {
            throw new ServletException("Cannot write directory " + uploadDir);
        }
        // Directory containing xslt stylesheets
        path = cfg.getInitParameter("xsltDir");
        if (path != null)
            xsltDir = ctx.getRealPath(path);

        errPage = cfg.getInitParameter("errorPage");

        internalXercesImpl = cfg.getInitParameter("internalXercesImpl");
        xercesImpl = cfg.getInitParameter("xercesImpl");
        internalXalanImpl = cfg.getInitParameter("internalXalanImpl");
        xalanImpl = cfg.getInitParameter("xalanImpl");
    }

    /**
     * A get request starts a new form.
     *
     * @param request The servlet request
     * @param response The servlet response
     * @throws ServletException Error in request handling
     * @throws IOException Error doing other IO
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(true);
        Adapter adapter = null;

        String templateFile = (String)
                                session.getAttribute("faban.submit.template");
        String styleSheet = (String)
                                session.getAttribute("faban.submit.stylesheet");

        String srcURL = new File(templateFile).toURI().toString();

        logger.finer("benchmark.template: " + srcURL);
        session.removeAttribute("faban.submit.template");
        session.removeAttribute("faban.submit.stylesheet");

        try {
            String requestURI = request.getRequestURI();
            String formURI = null;
            String contextPath = request.getContextPath();
            String benchPath = contextPath + "/bm_submit/";
            if (requestURI.startsWith(benchPath)) {
                int idx = requestURI.indexOf('/', benchPath.length());
                String benchName = requestURI.substring(benchPath.length(),
                                   idx);
                String formName = requestURI.substring(idx + 1);
                formURI = com.sun.faban.harness.common.Config.FABAN_HOME +
                        "benchmarks/" + benchName + "/META-INF/" + formName;
            } else {
                StringBuffer buffer = new StringBuffer(request.getScheme());
                buffer.append("://");
                buffer.append(request.getServerName());
                buffer.append(":");
                buffer.append(request.getServerPort()) ;
                buffer.append(request.getContextPath());
                buffer.append(request.getParameter("form"));
                formURI = buffer.toString();
            }

            if (formURI == null) {
                throw new IOException("Resource not found: " + formURI);
            }
            logger.finer("Form URI: " + formURI);

            String css = request.getParameter("css");
            String actionURL = response.encodeURL(request.getRequestURI());
            logger.finer("actionURL: " + actionURL);

            // Find the base URL used by Faban. We do not use Config.FABAN_URL
            // because this base URL can vary by the interface name the Faban
            // master is accessed in this session. Otherwise it is identical.
            StringBuffer baseURL = request.getRequestURL();
            int uriLength = baseURL.length() - requestURI.length() +
                            contextPath.length();
            baseURL.setLength(++uriLength); // Add the ending slash

            adapter = new Adapter();
            if (configFile != null && configFile.length() > 0)
                adapter.setConfigPath(configFile);

            File xsl = null;
            if (styleSheet != null)
                xsl = new File(styleSheet);

            if (xsl != null && xsl.exists()) {
                adapter.xslPath = xsl.getParent();
                adapter.stylesheet = xsl.getName();
            } else {
                adapter.xslPath = xsltDir;
                adapter.stylesheet = "faban.xsl";
            }

            adapter.baseURI = baseURL.toString();
            adapter.formURI = formURI;
            adapter.actionURL = actionURL;
            adapter.beanCtx.put("chiba.web.uploadDir", uploadDir);
            adapter.beanCtx.put("chiba.useragent", request.getHeader(
                                 "User-Agent"));
            adapter.beanCtx.put("chiba.web.request", request);
            adapter.beanCtx.put("chiba.web.session", session);
            adapter.beanCtx.put("benchmark.template", srcURL);


            if (css != null) {
                adapter.CSSFile = css;
                logger.fine("using css stylesheet: " + css);
            }

            Map servletMap = new HashMap();
            servletMap.put(ChibaAdapter.SESSION_ID, session.getId());
            adapter.beanCtx.put(ChibaAdapter.SUBMISSION_RESPONSE, servletMap);


            Enumeration params = request.getParameterNames();
            while (params.hasMoreElements()) {
                String s = (String) params.nextElement();
                //store all request-params we don't use in the beanCtx map
                if (!(s.equals("form") || s.equals("xslt") ||
                       s.equals("css") || s.equals("action_url"))) {
                    String value = request.getParameter(s);
                    adapter.beanCtx.put(s, value);
                    logger.finer("added request param '" + s + "' to beanCtx");
                }
            }
            adapter.init();
            adapter.execute();

            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            adapter.generator.setOutput(out);
            adapter.buildUI();
            session.setAttribute("chiba.adapter", adapter);
            out.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception processing XForms", e);
            shutdown(adapter, session, e, request, response);
        } finally {
           System.setProperty(XML_PARSER_FACTORY, internalXercesImpl);
        }
    }


    /**
     * A post request deals with form interactions.
     *
     * @param request  The servlet request
     * @param response The servlet response
     * @throws ServletException Error in request handling
     * @throws IOException Error doing other IO
     */
    public void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        Adapter adapter = null;

        try {
            adapter = (Adapter) session.getAttribute("chiba.adapter");
            if (adapter == null) {
                throw new ServletException(Config.getInstance().getErrorMessage(
                                           "session-invalid"));
            }
            adapter.beanCtx.put("chiba.useragent", request.getHeader(
                                 "User-Agent"));
            adapter.beanCtx.put("chiba.web.request", request);
            adapter.beanCtx.put("chiba.web.session", session);
            //adapter.executeHandler();
            adapter.execute();

            // Check for redirects
            String redirectURI = (String) adapter.beanCtx.get(
                                                  ChibaAdapter.LOAD_URI);
            if (redirectURI != null) {
                String redirectTo = redirectURI;
                adapter.shutdown();
                response.sendRedirect(response.encodeRedirectURL(redirectTo));
                adapter.beanCtx.put(ChibaAdapter.LOAD_URI, null);
                return;
            }

            // Check for forwards
            Map forwardMap = (Map) adapter.beanCtx.get(
                                           ChibaAdapter.SUBMISSION_RESPONSE);
            InputStream forwardStream = (InputStream) forwardMap.get(
                        ChibaAdapter.SUBMISSION_RESPONSE_STREAM);
            if (forwardStream != null) {
                adapter.shutdown();

                // fetch response stream
                InputStream responseStream = (InputStream) forwardMap.remove(
                        ChibaAdapter.SUBMISSION_RESPONSE_STREAM);

                // copy header info
                Iterator iterator = forwardMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String name = iterator.next().toString();
                    String value = forwardMap.get(name).toString();
                    response.setHeader(name, value);
                }

                // copy stream content
                byte[] copyBuffer = new byte[8092];
                OutputStream out = response.getOutputStream();
                int readLength = responseStream.read(copyBuffer);
                do {
                    out.write(copyBuffer, 0, readLength);
                    readLength = responseStream.read(copyBuffer);
                } while (readLength >= 0);

                responseStream.close();
                out.close();

                // remove forward response and terminate
                adapter.beanCtx.put(ChibaAdapter.SUBMISSION_RESPONSE, null);
                return;
            }

            // Neither redirects nor forwards, handle it the normal way.
            response.setContentType("text/html");
            Writer writer = response.getWriter();
            adapter.generator.setOutput(writer);
            adapter.buildUI();
            writer.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception processing XForms", e);
            shutdown(adapter, session, e, request, response);
        } finally {
            System.setProperty(XML_PARSER_FACTORY, internalXercesImpl);
        }
    }

    private void shutdown(Adapter adapter, HttpSession session, Exception e,
                          HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        // attempt to shutdown processor
        if (adapter != null) {
            try {
                adapter.shutdown();
            } catch (XFormsException xe) {
                logger.log(Level.WARNING,
                           "Error shutting down Chiba bean.", xe);
            }
        }

        // store exception
        session.setAttribute("chiba.exception", e);

        // redirect to error page, if set
        if (errPage != null)
            response.sendRedirect(response.encodeRedirectURL(
                    request.getContextPath() + "/" + errPage));
        else if (e.getMessage().startsWith(
                "could not create document container"))
            // This specific message is so misleading, so we'll change it
            // to what it really means.
            throw new ServletException("XForms xml parsing error. " +
                    "Please check the XForm for xml errors.", e);
        else
            throw new ServletException(e);
    }

    static class Adapter extends AbstractChibaAdapter
            implements InteractionHandler {

        private HashMap beanCtx = null;
        private UIGenerator generator = null;

        private String xslPath = null;
        private String baseURI = null;
        private String formURI = null;
        private String actionURL = null;
        private String CSSFile = null;
        private String stylesheet = null;
        private String dataPrefix;
        private String selectorPrefix;
        private String triggerPrefix;
        private String removeUploadPrefix;
        private String uploadRoot;

        /**
         * Creates a new Adapter object.
         */
        public Adapter() {
            chibaBean = createProcessor();
            beanCtx = new HashMap();
            chibaBean.setContext(beanCtx);

        }

        /**
         * Initializes the per-session adaptor.
         *
         * @throws XFormsException An error occurred
         */
        public void init() throws XFormsException {

            if (formURI != null) {
                try {
                    // A local file can be /... or c:\... but it is
                    // certainly under the Faban directory.
                    if (formURI.startsWith(
                            com.sun.faban.harness.common.Config.FABAN_HOME)) {
                        FileInputStream stream = new FileInputStream(formURI);
                        setXForms(stream);
                    } else {
                        setXForms(new URI(formURI));
                    }
                } catch (URISyntaxException e) {
                    throw new XFormsException("URI not well-formed",e);
                } catch (FileNotFoundException e) {
                    throw new XFormsException("File " + formURI +
                            " not found.", e);
                }
                chibaBean.setBaseURI(baseURI);
            }

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(toString());
                logger.finer("Form URI: " + formURI);
                logger.finer("CSS-File: " + CSSFile);
                logger.finer("XSLT stylesheet: " + stylesheet);
                logger.finer("action URL: " + actionURL);
            }

            chibaBean.init();

            StylesheetLoader stylesLoader = new StylesheetLoader(xslPath);

            if (stylesheet != null)
                stylesLoader.setStylesheetFile(stylesheet);

            if (generator == null)
                generator = new XSLTGenerator(stylesLoader);

            generator.setParameter("action-url", actionURL);
            generator.setParameter("debug-enabled", String.valueOf(
                    logger.isLoggable(Level.FINE)));
            String selectorPrefix1 = Config.getInstance().getProperty(
                    "chiba.web.selectorPrefix", "s_");
            generator.setParameter("selector-prefix", selectorPrefix1);
            String removeUploadPrefix1 = Config.getInstance().getProperty(
                    "chiba.web.removeUploadPrefix", "ru_");
            generator.setParameter("remove-upload-prefix", removeUploadPrefix1);
            if (CSSFile != null) {
                generator.setParameter("css-file", CSSFile);
            }
        }

        /**
         * Shuts down the xforms processor.
         *
         * @throws org.chiba.xml.xforms.exception.XFormsException
         *
         */
        public void shutdown() throws XFormsException {
            if (chibaBean != null)
                chibaBean.shutdown();
        }

        /**
         * Handles the request.
         *
         * @throws XFormsException
         */
        public void execute() throws XFormsException {
            HttpServletRequest request = (HttpServletRequest) beanCtx.get(
                    "chiba.web.request");

            String contextRoot = request.getSession().getServletContext().
                                 getRealPath("");
            if (contextRoot == null) {
                contextRoot = request.getSession().getServletContext().
                              getRealPath(".");
            }

            String uploadDir = (String) beanCtx.get("chiba.web.uploadDir");
            uploadRoot = new File(contextRoot, uploadDir).getAbsolutePath();

            String trigger = null;

            // Check that we have a file upload request
            boolean isMultipart = FileUpload.isMultipartContent(request);
            if (logger.isLoggable(Level.FINE)) {
                logger.finer("request isMultipart: " + isMultipart);
                logger.finer("base URI: " + chibaBean.getBaseURI());
                logger.finer("user agent: " + request.getHeader("User-Agent"));
            }

            if (isMultipart) {
                trigger = processMultiPartRequest(request, trigger);
            } else {
                trigger = processUrlencodedRequest(request, trigger);
            }

            // finally activate trigger if any
            if (trigger != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.finer("trigger '" + trigger + "'");
                }

                chibaBean.dispatch(trigger, EventFactory.DOM_ACTIVATE);
            }
        }

        void buildUI() throws XFormsException {
            Config cfg = Config.getInstance();
            String dataPrefix = cfg.getProperty("chiba.web.dataPrefix");
            String triggerPrefix = cfg.getProperty("chiba.web.triggerPrefix");
            String userAgent = (String) beanCtx.get("chiba.useragent");

            generator.setParameter("data-prefix", dataPrefix);
            generator.setParameter("trigger-prefix", triggerPrefix);
            generator.setParameter("user-agent", userAgent);
            if (CSSFile != null) {
                generator.setParameter("css-file", CSSFile);
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(">>> setting UI generator params...");
                logger.fine("data-prefix=" + dataPrefix);
                logger.fine("trigger-prefix=" + triggerPrefix);
                logger.fine("user-agent=" + userAgent);
                if (CSSFile != null) {
                    logger.fine("css-file=" + CSSFile);
                }
                logger.fine(">>> setting UI generator params...end");
            }

            generator.setInputNode(chibaBean.getXMLContainer());
            System.setProperty(XSLT_TRANSFORMER_FACTORY, xalanImpl);
            try {
                generator.generate();
            } finally {
                System.setProperty(XSLT_TRANSFORMER_FACTORY, internalXalanImpl);
            }
        }


        private String processMultiPartRequest(HttpServletRequest request,
                                               String trigger)
                throws XFormsException {
            DiskFileUpload upload = new DiskFileUpload();

            String encoding = request.getCharacterEncoding();
            if (encoding == null) {
                encoding = "ISO-8859-1";
            }

            upload.setRepositoryPath(uploadRoot);

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("root dir for uploads: " + uploadRoot);
            }

            List items;
            try {
                items = upload.parseRequest(request);
            } catch (FileUploadException e) {
                throw new XFormsException(e);
            }

            Map formFields = new HashMap();
            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
                String itemName = item.getName();
                String fieldName = item.getFieldName();
                String id = fieldName.substring(Config.getInstance().
                            getProperty("chiba.web.dataPrefix").length());

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Multipart item name is: " + itemName
                            + " and fieldname is: " + fieldName
                            + " and id is: " + id);
                    logger.fine("Is formfield: " + item.isFormField());
                    logger.fine("Content: " + item.getString());
                }

                if (item.isFormField()) {

                    if (removeUploadPrefix == null) {
                        try {
                            removeUploadPrefix = Config.getInstance().
                                    getProperty("chiba.web.removeUploadPrefix",
                                                "ru_");
                        } catch (Exception e) {
                            removeUploadPrefix = "ru_";
                        }
                    }

                    if (fieldName.startsWith(removeUploadPrefix)) {
                        id = fieldName.substring(removeUploadPrefix.length());
                        chibaBean.updateControlValue(id, "", "", null);
                        continue;
                    }

                    // It's a field name, not a file. Do the same
                    // as processUrlencodedRequest
                    String values[] = (String[]) formFields.get(fieldName);
                    String formFieldValue = null;
                    try {
                        formFieldValue = item.getString(encoding);
                    } catch (UnsupportedEncodingException e1) {
                        throw new XFormsException(e1.getMessage(), e1);
                    }

                    if (values == null) {
                        formFields.put(fieldName, new String[]{formFieldValue});
                    } else {
                        String[] tmp = new String[values.length + 1];
                        System.arraycopy(values, 0, tmp, 0, values.length);
                        tmp[values.length] = formFieldValue;
                        formFields.put(fieldName, tmp);
                    }
                } else {
                    String uniqueFilename = new File("file" + Integer.
                            toHexString((int) (Math.random() * 10000)),
                            new File(itemName).getName()).getPath();

                    File savedFile = new File(uploadRoot, uniqueFilename);

                    byte[] data = null;

                    if (item.getSize() > 0)
                        if (chibaBean.storesExternalData(id)) {

                            try {
                                savedFile.getParentFile().mkdir();
                                item.write(savedFile);
                            } catch (Exception e) {
                                throw new XFormsException(e);
                            }
                            try {
                                data = savedFile.toURI().toString().
                                       getBytes(encoding);
                            } catch (UnsupportedEncodingException e) {
                                throw new XFormsException(e);
                            }

                        } else {
                            data = item.get();
                        }

                    chibaBean.updateControlValue(id, item.getContentType(),
                            itemName, data);
                }

                // handle regular fields
                if (formFields.size() > 0)
                    for (Iterator entries = formFields.entrySet().iterator();
                            entries.hasNext();) {
                        Map.Entry entry = (Map.Entry) entries.next();
                        fieldName = (String) entry.getKey();
                        String[] values = (String[]) entry.getValue();
                        handleData(fieldName, values);
                        handleSelector(fieldName, values[0]);
                        trigger = handleTrigger(trigger, fieldName);
                    }
            }
            return trigger;
        }

        private String processUrlencodedRequest(HttpServletRequest request,
                                                String trigger)
                throws XFormsException {

            Map paramMap = request.getParameterMap();
            for (Iterator entries = paramMap.entrySet().iterator();
                    entries.hasNext();) {
                Map.Entry entry = (Map.Entry) entries.next();
                String paramName = (String) entry.getKey();
                String[] values = (String[]) entry.getValue();

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(this + " parameter-name: " + paramName);
                    for (int i = 0; i < values.length; i++) {
                        logger.fine(this + " value: " + values[i]);
                    }
                }
                handleData(paramName, values);
                handleSelector(paramName, values[0]);
                trigger = handleTrigger(trigger, paramName);
            }
            return trigger;
        }

        private void handleData(String name, String[] values)
                throws XFormsException {
            if (name.startsWith(getDataPrefix())) {
                String id = name.substring(getDataPrefix().length());

                // assemble new control value
                String newValue;

                if (values.length > 1) {
                    StringBuffer buffer = new StringBuffer(values[0]);

                    for (int i = 1; i < values.length; i++) {
                        buffer.append(" ").append(values[i]);
                    }

                    newValue = trim( buffer.toString() );
                } else {
                    newValue = trim( values[0] );
                }

                chibaBean.updateControlValue(id, newValue);
            }
        }

        private String trim(String value) {
            if (value != null && value.length() > 0) {
                value = value.replaceAll("\r\n", "\r");
                value = value.trim();
            }
            return value;
        }

        private void handleSelector(String name, String value)
                throws XFormsException {
            if (name.startsWith(getSelectorPrefix())) {
                int separator = value.lastIndexOf(':');

                String id = value.substring(0, separator);
                int index = Integer.valueOf(value.substring(separator + 1)).
                            intValue();

                Repeat repeat = (Repeat) chibaBean.lookup(id);
                repeat.setIndex(index);
            }
        }

        private String handleTrigger(String trigger, String name) {
            if ((trigger == null) && name.startsWith(getTriggerPrefix())) {
                String parameter = name;
                int x = parameter.lastIndexOf(".x");
                int y = parameter.lastIndexOf(".y");

                if (x > -1) {
                    parameter = parameter.substring(0, x);
                }

                if (y > -1) {
                    parameter = parameter.substring(0, y);
                }

                // keep trigger id
                trigger = name.substring(getTriggerPrefix().length());
            }
            return trigger;
        }

        private final String getTriggerPrefix() {
            if (triggerPrefix == null) {
                try {
                    triggerPrefix = Config.getInstance().getProperty(
                                    "chiba.web.triggerPrefix", "t_");
                } catch (Exception e) {
                    triggerPrefix = "t_";
                }
            }

            return triggerPrefix;
        }

        private final String getDataPrefix() {
            if (dataPrefix == null) {
                try {
                    dataPrefix = Config.getInstance().getProperty(
                                 "chiba.web.dataPrefix", "d_");
                } catch (Exception e) {
                    dataPrefix = "d_";
                }
            }

            return dataPrefix;
        }

        private final String getSelectorPrefix() {
            if (selectorPrefix == null) {
                try {
                    selectorPrefix = Config.getInstance().getProperty(
                                     "chiba.web.selectorPrefix",
                                    "s_");
                } catch (Exception e) {
                    selectorPrefix = "s_";
                }
            }

            return selectorPrefix;
        }
    }
}
