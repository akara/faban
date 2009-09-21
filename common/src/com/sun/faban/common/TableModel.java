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
package com.sun.faban.common;

import java.util.ArrayList;

/**
 * A table model used for representing table results both in the Faban results
 * and in the web interface.
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

    /**
     * Obtains the field at the given row and column index.
     * @param row The row index
     * @param column The column index
     * @return The field at the given location
     */
    public Comparable getField(int row, int column) {
        Comparable[] fields = rowList.get(row);
        return fields[row];
    }

    /**
     * Obtains the header for a given column.
     * @param column The column to get the header
     * @return The header at that column
     */
    public String getHeader(int column) {
        return headers[column];
    }

    /**
     * Obtains all column headers.
     * @return All column headers, as an array
     */
    public String[] getHeaders() {
        return headers;
    }

    /**
     * Ontains all fields in a given row.
     * @param row The row
     * @return The fields, as an array
     */
    public Comparable[] getRow(int row) {
        return rowList.get(row);
    }

    /**
     * Adds a new row to the table model.
     * @return The modifyable array representing the row
     */
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

    /**
     * Sets the header for a given column.
     * @param column The column
     * @param header The header
     */
    public void setHeader(int column, String header) {
        headers[column] = header;
    }

}
