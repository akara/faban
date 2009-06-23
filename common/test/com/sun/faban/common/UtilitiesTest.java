/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.common;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
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
}