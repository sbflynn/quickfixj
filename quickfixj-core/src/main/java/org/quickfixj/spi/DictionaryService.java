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
package org.quickfixj.spi;

import java.util.Map;

import org.quickfixj.xml.dictionary.DataDictionaryConfig;
import org.quickfixj.xml.dictionary.Engine;

/**
 * DictionaryService - interface to support Jave SE 1.6 service discovery.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface DictionaryService {

    Engine getEngine();

    Map<String, DataDictionaryConfig> getDictionaries();
}
