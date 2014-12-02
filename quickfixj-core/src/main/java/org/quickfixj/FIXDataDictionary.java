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
 * FIXDataDictionary - provides...
 *
 * Dictionaries are intended to be immutable and threadsafe.
 * 
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXDataDictionary {

    FIXField<?> produceField(MessageBuilder builder, String msgType, int tag, CharSequence message,
            int start, int end);

    boolean isDataField(int tag);
}
