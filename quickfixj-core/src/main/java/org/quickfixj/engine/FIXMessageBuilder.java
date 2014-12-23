/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

import java.util.Map;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXMessage;

/**
 * Creates and populates a {@link FIXMessage}.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXMessageBuilder extends GraphBuilder<FIXMessage> {

    FIXBeginString getBeginString();

    FIXApplication getApplication();

    String getMsgType();

    Map<Integer, GraphBuilder<? extends FIXGroup>> getGroupBuilders();
    //
    //    public interface ComponentBuilder {
    //
    //        Map<Integer, FieldBuilder> getFieldBuilders();
    //
    //        Map<Integer, GraphBuilder<? extends FIXGroup>> getGroupBuilders();
    //    }
}
