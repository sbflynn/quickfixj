/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.engine.FIXSession.FIXSessionID;

/**
 * Identifier for a session. Only supports a company ID (target, sender) and a
 * session qualifier. Sessions are also identified by FIX version so that it's
 * possible to have multiple sessions to the same counterparty but using
 * different FIX versions (and/or session qualifiers).
 */
public class SessionID implements FIXSessionID, Serializable {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private static Pattern pattern = Pattern
            .compile("(.*?):(.*?)(?:/(.*?)|)(?:/(.*?)|)->(.*?)(?:/(.*?)|)(?:/(.*?)|)(?::(.*)|)");

    public static final String NOT_SET = "";

    private final String id;

    private final FIXBeginString beginString;

    private final String senderCompID;

    private final String senderSubID;

    private final String senderLocationID;

    private final String targetCompID;

    private final String targetSubID;

    private final String targetLocationID;

    private final String sessionQualifier;

    public SessionID(FIXBeginString beginString, String senderCompID, String senderSubID,
            String senderLocationID, String targetCompID, String targetSubID,
            String targetLocationID, String sessionQualifier) {

        this.beginString = beginString;
        this.senderCompID = value(senderCompID);
        this.senderSubID = value(senderSubID);
        this.senderLocationID = value(senderLocationID);
        this.targetCompID = value(targetCompID);
        this.targetSubID = value(targetSubID);
        this.targetLocationID = value(targetLocationID);
        this.sessionQualifier = value(sessionQualifier);
        id = createID();
    }

    public SessionID(FIXBeginString beginString, FIXField<String> senderCompID,
            FIXField<String> senderSubID, FIXField<String> senderLocationID,
            FIXField<String> targetCompID, FIXField<String> targetSubID,
            FIXField<String> targetLocationID, String qualifier) {

        this(beginString, value(senderCompID), value(senderSubID), value(senderLocationID),
                value(targetCompID), value(targetSubID), value(targetLocationID), value(qualifier));
    }

    public SessionID(FIXBeginString beginString, String senderCompID, String senderSubID,
            String targetCompID, String targetSubID) {

        this(beginString, senderCompID, senderSubID, NOT_SET, targetCompID, targetSubID, NOT_SET,
                NOT_SET);
    }

    public SessionID(FIXBeginString beginString, FIXField<String> senderCompID,
            FIXField<String> senderSubID, FIXField<String> targetCompID,
            FIXField<String> targetSubID) {

        this(beginString, value(senderCompID), value(senderSubID), value(targetCompID),
                value(targetSubID));
    }

    public SessionID(FIXBeginString beginString, String senderCompID, String targetCompID,
            String qualifier) {

        this(beginString, senderCompID, NOT_SET, NOT_SET, targetCompID, NOT_SET, NOT_SET, qualifier);
    }

    public SessionID(FIXBeginString beginString, FIXField<String> senderCompID,
            FIXField<String> targetCompID, String qualifier) {

        this(beginString, value(senderCompID), value(targetCompID), value(qualifier));
    }

    public SessionID(FIXBeginString beginString, String senderCompID, String targetCompID) {

        this(beginString, senderCompID, NOT_SET, NOT_SET, targetCompID, NOT_SET, NOT_SET, NOT_SET);
    }

    public SessionID(FIXBeginString beginString, FIXField<String> senderCompID,
            FIXField<String> targetCompID) {

        this(beginString, value(senderCompID), value(targetCompID));
    }

    public SessionID() {

        throw new UnsupportedOperationException(
                "Unsupported QuickFIX feature: use constructor with arguments");
    }

    public SessionID(String id) {

        Matcher matcher = pattern.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid session ID string: " + id);
        }
        beginString = FIXBeginString.parse(matcher.group(1));
        senderCompID = matcher.group(2);
        senderSubID = value(matcher.group(3));
        senderLocationID = value(matcher.group(4));
        targetCompID = matcher.group(5);
        targetSubID = value(matcher.group(6));
        targetLocationID = value(matcher.group(7));
        sessionQualifier = value(matcher.group(8));
        this.id = createID();
    }

    @Override
    public FIXBeginString getBeginString() {

        return beginString;
    }

    @Override
    public String getSenderCompID() {

        return senderCompID;
    }

    @Override
    public String getTargetCompID() {

        return targetCompID;
    }

    @Override
    public String getSenderSubID() {

        return senderSubID;
    }

    @Override
    public String getSenderLocationID() {

        return senderLocationID;
    }

    @Override
    public String getTargetSubID() {

        return targetSubID;
    }

    @Override
    public String getTargetLocationID() {

        return targetLocationID;
    }

    /**
     * Session qualifier can be used to identify different sessions for the same
     * target company ID. Session qualifiers can only me used with initiated
     * sessions. They cannot be used with accepted sessions.
     *
     * @return the session qualifier
     */
    @Override
    public String getSessionQualifier() {

        return sessionQualifier;
    }

    @Override
    public boolean equals(Object object) {

        return object != null && toString().equals(object.toString());
    }

    @Override
    public String toString() {

        return id;
    }

    @Override
    public int hashCode() {

        return toString().hashCode();
    }

    private String createID() {

        return FIXBeginString.print(beginString)
                + ":"
                + senderCompID
                + (isSet(senderSubID) ? "/" + senderSubID : "")
                + (isSet(senderLocationID) ? "/" + senderLocationID : "")
                + "->"
                + targetCompID
                + (isSet(targetSubID) ? "/" + targetSubID : "")
                + (isSet(targetLocationID) ? "/" + targetLocationID : "")
                + (sessionQualifier != null && !sessionQualifier.equals(NOT_SET) ? ":"
                        + sessionQualifier : NOT_SET);
    }

    private boolean isSet(String value) {
        return !value.equals(NOT_SET);
    }

    private static String value(FIXField<?> f) {
        return f != null ? f.getCharacters().toString() : NOT_SET;
    }

    private static String value(String s) {
        return s == null ? NOT_SET : s;
    }
}
