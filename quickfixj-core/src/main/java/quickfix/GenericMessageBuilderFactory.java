/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 7 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionary;

/**
 * GenericMessageBuilderFactory - provides {@link GenericMessageBuilder} instances 
 * that will create string backed generic {@link FIXMessage} objects.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class GenericMessageBuilderFactory implements FIXMessageBuilderFactory {

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String getNamespace() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXBeginString getBeginString() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageBuilder getMessageBuilder(String msgType) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageBuilder getMessageBuilder(FIXMessageDictionary dictionary) {
        return new GenericMessageBuilder(null, dictionary);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageBuilder getMessageBuilder(FIXApplication application, String msgType) {
        return new GenericMessageBuilder(null, null);
    }
}
