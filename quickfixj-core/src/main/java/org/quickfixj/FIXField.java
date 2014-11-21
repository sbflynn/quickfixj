/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 19 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj;

/**
 * FIXField - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXField<T> {

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
    T getObject();

    /**
     * Get the field's character value.
     *
     * @since 2.0
     */
    CharSequence getCharacters();

    /**
     *
     * @since 2.0
     */
    int getLength();

    /**
     *
     * @since 2.0
     */
    int getChecksum();
}
