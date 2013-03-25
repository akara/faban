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

import java.io.IOException;

/**
 * The text table assists in formatting tabular data for text output.
 * I takes care of column alignments. The output format is compatible with
 * FenXi's Xan format.
 *
 * @author Akara Sucharitakul
 */
public class TextTable {

    static final String PAD =
            "                                                                " +
                    "                                                                " +
                    "                                                                " +
                    "                                                                ";

    static final String LN =
            "----------------------------------------------------------------" +
                    "----------------------------------------------------------------" +
                    "----------------------------------------------------------------" +
                    "----------------------------------------------------------------";

    static final String FS = "  ";

    static final String NULL_FIELD = "NULL";

    CharSequence[][] table;

    /**
     * Constructs a text table with predefined number of rows and columns.
     *
     * @param rows    The number of rows
     * @param columns The number of columns
     */
    public TextTable(int rows, int columns) {
        // keep a row for the header;
        table = new CharSequence[++rows][columns];
    }

    /**
     * Sets the header fields' value.
     *
     * @param column The column to set the header
     * @param header The header
     */
    public void setHeader(int column, CharSequence header) {
        table[0][column] = header;
    }

    /**
     * Gets the header field's value.
     *
     * @param column The colund to get the header
     * @return The current field value
     */
    public CharSequence getHeader(int column) {
        return table[0][column];
    }

    /**
     * Sets the data fields' value.
     *
     * @param row    The row index of the data
     * @param column The column index of the data
     * @param field  The data field
     */
    public void setField(int row, int column, CharSequence field) {
        if (field == null || field.length() == 0)
            field = "--";
        table[++row][column] = field;
    }

    /**
     * Obtains the field so formatters can insert values directly.
     *
     * @param row    The row index
     * @param column The column index
     * @return The CharSequence representing this field.
     */
    public CharSequence getField(int row, int column) {
        return table[++row][column];
    }

    private void _format(Appendable b) throws IOException {

        // The size of each column
        short[] colSize = new short[table[0].length];

        // Find the largest field
        // left-align first column.
        int maxWidth = 0;
        for (int i = 0; i < table.length; i++) {
            if (table[i][0] == null)
                table[i][0] = NULL_FIELD;
            if (table[i][0].length() > maxWidth)
                maxWidth = table[i][0].length();
        }
        colSize[0] = (short) maxWidth;

        // Find largest field in subsequent columns.
        for (int j = 1; j < table[0].length; j++) {
            maxWidth = 0;
            for (int i = 0; i < table.length; i++) {
                if (table[i][j] == null)
                    table[i][j] = NULL_FIELD;
                if (table[i][j].length() > maxWidth)
                    maxWidth = table[i][j].length();
            }
            colSize[j] = (short) maxWidth;
        }

        // Now we have the size of all columns. We can now dump
        // it out. We do the headers first, appending first column and
        // prepending the others.
        b.append(table[0][0]).append(PAD, 0, colSize[0] - table[0][0].length());
        for (int j = 1; j < table[0].length; j++) {
            b.append(FS).append(PAD, 0, colSize[j] - table[0][j].length()).
                    append(table[0][j]);
        }
        b.append('\n');

        // Then we do the line that separate headers.
        b.append(LN, 0, colSize[0]);
        for (int j = 1; j < colSize.length; j++) {
            b.append(FS).append(LN, 0, colSize[j]);
        }
        b.append('\n');

        // Then all the table data.
        for (int i = 1; i < table.length; i++) {
            b.append(table[i][0]).
                    append(PAD, 0, colSize[0] - table[i][0].length());
            for (int j = 1; j < table[0].length; j++) {
                b.append(FS).append(PAD, 0, colSize[j] - table[i][j].length()).
                        append(table[i][j]);
            }
            b.append('\n');
        }
        b.append('\n');
    }

    /**
     * StringBuilder version of format. Same as format(Appendable) but
     * does not throw exceptions.
     *
     * @param b The StringBuilder to append to
     * @return The same StringBuilder
     */
    public StringBuilder format(StringBuilder b) {
        try {
            _format(b);
        } catch (IOException e) {
            // Never happens.
        }
        return b;
    }

    /**
     * StringBuffer version of format. Same as format(Appendable) but
     * does not throw exceptions.
     *
     * @param b The StringBuffer to append to
     * @return The same StringBuffer
     */
    public StringBuffer format(StringBuffer b) {
        try {
            _format(b);
        } catch (IOException e) {
            // Never happens.
        }
        return b;
    }

    /**
     * Formats the TextTable and outputs to an Appendable.
     *
     * @param a The Appendable to output the text
     * @return The same Appendable
     * @throws IOException Problems writing to the Appendable
     */
    public Appendable format(Appendable a) throws IOException {
        _format(a);
        return a;
    }

    /**
     * Returns the formatted table in a string.
     *
     * @return a formatted string representation of the table.
     */
    public String toString() {
        StringBuilder b = new StringBuilder(8192);
        return format(b).toString();
    }
}