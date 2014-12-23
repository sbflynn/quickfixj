/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package org.quickfixj.field;

/**
 * A string-valued message field.
 */
public class GenericField extends AbstractField<String> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private final int tag;

    private final String object;

    public GenericField(int tag, String object) {

        this.tag = tag;
        this.object = object;
    }

    public GenericField(int tag, int value) {

        this.tag = tag;
        this.object = String.valueOf(value);
    }

    public GenericField(int tag, boolean value) {

        this.tag = tag;
        this.object = value ? "Y" : "N";
    }

    public GenericField(int tag, char[] value, int offset, int count) {

        this.tag = tag;
        this.object = new String(value, offset, count);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public int getTag() {
        return tag;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String getValue() {
        return object;
    }
}
