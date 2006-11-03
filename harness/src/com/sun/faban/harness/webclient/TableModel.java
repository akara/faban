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
 * $Id: TableModel.java,v 1.1 2006/11/03 09:45:46 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Comparator;

/**
 * The TableModel represents a generic model for the LiveGrid data table
 * AJAX component. It can be used to represent almost any data in a table
 * form.
 *
 * @author Akara Sucharitakul
 */
public class TableModel {

    String[] headers;
    ArrayList<Comparable[]> rowList;
    int sortColumn = -1;
    SortDirection direction = SortDirection.ASCENDING;
    TreeMap<Comparable, Comparable[]> ascMap;
    TreeMap<Comparable, Comparable[]> descMap;

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
     * Obtains the number of rows currently in the table.
     * @return The number of rows in the table
     */
    public int rows() {
        return rowList.size();
    }

    /**
     * Obtains the number of columns the table is defined for.
     * @return The number of columns in the table
     */
    public int columns() {
        return headers.length;
    }

    /**
     * Obtains a column header.
     * @param column The column index
     * @return The header of the given column
     */
    public String getHeader(int column) {
        return headers[column];
    }

    /**
     * Obtains the headers of all columns.
     * @return All the column headers
     */
    public String[] getHeaders() {
        return headers;
    }

    /**
     * Sets the column header.
     * @param column The column index
     * @param header The new header to set
     */
    public void setHeader(int column, String header) {
        headers[column] = header;
    }

    /**
     * Obtains a row of data in the table.
     * @param row The row index
     * @return The row of data
     */
    public Comparable[] getRow(int row) {
        return rowList.get(row);
    }

    /**
     * Adds a new row in the table. This needs to be called before any
     * operation on the row can be done.
     * @return The row index of the row just added
     */
    public int addRow() {
        int idx = rowList.size();
        rowList.add(new Comparable[headers.length]);
        sortColumn = -1;
        return idx;
    }

    /**
     * Obtains the data from the table at a specific position.
     * @param row The row position
     * @param column The column position
     * @return The data at that position
     */
    public Comparable getField(int row, int column) {
        Comparable[] fields = rowList.get(row);
        return fields[row];
    }

    /**
     * Sorts the TableModel according to the column and direction.
     * @param columnName The name of the column matching the header
     * @param direction The direction of the sort
     */
    public void sort(String columnName, SortDirection direction) {
        int idx;
        for (idx = 0; idx < headers.length; idx++)
            if (columnName.equals(headers[idx]))
                break;

        if (idx >= headers.length)
            throw new IndexOutOfBoundsException("Field \"" + columnName +
                    "\" not found!");
        sort(idx, direction);
    }

    /**
     * Sorts the TableModel according to the column index and direction.
     * @param column The index of the column to sort by
     * @param direction The direction of the sort
     */
    public void sort(int column, SortDirection direction) {
        if (column == sortColumn) {
            if (direction != this.direction) { // Here we just swap fields
                int maxRow = rowList.size();
                int range = maxRow / 2;
                --maxRow;
                for (int i = 0; i < range; i++) {
                    Comparable[] tmpFields = rowList.get(i);
                    int j = maxRow - i;
                    rowList.set(i, rowList.get(j));
                    rowList.set(j, tmpFields);
                }
                this.direction = direction;
            }
        } else {
            // We use TreeMaps to do the sort here. If this turns out to be
            // a bottleneck, we can always switch to some other sort algorithms.
            TreeMap<Comparable, Comparable[]> sorterMap;
            if (direction == SortDirection.ASCENDING) {
                if (ascMap == null)
                    ascMap = new TreeMap<Comparable, Comparable[]>();
                else
                    ascMap.clear();
                sorterMap = ascMap;
            } else {
                if (descMap == null)
                    descMap = new TreeMap<Comparable, Comparable[]>(
                            new Comparator<Comparable>() {
                                public int compare(Comparable c, Comparable d) {
                                    return d.compareTo(c);
                                }
                            }
                    );
                else
                    descMap.clear();
                sorterMap = descMap;
            }

            for (Comparable[] fields : rowList)
                sorterMap.put(fields[column], fields);
            rowList.clear();
            rowList.addAll(sorterMap.values());
            sortColumn = column;
            this.direction = direction;
        }
    }

    /**
     * Obtains the column index the TableModel is currently sorted by.
     * @return The column index, or -1 if the model is not sorted.
     */
    public int getSortColumn() {
        return sortColumn;
    }

    /**
     * Obtains the direction the TableModel is currently sorted. If the model
     * is not sorted, this method's return value is undefined.
     * @return The direction the table is currently sorted
     */
    public SortDirection getSortDirection() {
        return direction;
    }
}
