package com.sun.faban.harness;

import java.io.File;
import java.util.regex.Pattern;

/**
 * A file name filter that takes wildcards characters '*' and '?' and
 * interprets them like a shell. For example, "*.exe" will match all
 * files starting ending with .exe extension.
 */
public class WildcardFileFilter implements FileFilter {

    Pattern pattern;
    /**
     * Constructs a wildcard file filter with the given pattern.
     * @param pattern The file name pattern
     */
    public WildcardFileFilter(String pattern) {
        // Change the pattern to a regex.
        char[] src = pattern.toCharArray();
        char[] dest = new char[2 * src.length];
        int idx = 0;
        for (char c : src)
            switch (c) {
                case '.' : dest[idx++] = '\\';
                           dest[idx++] = c; break;
                case '*' :
                case '?' : dest[idx++] = '.';
                default  : dest[idx++] = c;
            }
        this.pattern = Pattern.compile(new String(dest, 0, idx));
    }

    /**
     * Tests whether or not the specified abstract pathname should be
     * included in a pathname list.
     *
     * @param pathname The abstract pathname to be tested
     * @return <code>true</code> if and only if <code>pathname</code>
     *         should be included
     */
    public boolean accept(File pathname) {
        return pattern.matcher(pathname.getName()).matches();
    }
}
