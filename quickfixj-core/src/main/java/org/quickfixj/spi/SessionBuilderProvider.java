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
import java.util.Map;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.MessageBuilder;
import org.quickfixj.MessageBuilder.FieldBuilder;

/**
 * SessionBuilderProvider - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface SessionBuilderProvider {

    FIXBeginString getBeginString();

    List<MessageBuilder> getMessageBuilders();

    Map<Integer, FieldBuilder> getHeaderFieldBuilders();

    Map<Integer, FieldBuilder> getTrailerFieldBuilders();

    List<FIXApplication> getSupportedApplications();

}
