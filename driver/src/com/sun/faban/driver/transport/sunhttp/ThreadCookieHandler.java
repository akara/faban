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
package com.sun.faban.driver.transport.sunhttp;

import com.sun.faban.driver.FatalException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * ThreadCookieHandler stores cookies from a response and retrieves all applicable
 * cookies for a particular request. On retrieval, the cookies will be
 * formatted as required. If a cookie does not specify a version when it is
 * received - it is non-conformant to IEEE 2965 and 2109 - it is considered
 * a cookie according to Netscape's original proposal. Such cookies will
 * be formatted according to Netscape's proposal. A cookie that specifies
 * a version is considered to follow IEEE2965 and 2109 even if version is 0.
 * Such cookies will be formatted according to IEEE2965. In all cases, the
 * selection rules specified in IEEE2965 applies.<p>
 *
 * Note that the interface of this cookie handler does not comply with
 * java.net.ThreadCookieHandler.
 *
 * @author Akara Sucharitakul
 */
public class ThreadCookieHandler {

    private static InheritableThreadLocal<ThreadCookieHandler> localRef =
            new InheritableThreadLocal<ThreadCookieHandler>();

    TreeMap<Integer, CookieStore> cookieStore =
            new TreeMap<Integer, CookieStore>();

    int sequence = 0; // Used for ordering the cookies

    static Logger logger = Logger.getLogger(
            ThreadCookieHandler.class.getName());

    SimpleDateFormat dateParser = null; // Used for parsing "Expires"

    /**
     * Creates a new instance of the thread cookie handler for this thread.
     * @return The thread's cookie handler
     */
    public static ThreadCookieHandler newInstance() {
        ThreadCookieHandler handler = new ThreadCookieHandler();
        localRef.set(handler);
        return handler;
    }

    /**
     * Obtains the thread cookie handler for this thread.
     * @return The thread's cookie handler
     */
    public static ThreadCookieHandler getInstance() {
        ThreadCookieHandler instance = localRef.get();
        if (instance == null) {
			throw new FatalException(
                    "Cookie handler not initialized for thread.");
		}
        return instance;
    }

    private ThreadCookieHandler() {
    	super();
    }

    /**
     * Puts the cookies from the response headers into the cookie store.
     * @param request The URI of the request
     * @param responseHeaders The response headers.
     */
    public void put(URI request,
                    Map<String, List<String>> responseHeaders) {
        // The main cookie store has domain/host name as key and another
        // map of each individual cookie as the value.
        for (Iterator<String> iter = responseHeaders.keySet().iterator();
             iter.hasNext();) {
            String headerName = iter.next();
            if ("Set-Cookie".equalsIgnoreCase(headerName)) {
                List<String> cookieList = responseHeaders.get(headerName);
                // Apparently, the list contains cookies in the reverse order
                // so we also need to traverse it in the reverse order.
                for (int i = cookieList.size() - 1; i >= 0; i--) {
                    Cookie cookie = parseAndPutCookie(cookieList.get(i),
                            request, headerName);
                    cookie.type = 1;
                }
            } else if ("Set-Cookie2".equalsIgnoreCase(headerName)) {
                List<String> cookieList = responseHeaders.get(headerName);
                for (int i = cookieList.size() - 1; i >= 0; i--) {
                    Cookie cookie = parseAndPutCookie(cookieList.get(i),
                            request, headerName);
                    cookie.type = 2;
                }
            }
        }
    }

    private Cookie parseAndPutCookie(String cookieString, URI request,
                                     String headerName) {
        Cookie cookie = Cookie.parseCookie(cookieString, this);
        cookie.validate(request);
        putCookies(cookie);
        logger.finer("ResponseHeader - " + headerName + ": "
                + cookieString);
        return cookie;
    }

    /**
     * Selects the cookies applicable to the request URI from the cookie
     * store and puts them into the request header. The cookies are ordered
     * as specified in RFE 2965.
     * @param request The request URI
     * @param requestHeaders The request header map
     * @return The request header map with the cookies put in
     */
    @SuppressWarnings("boxing")
	public Map<String, List<String>> get(URI request, Map<String,
                                          List<String>> requestHeaders) {

        Map<String, List<String>> cookieHeaders =
                new LinkedHashMap<String, List<String>>();
        cookieHeaders.putAll(requestHeaders);
        for (Map.Entry<Integer, CookieStore> entry : cookieStore.entrySet()) {
            int version = entry.getKey();
            CookieStore store = entry.getValue();
            List<Cookie> cookieList = store.select(request);
            if (cookieList == null) {
				continue;
			}
            if (version == -1) {
				formatNetscapeCookies(cookieList, cookieHeaders);
			} else {
				format2965Cookies(cookieList, cookieHeaders);
			}
        }
        return cookieHeaders;
    }

    private static String parseDomain(String hostName) {
        String domainName = null;
        int idx = hostName.indexOf('.', 1);
        if (idx > 0 && idx < hostName.length() - 1) {
			domainName = hostName.substring(idx);
		}
        return domainName;
    }

    /**
     * Adds the applicable cookies from the cookie handler to the request
     * header of the connection.
     * @param c The connection
     * @throws URISyntaxException 
     */
    public void addRequestCookies(java.net.HttpURLConnection c)
            throws URISyntaxException {
        Map<String, List<String>> requestProps =
                new LinkedHashMap<String, List<String>>();
        get(c.getURL().toURI(), requestProps);
        for (Iterator<String> iter = requestProps.keySet().iterator();
             iter.hasNext();) {
            String key = iter.next();
            List<String> valueList = requestProps.get(key);
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.get(i);
                c.addRequestProperty(key, value);
                logger.finer("RequestHeader - " + key + ": " + value);
            }
        }
    }

    /**
     * Obtains the value of all cookies in the cookie store by the name of the
     * cookie.
     * @param name The cookie name
     * @return The values of all cookies matching this name
     */
    public String[] getCookieValuesByName(String name) {
        HashSet<String> valueSet = new HashSet<String>();
        for (CookieStore store : cookieStore.values()) {
            store.getValuesByName(name, valueSet);
        }
        String[] values = new String[valueSet.size()];
        return valueSet.toArray(values);
    }

    /**
     * Format cookies according to IETF RFE2965. Note that all cookies in the
     * collection must be of the same version.
     * @param cookies The collection of cookies to format
     * @param requestHeaders The request headers to put the cookie out
     */
    private static void format2965Cookies(Collection<Cookie> cookies,
                                   Map<String, List<String>> requestHeaders) {

        StringBuilder b = null;
        for (Cookie cookie : cookies) {
            // We only support cookie version 1
            if (b == null) { // First time initialization
                if (cookie.version > 1 &&
                        requestHeaders.get("Cookie2") == null) {
                    List<String> cookie2 = new ArrayList<String>(1);
                    cookie2.add("$Version=\"1\"");
                    requestHeaders.put("Cookie2", cookie2);
                }
                b = new StringBuilder();
                b.append("$Version=\"");
                b.append(cookie.version);
                b.append('"');
            }
            b.append("; ");
            b.append(cookie.name);
            b.append('=');
            b.append(cookie.value);

            if (cookie.pathString != null) {
                b.append("; $Path=");
                b.append(cookie.path);
            }
            if (cookie.domainString != null) {
                b.append("; $Domain=");
                b.append(cookie.domain);
            }
            if (cookie.portString != null) {
                b.append("; $Port=");
                b.append(cookie.portString);
            }
        }
        if (b != null) {
            List<String> list = requestHeaders.get("Cookie");
            if (list == null) {
                list = new ArrayList<String>();
                requestHeaders.put("Cookie", list);
            }
            String cookieString = b.toString();
            list.add(cookieString);
            logger.finest("Cookie:" + cookieString);
        } else {
            logger.finer("No request cookies");
        }
    }

    /**
     * Format cookies according to Netscape's origina proposal.
     * Note that all cookies in the collection must be of the same version.
     * @param cookies The collection of cookies to format
     * @param requestHeaders The request headers to put the cookie out
     */
    private static void formatNetscapeCookies(Collection<Cookie> cookies,
                               Map<String, List<String>> requestHeaders) {

        StringBuilder b = null;
        for (Cookie cookie : cookies) {
            if (b == null) {
				b = new StringBuilder();
			} else {
				b.append("; ");
			}
            b.append(cookie.name);
            b.append('=');
            b.append(cookie.value);
        }
        if (b != null) {
            List<String> list = requestHeaders.get("Cookie");
            if (list == null) {
                list = new ArrayList<String>();
                requestHeaders.put("Cookie", list);
            }
            String cookieString = b.toString();
            list.add(cookieString);
            logger.finest("Cookie:" + cookieString);
        } else {
            logger.finer("No request cookies");
        }
    }

    /**
     * Puts the cookie in the store corresponding to the cookie version.
     * @param cookie The cookie to put in.
     */
    private void putCookies(Cookie cookie) {
        CookieStore store = cookieStore.get(new Integer(cookie.version));
        if (store == null) {
            store = new CookieStore(cookie.version);
            cookieStore.put(new Integer(cookie.version), store);
        }
        store.add(cookie, this);
    }

    /**
     * Object structure corresponding to the cookie itself.
     */
    static class Cookie {
        int type;       // 1 or 2 for Cookie or Cookie2
        String name;
        String value;
        String comment;
        String commentURL;  // Set-Cookie2 only
        int discard = -1;    // Set-Cookie2 only
        String domain;
        String domainString;
        long timeStamp;
        int maxAge = -1;
        String path;
        private char[] cPath;   // Used for context-sensitive comparison
        String pathString;  // Original path string
        int[] ports;        // Set-Cookie2 only
        String portString;  // Set-Cookie2 only
        boolean secure;
        int version = -1;   // -1 means not set for the netscape case

        /**
         * Parses the cookie as represented in the response header into a
         * cookie object.
         * @param cookieString The string from the response header
         * @param h The instance of the ThreadCookieHandler to parse and store
         * @return The resulting cookie
         */
        static Cookie parseCookie(String cookieString, ThreadCookieHandler h) {

            StringTokenizer tokenizer = new StringTokenizer(
                    cookieString, ";", false);
            String nameValue = tokenizer.nextToken();
            int idx = nameValue.indexOf('=');
            if (idx == -1) {
				return null;
			}

            // Determine the name
            String token = nameValue.substring(0, idx).trim();
            if (token.startsWith("$")) {
				return null;
			}
            Cookie cookie = new Cookie();
            cookie.timeStamp = System.currentTimeMillis();
            cookie.name = token;

            // Value
            cookie.value = nameValue.substring(idx + 1).trim();

            // Now deal with all attributes
            while (tokenizer.hasMoreTokens()) {
                nameValue = tokenizer.nextToken();

                // Extract the attributes
                idx = nameValue.indexOf('=');
                if (idx != -1) {
                    token = nameValue.substring(idx + 1).trim();
                    nameValue = nameValue.substring(0, idx).trim();
                } else {
                    nameValue = nameValue.trim();
                    token = null;
                }

                if ("Comment".equalsIgnoreCase(nameValue)) {
                    cookie.comment = token;
                } else if ("CommentURL".equalsIgnoreCase(nameValue)) {
                    cookie.commentURL = token;
                } else if ("Discard".equalsIgnoreCase(nameValue)) {
                    cookie.discard = 1;
                } else if ("Domain".equalsIgnoreCase(nameValue)) {
                    cookie.domainString = token;
                    if (token.startsWith("\"") && token.endsWith("\"")) {
						cookie.domain = token.substring(1,
                                token.length() - 1).trim();
					} else {
						cookie.domain = token;
					}

                    if (!cookie.domain.startsWith(".")) {
						cookie.domain = "." + cookie.domain;
					}
                } else if ("Expires".equalsIgnoreCase(nameValue)) {
                    // Old Netscape cookie spec, we translate to maxAge
                    long expires = -1;
                    if (h.dateParser == null) {
                        // The Netscape cookie spec takes this format:
                        // "EEE, d-MMM-yyyy HH:mm:ss z" but we've seen cases of
                        // "EEE, d MMM yyyy HH:mm:ss z". Due to ease of
                        // conversion, we replace the '-' with ' ' and parse
                        // the format without the '-'.
						h.dateParser = new SimpleDateFormat(
                                "EEE, d MMM yyyy HH:mm:ss z");
					}
                    try {
                        if (token.startsWith("\"") && token.endsWith("\"")) {
                            token = token.substring(1, token.length() - 1).
                                            trim();
						}
                        token = token.replace('-', ' ');
                        expires = h.dateParser.parse(token).getTime();
                    } catch (ParseException e) {
                        throw new IllegalArgumentException(e);
                    }
                    long maxAge = expires - cookie.timeStamp;
                    if (maxAge <= 0) {
                        cookie.maxAge = 0;
                    } else {
                        int remainder = (int) (maxAge % 1000);
                        maxAge /= 1000;
                        if (remainder > 0) {
							++maxAge;
						}
                        cookie.maxAge = (int) maxAge;
                    }
                } else if ("Max-Age".equalsIgnoreCase(nameValue)) {
                    if (token.startsWith("\"") && token.endsWith("\"")) {
						cookie.maxAge = Integer.parseInt(token.substring(1,
                                token.length() - 1).trim());
					} else {
						cookie.maxAge = Integer.parseInt(token);
					}

                } else if ("Path".equalsIgnoreCase(nameValue)) {
                    cookie.pathString = token;
                    if (token.startsWith("\"") && token.endsWith("\"")) {
						cookie.path = token.substring(1,
                                token.length() - 1).trim();
					} else {
						cookie.path = token;
					}
                } else if ("Port".equalsIgnoreCase(nameValue)) {
                    // Strip off the quotes
                    if (token == null) { // Port attribute without value
                        cookie.ports = new int[1];
                        // Leave it null for now. Validate will fill this in.
                        continue;
                    }

                    cookie.portString = token; // Save as is for sending.
                    if (token.startsWith("\"") && token.endsWith("\"")) {
						token = token.substring(1,
                                token.length() - 1).trim();
					}

                    // Put all ports into the list
                    ArrayList<String> portList = new ArrayList<String>();
                    for (StringTokenizer t = new StringTokenizer(
                            token, ", ", false); t.hasMoreTokens();
                         portList.add(t.nextToken())) {
						// do nothing, the loops does the rest
					}

                    // Allocate array and parse all ports in the array
                    cookie.ports = new int[portList.size()];
                    for (int i = 0; i < cookie.ports.length; i++) {
						cookie.ports[i] = Integer.parseInt(portList.get(i));
					}
                } else if ("Secure".equalsIgnoreCase(nameValue)) {
                    cookie.secure = true;
                } else if ("Version".equalsIgnoreCase(nameValue)) {
                    if (token.startsWith("\"") && token.endsWith("\"")) {
						cookie.version = Integer.parseInt(token.substring(1,
                                token.length() - 1).trim());
					} else {
						cookie.version = Integer.parseInt(token);
					}
                    if (cookie.version < 0) {
						throw new IllegalArgumentException("Cookie version " +
                                "must be greater than 0, received " +
                                cookie.version + '.');
					}
                }
            }
            return cookie;
        }

        /**
         * Minimally validates the cookie. Note that this implementation is
         * for a benchmark driver. We only do minimal validation and no
         * cookie spoofing/security checks.
         * @param request The URL of this request
         * @return true if this cookie is valid or is made valid.
         */
        boolean validate(URI request) {

            if (domain == null) {
                domain = request.getHost();
                logger.finest("Set cookie domain to " + domain);
            }

            if (path == null) {
                path = request.getPath();
                int idx = path.lastIndexOf('/');
                path = path.substring(0, idx + 1);
                logger.finest("Set cookie path to " + path);
            }


            if (discard == -1) {// unset
                if (maxAge == -1) {
					discard = 1; // discards on exit
				} else {
					discard = 0; // if max-time set, don't discard
				}
            }

            // Check for case of ports set without attribute.
            if (portString == null && ports != null) {
                ports[0] = request.getPort();
                if (ports[0] == -1) {
					ports[0] = 80; // HTTP default port
				}
            }
            return true;
        }

        /**
         * Matches a cookie's path to a request path to decide to send
         * or not to send cookie.
         * @param requestPath The URI path of the request
         * @return true if the request path matches this cookie, false otherwise
         */
        boolean matchPath(String requestPath) {
            if (cPath == null) // Initialize cPath for multiple uses.
                cPath = path.toCharArray();

            char[] rPath = requestPath.toCharArray(); // request path
            boolean[] mark = new boolean[2]; // boolean array saves space.

            for (int i = 0, j = 0; i < cPath.length; i++, j++) {

                if (j >= rPath.length) // request path too short to match
                    return false;

                if (cPath[i] == '/')
                    if (mark[0]) {
                        ++i;
                        while (i < cPath.length && cPath[i] == '/')
                            ++i;
                        if (i == cPath.length)
                            break;
                        mark[0] = false;
                    } else {
                        mark[0] = true;
                    }
                else
                    mark[0] = false;

                if (rPath[j] == '/')
                    if (mark[1]) {
                        ++j;
                        while (j < rPath.length && rPath[j] == '/')
                            ++j;
                        if (j == rPath.length) // too short to match
                            return false;
                        mark[1] = false;
                    } else {
                        mark[1] = true;
                    }
                else
                    mark[1] = false;

                if (cPath[i] != rPath[j])
                    return false;
            }
            return true;
        }

        /**
         * A cookie is equal to another cookie and replaces the other cookie
         * if for the same domain or request it has the same name and the same
         * path. Cookies that do not share the effective host/domain may not
         * be compared.
         * @param o The object to compare to
         * @return True if it is equivalent, false otherwise
         */
        @Override
		public boolean equals(Object o) {
            if (o instanceof Cookie) {
                Cookie other = (Cookie) o;
                if (name.equals(other.name) && path.equals(other.path)) {
					return true;
				}
            }
            return false;
        }

        /**
         * The hashCode of a cookie is represented from the equivalency
         * standpoint. That is, if two cookies have the same name and path,
         * they are considered equivalent within the effective host's domain.
         * Cookies that do not share the effective host/domain may not
         * be compared.
         * @return The hash code of this cookie
         */
        @Override
		public int hashCode() {
            return (name + path).hashCode();
        }

        /**
         * Returns a string representation of the object. In general, the
         * <code>toString</code> method returns a string that
         * "textually represents" this object. The result should
         * be a concise but informative representation that is easy for a
         * person to read.
         * It is recommended that all subclasses override this method.
         * <p/>
         * The <code>toString</code> method for class <code>Object</code>
         * returns a string consisting of the name of the class of which the
         * object is an instance, the at-sign character `<code>@</code>', and
         * the unsigned hexadecimal representation of the hash code of the
         * object. In other words, this method returns a string equal to the
         * value of:
         * <blockquote>
         * <pre>
         * getClass().getName() + '@' + Integer.toHexString(hashCode())
         * </pre></blockquote>
         *
         * @return a string representation of the object.
         */
        @Override public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(name).append('=').append(value).append(", domain: ");
            b.append(domain).append(", path: ").append(path).
                    append(", ports: ").append(ports);
            return b.toString();
        }
    }

    /**
     * Stores a cookie of a particular version, searchable by domain.
     */
    static class CookieStore implements Comparable<CookieStore> {

        int version;

        Map<String, DomainCookieStore> store =
                new HashMap<String, DomainCookieStore>();

        /**
         * Constructs the cookie store.
         * @param version The cookie version being used
         */
        public CookieStore(int version) {
            this.version = version;
        }

        /**
         * Adds the cookie to the applicable domain.
         * @param cookie The cookie
         * @param h the ThreadCookieHandler to add the cookie
         */
        void add(Cookie cookie, ThreadCookieHandler h) {
            DomainCookieStore dStore = store.get(cookie.domain);
            if (dStore == null) {
                dStore = new DomainCookieStore(cookie.domain);
                store.put(cookie.domain, dStore);
            }
            dStore.add(cookie, h);
        }

        /**
         * Selects the cookies applicable to the given URL.
         * @param request The URL
         * @return A list of applicable cookies
         */
        List<Cookie> select(URI request) {

            // Figure out the applicable domains
            InetAddress[] address = null;
            String hostName = request.getHost();
            
            if (hostName == null) {
                logger.warning("Request " + request + " does not contain a " +
                               "hostname or has an invalid hostname.");
            }

            try {
                address = InetAddress.getAllByName(hostName);
            } catch (UnknownHostException e) {
                return null;
            }

            HashSet<String> domainSet = new HashSet<String>();

            String domain = hostName;
            while (domain != null) {
                logger.finest("Select(0) cookie, domain: " + domain);
                domainSet.add(domain);
                domain = parseDomain(domain);
            }

            domain = address[0].getCanonicalHostName();
            while (domain != null) {
                logger.finest("Select(1) cookie, domain: " + domain);
                domainSet.add(domain);
                domain = parseDomain(domain);
            }

            domain = address[0].getHostName();
            while (domain != null) {
                logger.finest("Select(2) cookie, domain: " + domain);
                domainSet.add(domain);
                domain = parseDomain(domain);
            }

            for (int i = 0; i < address.length; i++) {
                String addr = address[i].getHostAddress();
                logger.finest("Select(3) cookie, host: " + addr);
                domainSet.add(addr);
            }

            // Merge result for applicable domains
            DomainCookieStore dStore = null;
            for (String dm : domainSet) {
                DomainCookieStore dxStore = store.get(dm);
                logger.finest("Fetching cookies for domain " + dm);
                if (dxStore == null) {
                    logger.finer("No cookies found for domain " + dm);
                    continue;
                }
                if (dStore == null) {
					dStore = dxStore.select(request);
				} else {
					dStore.merge(dxStore.select(request));
				}
            }

            if (dStore == null) {
                logger.finer("No cookies found for request " + request);
				return null;
			}

            return dStore.getSortedCookieList();
        }

        void getValuesByName(String name, Collection<String> c) {
            for (DomainCookieStore dStore : store.values()) {
				dStore.getValuesByName(name, c);
			}
        }

        /**
         * Compares this cookie store to another cookie store.
         * @param s The other cookie store
         * @return Positive, negative integer or 0 based on the comparison
         */
        public int compareTo(CookieStore s) {
            return s.version - version;
        }

        /**
         * Tests the cookis store for equivalence.
         * @param o The other object
         * @return True if stores are equivalent
         */
        @Override
		public boolean equals(Object o) {
            if (o instanceof CookieStore) {
                CookieStore s = (CookieStore) o;
                return version == s.version;
            }
            return false;
        }
    }

    /**
     * Stores the cookies for a particular domain, separated by name.
     */
    static class DomainCookieStore {
        String domain;
        Map<String, NameCookieStore> store =
                new HashMap<String, NameCookieStore>();

        /**
         * Creates a cookie store for the given domain.
         * @param domain The domain this store is applicable to.
         */
        DomainCookieStore(String domain) {
            this.domain = domain;
        }

        /**
         * Adds a cookie to this, separated by its name.
         * @param cookie The cookie
         * @param h The thread cookie handler to store
         */
        void add(Cookie cookie, ThreadCookieHandler h) {
            NameCookieStore nStore = store.get(cookie.name);
            if (nStore == null) {
                nStore = new NameCookieStore(cookie.name, h);
                store.put(cookie.name, nStore);
            }
            nStore.add(cookie);
        }

        /**
         * Selects the cookies in this store applicable to the request.
         * @param request The URL of the request
         * @return Another cookie store containing only cookies applicable
         *         to the request.
         */
        DomainCookieStore select(URI request) {
            DomainCookieStore result = null;
            for (NameCookieStore nStore : store.values()) {
                NameCookieStore nResult = nStore.select(request);
                if (nResult != null) {
                    if (result == null) {
						result = new DomainCookieStore(domain);
					}
                    result.store.put(nResult.name, nResult);
                }
            }
            return result;
        }

        void getValuesByName(String name, Collection<String> c) {
            NameCookieStore nStore = store.get(name);
            for (Cookie cookie : nStore.store.values()) {
				c.add(cookie.value);
			}
        }

        /**
         * Merges the domain cookie store with a cookie store of
         * another domain. These two domains are usually applicable to the
         * request.
         * @param other The store of the other domain
         * @return This store
         */
        DomainCookieStore merge(DomainCookieStore other) {
            if (other == null) {
				return this;
			}
            if (domain.equals(other.domain)) {
				throw new IllegalArgumentException(
                        "Cannot merge store with same name.");
			}

            for (NameCookieStore nStore : other.store.values()) {
                NameCookieStore oldNStore = store.put(nStore.name, nStore);
                if (oldNStore != null) {
					nStore.merge(oldNStore);
				}
            }
            domain += ',' + other.domain;
            return this;
        }

        /**
         * Obtains a list of cookies from this store sorted as specified by
         * RFE 2965 and the sequence it is received if the sequence is left
         * undefined in RFE 2965.
         * @return The list of cookies, appropriately sorted
         */
        List<Cookie> getSortedCookieList() {
            TreeSet<NameCookieStore> nStoreSet =
                    new TreeSet<NameCookieStore>(store.values());
            ArrayList<Cookie> list = new ArrayList<Cookie>(nStoreSet.size());
            TreeSet<Cookie> cookieSet =
                    new TreeSet<Cookie>(new CookiePathComparator());

            for (NameCookieStore nStore : nStoreSet) {
                cookieSet.addAll(nStore.store.values());
                for (Cookie cookie : cookieSet) {
                    list.add(cookie);
                }
                cookieSet.clear();
            }
            return list;
        }
    }

    /**
     * Stores the cookies for a certain cookie name.
     */
    static class NameCookieStore implements Comparable<NameCookieStore> {

        int id;
        String name;

        // Cookie path is the key here
        Map<String, Cookie> store = new HashMap<String, Cookie>();

        /**
         * Constructs a cookie store for a certain name.
         * @param name The name of the cookie
         * @param h The thread cookie handler to use
         */
        NameCookieStore(String name, ThreadCookieHandler h) {
            this.name = name;
            id = h.sequence++;
        }

        /**
         * Constructs a cookie store with a certain sequence id.
         * @param name Name of the cookies to be stored
         * @param id The sequence number of this name, used for sorting
         */
        private NameCookieStore(String name, int id) {
            this.name = name;
            this.id = id;
        }

        /**
         * Adds a cookie matching the name of this store to the store.
         * @param cookie The cookie to be added
         */
        void add(Cookie cookie) {
            // Check whether this is actually a remove - maxAge == 0
            if (cookie.maxAge == 0) {
                store.remove(cookie.path);
                return;
            }

            // Optimistically replace
            Cookie oldCookie = store.put(cookie.path, cookie);

            // Then check validity and add back if the old one is newer
            if (oldCookie != null && oldCookie.timeStamp > cookie.timeStamp) {
				store.put(oldCookie.path,  oldCookie);
			}
        }

        /**
         * Merges the stores and will return this store.
         * No new store is allocated.
         * @param otherStore The store to merge this storte with
         * @return This store after the merge
         */
        public NameCookieStore merge(NameCookieStore otherStore) {
            if (otherStore == null) {
				return this;
			}

            if (!name.equals(otherStore.name)) {
				throw new IllegalArgumentException(
                        "Can only merge stores with same name!");
			}

            if (id > otherStore.id) {
				id = otherStore.id;
			}

            for (Cookie cookie : otherStore.store.values()) {
				add(cookie);
			}

            return this;
        }

        /**
         * Selects the cookies for this name accroding to the rules specified
         * in RFE 2965.
         * @param request The URI of the request
         * @return A name cookie store containing the selected cookies
         */
        public NameCookieStore select(URI request) {

            NameCookieStore result = null;

            for (Cookie cookie : store.values()) {
                // 1. Domain selection - the domain was selected by now.

                // 2. Max age selection.
                // We do this first to purge all timed-out cookies.
                long currentTime;
                if (cookie.maxAge != -1 &&
                        (currentTime = System.currentTimeMillis()) >=
                        cookie.maxAge * 1000l + cookie.timeStamp) {
                    logger.fine("Current time: " + currentTime + ", " +
                            "purging timed out cookie " + cookie.name + '=' +
                            cookie.value + ", timestamp: " + cookie.timeStamp +
                            ", ,max-age: " + cookie.maxAge);
                    store.remove(cookie.path);
                    if (store.size() == 0) {
                        return null;
                    }
                }

                // 3. Port selection
                int requestPort = request.getPort();
                if (requestPort == -1) {
					requestPort = 80; // Default HTTP port
				}

                boolean portDenied = true;
                if (cookie.ports == null) {
					portDenied = false;
				} else {
					for (int allowedPort : cookie.ports) {
						if (requestPort == allowedPort) {
                            portDenied = false;
                            break;
                        }
					}
				}
                if (portDenied) {
                    if (logger.isLoggable(Level.FINEST))
                        logger.finest("Port denied. Cookie: " + cookie +
                                                ", Request: " + request);
					continue;
				}

                // Path selection
                String path = request.getPath();
                if (path == null) {
					path = "/";
				}
                if (!cookie.matchPath(path)) {
                    if (logger.isLoggable(Level.FINEST))
                        logger.finest("Path denied. Cookie: " + cookie +
                                                ", Request path: " + path);
					continue;
				}

                if (result == null) {
					result = new NameCookieStore(name, id);
				}

                result.add(cookie);
            }
            return result;
        }

        /**
         * Compares this store to another store for ordering by id.
         * @param nStore The other store
         * @return A positive integer, a negative integer, or 0
         */
        public int compareTo(NameCookieStore nStore) {
            return id - nStore.id;
        }
    }

    /**
     * The comparator used to order the cookies by path.
     */
    static class CookiePathComparator implements Comparator<Cookie> {

        /**
         * Coompares the cookie by path for ordering according the RFE 2965.
         * @param c The first cookie
         * @param c1 The second cookie
         * @return The comparison result, either positive, negative integer,
         *         or 0
         */
        public int compare(Cookie c, Cookie c1) {
            return c1.path.length() - c.path.length();
        }
    }

    /**
     * Main method to test cookie handler.
     * @param args Name of each file representing each request header
     * @throws IOException Cannot find or read file
     * @throws URISyntaxException 
     */
    public static void main(String[] args)
            throws IOException, URISyntaxException {
        ThreadCookieHandler handler = new ThreadCookieHandler();
        Map<String, List<String>> reqHeader =
                new HashMap<String, List<String>>();
        Map<String, List<String>> respHeader =
                new HashMap<String, List<String>>();
        for (int i = 0; i < args.length; i++) {
            BufferedReader r = new BufferedReader(new FileReader(args[i]));
            String cookieHeader = null;
            System.out.println("---- Response Header ----");
            while((cookieHeader = r.readLine()) != null) {
                System.out.println(cookieHeader);
                int idx = cookieHeader.indexOf(": ");
                if (idx == -1) {
					continue;
				}
                String key = cookieHeader.substring(0, idx);
                String value = cookieHeader.substring(idx + 2);
                List<String> values = respHeader.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                    respHeader.put(key, values);
                }
                values.add(value);
            }
            r.close();
            URL url = new URL("http", "sunstorm.sfbay.sun.com", 80, "/uwc");
            handler.put(url.toURI(), respHeader);
            handler.get(url.toURI(), reqHeader);
            System.out.println("---- Request Header ----");
            for (Iterator<String> iter = reqHeader.keySet().iterator();
                 iter.hasNext(); ) {
                String key = iter.next();
                List<String> values = reqHeader.get(key);
                for (int j = 0; j < values.size(); j++) {
					System.out.println(key + ": " + values.get(j));
				}
            }
            reqHeader.clear();
        }
    }
}