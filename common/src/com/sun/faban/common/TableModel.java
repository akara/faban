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
 * $Id: TableModel.java,v 1.1 2009/05/17 19:56:21 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.util.ArrayList;

/**
 *
 * @author akara
 */
public class TableModel {
    String[] headers;
    ArrayList<Comparable[]> rowList;

    /**
     * Constructs a TableModel.
     * @param columns The number of columns in the table
     */
    public TableModel(int columns) {
        this(columns, 0);
    }

    /**
     * Constructs a TableModel.
     * @param columns The number of columns in the table
     * @param rowInitCapacity The initial row capacity of the table
     */
    public TableModel(int columns, int rowInitCapacity) {
        headers = new String[columns];
        if (rowInitCapacity > 0)
            rowList = new ArrayList<Comparable[]>(rowInitCapacity);
        else
            rowList = new ArrayList<Comparable[]>();
    }
    /**
     * Obtains the number of columns the table is defined for.
     * @return The number of columns in the table
     */
    public int columns() {
        return headers.length;
    }

    public Comparable getField(int row, int column) {
        Comparable[] fields = rowList.get(row);
        return fields[row];
    }

    public String getHeader(int column) {
        return headers[column];
    }

    public String[] getHeaders() {
        return headers;
    }

    public Comparable[] getRow(int row) {
        return rowList.get(row);
    }

    public Comparable[] newRow() {
        Comparable[] row = new Comparable[headers.length];
        rowList.add(row);
        return row;
    }

    /**
     * Obtains the number of rows currently in the table.
     * @return The number of rows in the table
     */
    public int rows() {
        return rowList.size();
    }

    public void setHeader(int column, String header) {
        headers[column] = header;
    }

}
