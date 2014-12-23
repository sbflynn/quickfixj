/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 29 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj;

/**
 * FIXMessage - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXMessage extends FIXFieldGraph {

    FIXMessageHeader getHeader();

    FIXMessageTrailer getTrailer();

    String getMsgType();

    /**
     * TODO - shouldn't really be storing exceptions in a data structure ?
     * @since 2.0
     */
    @Deprecated
    RuntimeException getParseException();

    /**
     * TODO - shouldn't really be storing exceptions in a data structure ?
     * @since 2.0
     */
    @Deprecated
    void setParseException(RuntimeException exception);
}
