/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 13 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

import org.quickfixj.FIXField;
import org.quickfixj.FIXMessage;

/**
 * Validator - resposible for validating a {@link FIXMessage} with respect to a
 * {@link FIXMessageDictionary}.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface Validator {

	/**
	 * @since 2.0
	 */
	boolean isCheckFieldsOutOfOrder();

	/**
	 * @since 2.0
	 */
	boolean isCheckUnorderedGroupFields();

	/**
	 * @since 2.0
	 */
	void validate(FIXMessageDictionary dictionary, FIXMessage message);

	/**
	 * @since 2.0
	 */
	void validate(FIXMessageDictionary dictionary, FIXMessage message,
			boolean bodyOnly);

	void checkValidTagNumber(GraphDictionary dictionary, FIXField<?> field);
}
