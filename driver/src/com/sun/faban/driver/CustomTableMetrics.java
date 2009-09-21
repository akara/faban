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
package com.sun.faban.driver;

import com.sun.faban.common.TableModel;
import java.io.Serializable;

/**
 * Benchmarks that keep track of custom metrices will need to create a
 * metrics class implementing this CustomMetrics interface. Then,
 * upon initialization it will have to attach the metrics object by calling
 * the attachMectrics(CustomMetrics) method of the driver context.
 *
 * @author Akara Sucharitakul
 * @see com.sun.faban.driver.DriverContext#attachMetrics(CustomMetrics) 
 */
public interface CustomTableMetrics extends Serializable, Cloneable {

    /**
     * Aggregates the metrics from another source or thread with
     * the current one.
     * @param other The metrics from another source
     */
    public void add(CustomTableMetrics other);

    /**
     * Obtains the results of the metrics represented by this object.
     * The results are string results to allow for proper formatting
     * as required by the implementor. There is no restriction that
     * the internal data be a String.
     * @return The result elements for each metric.
     */
    public TableModel getResults();

    /**
     * The metrics need to be cloneable and not throw any exceptions.
     * @return The CustomMetrics clone
     */
    public Object clone();
}
