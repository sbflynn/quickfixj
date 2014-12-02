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

import quickfix.field.converter.DoubleConverter;

/**
 * A double-values message field.
 */
public abstract class DoubleField extends AbstractField<Double> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private final double value;

    private final int padding;

    protected DoubleField(double value) {
        this(value, 0);
    }

    protected DoubleField(double value, int padding) {
        checkForValidDouble(value);
        this.value = value;
        this.padding = padding;
    }

    protected DoubleField(CharSequence charSequence) {

        this(Double.parseDouble(charSequence.toString()), 0);
    }

    @Override
    public Double getValue() {
        return value;
    }

    public int getPadding() {
        return padding;
    }

    // QFJ-808: NaN or infinity values cannot be transmitted via FIX in a DoubleField
    private void checkForValidDouble(Double value) throws NumberFormatException {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException("Tried to set NaN or infinite value.");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public CharSequence getCharacters() {
        return DoubleConverter.convert(getValue(), getPadding());
    }
}
