/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 17 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

import java.io.IOException;
import java.net.InetAddress;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;

/**
 * FIXSession - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXSession {

    /**
     * @since 2.0
     */
    FIXSessionID getSessionID();

    /**
     * @since 2.0
     */
    boolean isLoggedOn();

    /**
     * 
     * @since 2.0
     */
    void logout();

    /**
     * 
     * @since 2.0
     */
    int getLogoutTimeout();

    /**
     * Logs out from session and closes the network connection.
     *
     * @param reason the reason why the session is disconnected
     * @param logError set to true if this disconnection is an error
     * @throws IOException IO error
     */
    void disconnect(String reason, boolean logError) throws IOException;

    /**
     * @since 2.0
     */
    void close() throws IOException;

    /**
     * @since 2.0
     */
    Log getLog();

    /**
     * @since 2.0
     */
    FIXMessageBuilderFactory getMessageFactory();

    /**
     * @since 2.0
     */
    FIXMessageDictionaryFactory getDataDictionary();

    /**
     * @since 2.0
     */
    FIXApplication getTargetDefaultApplicationVersionID();

    /**
     * @since 2.0
     */
    String getRemoteAddress();

    /**
     * @param remoteInetAddress
     * @since 2.0
     */
    boolean isAllowedForSession(InetAddress remoteInetAddress);

    /**
     * @param heartbeatInterval
     * @since 2.0
     */
    void setHeartBeatInterval(int heartbeatInterval);

    /**
     * @since 2.0
     */
    boolean isSessionTime();

    /**
     * @since 2.0
     */
    boolean isEnabled();

    /**
     * 
     * @since 2.0
     */
    void logon();

    /**
     * 
     * @since 2.0
     */
    void reset() throws IOException;

    /**
     * @since 2.0
     */
    boolean getResetOnLogout();

    /**
     * @since 2.0
     */
    boolean getRefreshOnLogon();

    /**
     * @since 2.0
     */
    boolean getCheckCompID();

    public interface FIXSessionID {

        /**
         * @since 2.0
         */
        FIXBeginString getBeginString();

        /**
         * @since 2.0
         */
        String getSenderCompID();

        /**
         * @since 2.0
         */
        String getTargetCompID();

        /**
         * @since 2.0
         */
        String getSenderSubID();

        /**
         * @since 2.0
         */
        String getSenderLocationID();

        /**
         * @since 2.0
         */
        String getTargetSubID();

        /**
         * @since 2.0
         */
        String getTargetLocationID();

        /**
         * @since 2.0
         */
        String getSessionQualifier();
    }
}
