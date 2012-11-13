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
package com.sun.faban.driver.transport.hc3;

import com.sun.faban.driver.HttpTransport;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The ApacheHC3Transport provides initialization services and utility methods
 * for using the HTTP and HTTPS protocols through the Apache HttpClient 3.x
 * Libraries. The convention for the method names in this class are as
 * follows:<ul>
 * <li>Methods starting with "read..." read the data from the network.
 *     They however DO NOT keep a copy of the data. The internal read buffer
 *     is recycled immediately. These methods are useful for reading data for
 *     which the content is irrelevant to the benchmark driver implementation.
 *     For example, tests where the server send large chunks of binary data,
 *     i.e. images do not care about the content. Using these methods will save
 *     both memory and cpu cycles on the driver side.</li>
 * <li>Methods starting with "fetch..." actually read and keep a copy of the
 *     data for further analysis. These methods only work properly with text
 *     data as the result is saved to a java.lang.StringBuilder.</li>
 * <li>Methods starting with "match.." internally fetch the data just like the
 *     "fetch..." methods. In addition, they perform analysis on the data
 *     received.</li>
 * </ul>
 * Currenly, the ApacheHC3Transport class does not provide a way to keep binary
 * data for further analysis. This function can and will be added if there is a
 * use case for keeping such binary data.
 *
 * @author Akara Sucharitakul
 */
public class ApacheHC3Transport extends HttpTransport {

    static {
        final Protocol http =
                new Protocol("http", new ProtocolTimedSocketFactory(), 80);
        Protocol.registerProtocol("http", http);

        final Protocol https = new Protocol("https", (ProtocolSocketFactory)
                TimedSSLFactories.getFactory().getInstance(), 443);
        Protocol.registerProtocol("https", https);

        CookiePolicy.registerCookieSpec(CookiePolicy.DEFAULT,
                                        FabanCookieSpec.class);
    }

    private HttpClient hc = new HttpClient();


    /** The main appendable buffer for the total results. */
    private StringBuilder charBuffer;

    /** The response code of the last response. */
    private int responseCode;

    /** The response headers of the last response. */
    private Map<String, List<String>> responseHeader;

    /** The content size of the last read page. */
    private int contentSize;

    /** The byte buffer used for the reads in read* methods. */
    private byte[] byteReadBuffer = new byte[BUFFER_SIZE];

    /** The char used for the reads in fetch* methods. */
    private char[] charReadBuffer = new char[BUFFER_SIZE];

    /** A cache for already-compiled regex patterns. */
    private HashMap<String, Pattern> patternCache;

    private boolean followRedirects = false;

    private HashSet<String> texttypes;

    /**
     * Constructs a new ApacheHC3Transport object.
     */
    public ApacheHC3Transport() {
        hc.getHttpConnectionManager().getParams().setConnectionTimeout(30000);

    	texttypes = new HashSet<String>();
        texttypes.add("application/json");
    }

    /**
     * Sets whether the client should retry or not.
     * @param retry Whether to retry failed attempts
     */
    public void setRetry(boolean retry) {
        if (retry)
            hc.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                    new DefaultHttpMethodRetryHandler(1, true));
        else
            hc.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                    new DefaultHttpMethodRetryHandler(0, false));
    }

    /**
     * Obtains the HttpClient instance backing this transport.
     * @return The backing instance
     */
    public HttpClient getHttpClient() {
        return hc;
    }

    /**
     * Sets the http connections managed by this transport to follow or
     * not follow HTTP redirects.
     * @param follow True if HTTP redirects should be automatically followed,
     *        false otherwise
     */
    public void setFollowRedirects(boolean follow) {
        followRedirects = follow;
    }

    /**
     * Add a MIME type to the list of text types. If the response is of this
     * type the fetchULR() methods will return the response data.
     *
     * @param texttype The content type of a HTTP response that contains text.
     */
    public void addTextType(String texttype) {
    	texttypes.add(texttype);
    }

    /**
     * Checks whether the connections managed by this transport follows
     * redirects or not.
     * @return True if redirects are followed, false otherwise
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * Initializes or re-initializes the buffer.
     * @param size The size of the buffer
     */
    private void reInitBuffer(int size) {
        if (charBuffer == null)
            charBuffer = new StringBuilder(size);
        else
            charBuffer.setLength(0);
    }

    /**
     * Obtains the reference of the current response buffer.
     * @return The response buffer
     */
    public StringBuilder getResponseBuffer() {
        return charBuffer;
    }

    /**
     * Reads data from the URL and discards it, keeping just the size of the
     * total read. This is useful for ensuring receival of binary or text
     * data that do not need further analysis.
     * @param url The URL to read from
     * @param headers The request headers
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(URL url, Map<String, String> headers)
            throws IOException {
        return readURL(url.toString(), headers);
    }

    /**
     * Reads data from the URL and discards it, keeping just the size of the
     * total read. This is useful for ensuring receival of binary or text
     * data that do not need further analysis.
     * @param url The URL to read from
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(URL url) throws IOException {
        return readURL(url.toString(), (Map<String, String>) null);
    }

    /**
     * Reads data from the URL and discards it, keeping just the size of the
     * total read. This is useful for ensuring receival of binary or text
     * data that do not need further analysis.
     * @param url The URL to read from
     * @param headers The request headers
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(String url, Map<String, String> headers)
            throws IOException {
        GetMethod method;
        method = new GetMethod(url);
        method.setFollowRedirects(followRedirects);
        setHeaders(method, headers);
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);
            return readResponse(method);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Reads data from the URL and discards it, keeping just the size of the
     * total read. This is useful for ensuring receival of binary or text
     * data that do not need further analysis.
     * @param url The URL to read from
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(String url) throws IOException {
        return readURL(url, (Map<String, String>) null);
    }

    /**
     * Makes a POST request to the URL. Reads data back and discards the data,
     * keeping just the size of the total read. This is useful for ensuring
     * receival of binary or text data that do not need further analysis.
     *
     * @param url The URL to read from
     * @param postRequest The post request string
     * @param headers The request headers
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(String url, String postRequest,
                       Map<String, String> headers) throws IOException {

        PostMethod method = new PostMethod(url);
        method.setFollowRedirects(followRedirects);
        setHeaders(method, headers);
        setParameters(method, postRequest);
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);
            return readResponse(method);
        } finally {
            method.releaseConnection();
        }
    }

    private void setParameters(PostMethod method, String request) throws UnsupportedEncodingException {
        // Check whether request is XML or JSON
        if (request.startsWith("<?xml") || request.startsWith("{")) {
            Header h = method.getRequestHeader("Content-Type");
            if (h == null) {
                h = method.getRequestHeader("content-type");
            }
            if (h != null) {
                method.setRequestEntity(new StringRequestEntity(request,
                    h.getValue(), method.getRequestCharSet()));
                return;
            }
        }

        // If none of both, just treat it as html.
        int idx = 0;
        if (request == null || request.length() == 0)
            return;
        if (request.charAt(0) == '?')
            ++idx;
        do {
            int endIdx = request.indexOf('&', idx);
            if (endIdx == -1)
                endIdx = request.length();
            int eqIdx = request.indexOf('=', idx);
            if (eqIdx != -1 && eqIdx < endIdx) {
                method.setParameter(request.substring(idx, eqIdx),
                                    request.substring(eqIdx + 1, endIdx));
            } else {
                method.setParameter(request.substring(idx, endIdx), null);
            }
            idx = endIdx + 1;
        } while (idx < request.length());
    }

    private void buildResponseHeaders(HttpMethod method) {
        Header[] respHeaders = method.getResponseHeaders();
        responseHeader =
                new LinkedHashMap<String, List<String>>(respHeaders.length);
        for (Header header : respHeaders) {
            String name = header.getName().toLowerCase();
            List<String> values = responseHeader.get(name);
            if (values == null) {
                values = new ArrayList<String>();
                responseHeader.put(name, values);
            }
            values.add(header.getValue());
        }
    }

    /**
     * Makes a POST request to the URL. Reads data back and discards the data,
     * keeping just the size of the total read. This is useful for ensuring
     * receival of binary or text data that do not need further analysis.
     * Note that the POST request will be URL encoded.
     * @param url The URL to read from
     * @param postRequest The post request string
     * @param headers The request headers
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(URL url, String postRequest, Map<String, String> headers)
            throws IOException {
        return readURL(url.toString(), postRequest, headers);
    }

    /**
     * Makes a POST request to the URL without encoding the data (the
     * header type is application/octet-stream).
     *
     * @param url The URL to read from
     * @param postRequest The binary data to send
     * @param headers The request headers
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(URL url, byte[] postRequest, Map<String, String> headers)
            throws IOException {
        return readURL(url.toString(), new String(postRequest), headers);
    }

    /**
     * Makes a POST request to the URL without encoding the data (the
     * header type is application/octet-stream).
     *
     * @param url The URL to read from
     * @param postRequest The binary data to send
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(String url, byte[] postRequest) throws IOException {
        return readURL(url, new String(postRequest), null);
    }

    /**
     * Sets the request header. If there are multiple values for this header,
     * use a comma-separated list for the values.
     * @param method The HttpMethod
     * @param headers The request headers
     */
    private void setHeaders(HttpMethod method, Map<String, String> headers) {
        if (headers == null) {
            method.setRequestHeader("Accept-Language", "en-us,en;q=0.5");
            return;
        } else if (!headers.containsKey("Accept-Language")) {
            method.setRequestHeader("Accept-Language", "en-us,en;q=0.5");
        }
        for (Map.Entry<String, String> entry : headers.entrySet())
            method.setRequestHeader(entry.getKey(), entry.getValue());
    }

    /**
     * Makes a POST request to the URL. Reads data back and discards the data,
     * keeping just the size of the total read. This is useful for ensuring
     * receival of binary or text data that do not need further analysis.
     *
     * @param url The URL to read from
     * @param postRequest The post request string
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(String url, String postRequest) throws IOException {
        return readURL(url, postRequest, null);
    }

    /**
     * Makes a Multi-part POST request to the URL. Reads data back and discards
     * the data, keeping just the size of the total read. This is useful for
     * ensuring receival of binary or text data that do not need further
     * analysis.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @param headers The request headers
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(String url, List<Part> parts,
                       Map<String, String> headers) throws IOException {

        Part[] partsArray = parts.toArray(new Part[parts.size()]);
        PostMethod method = new PostMethod(url);
        method.setFollowRedirects(followRedirects);
        setHeaders(method, headers);
        method.setRequestEntity(new MultipartRequestEntity(
                partsArray, method.getParams()));
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);
            return readResponse(method);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Makes a Multi-part POST request to the URL. Reads data back and discards
     * the data, keeping just the size of the total read. This is useful for
     * ensuring receival of binary or text data that do not need further
     * analysis.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(URL url, List<Part> parts) throws IOException {
        return readURL(url.toString(), parts, null);
    }

    /**
     * Makes a Multi-part POST request to the URL. Reads data back and discards
     * the data, keeping just the size of the total read. This is useful for
     * ensuring receival of binary or text data that do not need further
     * analysis.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(String url, List<Part> parts) throws IOException {
        return readURL(url, parts, null);
    }

    /**
     * Makes a Multi-part POST request to the URL. Reads data back and discards
     * the data, keeping just the size of the total read. This is useful for
     * ensuring receival of binary or text data that do not need further
     * analysis.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @param headers The request headers
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    public int readURL(URL url, List<Part> parts,
                       Map<String, String> headers) throws IOException {
        return readURL(url.toString(), parts, headers);
    }

    /**
     * Reads data from the URL and returns the data read. Note that this
     * method only works with text data as it does the byte-to-char
     * conversion. This method will return null for responses with binary
     * MIME types. The addTextType(String) method is used to register
     * additional MIME types as text types.
     *
     * @param url The URL to read from
     * @param headers The request headers
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(URL url, Map<String, String> headers)
            throws IOException {
        return fetchURL(url.toString(), headers);
    }

    /**
     * Reads data from the URL and returns the data read. Note that this
     * method only works with text data as it does the byte-to-char
     * conversion. This method will return null for responses with binary
     * MIME types. The addTextType(String) method is used to register
     * additional MIME types as text types. Use getContentSize()
     * to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(URL url)
            throws IOException {
        return fetchURL(url.toString(), (Map<String, String>) null);
    }

    /**
     * Reads data from the URL and returns the data read. Note that this
     * method only works with text data as it does the byte-to-char
     * conversion. This method will return null for responses with binary
     * MIME types. The addTextType(String) method is used to register
     * additional MIME types as text types. Use getContentSize()
     * to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param headers The request headers
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(String url, Map<String, String> headers)
            throws IOException {
        GetMethod method = new GetMethod(url);
        method.setFollowRedirects(followRedirects);
        setHeaders(method, headers);
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);
            return fetchResponse(method);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Reads data from the URL and returns the data read. Note that this
     * method only works with text data as it does the byte-to-char
     * conversion. This method will return null for responses with binary
     * MIME types. The addTextType(String) method is used to register
     * additional MIME types as text types. Use getContentSize()
     * to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(String url) throws IOException {
        return fetchURL(url, (Map<String, String>) null);
    }

    /**
     * Retrieve large response from the URL and returns the data read. Use this
     * method for any arbitrary return data type e.g. file downloads. This method will only
     * download upto 1 MB to conserve memory. However, it will read all of the response and
     * update contentSize appropriately.
     *
     * @param url The URL to read from
     * @return The byte array containing the resulting data
     * @throws java.io.IOException
     * @see #getContentSize()
     */
    public byte[] downloadURL(String url) throws IOException {
        GetMethod method = new GetMethod(url);
        method.setFollowRedirects(followRedirects);
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);

            byte buffer[] = new byte[1048576];
            InputStream is = method.getResponseBodyAsStream();
            if (is != null) {
                int totalLength = 0, bufferLen = 0;
                int length = is.read(buffer);
                while (length != -1) {
                    totalLength += length;
                    if (totalLength >= buffer.length) {
                        // we read the remaining data but discard it
                        length = is.read(byteReadBuffer);
                    } else {
                        bufferLen += length;
                        length = is.read(buffer, bufferLen, buffer.length - bufferLen);
                    }
                }
                is.close();
                contentSize = totalLength;
                return(Arrays.copyOf(buffer, bufferLen));
            } else
                return null;
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Makes a POST request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize()
     * to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param postRequest The post request string
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(String url, String postRequest)
            throws IOException {
        return fetchURL(url, postRequest, (Map<String, String>) null);
    }

    /**
     * Makes a POST request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize()
     * to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param postRequest The post request string
     * @param headers The request headers
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(String url, String postRequest,
                                  Map<String, String> headers)
            throws IOException {
        PostMethod method = new PostMethod(url);
        method.setFollowRedirects(followRedirects);
        setHeaders(method, headers);
        setParameters(method, postRequest);
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);
            return fetchResponse(method);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Makes a POST request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize()
     * to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param postRequest The post request string
     * @param headers The request headers
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(URL url, String postRequest,
                                  Map<String, String> headers)
            throws IOException {
        return fetchURL(url.toString(), postRequest, headers);
    }

    /**
     * Makes a POST request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize()
     * to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param postRequest The post request string
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(URL url, String postRequest)
            throws IOException {
        return fetchURL(url.toString(), postRequest,
                (Map<String, String>) null);
    }

    /**
     * Makes a Multi-part POST request to the URL. Reads data back and
     * returns the data read. Note that this method only works with text
     * data as it does the byte-to-char conversion. This method will return
     * null for responses with binary MIME types. The addTextType(String)
     * method is used to register additional MIME types as text types.
     * Use getContentSize() to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @param headers The request headers
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder fetchURL(String url, List<Part> parts,
                       Map<String, String> headers) throws IOException {

        Part[] partsArray = parts.toArray(new Part[parts.size()]);
        PostMethod method = new PostMethod(url);
        method.setFollowRedirects(followRedirects);
        setHeaders(method, headers);
        method.setRequestEntity(new MultipartRequestEntity(
                partsArray, method.getParams()));
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);
            return fetchResponse(method);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Makes a Multi-part POST request to the URL. Reads data back and
     * returns the data read. Note that this method only works with text
     * data as it does the byte-to-char conversion. This method will return
     *  null for responses with binary MIME types. The addTextType(String)
     * method is used to register additional MIME types as text types.
     * Use getContentSize() to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder fetchURL(URL url, List<Part> parts) throws IOException {
        return fetchURL(url.toString(), parts, null);
    }

    /**
     * Makes a Multi-part POST request to the URL. Reads data back and
     * returns the data read. Note that this method only works with text
     * data as it does the byte-to-char conversion. This method will return
     * null for responses with binary MIME types. The addTextType(String)
     * method is used to register additional MIME types as text types.
     * Use getContentSize() to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder fetchURL(String url, List<Part> parts) throws IOException {
        return fetchURL(url, parts, null);
    }

    /**
     * Makes a Multi-part POST request to the URL. Reads data back and
     * returns the data read. Note that this method only works with text
     * data as it does the byte-to-char conversion. This method will return
     * null for responses with binary MIME types. The addTextType(String)
     * method is used to register additional MIME types as text types. Use getContentSize()
     * to obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @param headers The request headers
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder fetchURL(URL url, List<Part> parts,
                       Map<String, String> headers) throws IOException {
        return fetchURL(url.toString(), parts, headers);
    }

    /*
     * Method not implemented. Makes a GET request. Fetches the main page
     * and all other image or resource pages based on the given URLs.
     *
     * @param page The page URL
     * @param images The image or other resource URLs to fetch with page
     * @return The buffer of the main page
     * @throws IOException If an I/O error occurred
     *
	public StringBuilder fetchPage(URL page, URL[] images) throws IOException {
        // TODO: implement method
        return null;
    }
    */

    /*
     * Fetch page and images in the same call. Currently not used.
     *
    public StringBuilder fetchPage(String page, String[] images)
            throws IOException {
        URL[] imgURLs = new URL[images.length];
        for (int i = 0; i < imgURLs.length; i++)
            imgURLs[i] = new URL(images[i]);
        return fetchPage(new URL(page), imgURLs);
    }
    */

    /**
     * Method not implemented. Makes a POST request. Fetches the main page
     * and all other image or resource pages based on the given URLs.
     *
     * @param page The page URL
     * @param images The image or other resource URLs to fetch with page
     * @param postRequest The post string
     * @return The buffer of the main page
     * @throws java.io.IOException If an I/O error occurred
     */
	public StringBuilder fetchURL(URL page, URL[] images, String postRequest)
            throws IOException {
        // TODO: implement method
        return null;
    }

    /**
     * Makes a POST request, fetches the main page and all other image or
     * resource pages.
     *
     * @param page The page URL
     * @param images The image or other resource URLs to fetch with page
     * @param postRequest The post string
     * @return The buffer of the main page
     * @throws java.io.IOException If an I/O error occurred
     */
    public StringBuilder fetchPage(String page, String[] images,
                                  String postRequest) throws IOException {
        URL[] imgURLs = new URL[images.length];
        for (int i = 0; i < imgURLs.length; i++)
            imgURLs[i] = new URL(images[i]);
        return fetchURL(new URL(page), imgURLs, postRequest);
    }

    private StringBuilder fetchResponse(HttpMethod method) throws IOException {
        Header contentTypeHdr = method.getResponseHeader("content-type");
        String contentType = null;
        if (contentTypeHdr != null)
            contentType = contentTypeHdr.getValue();
        String hdr = "charset=";
        int hdrLen = hdr.length();
        String encoding = "ISO-8859-1";
        if (contentType != null) {
            StringTokenizer t = new StringTokenizer(contentType, ";");
            contentType = t.nextToken().trim();
            while (t.hasMoreTokens()) {
                String param = t.nextToken().trim();
                if (param.startsWith(hdr)) {
                    encoding = param.substring(hdrLen);
                    break;
                }
            }
        }
        if (contentType != null && (contentType.startsWith("text/") ||
                                    texttypes.contains(contentType))) {
            InputStream is = method.getResponseBodyAsStream();
            if (is != null) {
                Reader reader = new InputStreamReader(is, encoding);

                // We have to close the input stream in order to return it to
                // the cache, so we get it for all content, even if we don't
                // use it. It's (I believe) a bug that the content handlers
                // used by getContent() don't close the input stream, but the
                // JDK team has marked those bugs as "will not fix."
                fetchResponseData(reader);
                reader.close();
            } else {
                reInitBuffer(2048); // Ensure we have an empty buffer.
            }
            return charBuffer;
        }
        readResponse(method);
        return null;
    }

    /**
     * Reads the http response from a connection, counts the size of the
     * resulting document, and discards the data. This method recycles its
     * buffer during large reads and therefore has very little weight.
     * @param method The HttpMethod to read from
     * @return The number of bytes read
     * @throws java.io.IOException
     */
    private int readResponse(HttpMethod method) throws IOException {
        int totalLength = 0;
        InputStream in = method.getResponseBodyAsStream();
        if (in != null) {
        int length = in.read(byteReadBuffer);
        while (length != -1) {
            totalLength += length;
            length = in.read(byteReadBuffer);
        }
        in.close();
        contentSize = totalLength;
        }
        return totalLength;
    }

    /**
     * Obtains the size of the last read page or resource. The result is in
     * bytes for non-decoded content and in characters for decoded content.
     * All binary content is not decoded. Text content is decoded only using
     * the fetch or match commands.
     * @return The size, in bytes, of the last page read
     */
    public int getContentSize() {
        return contentSize;
    }

    /**
     * Fetches the data from the stream, converts to char, and returns it as
     * a StringBuilder.
     * @param stream The stream to read from
     * @return The resulting data
     * @throws java.io.IOException
     */
    public StringBuilder fetchResponseData(InputStream stream)
            throws IOException {
        return fetchResponseData(new InputStreamReader(stream));
    }

    /**
     * Fetches the data from the reader and returns it as a StringBuilder.
     * @param reader The reader to read from
     * @return The resulting data
     * @throws java.io.IOException
     */
    public StringBuilder fetchResponseData(Reader reader) throws IOException {
        int totalLength = 0;
        int length = reader.read(charReadBuffer, 0, charReadBuffer.length);
        if (length > 0)
            reInitBuffer(length);
        else
            reInitBuffer(2048);

        while (length != -1) {
            totalLength += length;
            charBuffer.append(charReadBuffer, 0, length);
            length = reader.read(charReadBuffer, 0, charReadBuffer.length);
        }
        contentSize = totalLength;
        return charBuffer;
    }

    /**
     * Maches the regular expression against the data in the current buffer.
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     */
    public boolean matchResponse(String regex) {
        if (patternCache == null)
            patternCache = new HashMap<String, Pattern>();
        Pattern pattern = patternCache.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            patternCache.put(regex, pattern);
        }
        Matcher matcher = pattern.matcher(charBuffer);
        return matcher.find();
    }

    /**
     * Matches the regular expression against the data read from the stream.
     * @param stream The source of the data
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchResponse(InputStream stream, String regex)
            throws IOException {
        fetchResponseData(stream);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the data read from the reader.
     * @param reader The source of the data
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchResponse(Reader reader, String regex)
            throws IOException {
        fetchResponseData(reader);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * URL.
     * @param url The source of the data
     * @param regex THe regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(String url, String regex) throws IOException {
        fetchURL(url);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * URL.
     * @param url The source of the data
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(String url, String regex, Map<String, String> headers)
            throws IOException {
        fetchURL(url, headers);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * URL.
     * @param url The source of the data
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(URL url, String regex) throws IOException {
        fetchURL(url);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * URL.
     * @param url The source of the data
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(URL url, String regex, Map<String, String> headers)
            throws IOException {
        fetchURL(url, headers);
        return matchResponse(regex);
    }

    /**
     * Mathces the regular expression against the response fetched from the
     * post request made to the URL.
     * @param url The source of the data
     * @param postRequest The post request string
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(URL url, String postRequest, String regex)
            throws IOException {
        fetchURL(url, postRequest);
        return matchResponse(regex);
    }

    /**
     * Mathces the regular expression against the response fetched from the
     * post request made to the URL.
     * @param url The source of the data
     * @param postRequest The post request string
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(URL url, String postRequest, String regex,
                            Map<String, String> headers) throws IOException {
        fetchURL(url, postRequest, headers);
        return matchResponse(regex);
    }

    /**
     * Mathces the regular expression against the response fetched from the
     * post request made to the URL.
     * @param url The source of the data
     * @param postRequest The post request string
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(String url, String postRequest, String regex)
            throws IOException {
        fetchURL(url, postRequest);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * post request made to the URL.
     * @param url The source of the data
     * @param postRequest The post request string
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(String url, String postRequest, String regex,
                            Map<String, String> headers) throws IOException {
        fetchURL(url, postRequest, headers);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * multi-part post request made to the URL.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(String url, List<Part> parts, String regex,
                       Map<String, String> headers) throws IOException {

        fetchURL(url, parts, headers);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * multi-part post request made to the URL.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(URL url, List<Part> parts, String regex)
            throws IOException {
        fetchURL(url.toString(), parts, null);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * multi-part post request made to the URL.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(String url, List<Part> parts, String regex)
            throws IOException {
        fetchURL(url, parts, null);
        return matchResponse(regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * multi-part post request made to the URL.
     *
     * @param url The URL to read from
     * @param parts The parts list
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws java.io.IOException
     */
    public boolean matchURL(URL url, List<Part> parts, String regex,
                       Map<String, String> headers) throws IOException {
        fetchURL(url.toString(), parts, headers);
        return matchResponse(regex);
    }

    /**
     * Makes a PUT request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param buffer containing the PUT data
     * @param contentType the content type, or null
     * @param headers The request headers, or null
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder putURL(String url, byte[] buffer, String contentType,
                                Map<String, String> headers)
            throws IOException {
        PutMethod method = new PutMethod(url);
        method.setFollowRedirects(followRedirects);
        setHeaders(method, headers);
        method.setRequestEntity(
                new ByteArrayRequestEntity(buffer, contentType));
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);
            return fetchResponse(method);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Makes a PUT request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param buffer containing the PUT data
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder putURL(String url, byte[] buffer) throws IOException {
        return putURL(url, buffer, null, null);
    }

    /**
     * Makes a PUT request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param buffer containing the PUT data
     * @param contentType the content type, or null
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder putURL(String url, byte[] buffer, String contentType)
            throws IOException {
        return putURL(url, buffer, contentType, null);
    }

    /**
     * Makes a PUT request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param buffer containing the PUT data
     * @param headers The request headers, or null
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder putURL(String url, byte[] buffer,
                                Map<String, String> headers)
            throws IOException {
        return putURL(url, buffer, null, headers);
    }

    /**
     * Makes a PUT request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param buffer containing the PUT data
     * @param contentType the content type, or null
     * @param headers The request headers, or null
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder putURL(URL url, byte[] buffer, String contentType,
                                Map<String, String> headers)
            throws IOException {
        return putURL(url.toString(), buffer, contentType, headers);
    }

    /**
     * Makes a PUT request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param buffer containing the PUT data
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder putURL(URL url, byte[] buffer) throws IOException {
        return putURL(url.toString(), buffer, null, null);
    }

    /**
     * Makes a PUT request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param buffer containing the PUT data
     * @param contentType the content type, or null
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder putURL(URL url, byte[] buffer, String contentType)
            throws IOException {
        return putURL(url.toString(), buffer, contentType, null);
    }

    /**
     * Makes a PUT request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param buffer containing the PUT data
     * @param headers The request headers, or null
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder putURL(URL url, byte[] buffer,
                                Map<String, String> headers)
            throws IOException {
        return putURL(url.toString(), buffer, null, headers);
    }

    /**
     * Makes a DELETE request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param headers The request headers, or null
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder deleteURL(String url, Map<String, String> headers)
            throws IOException {
        DeleteMethod method = new DeleteMethod(url);
        method.setFollowRedirects(followRedirects);
        setHeaders(method, headers);
        try {
            responseCode = hc.executeMethod(method);
            buildResponseHeaders(method);
            return fetchResponse(method);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Makes a DELETE request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder deleteURL(String url) throws IOException {
        return deleteURL(url, null);
    }

    /**
     * Makes a DELETE request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @param headers The request headers, or null
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder deleteURL(URL url, Map<String, String> headers)
            throws IOException {
        return deleteURL(url.toString(), headers);
    }

    /**
     * Makes a DELETE request to the URL. Reads data back and returns the data
     * read. Note that this method only works with text data as it does the
     * byte-to-char conversion. This method will return null for responses
     * with binary MIME types. The addTextType(String) method is used to
     * register additional MIME types as text types. Use getContentSize() to
     *  obtain the bytes of binary data read.
     *
     * @param url The URL to read from
     * @return The StringBuilder buffer containing the resulting document
     * @throws java.io.IOException
     */
    public StringBuilder deleteURL(URL url) throws IOException {
        return deleteURL(url.toString());
    }

    /**
     * Obtains the list of cookie values by the name of the cookies.
     * @param name The cookie name
     * @return An array of non-duplicating cookie values.
     */
    public String[] getCookieValuesByName(String name) {
        LinkedHashSet<String> valueSet = new LinkedHashSet<String>();
        Cookie[] cookies = hc.getState().getCookies();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                valueSet.add(cookie.getValue());
            }
        }
        String[] values = new String[valueSet.size()];
        return valueSet.toArray(values);
    }

    /**
     * Returns all the cookies
     * @return array of Cookie objects
     */
    public Cookie[] getCookies() {
         return hc.getState().getCookies();
    }

    /**
     * Obtains the header fields of the last request's response.
     * @param name The response header field of interest
     * @return An array of response header values
     */
    public String[] getResponseHeader(String name) {
        List<String> values = responseHeader.get(name.toLowerCase());
        String[] v = null;
        if (values != null)
            v = values.toArray(new String[values.size()]);
        return v;
    }

    /**
     * Utility class to get responseHeaders as a string.  The formatting is
     * not localized
     *
     * @return responseHeaders
     */
    public String dumpResponseHeaders() {
        StringBuilder s = new StringBuilder();
        for (Iterator<Map.Entry<String, List<String>>> iter =
                responseHeader.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, List<String>> entry = iter.next();
            String name = entry.getKey();
            List<String> values = entry.getValue();
            for (Iterator<String> iter2 = values.iterator(); iter2.hasNext();) {
                if (name != null) {
                    s.append(name);
                    s.append(": ");
                }
                s.append(iter2.next());
                s.append('\n');
            }
        }
        return s.toString();
    }

    /**
     * Obtains the response code of the previous request.
     * @return responseCode The response code
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Close all connections currently not in use. If the only way of using
     * the Apache HttpClient is through this transport, connections will always
     * be released after a request. The close will close all connections in
     * this case.
     */
    public void closeConnections() {
        hc.getHttpConnectionManager().closeIdleConnections(0);
    }
}
