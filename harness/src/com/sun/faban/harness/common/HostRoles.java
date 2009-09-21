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
package com.sun.faban.harness.common;

import com.sun.faban.common.NameValuePair;
import com.sun.faban.common.Utilities;
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.engine.CmdService;

import java.io.*;
import java.util.*;

/**
 * The HostRoles class maintains the role - host - alias relationship.
 * @author Akara Sucharitakul
 */
public class HostRoles {

    private static final String FS = "\t"; // Field Separator

    LinkedHashMap<String, Role> roleMap = new LinkedHashMap<String, Role>();
    LinkedHashMap<String, Host> hostMap = new LinkedHashMap<String, Host>();
    LinkedHashMap<String, Alias> aliasMap = new LinkedHashMap<String, Alias>();

    // Maps the short alias to the full domain name being used in config file.
    HashMap<String, String> domainMap = new HashMap<String, String>();

    /**
     * Constructs the HostRoles at benchmark run time from the benchmark
     * parameter file and run information.
     * @param par The parameter repository/file
     * @throws ConfigurationException If there is a configuration error
     */
    public HostRoles(ParamRepository par) throws ConfigurationException {
        CmdService cmds = CmdService.getHandle();
        List<NameValuePair<String>> hostRoles = par.getHostRoles();
        ArrayList<String> drivers = new ArrayList<String>();

        for (NameValuePair<String> hostRole : hostRoles) {

            // We need to map certain role names to be more understandable.
            String roleName = hostRole.value;
            if (roleName.endsWith(":runConfig")) {
                // Save the drivers for last. The result stats will be ordered
                // this way and the drivers are least relevant in a run.
                // In some cases, we have 60 driver systems and we don't want
                // the user to wade through the 60 entries to find the
                // SUT components last.
                drivers.add(hostRole.name);
                continue;
            }

            StringBuilder b = new StringBuilder(roleName);
            if (roleName.endsWith("Config"))
                b.setLength(roleName.length() - "Config".length());
            b.setCharAt(0, Character.toUpperCase(b.charAt(0)));
            roleName = b.toString();

            constructHostRole(hostRole.name, roleName, cmds);

        }

        // Now process the drivers
        for (String aliasName : drivers)
            constructHostRole(aliasName, "Driver", cmds);
    }

    private void constructHostRole(String aliasName, String roleName,
                                   CmdService cmds)
            throws ConfigurationException {

        Alias a = aliasMap.get(aliasName);
        if (a == null) {

            // Map short name and domain names.
            int idx = aliasName.indexOf('.');
            if (idx > 0 && !Utilities.isIpv4Address(aliasName)) {                
                String shortName = aliasName.substring(0, idx);
                String fullName = domainMap.get(shortName);
                if (fullName == null) {
                    domainMap.put(shortName, aliasName);
                } else if (!fullName.equals(aliasName)) {
                    throw new ConfigurationException(
                            "Duplicate host names with different domains: " +
                            fullName + " and " + aliasName);
                }
            }

            // Then use the name referred in the config file for the alias.
            a = new Alias();
            a.name = aliasName;
            aliasMap.put(aliasName, a);
        }

        // Then we use this new name in our role map.
        Role r = roleMap.get(roleName);
        if (r == null) {
            r = new Role();
            r.name = roleName;
            roleMap.put(roleName, r);
        }
        a.roles.add(r);
        r.aliases.add(a);

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
        a.host.roles.add(r);
        r.hosts.add(a.host);
    }

    /**
     * Constructs the HostRoles after the benchmark run time.
     * @param fileName The file containing the stored HostRoles configuration
     * @throws IOException Error reading the file or invalid file format
     */
    public HostRoles(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        for (;;) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.startsWith("#")) // Commented line
                continue;
            StringTokenizer tk = new StringTokenizer(line, FS);
            try {
                String host = tk.nextToken();
                String alias = tk.nextToken();
                String role = tk.nextToken();

                Host h = hostMap.get(host);
                if (h == null) {
                    h = new Host();
                    h.name = host;
                    hostMap.put(host, h);
                }

                Alias a = aliasMap.get(alias);
                if (a == null) {

                    // Map short name and domain names.
                    int idx = alias.indexOf('.');
                    if (idx > 0 && !Utilities.isIpv4Address(alias)) {
                        String shortName = alias.substring(0, idx);
                        String fullName = domainMap.get(shortName);
                        if (fullName == null) {
                            domainMap.put(shortName, alias);
                        }
                    }

                    a = new Alias();
                    a.name = alias;
                    aliasMap.put(alias, a);
                }

                Role r = roleMap.get(role);
                if (r == null) {
                    r = new Role();
                    r.name = role;
                    roleMap.put(role, r);
                }

                h.aliases.add(a);
                h.roles.add(r);
                if (a.host == null)
                    a.host = h;
                a.roles.add(r);
                r.hosts.add(h);
                r.aliases.add(a);

            } catch (NoSuchElementException e) {
                throw new IOException("File " + fileName +
                                            " : Invalid format!");
            }
        }
    }

    /**
     * Writes the HostRoles out to file for later reading.
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
     * Writes the HostRoles out to an appendable for later reading.
     * @param b The appendable buffer.
     * @throws IOException A write error occurred
     */
    public void write(Appendable b) throws IOException {
        b.append("#Host\t\tAlias\t\tRole\n");
        for (Host h : hostMap.values()) {
            String host = h.name;
            for (Alias a : h.aliases) {
                String alias = a.name;
                for (Role r : a.roles) {
                    String role = r.name;
                    b.append(host).append(FS).append(alias).append(FS).
                                                append(role).append('\n');
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
     * Obtains the hosts pertaining to a certain role or function in the
     * benchmark run.
     * @param role The role name.
     * @return An array of host names used for the role in question.
     */
    public String[] getHostsByRole(String role) {
        Role r = roleMap.get(role);
        if (r == null)
            return null;
        String[] hosts = new String[r.hosts.size()];
        int i = 0;
        for (Host h : r.hosts) {
            hosts[i++] = h.name;
        }
        return hosts;
    }

    /**
     * Obtains the host aliases used for a certain role or function in the
     * benchmark run. The alias is usually an interface name used to refer to
     * a host.
     * @param role The role name.
     * @return An array of aliases used for the role in question
     */
    public String[] getAliasesByRole(String role) {
        Role r = roleMap.get(role);
        if (r == null)
            return null;
        String[] aliases = new String[r.aliases.size()];
        int i = 0;
        for (Alias a : r.aliases) {
            aliases[i++] = a.name;
        }
        return aliases;
    }

    /**
     * Obtains the function roles the host in question is used for.
     * @param host The host name
     * @return An array of role names the host is used for
     */
    public String[] getRolesByHost(String host) {
        Host h = hostMap.get(host);
        if (h == null)
            return null;
        String[] roles = new String[h.roles.size()];
        int i = 0;
        for (Role r : h.roles) {
            roles[i++] = r.name;
        }
        return roles;
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
     * @param role The role name
     * @return An array of applicable aliases
     */
    public String[] getAliasesByHostAndRole(String host, String role) {
        Host h = hostMap.get(host);
        Role r = roleMap.get(role);
        HashSet<Alias> aliasSet = new HashSet<Alias>(h.aliases);
        aliasSet.retainAll(r.aliases); // Intersect between the two sets.
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
        // Sometimes the short name without the domain is used.
        if (a == null && alias.indexOf('.') < 0) {
            String fullName = domainMap.get(alias);
            if (fullName != null)
                a = aliasMap.get(fullName);
        }
        // Sometimes this is the host name itself, not an alias
        if (a == null) {
            if (hostMap.containsKey(alias))
                return alias;
            return null;
        }
        return a.host.name;
    }

    /**
     * Obtains the roles or function an interface or alias is used for.
     * @param alias The alias name
     * @return An array of function roles the alias is used for.
     */
    public String[] getRolesByAlias(String alias) {
        String[] roles = null;
        Alias a = aliasMap.get(alias);
        // Sometimes the short name without the domain is used.
        if (a == null && alias.indexOf('.') < 0) {
            String fullName = domainMap.get(alias);
            if (fullName != null)
                a = aliasMap.get(fullName);
        }
        // Sometimes this is the host name itself, not an alias
        if (a == null) {
            Host h = hostMap.get(alias);
            if (h != null) {
                roles = new String[h.roles.size()];
                int i = 0;
                for (Role r : h.roles) {
                    roles[i++] = r.name;
                }
            }
        } else { // In the case of really an alias...
            roles = new String[a.roles.size()];
            int i = 0;
            for (Role r : a.roles) {
                roles[i++] = r.name;
            }            
        }
        return roles;
    }



    static class Role {
        public String name;
        public LinkedHashSet<Host> hosts = new LinkedHashSet<Host>();
        public LinkedHashSet<Alias> aliases = new LinkedHashSet<Alias>();

        @Override public boolean equals(Object obj) {
            boolean retVal = false;
            if (obj instanceof Role) {
                Role other = (Role) obj;
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
        public LinkedHashSet<Role> roles = new LinkedHashSet<Role>();
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
        public LinkedHashSet<Role> roles = new LinkedHashSet<Role>();

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
