/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 16 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

import java.util.List;
import java.util.Set;

import org.quickfixj.FIXFieldType;

public interface GraphDictionary {

    /**
     * @param tag The field to test.
     * @return True if the tag is defined in the parent {@link FIXMessageDictionaryFactory}, false if not.
     * @since 2.0
     */
    boolean isValid(int tag);

    /**
     * @param field The field to test.
     * @since 2.0
     */
    boolean isField(int tag);

    /**
     * @param field The field to test.
     * @since 2.0
     */
    boolean isGroupField(int tag);

    /**
     * @since 2.0
     */
    Set<Integer> getRequiredFields();

    /**
     * @since 2.0
     */
    GroupDictionary getGroupDictionary(int tag);

    /**
     * @since 2.0
     */
    FieldDictionary getFieldDictionary(int tag);

    /**
     * @since 2.0
     */
    public interface GroupDictionary extends GraphDictionary {

        /**
         * @since 2.0
         */
        int getDelimiterField();

        /**
         * @since 2.0
         */
        List<Integer> getOrderedFields();

    }

    public interface FieldDictionary {

        /**
         * @since 2.0
         */
        boolean isDataField();

        /**
         * @since 2.0
         */
        Set<String> getFieldValues();

        /**
         * @since 2.0
         */
        boolean allowOtherFieldValues();

        /**
         * @since 2.0
         */
        String getName();

        /**
         * @since 2.0
         */
        FIXFieldType getType();
    }
}