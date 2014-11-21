/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 14 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

/**
 * Tags - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FixTags {

    static final int BEGIN_SEQ_NO = 7;

    static final int BEGIN_STRING = 8;

    static final int BODY_LENGTH = 9;

    static final int CHECK_SUM = 10;

    static final int END_SEQ_NO = 16;

    static final int MSG_SEQ_NUM = 34;

    static final int MSG_TYPE = 35;

    static final int NEW_SEQ_NO = 36;

    static final int POSS_DUP_FLAG = 43;

    static final int REF_SEQ_NUM = 45;

    static final int SENDER_COMP_ID = 49;

    static final int SENDER_SUB_ID = 50;

    static final int SENDING_TIME = 52;

    static final int TARGET_COMP_ID = 56;

    static final int TARGET_SUB_ID = 57;

    static final int TEXT = 58;

    static final int SIGNATURE = 89;

    static final int SIGNATURE_LENGTH = 93;

    static final int SECURE_DATA_LEN = 90;

    static final int POSS_RESEND = 97;

    static final int ENCRYPT_METHOD = 98;

    static final int HEART_BT_INT = 108;

    static final int TEST_REQ_ID = 112;

    static final int ON_BEHALF_OF_COMP_ID = 115;

    static final int ON_BEHALF_OF_SUB_ID = 116;

    static final int ORIG_SENDING_TIME = 122;

    static final int GAP_FILL_FLAG = 123;

    static final int DELIVER_TO_COMP_ID = 128;

    static final int DELIVER_TO_SUB_ID = 129;

    static final int RESET_SEQ_NUM_FLAG = 141;

    static final int SENDER_LOCATION_ID = 142;

    static final int TARGET_LOCATION_ID = 143;

    static final int ON_BEHALF_OF_LOCATION_ID = 144;

    static final int DELIVER_TO_LOCATION_ID = 145;

    static final int XML_DATA_LENGTH = 212;

    static final int XML_DATA = 213;

    static final int MESSAGE_ENCODING = 347;

    static final int LAST_MSG_SEQ_NUM_PROCESSED = 369;

    static final int ON_BEHALF_OF_SENDING_TIME = 370;

    static final int REF_TAG_ID = 371;

    static final int REF_MSG_TYPE = 372;

    static final int SESSION_REJECT_REASON = 373;

    static final int BUSINESS_REJECT_REASON = 380;

    static final int NO_HOPS = 627;

    static final int NEXT_EXPECTED_MSG_SEQ_NUM = 789;

    static final int APPL_VER_ID = 1128;

    static final int CSTM_APPL_VER_ID = 1129;

    static final int DEFAULT_APPL_VER_ID = 1137;

    static final int SESSION_STATUS = 1409;

}
