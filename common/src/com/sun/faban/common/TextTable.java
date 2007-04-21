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
 * $Id: TextTable.java,v 1.1 2007/04/21 07:17:08 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

/**
 * The text table assists in formatting tabular data for text output.
 * I takes care of column alignments. The output format is compatible with
 * Xanadu's Xan format.
 * @author Akara Sucharitakul
 */
public class TextTable {

    static final String PAD = "                                              " +
                              "                                              ";
    static final String LN = "-----------------------------------------------" +
                             "-----------------------------------------------";
    static final String FS = "  ";

    StringBuilder[][] table;

    /**
     * Constructs a text table with predefined number of rows and columns.
     * @param rows The number of rows
     * @param columns The number of columns
     */
    public TextTable(int rows, int columns) {
        // keep a row for the header;
        table = new StringBuilder[++rows][columns];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < columns; j++)
                table[i][j] = new StringBuilder();
    }

    /**
     * Sets the header fields' value.
     * @param column The column to set the header
     * @param header The header
     */
    public void setHeader(int column, CharSequence header) {
        table[0][column].setLength(0);
        table[0][column].append(header);
    }

    /**
     * Sets the data fields' value.
     * @param row The row index of the data
     * @param column The column index of the data
     * @param field The data field
     */
    public void setField(int row, int column, CharSequence field) {
        table[++row][column].setLength(0);
        table[row][column].append(field);
    }

    /**
     * Obtains the field so formatters can insert values directly.
     * @param row The row index
     * @param column The column index
     * @return The StringBuilder instance representing this field.
     */
    public StringBuilder getField(int row, int column) {
        return table[++row][column];
    }

    /**
     * Formats the TextTable for output into a StringBuilder.
     * @param b The buffer to output the text
     * @return The same buffer
     */
    public StringBuilder format(StringBuilder b) {
        // Find the largest field
        // left-align first column.
        int maxWidth = 0;
        for (int i = 0; i < table.length; i++)
            if (table[i][0].length() > maxWidth)
                maxWidth = table[i][0].length();
        // Left-align first column, right-pad it.
        for (int i = 0; i < table.length; i++) {
            int padWidth = maxWidth - table[i][0].length();
            if (padWidth > 0)
                table[i][0].append(PAD, 0, padWidth);
        }


        // Find largest field in subsequent columns.
        for (int j = 1; j < table[0].length; j++) {
            maxWidth = 0;
            for (int i = 0; i < table.length; i++)
                if (table[i][j].length() > maxWidth)
                    maxWidth = table[i][j].length();
            // Right align
            for (int i = 0; i < table.length; i++ ) {
                int padWidth = maxWidth - table[i][j].length();
                if (padWidth > 0)
                    table[i][j].insert(0, PAD, 0, padWidth);
            }
        }

        // Now all fields should be just the right sicze. We can now dump
        // it out. We do the headers first.
        b.append(table[0][0]);
        for (int j = 1; j < table[0].length; j++) {
            b.append(FS).append(table[0][j]);
        }
        b.append('\n');

        // Then we do the line that separate headers.
        b.append(LN, 0, table[0][0].length());
        for (int j = 1; j < table[0].length; j++) {
            b.append(FS).append(LN, 0, table[0][j].length());
        }
        b.append('\n');

        // Then all the table data.
        for (int i = 1; i < table.length; i++) {
            b.append(table[i][0]);
            for (int j = 1; j < table[0].length; j++) {
                b.append(FS).append(table[i][j]);
            }
            b.append('\n');
        }
        b.append('\n');
        return b;
    }
}
