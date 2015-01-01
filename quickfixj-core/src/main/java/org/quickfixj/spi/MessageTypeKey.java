/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 12 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import org.quickfixj.FIXApplication;

/**
 * MessageTypeKey - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class MessageTypeKey {

    private final FIXApplication application;
    private final String msgType;
    private final int hashcode;

    public MessageTypeKey(FIXApplication application, String msgType) {
        this.application = application;
        this.msgType = msgType;

        int hash = 17;
        if (application != null) {
            hash = hash * 37 + application.hashCode();
        }
        if (msgType != null) {
            hash = hash * 37 + msgType.hashCode();
        }
        hashcode = hash;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public int hashCode() {
        return hashcode;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public boolean equals(Object obj) {

        MessageTypeKey key = (MessageTypeKey) obj;

        return (this.application == key.application) && equals(this.msgType, key.msgType);
    }

    public boolean equals(Object o1, Object o2) {

        if (o1 == o2) {
            return true;
        }

        if (o1 != null) {
            return o1.equals(o2);
        }

        return false;
    }
}