/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj;

import java.io.Serializable;

/**
 * FIXField - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXField<T extends Serializable> {

    /**
     * Gets the field's tag.
     *
     * @return the tag
     * @since 2.0
     */
    int getTag();

    /**
     * Get the field's value.
     *
     * @return an object representing the field's value
     * @since 2.0
     */
    T getValue();

    /**
     * Get the field's character value.
     *
     * @since 2.0
     */
    CharSequence getCharacters();

    /**
     * Write the tag/value pair to the {@link Appendable} target in nn=xxxSOH format. 
     * @since 2.0
     */
    Appendable serialize(Appendable appendable);

    /**
     * @since 2.0
     */
    FIXField<T> clone();
}
