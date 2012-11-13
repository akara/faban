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
 * Copyright 2005-2010 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver;

import org.apache.commons.httpclient.Cookie;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The HttpTransport provides initialization services and utility methods for
 * using the HTTP and HTTPS protocols. The convention for the method names in
 * this class are as follows:<ul>
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
 * Currenly, the HttpTransport class does not provide a way to keep binary data
 * for further analysis. This function can and will be added if there is a use
 * case for keeping such binary data.
 *
 * @author Akara Sucharitakul
 */
public class HttpTransport {

    static String provider =
            "com.sun.faban.driver.transport.sunhttp.SunHttpTransport";

    private static int bufferSize = 8192;

    // Check whether the buffer size is overridden by the system property.
    static {
        String bufferSizeString = System.getProperty("faban.socket.buffer.size");
        if (bufferSizeString != null) {
            int multiplier = 1;
            if (bufferSizeString.endsWith("k") ||
                    bufferSizeString.endsWith("K")) {
                bufferSizeString = bufferSizeString.substring(0,
                        bufferSizeString.length() - 1);
                multiplier = 1024;
            }
            try {
                bufferSize = Integer.parseInt(bufferSizeString) * multiplier;
                Logger.getLogger(HttpTransport.class.getName()).
                        log(Level.INFO, "HTTP buffer size set to " +
                        bufferSize);

            } catch (NumberFormatException e) {
                Logger.getLogger(HttpTransport.class.getName()).
                        log(Level.WARNING, "faban.http.buffer.size " +
                        "property format must be 999 or 999k. " +
                        "Leaving at default (8k).");
            }
        }
    }

    public static final int BUFFER_SIZE = bufferSize;

    HttpTransport delegate = null;

    /**
     * Sets the provider class. NewInstance() will instantiate the
     * given provider.
     * @param newProvider The fully qualified name of the provider class
     */
    public static void setProvider(String newProvider) {
        provider = newProvider;
    }

    /**
     * Creates an instance of HttpTransport with a fresh state. The actual
     * instance created may be a subclass of HttpTransport fitting to the
     * desired transport mechanism. You can select the transport provider
     * by calling setProvider() before calling newInstance().
     * @return A new instance of HttpTransport or a subclass thereof
     * @see #setProvider(java.lang.String)
     */
    public static HttpTransport newInstance() {
        try {
            return (HttpTransport) Class.forName(provider).
                    asSubclass(HttpTransport.class).newInstance();
        } catch (ClassNotFoundException e) {
            throw new FatalException("Cannot find transport class.", e);
        } catch (InstantiationException e) {
            throw new FatalException("Cannot instantiate transport.", e);
        } catch (IllegalAccessException e) {
            throw new FatalException("Cannot access transport constructor.", e);
        }
    }

    /**
     * Constructs a new HttpTransport object.
     * @deprecated Replaced by the newInstance() method
     */
    public HttpTransport() {
        // Older code that accesses the object directly will
        // access the actual transport through a delegate.
        if (getClass().equals(HttpTransport.class))
            delegate = newInstance();
    }

    /**
     * Sets the http connections managed by this transport to follow or
     * not follow HTTP redirects.
     * @param follow True if HTTP redirects should be automatically followed,
     *        false otherwise
     */
    public void setFollowRedirects(boolean follow) {
        delegate.setFollowRedirects(follow);
    }
    
    /**
     * Add a MIME type to the list of text types. If the response is of this
     * type the fetchULR() methods will return the response data.
     * 
     * @param texttype The content type of a HTTP response that contains text.
     */
    public void addTextType(String texttype) {
    	delegate.addTextType(texttype);
    }

    /**
     * Checks whether the connections managed by this transport follows
     * redirects or not.
     * @return True if redirects are followed, false otherwise
     */
    public boolean isFollowRedirects() {
        return delegate.isFollowRedirects();
    }

    /**
     * Obtains the reference of the current response buffer.
     * @return The response buffer
     */
    public StringBuilder getResponseBuffer() {
        return delegate.getResponseBuffer();
    }

    /**
     * Reads data from the URL and discards it, keeping just the size of the
     * total read. This is useful for ensuring receival of binary or text
     * data that do not need further analysis.
     * @param url The URL to read from
     * @param headers The request headers
     * @return The number of bytes read
     * @throws IOException
     */
    public int readURL(URL url, Map<String, String> headers)
            throws IOException {
        return delegate.readURL(url, headers);
    }

    /**
     * Reads data from the URL and discards it, keeping just the size of the
     * total read. This is useful for ensuring receival of binary or text
     * data that do not need further analysis.
     * @param url The URL to read from
     * @return The number of bytes read
     * @throws IOException
     */
    public int readURL(URL url) throws IOException {
        return delegate.readURL(url);
    }

    /**
     * Reads data from the URL and discards it, keeping just the size of the
     * total read. This is useful for ensuring receival of binary or text
     * data that do not need further analysis.
     * @param url The URL to read from
     * @param headers The request headers
     * @return The number of bytes read
     * @throws IOException
     */
    public int readURL(String url, Map<String, String> headers)
            throws IOException {
        return delegate.readURL(url, headers);
    }

    /**
     * Reads data from the URL and discards it, keeping just the size of the
     * total read. This is useful for ensuring receival of binary or text
     * data that do not need further analysis.
     * @param url The URL to read from
     * @return The number of bytes read
     * @throws IOException
     */
    public int readURL(String url) throws IOException {
        return delegate.readURL(url);
    }

    /**
     * Makes a POST request to the URL. Reads data back and discards the data,
     * keeping just the size of the total read. This is useful for ensuring
     * receival of binary or text data that do not need further analysis.
     * @param url The URL to read from
     * @param postRequest The post request string
     * @return The number of bytes read
     * @throws IOException
     */
    public int readURL(URL url, String postRequest) throws IOException {
        return delegate.readURL(url, postRequest);
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
     * @throws IOException
     */
    public int readURL(URL url, String postRequest, Map<String, String> headers)
            throws IOException {
        return delegate.readURL(url, postRequest, headers);
    }

    /**
     * Makes a POST request to the URL without encoding the data (the
     * header type is application/octet-stream).
     *
     * @param url The URL to read from
     * @param postRequest The binary data to send
     * @param headers The request headers
     * @return The number of bytes read
     * @throws IOException
     */
    public int readURL(URL url, byte[] postRequest, Map<String, String> headers)
            throws IOException {
        return delegate.readURL(url, postRequest, headers);
    }

    /**
     * Makes a POST request to the URL without encoding the data (the
     * header type is application/octet-stream).
     *
     * @param url The URL to read from
     * @param postRequest The binary data to send
     * @return The number of bytes read
     * @throws IOException
     */
    public int readURL(String url, byte[] postRequest) throws IOException {
        return delegate.readURL(url, postRequest);
    }

    /**
     * Makes a POST request to the URL. Reads data back and discards the data,
     * keeping just the size of the total read. This is useful for ensuring
     * receival of binary or text data that do not need further analysis.
     *
     * @param url The URL to read from
     * @param postRequest The post request string
     * @return The number of bytes read
     * @throws IOException
     */
    public int readURL(String url, String postRequest) throws IOException {
        return delegate.readURL(url, postRequest);
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
     * @throws IOException
     */
    public int readURL(String url, String postRequest,
                       Map<String, String> headers) throws IOException {
        return delegate.readURL(url, postRequest, headers);
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
     * @throws IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(URL url, Map<String, String> headers)
            throws IOException {
        return delegate.fetchURL(url, headers);
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
     * @throws IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(URL url)
            throws IOException {
        return delegate.fetchURL(url);
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
     * @throws IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(String url, Map<String, String> headers)
            throws IOException {
        return delegate.fetchURL(url, headers);
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
     * @throws IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(String url) throws IOException {
        return delegate.fetchURL(url);
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
        return delegate.downloadURL(url);
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
     * @throws IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(String url, String postRequest)
            throws IOException {
        return delegate.fetchURL(url, postRequest);
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
     * @throws IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(String url, String postRequest,
                                  Map<String, String> headers)
            throws IOException {
        return delegate.fetchURL(url, postRequest, headers);
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
     * @throws IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(URL url, String postRequest,
                                  Map<String, String> headers)
            throws IOException {
        return delegate.fetchURL(url, postRequest, headers);
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
     * @throws IOException
     * @see #addTextType(String)
     * @see #getContentSize()
     */
    public StringBuilder fetchURL(URL url, String postRequest)
            throws IOException {
        return delegate.fetchURL(url, postRequest);
    }

    /**
     * Method not implemented. Makes a POST request. Fetches the main page
     * and all other image or resource pages based on the given URLs.
     *
     * @param page The page URL
     * @param images The image or other resource URLs to fetch with page
     * @param postRequest The post string
     * @return The buffer of the main page
     * @throws IOException If an I/O error occurred
     */
	public StringBuilder fetchURL(URL page, URL[] images, String postRequest)
            throws IOException {
        return delegate.fetchURL(page, images, postRequest);
    }

    /**
     * Makes a POST request, fetches the main page and all other image or
     * resource pages.
     *
     * @param page The page URL
     * @param images The image or other resource URLs to fetch with page
     * @param postRequest The post string
     * @return The buffer of the main page
     * @throws IOException If an I/O error occurred
     */
    public StringBuilder fetchPage(String page, String[] images,
                                  String postRequest) throws IOException {
        return delegate.fetchPage(page, images, postRequest);
    }

    /**
     * Obtains the size of the last read page or resource. The result is in
     * bytes for non-decoded content and in characters for decoded content.
     * All binary content is not decoded. Text content is decoded only using
     * the fetch or match commands.
     * @return The size, in bytes, of the last page read
     */
    public int getContentSize() {
        return delegate.getContentSize();
    }

    /**
     * Fetches the data from the stream, converts to char, and returns it as
     * a StringBuilder.
     * @param stream The stream to read from
     * @return The resulting data
     * @throws IOException
     */
    public StringBuilder fetchResponseData(InputStream stream)
            throws IOException {
        return delegate.fetchResponseData(stream);
    }

    /**
     * Fetches the data from the reader and returns it as a StringBuilder.
     * @param reader The reader to read from
     * @return The resulting data
     * @throws IOException
     */
    public StringBuilder fetchResponseData(Reader reader) throws IOException {
        return delegate.fetchResponseData(reader);
    }

    /**
     * Maches the regular expression against the data in the current buffer.
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     */
    public boolean matchResponse(String regex) {
        return delegate.matchResponse(regex);
    }

    /**
     * Matches the regular expression against the data read from the stream.
     * @param stream The source of the data
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchResponse(InputStream stream, String regex)
            throws IOException {
        return delegate.matchResponse(stream, regex);
    }

    /**
     * Matches the regular expression against the data read from the reader.
     * @param reader The source of the data
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchResponse(Reader reader, String regex)
            throws IOException {
        return delegate.matchResponse(reader, regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * URL.
     * @param url The source of the data
     * @param regex THe regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchURL(String url, String regex) throws IOException {
        return delegate.matchURL(url, regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * URL.
     * @param url The source of the data
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchURL(String url, String regex, Map<String, String> headers)
            throws IOException {
        return delegate.matchURL(url, regex, headers);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * URL.
     * @param url The source of the data
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchURL(URL url, String regex) throws IOException {
        return delegate.matchURL(url, regex);
    }

    /**
     * Matches the regular expression against the response fetched from the
     * URL.
     * @param url The source of the data
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchURL(URL url, String regex, Map<String, String> headers)
            throws IOException {
        return delegate.matchURL(url, regex, headers);
    }

    /**
     * Mathces the regular expression against the response fetched from the
     * post request made to the URL.
     * @param url The source of the data
     * @param postRequest The post request string
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchURL(URL url, String postRequest, String regex)
            throws IOException {
        return delegate.matchURL(url, postRequest, regex);
    }

    /**
     * Mathces the regular expression against the response fetched from the
     * post request made to the URL.
     * @param url The source of the data
     * @param postRequest The post request string
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchURL(URL url, String postRequest, String regex,
                            Map<String, String> headers) throws IOException {
        return delegate.matchURL(url, postRequest, regex, headers);
    }

    /**
     * Mathces the regular expression against the response fetched from the
     * post request made to the URL.
     * @param url The source of the data
     * @param postRequest The post request string
     * @param regex The regular expression to match
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchURL(String url, String postRequest, String regex)
            throws IOException {
        return delegate.matchURL(url, postRequest, regex);
    }

    /**
     * Mathces the regular expression against the response fetched from the
     * post request made to the URL.
     * @param url The source of the data
     * @param postRequest The post request string
     * @param regex The regular expression to match
     * @param headers The request headers
     * @return True if the match succeeds, false otherwise
     * @throws IOException
     */
    public boolean matchURL(String url, String postRequest, String regex,
                            Map<String, String> headers) throws IOException {
        return delegate.matchURL(url, postRequest, regex, headers);
    }

    /**
     * Obtains the list of cookie values by the name of the cookies.
     * @param name The cookie name
     * @return An array of non-duplicating cookie values.
     */
    public String[] getCookieValuesByName(String name) {
        return delegate.getCookieValuesByName(name);
    }

    /**
     * Obtains the list of all cookies
     * @return array of Cookie objects
     */
    public Cookie[] getCookies() {
        return delegate.getCookies();
    }

    /**
     * Obtains the header fields of the last request's response.
     * @param name The response header field of interest
     * @return An array of response header values
     */
    public String[] getResponseHeader(String name) {
        return delegate.getResponseHeader(name);
    }

    /**
     * Utility class to get responseHeaders as a string.  The formatting is 
     * not localized
     * 
     * @return responseHeaders
     */
    public String dumpResponseHeaders() {
        return delegate.dumpResponseHeaders();
    }

    /**
     * Obtains the response code of the last request.
     * @return responseCode
     */
    public int getResponseCode() {
        return delegate.getResponseCode();
    }

    /**
     * Set the download speed for this HTTP transport object
     *
     * @param kbps desired speed in kilobytes per second
     * @throws UnsupportedOperationException if the underlying HTTP transport
     * doesn't support bandwidth-metered (throttled) sockets
     */
    public void setDownloadSpeed(int kbps) {
        // Subclasses can throw UnsupportedOperationException if necessary
        com.sun.faban.driver.engine.DriverContext engine =
                (com.sun.faban.driver.engine.DriverContext)
                        com.sun.faban.driver.DriverContext.getContext();
        engine.setDownloadSpeed(kbps);
    }

    /**
     * Set the upload speed for this HTTP transport object
     *
     * @param kbps desired speed in kilobytes per second
     * @throws UnsupportedOperationException if the underlying HTTP transport
     * doesn't support bandwidth-metered (throttled) sockets
     */
    public void setUploadSpeed(int kbps) {
        // Subclasses can throw UnsupportedOperationException if necessary
        com.sun.faban.driver.engine.DriverContext engine =
                (com.sun.faban.driver.engine.DriverContext)
                        com.sun.faban.driver.DriverContext.getContext();
        engine.setUploadSpeed(kbps);
    }
}
