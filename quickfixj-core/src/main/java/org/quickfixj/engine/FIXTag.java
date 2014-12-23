/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 14 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

/**
 * FIXTag holds all admin message tag constants required by the engine.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public final class FIXTag {

    public static final int BEGIN_SEQ_NO = 7;

    public static final int BEGIN_STRING = 8;

    public static final int BODY_LENGTH = 9;

    public static final int CHECK_SUM = 10;

    public static final int MSG_SEQ_NUM = 34;

    public static final int MSG_TYPE = 35;

    public static final int END_SEQ_NO = 16;

    public static final int NEW_SEQ_NO = 36;

    public static final int POSS_DUP_FLAG = 43;

    public static final int REF_SEQ_NUM = 45;

    public static final int SENDER_COMP_ID = 49;

    public static final int SENDER_SUB_ID = 50;

    public static final int SENDING_TIME = 52;

    public static final int TARGET_COMP_ID = 56;

    public static final int TARGET_SUB_ID = 57;

    public static final int TEXT = 58;

    public static final int SIGNATURE = 89;

    public static final int SIGNATURE_LENGTH = 93;

    public static final int SECURE_DATA_LENGTH = 90;

    public static final int SECURE_DATA = 91;

    public static final int POSS_RESEND = 97;

    public static final int ENCRYPT_METHOD = 98;

    public static final int HEART_BT_INT = 108;

    public static final int TEST_REQ_ID = 112;

    public static final int ON_BEHALF_OF_COMP_ID = 115;

    public static final int ON_BEHALF_OF_SUB_ID = 116;

    public static final int ORIG_SENDING_TIME = 122;

    public static final int DELIVER_TO_COMP_ID = 128;

    public static final int DELIVER_TO_SUB_ID = 129;

    public static final int SENDER_LOCATION_ID = 142;

    public static final int ON_BEHALF_OF_LOCATION_ID = 144;

    public static final int TARGET_LOCATION_ID = 143;

    public static final int DELIVER_TO_LOCATION_ID = 145;

    public static final int GAP_FILL_FLAG = 123;

    public static final int RESET_SEQ_NUM_FLAG = 141;

    public static final int XML_DATA_LENGTH = 212;

    public static final int XML_DATA = 213;

    public static final int MESSAGE_ENCODING = 347;

    public static final int REF_TAG_ID = 371;

    public static final int REF_MSG_TYPE = 372;

    public static final int SESSION_REJECT_REASON = 373;

    public static final int LAST_MSG_SEQ_NUM_PROCESSED = 369;

    public static final int BUSINESS_REJECT_REASON = 380;

    public static final int NO_HOPS = 627;

    public static final int NEXT_EXPECTED_MSG_SEQ_NUM = 789;

    public static final int APPL_VER_ID = 1128;

    public static final int CSTM_APPL_VER_ID = 1129;

    public static final int DEFAULT_APPL_VER_ID = 1137;

    public static final int SESSION_STATUS = 1409;

    private FIXTag() {
        // private - close this class
        super();
    }
}
