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

import static quickfix.MessageUtils.toBeginString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.QFJException;

public class DefaultDataDictionaryProvider implements DataDictionaryProvider {

    private Map<FIXBeginString, DataDictionary> transportDictionaries = new ConcurrentHashMap<FIXBeginString, DataDictionary>();

    private Map<AppVersionKey, DataDictionary> applicationDictionaries = new ConcurrentHashMap<AppVersionKey, DataDictionary>();

    private final boolean findDataDictionaries;

    public DefaultDataDictionaryProvider() {

        findDataDictionaries = true;
    }

    public DefaultDataDictionaryProvider(boolean findDataDictionaries) {

        this.findDataDictionaries = findDataDictionaries;
    }

    @Override
    public synchronized DataDictionary getSessionDataDictionary(FIXBeginString beginString) {

        DataDictionary dd = transportDictionaries.get(beginString);
        if (dd == null && findDataDictionaries) {
            String path = beginString.getValue().replace(".", "") + ".xml";
            try {
                dd = new DataDictionary(path);
                transportDictionaries.put(beginString, dd);
            } catch (ConfigError e) {
                throw new QFJException(e);
            }
        }
        return dd;
    }

    @Override
    public DataDictionary getApplicationDataDictionary(FIXApplication applVerID) {

        AppVersionKey appVersionKey = new AppVersionKey(applVerID);
        DataDictionary dd = applicationDictionaries.get(appVersionKey);
        if (dd == null && findDataDictionaries) {
            FIXBeginString beginString = toBeginString(applVerID);
            String path = beginString.getValue().replace(".", "") + ".xml";
            try {
                dd = new DataDictionary(path);
                applicationDictionaries.put(appVersionKey, dd);
            } catch (ConfigError e) {
                throw new QFJException(e);
            }
        }
        return dd;
    }

    public void addTransportDictionary(FIXBeginString beginString, DataDictionary dd) {

        transportDictionaries.put(beginString, dd);
    }

    public void addApplicationDictionary(FIXApplication applVerID, DataDictionary dataDictionary) {

        applicationDictionaries.put(new AppVersionKey(applVerID), dataDictionary);
    }

    private static class AppVersionKey {

        private final FIXApplication applVerID;

        public AppVersionKey(FIXApplication applVerID) {

            this.applVerID = applVerID;
        }

        @Override
        public int hashCode() {

            final int prime = 31;
            int result = 1;
            result = prime * result + ((applVerID == null) ? 0 : applVerID.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AppVersionKey other = (AppVersionKey) obj;
            if (applVerID == null) {
                if (other.applVerID != null) {
                    return false;
                }
            } else if (!applVerID.equals(other.applVerID)) {
                return false;
            }
            return true;
        }
    }
}
