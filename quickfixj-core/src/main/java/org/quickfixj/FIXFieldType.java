/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj;

import java.util.Date;

/**
 * A field type enum class that represents each data type listed in FIXT.1.1 Datatypes. 
 */
public enum FIXFieldType {

    INT("int", Integer.class),

    /**
     * Added FIX.4.3
     */
    LENGTH("Length", Integer.class),

    /**
     * Added FIX.4.3
     */
    TAGNUM("TagNum", Integer.class),

    /**
     * Added FIX.4.3
     */
    SEQNUM("SeqNum", Integer.class),

    /**
     * Added FIX.4.3
     */
    NUMINGROUP("NumInGroup", Integer.class),

    /**
     * Added FIX.4.1
     */
    DAYOFMONTH("DayOfMonth", Integer.class),

    FLOAT("float", Double.class),

    /**
     * Added FIX.4.2
     */
    QTY("Qty", Double.class),

    /**
     * Added FIX.4.2
     */
    PRICE("Price", Double.class),

    /**
     * Added FIX.4.2
     */
    PRICEOFFSET("PriceOffset", Double.class),

    /**
     * Added FIX.4.2
     */
    AMT("Amt", Double.class),

    /**
     * Added FIX.4.3
     */
    PERCENTAGE("Percentage", Double.class),

    CHAR("char", Character.class),

    /**
     * Added FIX.4.2
     */
    BOOLEAN("Boolean", Boolean.class),

    STRING("String"),

    /**
     * Added FIX.4.4
     */
    MULTIPLECHARVALUE("MultipleCharValue"),

    /**
     * Added FIX.4.2
     */
    MULTIPLESTRINGVALUE("MultipleStringValue"),

    /**
     * Added FIX.4.4
     */
    COUNTRY("Country"),

    /**
     * Added FIX.4.2
     */
    CURRENCY("Currency"),

    /**
     * Added FIX.4.2
     */
    EXCHANGE("Exchange"),

    /**
     * Added FIX.4.1
     */
    MONTHYEAR("MonthYear"),

    UTCDATE("UtcDate", Date.class),

    UTCTIMESTAMP("UtcTimeStamp", Date.class),

    UTCTIMEONLY("UtcTimeOnly", Date.class),

    UTCDATEONLY("UtcDateOnly", Date.class),

    LOCALMKTDATE("LocalMktDate", Date.class),

    TZTIMEONLY("TZTimeOnly", Date.class),

    TZTIMESTAMP("TZTimestamp", Date.class),

    DATA("data"),

    PATTERN("Pattern"),

    TENOR("Tenor"),

    RESERVED100PLUS("Reserved100Plus"),

    RESERVED1000PLUS("Reserved1000Plus"),

    RESERVED4000PLUS("Reserved4000Plus"),

    XMLDATA("XMLData"),

    LANGUAGE("Language"),

    /**
     * Added FIX.4.0, deprecated FIX.4.1
     */
    TIME("Time", Date.class),

    /**
     * Added FIX.4.0, deprecated FIX.4.1
     */
    DATE("Date", Date.class),

    UNKNOWN("Unknown");

    private String name;
    private Class<?> javaType;

    private FIXFieldType(String name) {
        this(name, String.class);
    }

    private FIXFieldType(String name, Class<?> javaType) {
        this.name = name;
        this.javaType = javaType;
    }

    public String getName() {
        return name;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public static FIXFieldType parse(String value) {

        return valueOf(value);
    }

    public static String print(FIXFieldType value) {

        return value.name();
    }
}
