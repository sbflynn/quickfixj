/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 6 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

import org.quickfixj.CharsetSupport;

class MessageTokenIterator implements CharSequence {

    private final String message;
    private final char[] buffer;

    private int currentStart, currentSeparator, currentEnd;

    private int tag;

    private boolean finished = false;

    MessageTokenIterator(String message) {
        this.message = message;
        this.buffer = message.toCharArray();

        scanAhead();
        currentSeparator = message.indexOf('=', currentStart);
        if (currentSeparator == -1) {

            throw new InvalidMessage("Bad tag format: missing initial \'=\' sign" + " in "
                    + message);
        }
        try {
            tag = Integer.parseInt(message.substring(currentStart, currentSeparator));
        } catch (final NumberFormatException e) {
            throw new InvalidMessage("Bad tag format: " + e.getMessage() + " in " + message);
        }
    }

    /**
     * Get the tag property.
     *
     * @return Returns the tag.
     * @since 2.0
     */
    public int getTag() {
        return tag;
    }

    /**
     * Get the currentStart property.
     *
     * @return Returns the currentStart.
     * @since 2.0
     */
    public int getCurrentStart() {
        return currentStart;
    }

    /**
     * Get the currentEnd property.
     *
     * @return Returns the currentEnd.
     * @since 2.0
     */
    public int getCurrentEnd() {
        return currentEnd;
    }

    /**
     * Get the currentSeparator property.
     *
     * @return Returns the currentSeparator.
     * @since 2.0
     */
    public int getCurrentSeparator() {
        return currentSeparator;
    }

    /**
     * Get the message property.
     *
     * @return Returns the message.
     * @since 2.0
     */
    public String getMessage() {
        return message;
    }

    private void scanAhead() {
        currentEnd = message.indexOf('\001', currentStart);
    }

    public boolean hasNext() {
        return currentEnd != -1 && message.length() > currentEnd + 1;
    }

    public boolean finished() {
        return finished;
    }

    public int next() {
        if (hasNext()) {
            currentStart = currentEnd + 1;
            scanAhead();
            currentSeparator = message.indexOf('=', currentStart);
            try {
                tag = Integer.parseInt(message.substring(currentStart, currentSeparator));
            } catch (final NumberFormatException e) {
                throw new InvalidMessage("Bad tag format: " + e.getMessage() + " in " + message);
            }
        } else {
            finished = true;
        }
        return tag;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String toString() {
        return message.substring(currentSeparator + 1, currentEnd);
    }

    /**
     * Get the buffer property.
     *
     * @return Returns the buffer.
     * @since 2.0
     */
    public char[] getBuffer() {
        return buffer;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public int length() {
        return currentEnd - currentSeparator - 1;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public char charAt(int index) {
        return message.charAt(currentSeparator + 1 + index);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return message.subSequence(currentSeparator + 1 + start, currentSeparator + 1 + end);
    }

    public void expandToByteLength(int fieldLength) {

        while (getCurrentEnd() - getCurrentSeparator() - 1 < fieldLength
                && message.substring(getCurrentSeparator() + 1, getCurrentEnd()).getBytes(
                        CharsetSupport.getCharsetInstance()).length < fieldLength) {
            currentEnd = message.indexOf('\001', currentEnd + 1);
            if (currentEnd == -1) {

                System.out.println("MessageTokenIterator.expandToByteLength() " + tag);
                System.out.println("MessageTokenIterator.expandToByteLength() " + currentSeparator);
                System.out.println("MessageTokenIterator.expandToByteLength() " + message.length());
                System.out.println("MessageTokenIterator.expandToByteLength() " + fieldLength);
                System.out.println("MessageTokenIterator.expandToByteLength() " + message);

                throw new InvalidMessage("SOH not found at end of field: " + tag + " in " + message);
            }
        }
    }
}