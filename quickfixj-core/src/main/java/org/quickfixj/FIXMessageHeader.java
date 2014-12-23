/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj;

/**
 * FIXMessageHeader.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXMessageHeader extends FIXFieldGraph {

    /**
     * @since 2.0
     */
    @Override
    FIXMessageHeader clone();

}
