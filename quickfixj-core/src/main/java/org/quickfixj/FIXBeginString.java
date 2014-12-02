/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj;

import java.util.HashMap;
import java.util.Map;

/**
 * FIXBeginString - enumeration of supported FIX begin string values.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public enum FIXBeginString {

    FIX40("FIX.4.0"),

    FIX41("FIX.4.1"),

    FIX42("FIX.4.2"),

    FIX43("FIX.4.3"),

    FIX44("FIX.4.4"),

    FIXT11("FIXT.1.1");

    private static final Map<String, FIXBeginString> VALUES = new HashMap<String, FIXBeginString>();

    static {
        addValue(FIX40);
        addValue(FIX41);
        addValue(FIX42);
        addValue(FIX43);
        addValue(FIX44);
        addValue(FIXT11);
    }

    private final String value;

    private FIXBeginString(String value) {
        this.value = value;
    }

    private static void addValue(FIXBeginString beginString) {
        VALUES.put(beginString.getValue(), beginString);
    }

    public String getValue() {
        return value;
    }

    public boolean gt(FIXBeginString beginString) {
        return ordinal() > beginString.ordinal();
    }

    public boolean gte(FIXBeginString beginString) {
        return ordinal() >= beginString.ordinal();
    }

    public boolean lt(FIXBeginString beginString) {
        return ordinal() < beginString.ordinal();
    }

    public boolean lte(FIXBeginString beginString) {
        return ordinal() <= beginString.ordinal();
    }

    public static FIXBeginString parse(String value) {

        if (value == null) {
            return null;
        }

        return VALUES.get(value);
    }

    public static String print(FIXBeginString value) {

        if (value == null) {
            return null;
        }

        return value.getValue();
    }
}
