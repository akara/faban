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
package com.sun.faban.harness.logging;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.io.OutputStream;
import java.io.IOException;

/**
 * FlexBuffer is an expandable byte buffer. It uses
 * 1 or more byte arrays internally and expands with
 * an array of the initial size at a time. This byte
 * buffer is intended for reuse. The clear() method
 * resets all pointers and the buffer to 0. It does
 * not free up any memory.
 *
 * @author Akara Sucharitakul
 */
public class FlexBuffer {

    /** Carriage return. */
    public static final byte CR = '\r';

    /** Line feed. */
    public static final byte LF = '\n';

    /** White space.  */
    public static final byte WS = ' ';

    private int initialSize = 0;
    private int totalSize = 0;
    private int currentPointer = 0;
    private int listPointer = 0;
    private ArrayList buffers = new ArrayList();
    private byte[] currentBuffer;

    /**
     * FlexBuffer constructor.
     * @param initial The initial size and expansion block
     */
    public FlexBuffer(int initial) {
        initialSize = initial;
        currentBuffer = new byte[initial];
        buffers.add(currentBuffer);
    }

    /**
     * Constructs a FlexBuffer with initial content. The
     * initial size will be the length of the content.
     * @param origin the byte array to take content from
     * @param start  the starting offset in the byte array
     * @param length the content length to copy
     */
    public FlexBuffer(byte[] origin, int start, int length) {
        this(length);
        System.arraycopy(origin, start, currentBuffer, 0, length);
        totalSize = length;
    }

    /**
     * Constructs a FlexBuffer with an initial backing array. This array
     * will be used directly and any data copy is avoided. As the array can
     * be written from whomever else has a reference to it, this constructor
     * is not very safe. However, it prevents another copy and is of very high
     * performance.
     * @param backingArray The byte array to be used as a backing array.
     */
    public FlexBuffer(byte[] backingArray) {
        initialSize = backingArray.length;
        totalSize = initialSize;
        currentBuffer = backingArray;
        buffers.add(currentBuffer);
    }

    /**
     * Clears the FlexBuffer. Resets pointers.
     * Does not free any of the allocated arrays.
     */
    public void clear() {
        currentBuffer = (byte[]) buffers.get(0);
        currentPointer = 0;
        listPointer = 0;
        totalSize = 0;
    }

    /**
     * Switches the buffer to the next one.
     */
    protected void nextBuffer() {
        ++listPointer;
        if (listPointer < buffers.size()) {
            currentBuffer = (byte[]) buffers.get(listPointer);
        } else {
            currentBuffer = new byte[currentBuffer.length];
            buffers.add(currentBuffer);
        }
        currentPointer = 0;
    }


    /**
     * Appends one byte to the buffer.
     * @param b byte to append
     */
    public void append(byte b) {
        if (currentPointer >= currentBuffer.length) {
            //System.out.println("Filled buffer: " + new String(currentBuffer));
            nextBuffer();
        }
        
        currentBuffer[currentPointer] = b;
        ++currentPointer;
        ++totalSize;
    }

    /**
     * Appends content from byte array to the buffer.
     * @param b byte array to append
     * @param offset the offset to start appending
     * @param length the length to start appending
     */
    public void append(byte[] b, int offset, int length) {
        for (;;) {
            int spaceLeft = currentBuffer.length - currentPointer;
            if (length <= spaceLeft) {
                System.arraycopy(b, offset, currentBuffer,
                                 currentPointer, length);
                currentPointer += length;
                totalSize += length;
                break;
            } else {
                System.arraycopy(b, offset, currentBuffer,
                                 currentPointer, spaceLeft);
                length -= spaceLeft;
                offset += spaceLeft;
                totalSize += spaceLeft;
                nextBuffer();
            }
        }
    }

    /**
     * Appends the content from the whole byte array to the buffer.
     * @param b byte array to append
     */
    public void append(byte[] b) {
        append(b, 0, b.length);
    }

    /**
     * Appends content of the nio ByteBuffer of a certain length. This
     * method will access the buffer's backing array if possible and
     * call appendDirect if there is no backing array.
     * @param b The buffer
     * @param length append
     */
    public void append(ByteBuffer b, int length) {
        if (b.hasArray())
            append(b.array(), b.arrayOffset() + b.position(), length);
        else
            appendDirect(b, length);
    }

    /**
     * Appends content of the nio direct ByteBuffer of a certain length
     * to the FlexBuffer.
     * @param b The buffer
     * @param length The length to append
     */
    public void appendDirect(ByteBuffer b, int length) {
        for (;;) {
            int spaceLeft = currentBuffer.length - currentPointer;
            if (length <= spaceLeft) {
                b.get(currentBuffer, currentPointer, length);
                currentPointer += length;
                totalSize += length;
                break;
            } else {
                b.get(currentBuffer, currentPointer, spaceLeft);
                length -= spaceLeft;
                totalSize += spaceLeft;
                nextBuffer();
            }
        }
    }

    /**
     * Obtains the size of this buffer.
     * @return the current buffer size
     */
    public int size() {
        return totalSize;
    }

    /**
     * Gets a byte array resembling the whole buffer.
     * @return a copy of the whole buffer.
     */
    public byte[] getBytes() {
        byte[] result = new byte[totalSize];
        getBytes(0, result, 0, totalSize);
        return result;
    }

    /**
     * Gets a byte array resembling part of the buffer.
     * @param pos starting position in the buffer
     * @param length content length
     * @return a copy of the whole buffer.
     */
    public byte[] getBytes(int pos, int length) {
        if (pos + length > totalSize)
            throw new ArrayIndexOutOfBoundsException();
        byte[] result = new byte[length];
        getBytes(pos, result, 0, length);
        return result;
    }


    /**
     * Copies buffer content into byte array. If the length
     * exceeds the end of the buffer, only bytes till the
     * buffer end will be copied. Therefore the return value
     * may not be the same as length.
     * @param pos position to start copy in the buffer
     * @param b byte array to copy into
     * @param off offset in the byte array to start copy
     * @param length content length to copy
     * @return the content length actually copied
     */
    public int getBytes(int pos, byte[] b, int off, int length) {
        if (pos + length > totalSize)
            length = totalSize - pos;
        int bufferIdx = pos / initialSize;
        int cIdx = pos % initialSize;
        int resultIdx = off;
        int remains = length;
        for (;;) {
            byte[] cBuffer = (byte[]) buffers.get(bufferIdx);
            int bytesLeft = initialSize - cIdx;
            if (remains <= bytesLeft) {
                System.arraycopy(cBuffer, cIdx, b, resultIdx, remains);
                break;
            } else {
                System.arraycopy(cBuffer, cIdx, b, resultIdx, bytesLeft); 
                remains -= bytesLeft;
                resultIdx += bytesLeft;
                ++bufferIdx;
                cIdx = 0;
            }
        }
        return length;
    }

    /**
     * Writes buffer content into an OutputStream. This method will do
     * a direct write from the buffer into the stream.
     * @param pos position to start copy in the buffer
     * @param length content length to copy
     * @param stream the output stream to write to
     * @return the content length actually written
     * @exception IOException write error
     */
    public int writeBytes(int pos, int length, OutputStream stream)
            throws IOException {
        if (pos + length > totalSize)
            length = totalSize - pos;
        int bufferIdx = pos / initialSize;
        int cIdx = pos % initialSize;
        int remains = length;
        for (;;) {
            byte[] cBuffer = (byte[]) buffers.get(bufferIdx);
            int bytesLeft = initialSize - cIdx;
            if (remains < bytesLeft) {
                stream.write(cBuffer, cIdx, remains);
                break;
            } else {
                stream.write(cBuffer, cIdx, bytesLeft);
                remains -= bytesLeft;
                ++bufferIdx;
                cIdx = 0;
            }
        }
        return length;
    }

    /**
     * Writes buffer content into an OutputStream. This method will do
     * a direct write from the buffer into the stream.
     * @param stream the output stream to write to
     * @return the content length actually written
     * @exception IOException write error
     */
    public int writeBytes(OutputStream stream) throws IOException {
        int fullBuffers = totalSize / initialSize;
        int remains = totalSize % initialSize;
        byte[] cBuffer = null;
        for (int bufferIdx = 0; bufferIdx < fullBuffers; bufferIdx++) {
            cBuffer = (byte[]) buffers.get(bufferIdx);
            stream.write(cBuffer);
        }
        if (remains > 0) {
            cBuffer = (byte[]) buffers.get(fullBuffers);
            stream.write(cBuffer, 0, remains);
        }
        return totalSize;
    }

    /**
     * Obtains a String representation of all or part of the buffer.
     * It is semantically equal to new String(getBytes(pos, length)
     * but contains further optimizations to avoid copies.
     *
     * @param pos The starting position
     * @param length The length
     * @return The string representing the requested part of the buffer.
     */
    public String getString(int pos, int length) {

        if (pos + length > totalSize)
            throw new ArrayIndexOutOfBoundsException();

        // Determine whether the beginning and the end is in the same buffer
        int bufferIdx;
        if ((bufferIdx = pos / initialSize) ==
                (pos + length - 1) / initialSize) {
            // If we can create the string directly, we do.
            byte[] cBuffer = (byte[]) buffers.get(bufferIdx);
            int cIdx = pos % initialSize;
            return new String(cBuffer, cIdx, length);
        } else {
            // Otherwise we'll need to incur an additional copy.
            byte[] result = new byte[length];
            getBytes(pos, result, 0, length);
            return new String(result);
        }
    }

    /**
     * Trims the string based on the position and the length.
     * @param pos The starting position
     * @param length The length
     * @return the resulting substring
     */
    public String getTrimmedString(int pos, int length) {

        // 1. Some basic checks.
        if (pos > totalSize)
            throw new ArrayIndexOutOfBoundsException("Buffer size: " +
                    totalSize + ", Position: " + pos);
        if (pos + length > totalSize)
            length = totalSize - pos;

        // 2. Select the buffer and position
        int cBufferIdx = pos / initialSize;
        byte[] cBuffer = (byte[]) buffers.get(cBufferIdx);
        int cIdx = pos % initialSize;
        int remainingLength = length;

        // 3. Scan the buffers for the starting position
        int actualStartIdx = -1;
        int actualStartBuffer = -1;
        int i, j;

        startSearchLoop:
        for (;;) {
            for (i = cIdx, j = 0; i < cBuffer.length &&
                    j < remainingLength; i++, j++) {
                if (cBuffer[i] > WS) {
                    actualStartIdx = i;
                    actualStartBuffer = cBufferIdx;
                    break startSearchLoop;
                }
            }

            // In case not found
            if (actualStartIdx == -1) {
                if (j < remainingLength)
                    remainingLength -= j;
                else // exhausted
                    return "";

                cBuffer = (byte[]) buffers.get(++cBufferIdx);
                cIdx = 0;
            }
        }

        // 4. Scan the buffers for the ending position
        int actualEndIdx = -1;
        int actualEndBuffer = -1;
        int endIdx = pos + length;
        cBufferIdx = endIdx / initialSize;
        cIdx = endIdx % initialSize;
        if (cIdx == 0) {
            cIdx = endIdx;
            cBufferIdx -= 1;
        }
        if (cBufferIdx != actualStartBuffer)
            cBuffer = (byte[]) buffers.get(cBufferIdx);

        endSearchLoop:
        for (;;) {
            for (i = cIdx - 1, j = remainingLength; i >= 0 && j > 0; i--, j--) {
                if (cBuffer[i] > WS) {
                    actualEndIdx = ++i;
                    actualEndBuffer = cBufferIdx;
                    break endSearchLoop;
                }
            }
            // Not yet found
            if (actualEndIdx == -1) {
                remainingLength = j;
                cBuffer = (byte[]) buffers.get(--cBufferIdx);
                cIdx = cBuffer.length - 1;
            }
        }

        // If start and end is in the same buffer, create string and return
        if (actualStartBuffer == actualEndBuffer)
            return new String(cBuffer, actualStartIdx,
                    actualEndIdx - actualStartIdx);

        // Otherwise we need to deal with it on a buffer-by-buffer basis.
        byte[] result = new byte[j];

        // Copy the current/last cBuffer.
        System.arraycopy(cBuffer, 0, result, j - i - 1, actualEndIdx);

        // Copy the first cBuffer.
        cBuffer = (byte[]) buffers.get(actualStartBuffer);
        int nextIdx = cBuffer.length - actualStartIdx;
        System.arraycopy(cBuffer, actualStartIdx, result, 0, nextIdx);
        for(cBufferIdx = actualStartBuffer + 1;
            cBufferIdx < actualEndBuffer; cBufferIdx++) {
            cBuffer = (byte[]) buffers.get(cBufferIdx);
            System.arraycopy(cBuffer, 0, result, nextIdx, cBuffer.length);
            nextIdx += cBuffer.length;
        }
        return new String(result);
    }

    /**
     * Obtains the byte a a certain position in the buffer.
     * @param pos the position of the byte
     * @return the value of the byte at the given position.
     */
    public int getByte(int pos) {
        if (pos >= totalSize)
            throw new ArrayIndexOutOfBoundsException();
        byte[] cBuffer = (byte[]) buffers.get(pos / initialSize);
        return cBuffer[pos % initialSize];
    }

    /**
     * Searches the byte buffer for the first occurrence of
     * the input string, converted to bytes.
     * @param s the string to match
     * @return the location of the match
     */
    public int indexOf(String s) {
        return indexOf(s, 0);
    }

    /**
     * Checks if the string ends with provided string.
     * @param s The string to check the buffer against
     * @return true if the buffer ends with the provided string, false otherwise
     */
    public boolean endsWith(String s) {
        byte[] sample = s.getBytes();
        int lastIdx = totalSize % initialSize;
        int bufferIdx = totalSize / initialSize;
        if (lastIdx == 0) {
            --bufferIdx;
            lastIdx = initialSize;
        }
        byte[] cBuffer = (byte[]) buffers.get(bufferIdx);
        for (int sampleIdx = sample.length - 1; sampleIdx >= 0; sampleIdx--) {
            if (lastIdx == 0) {
                --bufferIdx;
                lastIdx = initialSize;
                cBuffer = (byte[]) buffers.get(bufferIdx);
            }
            --lastIdx;
            if (sample[sampleIdx] != cBuffer[lastIdx])
                return false;
        }
        return true;
    }


    /**
     * Searches the byte buffer for the first occurrence of
     * the input string from a starting index, converted to bytes.
     * @param s the string to match
     * @param start the search starting point
     * @return the location of the match
     */
    public int indexOf(String s, int start) {
        // AI: Needs to be implemented properly
        byte[] sample = s.getBytes();
        byte[] cBuffer = null;
        int bufferIdx = start/initialSize;
        int lastIdx = totalSize % initialSize;
        if (lastIdx == 0)
            lastIdx = initialSize;
        int sizeBounds = Integer.MAX_VALUE;
        int cIdx = start - bufferIdx * initialSize;
        for (int i = bufferIdx; i < buffers.size(); i++) {
            cBuffer = (byte[]) buffers.get(i);
            for (int j = cIdx; j < cBuffer.length; j++) {
                byte[] ccBuffer = cBuffer;
                int ccBufferIdx = i;
                int ccDeductable = 0;
                int k = 0;
                if (ccBufferIdx == buffers.size() - 1)
                    sizeBounds = lastIdx;
                for (; k < sample.length; k++) {
                    int ccIdx = j + k - ccDeductable;
                    if (ccIdx >= sizeBounds)
                        return -1;
                    if (ccIdx >= initialSize) {
                        ccDeductable += initialSize;
                        ++ccBufferIdx;
                        ccBuffer = (byte[]) buffers.get(ccBufferIdx);
                        ccIdx -= initialSize;
                        if (ccBufferIdx == buffers.size() - 1)
                            sizeBounds = lastIdx;
                    }
                    if (sample[k] != ccBuffer[ccIdx])
                        break;
                }
                if (k == sample.length) // this means match succeeded
                    return (i * initialSize + j);
            }
        }
        return -1;
    }

    /**
     * Searches the buffer for an end-of-line. This can be marked by
     * the characters "\r", "\n", or "\r\n." The index of the first
     * EOL marker is returned.
     * @param start The offset to start searching
     * @return The index of the first EOL marker or -1 if not found
     */
    public int indexOfEOL(int start) {
        byte[] cBuffer = null;
        int bufferIdx = start / initialSize;
        int lastIdx = totalSize % initialSize;
        if (lastIdx == 0)
            lastIdx = initialSize;
        int cIdx = start - bufferIdx * initialSize;
        int numBuffers = buffers.size();
        for (int i = bufferIdx; i < numBuffers; i++) {
            cBuffer = (byte[]) buffers.get(i);
            int bufferLimit = cBuffer.length;
            if (i == numBuffers - 1)
                bufferLimit = lastIdx;
            for (int j = cIdx; j < bufferLimit; j++) {
                if (cBuffer[j] == CR || cBuffer[j] == LF)
                    return i * initialSize + j;
            }
        }
        return -1;
    }

    /**
     * Obtains the index of the next line begin, given the EOL marker.
     * This can be eol + 1 or eol + 2 dependent of whether the EOL marker
     * is "\r", "\n", or "\r\n."<p>
     * Note: the method does not actually check that
     * @param eol The EOL index
     * @return The beginning of the following line.
     */
    public int indexOfBOL(int eol) {
        // First, check whether we're at the bounds before
        // we waste time doing anything.
        if (eol == totalSize - 1)
            return totalSize;
        else if (eol >= totalSize)
            return -1;

        byte[] cBuffer = null;
        int bufferIdx = eol / initialSize;
        int cIdx = eol - bufferIdx * initialSize;
        cBuffer = (byte[]) buffers.get(bufferIdx);
        if (cBuffer[cIdx] == CR) {
            byte charAfter;
            ++cIdx;
            if (cIdx < cBuffer.length) {
                charAfter = cBuffer[cIdx];
            } else {
                cBuffer = (byte[]) buffers.get(++bufferIdx);
                charAfter = cBuffer[0];
            }
            if (charAfter == LF)
                return eol + 2;
        }
        return eol + 1;
    }

    /**
     * Provides a String representation of the buffer content.
     * The content is demarcated with buffer ends for debugging
     * purposes.
     * @return the string representation
     */
    public String toString() {
        String result = "";
        int fullBuffers = totalSize / initialSize;
        for (int i = 0; i < fullBuffers; i++) {
            result += new String((byte[]) buffers.get(i));
            result += "{end buffer line}\n";
        }
        if (totalSize % initialSize > 0)
            result += new String((byte[]) buffers.get(fullBuffers), 0,
                                 totalSize % initialSize);
        return result;
    }

    /**
     * Returns Tokenizer instance.
     * @return Tokenizer
     */
    public Tokenizer getTokenizer() {
        return new Tokenizer();
    }

    /**
     * A tokenizer for the FlexBuffer.
     */
    public class Tokenizer {

        private int position = 0;

        /**
         * Gets the next line from the buffer. The line may be terminated by
         * a CR, LF, or CRLF. The EOL characters are not returned.
         * @return A line from the buffer
         */
        public byte[] getLine() {
            int eol = indexOfEOL(position);
            if (eol == -1)
                return null;
            byte[] retVal = getBytes(position, eol - position);
            position = indexOfBOL(eol);
            return retVal;
        }

        /**
         * Gets the next line from the buffer, returning a string. The line
         * may be terminated by CR, LF, or CRLF. The EOL character is also
         * returned.
         * @return A line from the buffer
         */
        public String getLineAsString() {
            int eol = indexOfEOL(position);
            if (eol == -1)
                return null;
            String retVal = getString(position, eol - position + 1);
            position = indexOfBOL(eol);
            return retVal;
        }

        /**
         * Gets the rest of the line from the buffer, trims, and returns
         * as a string.
         * @return The remainder of the line
         */
        public String getTrimmedRemainder() {
            if (position == totalSize)
                return null;
            int oldPosition = position;
            int eol = indexOfEOL(position);
            if (eol == -1) {
                eol = totalSize;
                position = totalSize;
            } else {
                position = indexOfBOL(eol);
            }
            if (oldPosition == position)
                return null;
            return getTrimmedString(oldPosition, eol - oldPosition);
        }

        /**
         * Returns next token.
         * @param bytes
         * @return String
         */
        public String nextToken(byte[] bytes) {
            byte[] cBuffer = null;
            int start = position;
            int bufferIdx = start / initialSize;
            int lastIdx = totalSize % initialSize;
            if (lastIdx == 0)
                lastIdx = initialSize;
            int cIdx = start % initialSize;
            int numBuffers = buffers.size();
            int startBuffer = bufferIdx;
            int startIdx = cIdx;
            int endBuffer = -1;
            int endIdx = -1;

            startSearchLoop:
            for (int i = bufferIdx; i < numBuffers; i++) {
                cBuffer = (byte[]) buffers.get(i);
                int bufferLimit = cBuffer.length;
                if (i == numBuffers - 1)
                    bufferLimit = lastIdx;
                for (int j = cIdx; j < bufferLimit; j++) {
                    for (int k = 0; k < bytes.length; k++)
                        if (cBuffer[j] == bytes[k]) {
                            if (endIdx == -1) {
                                endIdx = j;
                                endBuffer = i;
                            }
                            break startSearchLoop;
                        }
                }
            }

            // If token not found, we copy eveything
            if (endIdx == -1) {
                endIdx = lastIdx;
                endBuffer = buffers.size() - 1;
                position = totalSize;
            } else {
                endSearchLoop:
                for (int i = endBuffer; i < numBuffers; i++) {
                    if (i != endBuffer)
                        cBuffer = (byte[]) buffers.get(i);
                    int bufferLimit = cBuffer.length;
                    if (i == numBuffers - 1)
                        bufferLimit = lastIdx;

                    endSearchBufferLoop:
                    for (int j = endIdx + 1; j < bufferLimit; j++) {
                        for (int k = 0; k < bytes.length; k++) {
                            if (cBuffer[j] == bytes[k])
                                continue endSearchBufferLoop;
                            position = i * initialSize + j;
                            bufferIdx = i;
                            break endSearchLoop;
                        }
                    }
                }
            }
            // Rewind to the end buffer, if needed.
            if (bufferIdx != endBuffer)
                cBuffer = (byte[]) buffers.get(endBuffer);

            // Things are easy on the same buffer.
            if (startBuffer == endBuffer)
                return new String(cBuffer, startIdx, endIdx - startIdx);

            // Otherwise we need to deal with it on a buffer-by-buffer basis.
            byte[] result = new byte[cBuffer.length * (endBuffer - startBuffer)
                    + endIdx - startIdx];

            // Copy the current/last cBuffer.
            System.arraycopy(cBuffer, 0, result, result.length - endIdx - 1,
                    endIdx);

            // Copy the first cBuffer.
            cBuffer = (byte[]) buffers.get(startBuffer);
            int nextIdx = cBuffer.length - startIdx;
            System.arraycopy(cBuffer, startIdx, result, 0,
                    result.length - startIdx);
            for(bufferIdx = startBuffer + 1;
                bufferIdx < endBuffer; bufferIdx++) {
                cBuffer = (byte[]) buffers.get(bufferIdx);
                System.arraycopy(cBuffer, 0, result, nextIdx, cBuffer.length);
                nextIdx += cBuffer.length;
            }
            return new String(result);
        }

        /**
         * Returns next token.
         * @param c
         * @return String
         */
        public String nextToken(byte c) {
            byte[] search = { c };
            return nextToken(search);
        }

        /**
         * Returns next token.
         * @param s
         * @return String
         */
        public String nextToken(String s) {
            int tokenStart = indexOf(s, position);
            if (tokenStart == -1)
                return getLineAsString();

            String result = getString(position, tokenStart - position);
            position = tokenStart + s.length();
            return result;
        }

        /**
         * Returns position of end of data.
         * @return boolean
         */
        public boolean endOfData() {
            return position == totalSize;
        }

        /**
         * Copies the data from the current position to the beginning of
         * the buffer. Data before the current position is discarded.
         */
        public void flip() {
            if (endOfData()) {
                clear();
            } else {
                int size = totalSize - position;
                int fromBufferIdx = position / initialSize;
                int fromIdx = position % initialSize;
                int toBufferIdx = 0;
                int toIdx = 0;
                byte[] fromBuffer = null;
                byte[] toBuffer = null;
                for (;;) {
                    fromBuffer = (byte[]) buffers.get(fromBufferIdx);
                    toBuffer = (byte[]) buffers.get(toBufferIdx);
                    if (fromBufferIdx == buffers.size() - 1 &&
                            size < toBuffer.length - toIdx) { //last buffer copy
                        System.arraycopy(fromBuffer, fromIdx, toBuffer,
                                toIdx, size);
                        toIdx += size;
                        size = 0;
                        break;
                    }
                    if (toIdx == 0) { // Copy to end of line
                        int copyLength = fromBuffer.length - fromIdx;
                        System.arraycopy(fromBuffer, fromIdx,
                                toBuffer, 0, copyLength);
                        ++fromBufferIdx;
                        fromIdx = 0;
                        toIdx += copyLength;
                        size -= copyLength;
                        break;
                    }
                    if (fromIdx == 0) { // Copy from begin of line
                        int copyLength = toBuffer.length - toIdx;
                        System.arraycopy(fromBuffer, 0,
                                toBuffer, toIdx, copyLength);
                        ++toBufferIdx;
                        toIdx = 0;
                        fromIdx += copyLength;
                        size -= copyLength;
                    }
                }
                totalSize -= position;
                position = 0;
                currentBuffer = toBuffer;
                currentPointer = toIdx;
                listPointer = toBufferIdx;
            }
            position = 0;
        }
    }
}
