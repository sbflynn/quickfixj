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
 * MessageFactoryService - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface MessageBuilderFactory {

    /**
     * Produces an admin {@link MessageBuilder}.
     * @param beginString
     * @param msgType
     * @since 2.0
     */
    MessageBuilder getMessageBuilder(FIXBeginString beginString, String msgType);

    /**
     * Produces an app {@link MessageBuilder}.
     * @param beginString
     * @param application
     * @param msgType
     * @since 2.0
     */
    MessageBuilder getMessageBuilder(FIXBeginString beginString, FIXApplication application,
            String msgType);
}
