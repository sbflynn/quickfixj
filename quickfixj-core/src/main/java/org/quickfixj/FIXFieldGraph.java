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
 * FIXFieldGraph - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXFieldGraph extends Iterable<FIXField<?>>, Cloneable {

    /**
     * @since 2.0
     */
    FIXField<?> getField(int tag);

    /**
     * @since 2.0
     */
    void setField(FIXField<?> field);

    /**
     * @since 2.0
     */
    String getFieldValue(int tag);

    /**
     * @param tag
     * @since 2.0
     */
    void removeField(int tag);

    /**
     * @param tag
     * @since 2.0
     */
    boolean isFieldSet(int tag);

    /**
     * @since 2.0
     */
    boolean isEmpty();

    /**
     * @since 2.0
     */
    int getFieldCount();

    /**
     * @since 2.0
     */
    void clear();

    /**
     * @since 2.0
     */
    FIXFieldGraph clone();
}
