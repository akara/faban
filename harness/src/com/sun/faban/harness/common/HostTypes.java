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
 * $Id: HostTypes.java,v 1.2 2008/04/04 22:09:26 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.common;

import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.engine.CmdService;
import com.sun.faban.common.NameValuePair;

import java.util.*;
import java.io.*;

/**
 * The HostTypes class maintains the type - host - alias relationship.
 * @author Akara Sucharitakul
 */
public class HostTypes {

    private static final String FS = "\t"; // Field Separator

    LinkedHashMap<String, Type> typeMap = new LinkedHashMap<String, Type>();
    LinkedHashMap<String, Host> hostMap = new LinkedHashMap<String, Host>();
    LinkedHashMap<String, Alias> aliasMap = new LinkedHashMap<String, Alias>();

    /**
     * Constructs the HostTypes at benchmark run time from the benchmark
     * parameter file and run information.
     * @param par The parameter repository/file
     */
    public HostTypes(ParamRepository par) {
        CmdService cmds = CmdService.getHandle();
        List<NameValuePair<String>> hostTypes = par.getHostTypes();
        ArrayList<String> drivers = new ArrayList<String>();

        for (NameValuePair<String> hostType : hostTypes) {

            // We need to map certain type names to be more understandable.
            String typeName = hostType.value;
            if (typeName.endsWith(":runConfig")) {
                // Save the drivers for last. The result stats will be ordered
                // this way and the drivers are least relevant in a run.
                // In some cases, we have 60 driver systems and we don't want
                // the user to wade through the 60 entries to find the
                // SUT components last.
                drivers.add(hostType.name);
                continue;
            }

            StringBuilder b = new StringBuilder(typeName);
            if (typeName.endsWith("Config"))
                b.setLength(typeName.length() - "Config".length());
            b.setCharAt(0, Character.toUpperCase(b.charAt(0)));
            typeName = b.toString();

            constructHostType(hostType.name, typeName, cmds);

        }

        // Now process the drivers
        for (String aliasName : drivers)
            constructHostType(aliasName, "Driver", cmds);
    }

    private void constructHostType(String aliasName, String typeName,
                                   CmdService cmds) {
        Alias a = aliasMap.get(aliasName);
        if (a == null) {
            a = new Alias();
            a.name = aliasName;
            aliasMap.put(aliasName, a);
        }

        // Then we use this new name in our type map.
        Type t = typeMap.get(typeName);
        if (t == null) {
            t = new Type();
            t.name = typeName;
            typeMap.put(typeName, t);
        }
        a.types.add(t);
        t.aliases.add(a);

        String hostName = null;
        if (a.host == null) {
            hostName = cmds.getHostName(a.name);
            a.host = hostMap.get(hostName);
        }
        if (a.host == null) {
            a.host = new Host();
            a.host.name = hostName;
            hostMap.put(a.host.name, a.host);
        }
        a.host.aliases.add(a);
        a.host.types.add(t);
        t.hosts.add(a.host);
    }

    /**
     * Constructs the HostTypes after the benchmark run time.
     * @param fileName The file containing the stored HostTypes configuration
     * @throws IOException Error reading the file or invalid file format
     */
    public HostTypes(String fileName) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(fileName));
        for (;;) {
            String line = r.readLine();
            if (line == null)
                break;
            if (line.startsWith("#")) // Commented line
                continue;
            StringTokenizer tk = new StringTokenizer(line, FS);
            try {
                String host = tk.nextToken();
                String alias = tk.nextToken();
                String type = tk.nextToken();

                Host h = hostMap.get(host);
                if (h == null) {
                    h = new Host();
                    h.name = host;
                    hostMap.put(host, h);
                }

                Alias a = aliasMap.get(alias);
                if (a == null) {
                    a  = new Alias();
                    a.name = alias;
                    aliasMap.put(alias, a);
                }

                Type t = typeMap.get(type);
                if (t == null) {
                    t = new Type();
                    t.name = type;
                    typeMap.put(type, t);
                }

                h.aliases.add(a);
                h.types.add(t);
                if (a.host != null)
                    a.host = h;
                a.types.add(t);
                t.hosts.add(h);
                t.aliases.add(a);

            } catch (NoSuchElementException e) {
                throw new IOException("File " + fileName +
                                            " : Invalid format!");
            }
        }
    }

    /**
     * Writes the HostTypes out to file for later reading.
     * @param fileName The file name to store the information
     * @throws IOException A write error occurred
     */
    public void write(String fileName) throws IOException {
        BufferedWriter b = new BufferedWriter(new FileWriter(fileName));
        write(b);
        b.flush();
        b.close();
    }

    /**
     * Writes the HostTypes out to an appendable for later reading.
     * @param b The appendable buffer.
     * @throws IOException A write error occurred
     */
    public void write(Appendable b) throws IOException {
        b.append("#Host\t\tAlias\t\tType\n");
        for (Host h : hostMap.values()) {
            String host = h.name;
            for (Alias a : h.aliases) {
                String alias = a.name;
                for (Type t : a.types) {
                    String type = t.name;
                    b.append(host).append(FS).append(alias).append(FS).
                                                append(type).append('\n');
                }
            }
        }
    }

    /**
     * Returns the list of hosts in the run in the order their aliases
     * are referred to in the configuration file.
     * @return The host list
     */
    public String[] getHostsInOrder() {
        Set<String> hostSet = hostMap.keySet();
        String[] hosts = new String[hostSet.size()];
        hosts = hostSet.toArray(hosts);
        return hosts;
    }

    /**
     * Obtains the hosts pertaining to a certain type or function in the
     * benchmark run.
     * @param type The type name.
     * @return An array of host names used for the type in question.
     */
    public String[] getHostsByType(String type) {
        Type t = typeMap.get(type);
        if (t == null)
            return null;
        String[] hosts = new String[t.hosts.size()];
        int i = 0;
        for (Host h : t.hosts) {
            hosts[i++] = h.name;
        }
        return hosts;
    }

    /**
     * Obtains the host aliases used for a certain type or function in the
     * benchmark run. The alias is usually an interface name used to refer to
     * a host.
     * @param type The type name.
     * @return An array of aliases used for the type in question
     */
    public String[] getAliasesByType(String type) {
        Type t = typeMap.get(type);
        if (t == null)
            return null;
        String[] aliases = new String[t.aliases.size()];
        int i = 0;
        for (Alias a : t.aliases) {
            aliases[i++] = a.name;
        }
        return aliases;
    }

    /**
     * Obtains the function types the host in question is used for.
     * @param host The host name
     * @return An array of type names the host is used for
     */
    public String[] getTypesByHost(String host) {
        Host h = hostMap.get(host);
        if (h == null)
            return null;
        String[] types = new String[h.types.size()];
        int i = 0;
        for (Type t : h.types) {
            types[i++] = t.name;
        }
        return types;
    }

    /**
     * Obtains all aliases or interface names used to refer to the host.
     * @param host The host name
     * @return An array of aliases used to refer to this host.
     */
    public String[] getAliasesByHost(String host) {
        Host h = hostMap.get(host);
        if (h == null)
            return null;
        String[] aliases = new String[h.aliases.size()];
        int i = 0;
        for (Alias a : h.aliases) {
            aliases[i++] = a.name;
        }
        return aliases;
    }

    /**
     * Obtains the aliases that are used for a certain host in a certain
     * function.
     * @param host The host name
     * @param type The type name
     * @return An array of applicable aliases
     */
    public String[] getAliasesByHostAndType(String host, String type) {
        Host h = hostMap.get(host);
        Type t = typeMap.get(type);
        HashSet<Alias> aliasSet = new HashSet<Alias>(h.aliases);
        aliasSet.retainAll(t.aliases); // Intersect between the two sets.
        String[] aliases = new String[aliasSet.size()];
        int i = 0;
        for (Alias a : aliasSet) {
            aliases[i++] = a.name;
        }
        return aliases;
    }

    /**
     * Obtains the real host name referred to by an alias.
     * @param alias The alias name
     * @return The real host name
     */
    public String getHostByAlias(String alias) {
        Alias a = aliasMap.get(alias);
        if (a == null)
            return null;
        return a.host.name;
    }

    /**
     * Obtains the types or function an interface or alias is used for.
     * @param alias The alias name
     * @return An array of function types the alias is used for.
     */
    public String[] getTypesByAlias(String alias) {
        Alias a = aliasMap.get(alias);
        if (a == null)
            return null;
        String[] types = new String[a.types.size()];
        int i = 0;
        for (Type t : a.types) {
            types[i++] = t.name;
        }
        return types;
    }



    static class Type {
        public String name;
        public LinkedHashSet<Host> hosts = new LinkedHashSet<Host>();
        public LinkedHashSet<Alias> aliases = new LinkedHashSet<Alias>();

        @Override public boolean equals(Object obj) {
            boolean retVal = false;
            if (obj instanceof Type) {
                Type other = (Type) obj;
                if (name != null)
                    retVal = name.equals(other.name);
            }
            return retVal;
        }

        @Override public int hashCode() {
            return name.hashCode();
        }
    }

    static class Host {
        public String name;
        public LinkedHashSet<Type> types = new LinkedHashSet<Type>();
        public LinkedHashSet<Alias> aliases = new LinkedHashSet<Alias>();

        @Override public boolean equals(Object obj) {
            boolean retVal = false;
            if (obj instanceof Host) {
                Host other = (Host) obj;
                if (name != null)
                    retVal = name.equals(other.name);
            }
            return retVal;
        }

        @Override public int hashCode() {
            return name.hashCode();
        }
    }

    static class Alias {
        public String name;
        public Host host;
        public LinkedHashSet<Type> types = new LinkedHashSet<Type>();

        @Override public boolean equals(Object obj) {
            boolean retVal = false;
            if (obj instanceof Alias) {
                Alias other = (Alias) obj;
                if (name != null)
                    retVal = name.equals(other.name);
            }
            return retVal;
        }

        @Override public int hashCode() {
            return name.hashCode();
        }
    }
}
