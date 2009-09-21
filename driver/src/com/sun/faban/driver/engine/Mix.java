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
package com.sun.faban.driver.engine;

import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.DefinitionException;
import com.sun.faban.driver.util.Random;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;

/**
 * Superclass of all mixes.
 * @author Akara Sucharitakul
 */
public abstract class Mix implements Serializable, Cloneable {

    BenchmarkDefinition.Operation[] operations;
    double deviation;

    /** The actual class name of the implementing subclass. */
    protected String className;
    private transient Logger logger;

    /**
     * Factory for obtaining the correct mix from the driver class.
     * This factory has the knowledge about each mix and how to
     * instantiate it.
     * @param driverClass The originating driver class
     * @return An instance of the appropriate mix subclass
     * @throws DefinitionException If there is an error in the definition
     */
    public static Mix getMix(Class<?> driverClass) throws DefinitionException {
        Mix returnMix = null;
        Annotation[] annotations = driverClass.getAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            String typeName = annotations[i].annotationType().getName();
            int cnBegin = typeName.lastIndexOf('.');
            ++cnBegin;
            String annotationName = typeName.substring(cnBegin);
            String pkgName = typeName.substring(0, cnBegin);
            String mixName = pkgName + "engine." + annotationName;
            Mix mix = null;
            try {
                mix = Class.forName(mixName).
                        asSubclass(Mix.class).newInstance();
            } catch (Exception e) {
                // If the annotation is not a mix, we just ignore it here.
                // It is the responsibility of the corresponding facility to
                // pick it up.
                continue;
            }
            if (returnMix != null) {
				throw new DefinitionException("Duplicate mix annotation @" +
                        annotationName);
			}
            mix.init(driverClass, annotations[i]);
            returnMix = mix;
        }

        // Now handle the default where no mix is specified
        // The default is a flat mix with all operations equal
        if (returnMix == null) {
            FlatMix mix = new FlatMix();
            mix.init(driverClass);
            returnMix = mix;
        }

        returnMix.normalize();
        return returnMix;
    }

    /**
     * Protected constructor ensures Mix cannot be constructed explicitly.
     */
    protected Mix() {
        className = getClass().getName();
    }

    /**
     * Instead of accessing the logger directly, we'll have to
     * access it through this method all the time as serialization
     * may cause the logger to become null.
     * @return The logger for this class
     */
    protected Logger getLogger() {
        if (logger == null) {
			logger = Logger.getLogger(className);
		}
        return logger;
    }

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
     * @see Cloneable
     */
    @Override
	public Object clone() {
        Mix clone = null;
        try {
            clone = (Mix) super.clone();
            for (int i = 0; i < operations.length; i++) {
				clone.operations[i] = (BenchmarkDefinition.Operation)
                        operations[i].clone();
			}
        } catch (CloneNotSupportedException e) {
            // Noop. This is not possible as we are implementing cloneable.
        }
        return clone;
    }

    /**
     * Initializes this mix according to the annotation.
     * @param driverClass The driver class annotating this mix
     * @param a The annotation
     * @throws DefinitionException If there is an error in the annotation
     */
    public abstract void init(Class<?> driverClass, Annotation a)
            throws DefinitionException;

    /**
     * Configures/overrides the mix from the driverConfig DOM node
     * read from the configuration file.
     *
     * @param driverConfigNode The driverConfig DOM node
     * @throws ConfigurationException If there is a configuration error
     */
    public abstract void configure(Element driverConfigNode)
            throws ConfigurationException;

    /**
     * Normalizes the mix so it is ready for use in selections.
     * This normalization adjusts each mix to be between 0.0d and 1.0d
     * and the sum of each selectable mix to be 1.0d
     */
    public abstract void normalize();

    /**
     * Returns the flat mix representation of this mix. This is used for
     * consistent reporting.
     * @return The flat mix representation.
     */
    public abstract FlatMix flatMix();

    /**
     * Obtains the per-thread and per-driver instance selector.
     * @param random The per-thread random value generator
     * @return The selector to be used by the driver
     */
    public abstract Selector selector(Random random);

    /**
     * The per-thread selector is used in the AgentThread for selection of
     * the operation.
     */
    public abstract static class Selector {
        
        /**
         * The select method selects the operation to run next.
         * @return The operation index selected to run next
         */
        public abstract int select();

        /**
         * Resets the selector's state to start at the first op,
         * if applicable.
         */
        public abstract void reset();
    }
}
