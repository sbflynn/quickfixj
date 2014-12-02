/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.util.List;

import org.quickfixj.FIXApplication;
import org.quickfixj.MessageBuilder;

/**
 * ApplicationBuilderProvider - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface ApplicationBuilderProvider {

    List<MessageBuilder> getMessageBuilders();

    FIXApplication getApplication();

}
