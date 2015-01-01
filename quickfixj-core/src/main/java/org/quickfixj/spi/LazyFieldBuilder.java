/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 21 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.quickfixj.FIXField;
import org.quickfixj.engine.GraphBuilder.FieldBuilder;
import org.quickfixj.engine.SessionRejectReason;
import org.quickfixj.field.FieldConversionException;
import org.quickfixj.field.FieldException;
import org.quickfixj.spi.DictionaryMetadata.FieldMetadata;
import org.quickfixj.xml.dictionary.BuildDirective;

/**
 * LazyBuilderSupport provides support for reflection based message prticle
 * creation factories.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class LazyFieldBuilder implements FieldBuilder {

	private final FieldMetadata metadata;
	private final BuildDirective buildDirective;

	private Constructor<?> constructor;

	LazyFieldBuilder(FieldMetadata metadata, BuildDirective buildDirective) {
		this.metadata = metadata;
		this.buildDirective = buildDirective;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 2.0
	 */
	@Override
	public int getTag() {
		return metadata.getTag();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 2.0
	 */
	@Override
	public String getName() {
		return metadata.getName();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 2.0
	 */
	@Override
	public FIXField<?> create(char[] value, int offset, int count) {
		try {

			if (constructor == null) {
				Class<?> type = Class.forName(buildDirective.getPackage() + "."
						+ metadata.getName());
				constructor = type.getConstructor(char[].class, int.class,
						int.class);
			}

			return (FIXField<?>) constructor.newInstance(value, offset, count);

		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof NumberFormatException) {
				throw new FieldException(
						SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
						SessionRejectReason
								.getMessage(SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE),
						getTag());
			}
			if (e.getCause() instanceof FieldConversionException) {
				throw new FieldException(
						SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
						SessionRejectReason
								.getMessage(SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE),
						getTag());
			}
			throw new RuntimeException(e.getCause());

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}