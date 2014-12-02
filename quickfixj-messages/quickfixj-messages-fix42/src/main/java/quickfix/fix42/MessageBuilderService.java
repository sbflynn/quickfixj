/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix.fix42;

import javax.xml.bind.JAXBException;

import org.quickfixj.spi.MessageBuilderServiceSupport;
import org.xml.sax.SAXException;

/**
 * MessageBuilderService - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class MessageBuilderService extends MessageBuilderServiceSupport {

    /**
     * @throws JAXBException
     * @throws SAXException
     * @since 2.0
     */
    public MessageBuilderService() throws SAXException, JAXBException {

        super("/FIX42.xml", "quickfix.fix42");
    }
}
