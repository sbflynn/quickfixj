/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;

/**
 * FIXMessageDictionaryFactory - provides {@link FIXMessageDictionary} instances for a single {@link FIXBeginString} partitioned by namespace.
 *
 * Dictionary factories are intended to be immutable and threadsafe - these means that implementations should store no parse time information. 
 * 
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXMessageDictionaryFactory {

    /**
     * The namespace that this factory is bound to.
     * @since 2.0
     */
    String getNamespace();

    /**
     * The {@link FIXBeginString} that this factory services.
     * @since 2.0
     */
    FIXBeginString getBeginString();

    /**
     * Provides a message dictionary for an application and message type
     * @param application
     * @param msgType
     * @return The required {@link FIXMessageDictionary}.
     * @since 2.0
     */
    FIXMessageDictionary getMessageDictionary(FIXApplication application, String msgType);
}
