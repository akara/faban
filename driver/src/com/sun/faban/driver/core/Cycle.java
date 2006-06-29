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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Cycle.java,v 1.1 2006/06/29 18:51:33 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import com.sun.faban.driver.CycleType;
import com.sun.faban.driver.DefinitionException;
import com.sun.faban.driver.util.Random;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Super class of all distributions.
 */
public abstract class Cycle implements Serializable, Cloneable {

    CycleType cycleType;
    double cycleDeviation;

    /**
     * Creates and returns a copy of this object.  The precise meaning
     * of "copy" may depend on the class of the object. The general
     * intent is that, for any object <tt>x</tt>, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be <tt>true</tt>, but these are not absolute requirements.
     * While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be <tt>true</tt>, this is not an absolute requirement.
     * <p/>
     * By convention, the returned object should be obtained by calling
     * <tt>super.clone</tt>.  If a class and all of its superclasses (except
     * <tt>Object</tt>) obey this convention, it will be the case that
     * <tt>x.clone().getClass() == x.getClass()</tt>.
     * <p/>
     * By convention, the object returned by this method should be independent
     * of this object (which is being cloned).  To achieve this independence,
     * it may be necessary to modify one or more fields of the object returned
     * by <tt>super.clone</tt> before returning it.  Typically, this means
     * copying any mutable objects that comprise the internal "deep structure"
     * of the object being cloned and replacing the references to these
     * objects with references to the copies.  If a class contains only
     * primitive fields or references to immutable objects, then it is usually
     * the case that no fields in the object returned by <tt>super.clone</tt>
     * need to be modified.
     * <p/>
     * The method <tt>clone</tt> for class <tt>Object</tt> performs a
     * specific cloning operation. First, if the class of this object does
     * not implement the interface <tt>Cloneable</tt>, then a
     * <tt>CloneNotSupportedException</tt> is thrown. Note that all arrays
     * are considered to implement the interface <tt>Cloneable</tt>.
     * Otherwise, this method creates a new instance of the class of this
     * object and initializes all its fields with exactly the contents of
     * the corresponding fields of this object, as if by assignment; the
     * contents of the fields are not themselves cloned. Thus, this method
     * performs a "shallow copy" of this object, not a "deep copy" operation.
     * <p/>
     * The class <tt>Object</tt> does not itself implement the interface
     * <tt>Cloneable</tt>, so calling the <tt>clone</tt> method on an object
     * whose class is <tt>Object</tt> will result in throwing an
     * exception at run time.
     *
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not
     *                                    support the <code>Cloneable</code> interface. Subclasses
     *                                    that override the <code>clone</code> method can also
     *                                    throw this exception to indicate that an instance cannot
     *                                    be cloned.
     * @see Cloneable
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    
    static void setCycles(BenchmarkDefinition.Operation[] operations,
                          Class<?> driverClass) throws DefinitionException {
        Cycle classCycle = null;
        Annotation[] annotations = driverClass.getAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            String typeName = annotations[i].annotationType().getName();
            int cnBegin = typeName.lastIndexOf('.');
            ++cnBegin;
            String annotationName = typeName.substring(cnBegin);
            String pkgName = typeName.substring(0, cnBegin);
            String cycleName = pkgName + "core." + annotationName;
            Cycle cycle = null;
            try {
                cycle = Class.forName(cycleName).
                        asSubclass(Cycle.class).newInstance();
            } catch (Exception e) {
                // If the annotation is not a cycle, we just ignore it here.
                // It is the responsibility of the corresponding facility to
                // pick it up.
                continue;
            }
            if (classCycle != null)
                throw new DefinitionException("Duplicate class cycle " +
                        "annotation @" + annotationName);
            cycle.init(annotations[i]);
            classCycle = cycle;
        }

        // Now we make the same set of tests at the operation level
        for (BenchmarkDefinition.Operation o : operations) {

            annotations = o.m.getAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                String typeName = annotations[i].annotationType().getName();
                int cnBegin = typeName.lastIndexOf('.');
                ++cnBegin;
                String annotationName = typeName.substring(cnBegin);
                String pkgName = typeName.substring(0, cnBegin);
                String cycleName = pkgName + "core." + annotationName;
                Cycle cycle = null;
                try {
                    cycle = Class.forName(cycleName).
                            asSubclass(Cycle.class).newInstance();
                } catch (Exception e) {
                    // If the annotation is not a cycle, we just ignore it here.
                    // It is the responsibility of the corresponding facility to
                    // pick it up.
                    continue;
                }
                if (o.cycle != null)
                    throw new DefinitionException("Duplicate operation cycle " +
                            "annotation for operation " +
                            o.name + " @" + annotationName);
                cycle.init(annotations[i]);
                o.cycle = cycle;
            }

            // Finally, we need to test for no cycle at all and handle the case
            if (o.cycle == null)
                o.cycle = classCycle;

            if (o.cycle == null)
                throw new DefinitionException("No cycle distribution " +
                        "annotation for operation " + o.name);
        }
    }

     /**
     * Initializes this cycle according to the annotation.
     * @param a The annotation
     * @throws DefinitionException If there is an error in the annotation
     */
    public abstract void init(Annotation a) throws DefinitionException;

    /**
     * Randoms/calculates the delay time for a thread based on its
     * supplied random number generator and the actual conditions in the
     * distribution.
     *
     * @param random        The random number generator used
     * @return The delay time
     */
    public abstract int getDelay(Random random);

    /**
     * Provides the maximum value to be represented inside a histogram.
     * @return The max reasonable delay to be presented in the output histogram.
     */
    public abstract double getHistogramMax();
}