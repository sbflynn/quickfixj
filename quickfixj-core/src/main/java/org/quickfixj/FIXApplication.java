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
 * FIXApplication - enumeration of supported FIX application versions.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public enum FIXApplication {

    FIX27("FIX.2.7", 0),

    FIX30("FIX.3.0", 1),

    FIX40("FIX.4.0", 2),

    FIX41("FIX.4.1", 3),

    FIX42("FIX.4.2", 4),

    FIX43("FIX.4.3", 5),

    FIX44("FIX.4.4", 6),

    FIX50("FIX.5.0", 7),

    FIX50SP1("FIX.5.0SP1", 8),

    FIX50SP2("FIX.5.0SP2", 9);

    private static final Map<String, FIXApplication> VALUES = new HashMap<String, FIXApplication>();
    private static final Map<Integer, FIXApplication> IDS = new HashMap<Integer, FIXApplication>();

    static {
        addValue(FIX27);
        addValue(FIX30);
        addValue(FIX40);
        addValue(FIX41);
        addValue(FIX42);
        addValue(FIX43);
        addValue(FIX44);
        addValue(FIX50);
        addValue(FIX50SP1);
        addValue(FIX50SP2);
    }

    private final String value;

    private final int id;

    private FIXApplication(String value, int id) {
        this.value = value;
        this.id = id;
    }

    private static void addValue(FIXApplication application) {
        VALUES.put(application.getValue(), application);
        IDS.put(application.getId(), application);
    }

    public String getValue() {
        return value;
    }

    public int getId() {
        return id;
    }

    public static FIXApplication parseId(String applVerId) {

        return IDS.get(Integer.parseInt(applVerId));
    }

    public static FIXApplication parse(String value) {

        return VALUES.get(value);
    }

    public static String print(FIXApplication value) {

        return value.getValue();
    }
}
