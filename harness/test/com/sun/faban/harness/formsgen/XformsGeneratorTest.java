/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.harness.formsgen;

import java.io.File;
import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *
 * @author sp208304
 */
public class XformsGeneratorTest extends TestCase{
    private static XformsGenerator xg;
    public XformsGeneratorTest() {
    }

    @BeforeClass
    public void setUpClass() throws Exception {
        super.setUp();
        System.out.println("* XformsGeneratorTest: setUp() method");
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        super.tearDown();
        System.out.println("* XformsGeneratorTest: tearDown() method");
    }

    /**
     * Test of main method, of class XformsGenerator.
     */
    public static void testGenerate() {
        System.out.println("generate");
        File infile = new File("run.xml");
        File outfile = new File("config.xhtml");
        File templateFile = new File("resources/config-template.xhtml");
        xg = new XformsGenerator();
        xg.generate(infile,outfile,templateFile);
    }
}