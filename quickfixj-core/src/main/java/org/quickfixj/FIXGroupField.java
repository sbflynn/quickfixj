/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj;

/**
 * FIXGroupField - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXGroupField<T extends FIXGroup> extends FIXField<Integer>, Iterable<T> {

    /**
     * @since 2.0
     */
    int getDelimiterField();

    /**
     * @since 2.0
     */
    T add(T group);

    /**
     * @since 2.0
     */
    void remove(T group);

    /**
     * @since 2.0
     */
    void remove(int index);

    /**
     * @since 2.0
     */
    T get(int index);

    /**
     * @since 2.0
     */
    void set(int index, T group);

    /**
     * @since 2.0
     */
    int size();

}
