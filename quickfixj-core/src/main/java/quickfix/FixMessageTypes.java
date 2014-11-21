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
 * FixMessageTypes - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class FixMessageTypes {

    public static final String HEARTBEAT = "0";

    public static final String TEST_REQUEST = "1";

    public static final String RESEND_REQUEST = "2";

    public static final String REJECT = "3";

    public static final String SEQUENCE_RESET = "4";

    public static final String LOGOUT = "5";

    public static final String LOGON = "A";

    public static final String BUSINESS_MESSAGE_REJECT = "j";

}
