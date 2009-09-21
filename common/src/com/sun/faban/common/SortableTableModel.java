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
import java.util.TreeMap;
import java.util.Comparator;

/**
 * The TableModel represents a generic model for the LiveGrid data table
 * AJAX component. It can be used to represent almost any data in a table
 * form.
 *
 * @author Akara Sucharitakul
 */
public class SortableTableModel extends TableModel {
    int sortColumn = -1;
    SortDirection direction = SortDirection.ASCENDING;
    TreeMap<Comparable, ArrayList<Comparable[]>> ascMap;
    TreeMap<Comparable, ArrayList<Comparable[]>> descMap;

    /**
     * Constructs a TableModel.
     * @param columns The number of columns in the table
     */
    public SortableTableModel(int columns) {
        super(columns, 0);
    }

    /**
     * Constructs a TableModel.
     * @param columns The number of columns in the table
     * @param rowInitCapacity The initial row capacity of the table
     */
    public SortableTableModel(int columns, int rowInitCapacity) {
        super(columns, rowInitCapacity);
    }

    /**
     * Sorts the TableModel according to the column and direction.
     * @param columnName The name of the column matching the header
     * @param direction The direction of the sort
     */
    public void sort(String columnName, SortDirection direction) {
        int idx;
        for (idx = 0; idx < headers.length; idx++)
            if (columnName.equalsIgnoreCase(headers[idx]))
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
            TreeMap<Comparable, ArrayList<Comparable[]>> sorterMap;
            if (direction == SortDirection.ASCENDING) {
                if (ascMap == null)
                    ascMap = new TreeMap<Comparable, ArrayList<Comparable[]>>();
                else
                    ascMap.clear();
                sorterMap = ascMap;
            } else {
                if (descMap == null)
                    descMap = new TreeMap<Comparable, ArrayList<Comparable[]>>(
                            new Comparator<Comparable>() {

                                // We intentionally deal with Comparable and not
                                // Comparable<T> to allow maximum type
                                // flexibility.
                                @SuppressWarnings("unchecked")
                                public int compare(Comparable c, Comparable d) {
                                    return d.compareTo(c);
                                }
                            }
                    );
                else
                    descMap.clear();
                sorterMap = descMap;
            }

            // Each sort key is allowed to have multiple occurences. So we
            // need the chain for all other instances. The order of the rows
            // in the chain conform to the order of the previous sort, if any,
            // or the order the rows are previously in the table.
            for (Comparable[] fields : rowList) {
                ArrayList<Comparable[]> chain = sorterMap.get(fields[column]);
                if (chain == null) {
                    chain = new ArrayList<Comparable[]>();
                    sorterMap.put(fields[column], chain);
                }
                chain.add(fields);
            }
            rowList.clear();
            for (ArrayList<Comparable[]> chain : sorterMap.values())
                rowList.addAll(chain);

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
