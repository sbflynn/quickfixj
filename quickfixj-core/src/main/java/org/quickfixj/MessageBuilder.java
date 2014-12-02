/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj;

import java.util.Map;

import quickfix.Message;

/**
 * Used by a Session to create a Message.
 *
 * @see quickfix.Session
 */
public interface MessageBuilder {

    FIXBeginString getBeginString();

    FIXApplication getApplication();

    String getMsgType();

    // return type should be FIXMessage
    Message create();

    FIXField<?> create(int tag, CharSequence chars);

    <T extends FIXGroup> GroupBuilder<T> create(FIXGroupField<T> groupField);

    Map<Integer, FieldBuilder> getFieldBuilders();

    Map<Integer, GroupBuilder<?>> getGroupBuilders();

    public interface FieldBuilder {

        int getTag();

        String getName();

        FIXField<?> create(CharSequence chars);
    }

    public interface GroupBuilder<G> {

        FIXGroup create();

        FIXField<?> create(int tag, CharSequence chars);

        <T extends FIXGroup> GroupBuilder<T> create(FIXGroupField<T> groupField);
    }

    public interface ComponentBuilder {

        Map<Integer, FieldBuilder> getFieldBuilders();

        Map<Integer, GroupBuilder<?>> getGroupBuilders();
    }
}
