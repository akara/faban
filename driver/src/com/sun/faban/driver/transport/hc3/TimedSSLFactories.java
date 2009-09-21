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
package com.sun.faban.driver.transport.hc3;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The catalog of the SSL factories at hand. By default, ABOVE is used
 * as it is more widely recognized. This times the traffic above the
 * SSL protocol layer. For pure, SUT only measurements,
 * you may want to consider BELOW which times the traffic after encryption
 * and thus won't include the time used for encrypting/decrypting the payload.
 * The system property faban.ssl.autotiming may be set to "above" or "below"
 * to influence the behavior.
 *
 * @author Akara Sucharitakul
 */
public enum TimedSSLFactories {

    /**
     * Attach timing above the SSL layer.
     */
    ABOVE (AboveTimedSSLSocketFactory.class),

    /**
     * Attach timing below the SSL layer.
     */
    BELOW (BelowTimedSSLSocketFactory.class);


    final Class<? extends SecureProtocolSocketFactory> factory;

    TimedSSLFactories(
            Class<? extends SecureProtocolSocketFactory> factoryClass) {
        this.factory = factoryClass;
    }

    /**
     * Obtains the currently configured factory class.
     * @return The factory class
     */
    static TimedSSLFactories getFactory() {
        String factoryStr = System.getProperty(
                "faban.ssl.autotiming", "above").toUpperCase();
        for (TimedSSLFactories factory : TimedSSLFactories.values()) {
            if (factory.name().equals(factoryStr))
                return factory;
        }
        // Default is ABOVE
        return ABOVE;
    }

    SecureProtocolSocketFactory getInstance() {
        SecureProtocolSocketFactory instance = null;
        try {
            instance = (SecureProtocolSocketFactory) factory.newInstance();
        } catch (InstantiationException e) {
            Logger.getLogger(TimedSSLFactories.class.getName()).
                    log(Level.WARNING, "Cannot instantiate " +
                    factory.getName() + '.', e);
        } catch (IllegalAccessException e) {
            Logger.getLogger(TimedSSLFactories.class.getName()).
                    log(Level.WARNING, "Access denied instantiating " +
                    factory.getName() + '.', e);
        }
        return instance;
    }
}
