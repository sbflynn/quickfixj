/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

import java.util.Map;

import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;

/**
 * Creates and populates a {@link FIXFieldGraph}.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface GraphBuilder<T extends FIXFieldGraph> {

    T create();

    FIXField<?> create(int tag, char[] value, int offset, int count);

    <G extends FIXGroup> GraphBuilder<G> create(FIXGroupField<G> groupField);

    Map<Integer, FieldBuilder> getFieldBuilders();

    public interface FieldBuilder {

        int getTag();

        String getName();

        FIXField<?> create(char[] value, int offset, int count);
    }
}
