/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.common;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Utilities class.
 * @author akara
 */
public class UtilitiesTest {

    /**
     * Test of escapeXML method, of class Utilities.
     */
    @Test
    public void testEscapeXML_String() {
        System.out.println("escapeXML");
        String test = "This is a test with common xml escapes: &, >, <, ', \"";
        String output = Utilities.escapeXML(test);
        int ampIdx = output.indexOf('&');
        int ampIdx2 = output.indexOf('&', ampIdx + 1);
        assertFalse(ampIdx2 == -1);
        System.out.println(output);

        test = "This test is pre-escaped: &amp;, &gt;, &lt;, &apos;, &quot;";
        output = Utilities.escapeXML(test);
        assertTrue(test.equals(output));
        System.out.println(output);
    }

    /**
     * Test of isIpv4Address method, of class Utilities.
     */
    @Test
    public void testIsIpv4Address() {
        System.out.println("isIpv4Address");
        assertTrue(Utilities.isIpv4Address("10.0.0.2"));
        assertTrue(Utilities.isIpv4Address("129.10.12.51"));
        assertTrue(Utilities.isIpv4Address("192.168.0.1"));
        assertTrue(Utilities.isIpv4Address("224.224.224.224"));
        assertTrue(Utilities.isIpv4Address("255.255.255.255"));
        assertFalse(Utilities.isIpv4Address("256.255.255.255"));
        assertFalse(Utilities.isIpv4Address("255.255.25b.255"));
        assertFalse(Utilities.isIpv4Address("192.168.0"));
        assertFalse(Utilities.isIpv4Address("192.168.0.15.2"));
    }
}