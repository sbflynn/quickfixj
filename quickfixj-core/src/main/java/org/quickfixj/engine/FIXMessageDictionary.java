/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 16 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

import org.quickfixj.FIXApplication;

public interface FIXMessageDictionary extends GraphDictionary {

    /**
     * @since 2.0
     */
    FIXApplication getApplication();

    /**
     * @since 2.0
     */
    String getMsgType();

    /**
     * @param field The field to test.
     * @return true if field is a trailer field, false otherwise.
     * @since 2.0
     */
    boolean isTrailerField(int tag);

    /**
     * @param field The field to test.
     * @return true if field is a header field, false otherwise.
     * @since 2.0
     */
    boolean isHeaderField(int tag);
}