/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.harness.webclient;

import java.util.Set;
import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author Sheetal Patil
 */
public class TagEngineTest extends TestCase {

    public TagEngineTest() {
    }

    @BeforeClass
    public void setUpClass() throws Exception {
        super.setUp();
        System.out.println("* TagEngineTest: setUp() method");
    }

    public void testEqual() {
        System.out.println("equal");
        int a = 5;
        int b = 5;
        assertEquals(a, b);
        //String tag = "Hadoop";
        //boolean result = tag.equalsIgnoreCase("hadoop");
        System.out.println("* TagEngineTest: testEqual() method");
    }

    public void testAdd() {
        String[] tagsadd1 = {"Hadoop/fs/gridmix", "Hadoop"};
        String[] tagsadd2 = {"Solaris/9", "Oracle/10", "gridmix"};
        String[] tagsadd3 = {"Solaris/10"};
        String[] tagsadd4 = {"web20/workload/sample", "web20"};
        String[] tagsadd5 = {"web20", "workload"};
        String[] tagsadd6 = {"Solaris/9", "Oracle/10"};
        Set<String> answer;
        TagEngine te = TagEngine.getInstance();
        te.add("HadoopGridMix.5G",tagsadd1);
        te.add("HadoopGridMix.5F",tagsadd2);
        te.add("HadoopGridMix.5E",tagsadd3);
        te.add("web101.5D",tagsadd4);
        te.add("web101.5C",tagsadd5);
        System.out.println("------------------------------------------------");
        System.out.println("Testing Intersection multiple tags");
        System.out.println("------------------------------------------------");
        answer = te.search("Solaris Solaris/10 Oracle/10");
        for(String ans : answer){
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
        System.out.println("Searching for solaris, single tag");
        System.out.println("------------------------------------------------");
        answer = te.search("solaris");
        for(String ans : answer){
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
        System.out.println("Searching for solaris with te.removeRunId(ans)");
        System.out.println("------------------------------------------------");
        answer = te.search("solaris");
        int i= 0;
        for(String ans : answer){
            if(i==1){
                te.removeRunId(ans);
                break;
            }
            i++;
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
        System.out.println("Searching for solaris after te.removeRunId(ans)");
        System.out.println("------------------------------------------------");
        answer = te.search("solaris");
        for(String ans : answer){
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
        System.out.println("Searching for gridmix, before removeEntry");
        System.out.println("------------------------------------------------");
        answer = te.search("gridmix");
        for(String ans : answer){
            System.out.println(ans);
        }
        te.add("HadoopGridMix.5F",tagsadd6);
        System.out.println("------------------------------------------------");
        System.out.println("Searching for gridmix, after removeEntry");
        System.out.println("------------------------------------------------");
        answer = te.search("gridmix");
        for(String ans : answer){
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        super.tearDown();
        System.out.println("* TagEngineTest: tearDown() method");
    }

}