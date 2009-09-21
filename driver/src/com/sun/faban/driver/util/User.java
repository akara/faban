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
package com.sun.faban.driver.util;

import java.util.HashSet;

/**
 * Username generator. This is still experimental.
 *
 * @author Akara Sucharitakul
 */
public class User {

    private static final char[] alpha =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
         'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
         'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
         'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
         'u', 'v', 'w', 'x', 'y', 'z'};
    private static final char[] characs =
        {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
         'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    private static final char[][][] scramble = {{{'0'}}, {{'0'}},
    {{'s', 'k', 'u', 'p', 'n', 't', 'j', 'z', 'b', 'g', 'q', 'l', 'w', 'd',
    'v', 'y', 'm', 'f', 'x', 'i', 'c', 'h', 'r', 'o', 'e', 'a'},
    {'w', '_', 'q', 'l', 'z', 'h', 'j', 'k', '1', '6', 'e', '5', '4', 't', 'b',
    '2', 'p', 'r', 'g', 'm', '8', '3', 'c', 'u', '7', 'v', 'i', 'y', 'o', 'a',
    'x', 'n', '0', '9', 's', 'd', 'f'},
    {'8', '7', '2', 'm', 'l', 's', 'q', '6', '1', 'j', 'b', 'n', '3', 'i', '_',
    'w', 'a', 'z', '4', 'x', 'e', 'f', '9', 'c', 'g', 't', 'h', '0', 'd', 'y',
    'k', 'v', 'u', 'p', 'r', '5', 'o'}},
    {{'q', 'c', 'r', 'p', 'm', 'o', 'n', 'y', 't', 'x', 'a', 'k', 'l', 's',
    'e', 'g', 'z', 'h', 'w', 'j', 'v', 'u', 'b', 'f', 'i', 'd'},
    {'0', 'g', 'l', '8', 'f', 'p', 'x', 'q', 'm', '6', 's', 'y', 'c', '2', '7',
    'e', '4', '5', 'v', 'b', 'z', 'i', 'd', 'h', '1', 'n', '3', '9', 'w', 'o',
    '_', 'r', 'j', 't', 'k', 'u', 'a'},
    {'5', 'e', '0', '4', 'j', 'd', 'a', 'b', 'g', 'w', 'n', 'v', '3', 'u', '9',
    'x', 'i', 'c', 'h', 'q', 'y', 'r', '_', 'm', 't', '6', '8', '2', 'l', 'k',
    'p', '7', 's', 'o', 'z', 'f', '1'},
    {'r', 'v', 'g', 'a', '0', 'w', 't', '7', '5', '_', 'o', '6', 'f', 'z', 'j',
    '2', 'c', 'e', '4', '1', 's', 'y', 'p', '8', 'n', 'u', '3', 'l', 'k', 'h',
    'd', 'b', 'q', 'x', 'i', '9', 'm'}},
    {{'p', 's', 'c', 'o', 'r', 't', 'u', 'i', 'k', 'q', 'v', 'e', 'f', 'x',
    'n', 'j', 'y', 'w', 'z', 'd', 'a', 'b', 'g', 'm', 'l', 'h'},
    {'i', 'f', 'w', '0', '5', 'p', '7', 'o', '6', 'z', '8', 'h', 't', '2', '4',
    'u', 'a', 'x', 'r', 's', '9', 'v', 'k', 'j', 'e', 'g', '3', 'q', 'm', '1',
    'l', 'y', 'd', 'n', '_', 'c', 'b'},
    {'n', 'z', 'q', 'd', '8', '1', '5', 'o', 'b', 'u', 'e', 'a', 'c', '2', 'y',
    'w', 'k', '9', 'i', '7', 'h', '_', '3', '0', 'j', 'g', 'l', 'm', 'p', 's',
    'v', 't', '4', 'x', '6', 'f', 'r'},
    {'f', 'd', '5', 't', 'j', 'e', 'h', 'q', 'u', 'c', 'b', 'w', 'l', 'k', 'a',
    'n', 'r', 'm', '1', 'o', '_', 'y', '6', 's', '7', 'g', 'i', '2', '0', 'p',
    'x', 'v', '3', '4', 'z', '9', '8'},
    {'4', 'm', 'q', 'o', '9', 'v', 'z', 'w', '7', '6', '5', '8', 'f', 'g', 'd',
    'a', 'p', 'j', '1', '_', 'y', '3', 'h', 'r', 't', 'c', 'n', 'l', '0', 'e',
    'k', 'b', 'x', 'i', '2', 's', 'u'}},
    {{'r', 'b', 'd', 'm', 'f', 't', 'x', 'e', 'i', 'o', 's', 'p', 'a', 'l',
    'g', 'h', 'n', 'w', 'z', 'q', 'u', 'v', 'k', 'j', 'c', 'y'},
    {'h', 'i', 'b', 'f', '2', 'o', 'd', 'u', '7', '9', 'w', 'v', 'j', '3', '6',
    'g', 'z', 'p', 'n', '8', 'y', 'k', 'x', 'q', '5', 's', 't', 'a', '1', '4',
    '0', 'e', 'c', 'm', 'r', 'l', '_'},
    {'y', '3', 'w', 'h', 'v', 'u', 'e', 'q', 'm', 'z', '9', 'x', 'k', '7', 'p',
    'r', 't', 'n', '4', 'f', 'o', '2', '5', 'i', 'l', 'a', '6', '8', 'g', '1',
    '_', 'b', 'd', '0', 's', 'j', 'c'},
    {'6', 'c', 'f', '3', '2', 'v', 'm', 'l', 'x', 'k', 'e', '_', '7', 'a', 's',
    '0', 'j', 'n', 'd', 'z', 'u', '4', 't', 'o', 'g', '5', 'w', 'h', 'p', 'b',
    'i', '1', 'q', '8', 'r', 'y', '9'},
    {'i', 'e', 'p', 'g', 'h', 'b', '1', 'a', 'x', 'm', 'o', '7', 'l', 'u', 'z',
    'w', '0', '8', '9', 'd', 'f', '2', '_', '5', 'c', 'n', 'r', '6', 'j', 'v',
    'q', 't', 's', '4', 'k', '3', 'y'},
    {'t', 'n', 'v', 'g', 's', 'j', 'p', 'l', 'b', '8', 'd', '_', 'q', 'u', 'z',
    '2', '5', '7', 'x', 'i', 'o', 'r', '9', '0', 'f', 'w', 'k', '6', '4', '3',
    'e', 'c', 'a', 'h', 'm', '1', 'y'}},
    {{'q', 'o', 'x', 'j', 'g', 'r', 'k', 'p', 'a', 'e', 'i', 'w', 'u', 'n',
    's', 'f', 'c', 'b', 'z', 'y', 't', 'v', 'm', 'h', 'l', 'd'},
    {'z', 'g', 'm', '7', 'r', 'l', 'o', 'q', 't', '0', '9', 'b', 'w', '3', '2',
    'y', '1', 'e', 'p', 's', '6', 'x', '5', 'v', 'i', 'n', '_', '4', 'a', 'k',
    'u', 'f', 'c', 'd', 'h', 'j', '8'},
    {'1', '9', 'c', 't', 'p', 'm', 'e', '5', 'f', 'y', 'r', 'g', 'w', 'j', 'i',
    'x', '3', 'u', '8', '6', 'd', 'k', 's', '4', 'b', 'l', 'h', 'q', 'n', '7',
    '2', 'z', 'a', 'o', '0', 'v', '_'},
    {'g', 'v', 'r', 'l', 'h', 'a', '4', '0', 'k', '_', '2', 'j', 'b', 't', 'p',
    'i', 'z', '5', '7', 'm', '3', '1', 'w', 'e', '6', 'u', 'f', 'y', '9', 'n',
    'x', 'o', 'c', 'd', 's', 'q', '8'},
    {'q', 'u', '0', '4', 'f', 'j', 'r', 'w', '8', '9', 't', 'k', 'h', '2', 'i',
    'b', 'n', 'g', 'z', 'x', '3', 'd', 'e', 'a', 's', '6', '1', '5', 'v', 'o',
    '_', 'l', 'm', 'y', 'c', 'p', '7'},
    {'b', '7', 'x', 'r', 'a', '8', 'z', 'm', 'q', 'i', 't', 'v', 'c', 'd', '6',
    'j', 'k', 'y', '0', '9', 'f', '3', '2', 'h', 's', 'w', 'l', '1', 'u', '_',
    'g', 'o', 'e', 'n', 'p', '5', '4'},
    {'v', 'j', '1', 'n', '8', 's', '_', 'q', 'u', 'e', '3', 'c', 'o', '9', 'm',
    'h', 't', '0', 'f', '4', 'd', 'r', '5', 'x', '2', '6', 'w', 'a', 'i', 'z',
    '7', 'b', 'l', 'k', 'g', 'y', 'p'}},
    {{'o', 'g', 'l', 'k', 'e', 'q', 'r', 'p', 't', 'w', 'u', 'h', 'j', 'a',
    'i', 'v', 'd', 'y', 'z', 'b', 'c', 'm', 'x', 'n', 'f', 's'},
    {'3', 'y', 'f', 't', '6', 'q', 'z', 'r', 'b', '1', 'j', '0', '7', '2', '_',
    'a', 'g', '9', '4', 'l', 'v', 'd', 'c', 'm', 'o', 'i', 'k', '8', '5', 'x',
    'w', 'n', 'h', 'u', 'p', 'e', 's'},
    {'d', 'v', 'l', 'b', 'j', '5', 'y', '8', 'o', 'p', '0', 'q', 'x', 'u', 's',
    'w', '7', '3', 'h', 't', '_', 'n', '2', 'c', 'm', 'r', '6', 'g', 'k', 'z',
    'e', '1', 'f', '9', '4', 'i', 'a'},
    {'a', 'f', 'y', 'o', 'w', 'z', 'b', 'i', 'd', '7', '_', 'm', 's', 'p', '1',
    '4', 'x', 'l', 'r', '9', 'j', 'q', 'k', 'v', '6', 'g', '3', 't', 'h', 'e',
    '2', '0', 'n', '5', '8', 'u', 'c'},
    {'r', 'c', 'q', 'x', '1', 'a', 'u', '7', 'k', '8', 'p', '0', '9', 'f', 'j',
    'n', 'b', '3', 'z', 'o', '_', 'd', 'v', '4', 'g', 'i', 's', 'e', 'w', 'y',
    '2', 'm', 't', '5', 'h', '6', 'l'},
    {'s', '_', '2', 'v', 'z', 'f', '1', '7', 'k', 'o', 'd', '6', '8', 'j', 'i',
    'q', '0', 'x', 'a', 'm', 'r', 't', 'w', 'h', 'y', 'n', 'l', 'u', 'c', 'p',
    'g', '4', '9', '3', '5', 'e', 'b'},
    {'u', '5', 'h', 'x', 'y', 'a', '4', '8', 'z', 'i', 'g', 's', '2', 'n', 'p',
    'b', 'q', 'o', '6', '1', '0', 'w', 'e', '3', '_', 'j', 'v', '9', 'k', 'm',
    'd', 'r', 't', '7', 'f', 'l', 'c'},
    {'t', 'z', 'n', 'y', '6', 'm', 'i', 'w', 'c', '2', 'f', 'q', 'e', 'h', '_',
    'v', 'j', '9', '0', '8', 's', '5', 'g', 'd', 'p', 'l', '4', 'u', 'o', '1',
    'k', '3', 'r', 'b', 'a', 'x', '7'}}};


    // Note that these sum up to 100
    private static final int[] length_percent = { 0, 0, 5, 8, 17, 25, 24, 21 };

    private static int[] selector = new int[length_percent.length];

    static {
        selector[0] = length_percent[0];
        for (int i = 1; i < selector.length; i++)
            selector[i] = selector[i - 1] + length_percent[i];
    }

    /**
     * Creates a unique user name from an id.
     * @param id The id
     * @return A unique user name
     */
    public static String getUserName(long id) {

        // Since id starts with 1, we have to shift it to start with 0 for
        // our operations.
        --id;

        // We divide the ids into sets, each set has 100 employees.
        int setId = (int) (id / 100);

        // Then we obtain the per-set id 0..99
        int psid = (int) (id % 100);

        // For selection, we do not want to make the same name lengths
        // contigous. So we switch the digits on psid.
        psid = (psid % 10) * 10 + (psid / 10);

        // Even then, the shorter names tend to clutter in the lower range
        // close to 1 and the longer in the upper close to 100. Distributing
        // odd and even numbers will correct this tendency.
        if (psid % 2 == 0)
            psid = 99 - psid;

        // This outcoming psid is used for digit selection.

        // Next, choose the length.
        int lengthSequence = 0; // This is the sequence number for the psid
                                // having this length within these 100 names.
        int len; // For now, pretend 0 is OK, but we'll shift is back to 1.
        for (len = 0; len < selector.length; len++) {
            if (psid < selector[len]) {
                if (len == 0)
                    lengthSequence = psid;
                else
                    lengthSequence = psid - selector[len - 1];
                break;
            }
        }
        // Here we shift it back so len is from 1 to whatever.
        ++len;

        // Now as we know the id, psid, and the name length to use,
        // we have to generate the name.
        char[] name = new char[len];
        int[] offset = new int[len];

        // The lengthId is the unique identifier for this length and is the
        // value we use to get the name.
        int lengthId = length_percent[len - 1] * setId + lengthSequence;

        for (int i = 0; i < len; i++) {
            offset[i] = lengthId % scramble[len - 1][i].length;
            lengthId /= scramble[len - 1][i].length;
        }

        name[0] = scramble[len - 1][0][offset[0]];

        for (int i = 1; i < len; i++) {
            offset[i] = (offset[i] + offset[i - 1]) %
                    scramble[len - 1][i].length;
            name[i] = scramble[len - 1][i][offset[i]];
        }

        return new String(name);
    }

    /**
     * Test code for the user name.
     * @param args The test command line arguments
     */
    public static void main(String[] args) {
        long limit = Long.parseLong(args[0]);
        int[] nameLength = new int[8];
        HashSet<String> set = new HashSet<String>((int) (limit - 1));

        for (long i = 1; i <= limit; i++) {
            String name = getUserName(i);
            System.out.println("User " + i + ": " + name);
            ++nameLength[name.length() - 1];
            if (!set.add(name))
                System.out.println("Alert! Duplicate name: " + name);
        }
        long count = 0;
        for (int i = 0; i < nameLength.length; i++) {
            count += nameLength[i];
        }
        for (int i = 0; i < nameLength.length; i++) {
            System.out.println("Length " + (i + 1) + ", count " +
                    nameLength[i] + ", " + (100d * nameLength[i]/count) + "%");
        }
    }
}
