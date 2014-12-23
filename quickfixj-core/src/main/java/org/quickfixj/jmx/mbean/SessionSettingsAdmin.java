/*******************************************************************************
 * Copyright (c) quickfixj.org  All rights reserved.
 *
 * This file is part of the QuickFIX/J FIX Engine
 *
 * This file may be distributed under the terms of the quickfixj.org
 * license as defined by quickfixj.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixj.org/LICENSE for licensing information.
 *
 ******************************************************************************/

package org.quickfixj.jmx.mbean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.quickfixj.engine.FIXSession.FIXSessionID;

import quickfix.ConfigError;
import quickfix.SessionSettings;

/**
 * This is a read-only view of a sessions settings.
 *
 * TODO JMX Add ability to identify defaults
 */
public class SessionSettingsAdmin implements DynamicMBean {

    private Properties settings;

    private FIXSessionID sessionID;

    public SessionSettingsAdmin(FIXSessionID sessionID, SessionSettings settings)
            throws ConfigError {
        this.sessionID = sessionID;
        Properties p = new Properties();
        p.putAll(settings.getDefaultProperties());
        p.putAll(settings.getSessionProperties(sessionID));
        this.settings = p;
    }

    @Override
    public Object getAttribute(String attribute) {
        return settings.get(attribute);
    }

    @Override
    public AttributeList getAttributes(String[] attributeNames) {
        AttributeList attributeList = new AttributeList();
        for (String attributeName : attributeNames) {
            attributeList.add(new Attribute(attributeName, getAttribute(attributeName)));
        }
        return attributeList;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        List<MBeanAttributeInfo> attributeInfos = new ArrayList<MBeanAttributeInfo>();
        for (Map.Entry<Object, Object> entry : settings.entrySet()) {
            String name = (String) entry.getKey();
            attributeInfos.add(new MBeanAttributeInfo(name, "Setting for " + name, entry.getValue()
                    .getClass().getName(), true, false, false));
        }
        return new MBeanInfo(SessionSettings.class.getName(), "Session Settings",
                attributeInfos.toArray(new MBeanAttributeInfo[attributeInfos.size()]), null, null,
                null);
    }

    @Override
    public Object invoke(String method, Object[] arguments, String[] params) throws MBeanException,
            ReflectionException {
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        // no-op
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    public FIXSessionID getSessionID() {
        return sessionID;
    }
}
