package com.sun.faban.harness.webclient;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletOutputStream;
import java.util.ArrayList;
import java.io.IOException;

/**
 * The superclass of all log handlers provides all basic services
 * to be subclassed by specific handlers or display formatters.
 */
abstract class LogParseHandler extends DefaultHandler {

    long recordCount = 0;
    long begin = 0;
    long end = Long.MAX_VALUE;

    ArrayList stack = new ArrayList();
    StringBuffer buffer = new StringBuffer();
    LogRecord logRecord = new LogRecord();
    boolean xmlComplete = false; // Sets by the caller if parsing complete

    /**
     * Receive notification of the start of an element.
     * <p/>
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the start of
     * each element (such as allocating a new tree node or writing
     * output to a file).</p>
     *
     * @param uri        The Namespace URI, or the empty string if the
     *                   element has no Namespace URI or if Namespace
     *                   processing is not being performed.
     * @param localName  The local name (without prefix), or the
     *                   empty string if Namespace processing is not being
     *                   performed.
     * @param qName      The qualified name (with prefix), or the
     *                   empty string if qualified names are not available.
     * @param attributes The attributes attached to the element.  If
     *                   there are no attributes, it shall be an empty
     *                   Attributes object.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *                                  wrapping another exception.
     * @see org.xml.sax.ContentHandler#startElement
     */
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {

        // Put the QName into the stack
        stack.add(qName);
    }

    /**
     * Receive notification of the end of an element.
     * <p/>
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the end of
     * each element (such as finalising a tree node or writing
     * output to a file).</p>
     *
     * @param uri       The Namespace URI, or the empty string if the
     *                  element has no Namespace URI or if Namespace
     *                  processing is not being performed.
     * @param localName The local name (without prefix), or the
     *                  empty string if Namespace processing is not being
     *                  performed.
     * @param qName     The qualified name (with prefix), or the
     *                  empty string if qualified names are not available.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *                                  wrapping another exception.
     * @see org.xml.sax.ContentHandler#endElement
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        int depth = stack.size();

        if (!stack.remove(depth - 1).equals(qName))
            throw new SAXException("endElement mismatch: " + qName);
        if ("record".equals(qName)) {
            logRecord.id = recordCount;
            if (recordCount >= begin)
                processRecord();
            if (++recordCount >= end)
                throw new SAXParseException(
                        "End request range, abort processing!", null);
        } else if (recordCount >= begin) {
            Object parent = null;
            if (depth >= 2)
                parent = stack.get(depth - 2);
            if ("host".equals(qName))
                logRecord.host = buffer.toString().trim();
            else if ("date".equals(qName))
                logRecord.date = buffer.toString().trim();
            else if ("level".equals(qName))
                logRecord.level = buffer.toString().trim();
            else if ("class".equals(qName) && "record".equals(parent))
                logRecord.clazz = buffer.toString().trim();
            else if ("method".equals(qName) && "record".equals(parent))
                logRecord.method = buffer.toString().trim();
            else if ("thread".equals(qName))
                logRecord.thread = buffer.toString().trim();
            else if ("message".equals(qName) && "record".equals(parent)) {
                logRecord.message = formatMessage(buffer.toString().trim());
            }
            else
                processDetail(qName);
        }
        buffer.setLength(0);
    }

    /**
     * Formats a multi-line message into html line breaks
     * for readability.
     * @param message The message to be formatted.
     * @return The new formatted message.
     */
    String formatMessage(String message) {
        int idx = message.indexOf('\n');
        if (idx == -1) // If there's no \n, don't even hassle.
            return message;
        StringBuffer msg = new StringBuffer(message);
        String crlf = "<br>";
        while (idx != -1) {
            msg.replace(idx, idx + 1, crlf);
            idx = msg.indexOf("\n", idx + crlf.length());
        }
        return msg.toString();
    }

    /**
     * The processRecord method allows subclasses to define
     * how a record should be processed.
     * @throws org.xml.sax.SAXException If the processing should stop.
     */
    public abstract void processRecord() throws SAXException;

    /**
     * Prints the html result of the parsing to the servlet output.
     * @param request The servlet request
     * @param out The servlet output stream
     * @param runId The run id
     * @throws java.io.IOException Error writing to the servlet output stream
     */
    public abstract void printHtml(HttpServletRequest request,
                          ServletOutputStream out, String runId)
            throws IOException;

    /**
     * The processDetail method allows subclasses to process
     * the exceptions not processed by default. This is called
     * from endElement.
     * @param qName The element qName
     * @throws org.xml.sax.SAXException If the processing should stop.
     */
    public abstract void processDetail(String qName) throws SAXException;

    /**
     * Receive notification of character data inside an element.
     * <p/>
     * <p>By default, do nothing.  Application writers may override this
     * method to take specific actions for each chunk of character data
     * (such as adding the data to a node or buffer, or printing it to
     * a file).</p>
     *
     * @param ch     The characters.
     * @param start  The start position in the character array.
     * @param length The number of characters to use from the
     *               character array.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *                                  wrapping another exception.
     * @see org.xml.sax.ContentHandler#characters
     */
    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (recordCount >= begin)
            buffer.append(ch, start, length);
    }

    /**
     * LogRecordDetail contains all the remaining fields of a logRecord.
     */
    static class LogRecordDetail {
        String millis;
        String sequence;
        String logger;
    }

    static class ExceptionRecord {
        String message;
        StackFrame[] stackFrames;
    }

    static class StackFrame {
        String clazz;
        String method;
        String line;
    }

    /**
     * LogRecord contains all the basic fields used from a logRecord.
     */
    static class LogRecord {
        long id = -1;
        String date;
        String host;
        String level;
        String clazz;
        String method;
        String thread;
        String message;
        boolean exceptionFlag = false;
        ExceptionRecord exception;

        public void clear() {
            id = -1;
            date = null;
            host = null;
            level = null;
            clazz = null;
            method = null;
            thread = null;
            message = null;
            exceptionFlag = false;
        }
    }
}
