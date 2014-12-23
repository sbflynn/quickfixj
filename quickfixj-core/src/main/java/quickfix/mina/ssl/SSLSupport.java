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

package quickfix.mina.ssl;

import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.field.FieldConversionException;

import quickfix.ConfigError;
import quickfix.SessionSettings;

public class SSLSupport {
    // This will be moved else when settings mechanism is refactored.

    public static final String FILTER_NAME = "SslFilter";
    public static final String SETTING_KEY_STORE_PWD = "SocketKeyStorePassword";
    public static final String SETTING_KEY_STORE_NAME = "SocketKeyStore";
    public static final String SETTING_USE_SSL = "SocketUseSSL";
    public static final String SETTING_ENABLE_PROTOCOLE = "EnabledProtocols";
    public static final String SETTING_CIPHER_SUITES = "CipherSuites";
    /* package */static final String QUICKFIXJ_CERT = "quickfixj.cert";
    /* package */static final String QUICKFIXJ_PW = "quickfixjpw";

    public static String getKeystoreName(SessionSettings settings, FIXSessionID sessionID) {
        String keyStoreName = QUICKFIXJ_CERT;
        if (settings.isSetting(sessionID, SSLSupport.SETTING_KEY_STORE_NAME)) {
            try {
                keyStoreName = settings.getString(sessionID, SSLSupport.SETTING_KEY_STORE_NAME);
            } catch (ConfigError ignored) {
                // ignore
            } catch (FieldConversionException ignored) {
                // ignore
            }
        }
        return keyStoreName;
    }

    public static String getKeystorePasswd(SessionSettings settings, FIXSessionID sessionID) {
        String keyStorePassword = QUICKFIXJ_PW;
        if (settings.isSetting(sessionID, SSLSupport.SETTING_KEY_STORE_PWD)) {
            try {
                keyStorePassword = settings.getString(sessionID, SSLSupport.SETTING_KEY_STORE_PWD);
            } catch (ConfigError ignored) {
                // ignore
            } catch (FieldConversionException ignored) {
                // ignore
            }
        }
        return keyStorePassword;
    }

    public static String getEnableProtocole(SessionSettings settings, FIXSessionID sessionID) {
        String strEnableProtocole = null;
        if (settings.isSetting(sessionID, SSLSupport.SETTING_ENABLE_PROTOCOLE)) {
            try {
                strEnableProtocole = settings.getString(sessionID,
                        SSLSupport.SETTING_ENABLE_PROTOCOLE);
            } catch (ConfigError ignored) {
                // ignore
            } catch (FieldConversionException ignored) {
                // ignore
            }
        }
        return strEnableProtocole;
    }

    public static String getCipherSuite(SessionSettings settings, FIXSessionID sessionID) {
        String strCipherSuite = null;
        if (settings.isSetting(sessionID, SSLSupport.SETTING_CIPHER_SUITES)) {
            try {
                strCipherSuite = settings.getString(sessionID, SSLSupport.SETTING_CIPHER_SUITES);
            } catch (ConfigError ignored) {
                // ignore
            } catch (FieldConversionException ignored) {
                // ignore
            }
        }
        return strCipherSuite;
    }

}
