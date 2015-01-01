/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 13 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXFieldType;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.GraphDictionary.FieldDictionary;
import org.quickfixj.engine.GraphDictionary.GroupDictionary;

/**
 * SystemDataDictionary - provides a default 'no-knowledge' or 'null' implementation of {@link FIXMessageDictionaryFactory}.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class GenericMessageDictionaryFactory implements FIXMessageDictionaryFactory {

    private final String namespace;
    private final FIXBeginString beginString;

    public GenericMessageDictionaryFactory(String namespace, FIXBeginString beginString) {
        this.namespace = namespace;
        this.beginString = beginString;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXBeginString getBeginString() {
        return beginString;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageDictionary getMessageDictionary(FIXApplication application, String msgType) {

        return new MessageDictionaryImpl(application, msgType);
    }

    private class MessageDictionaryImpl implements FIXMessageDictionary {

        private final FIXApplication application;
        private final String msgType;

        MessageDictionaryImpl(FIXApplication application, String msgType) {
            this.application = application;
            this.msgType = msgType;
        }

        /**
        * {@inheritDoc}
        *
        * @since 2.0
        */
        @Override
        public FIXApplication getApplication() {
            return application;
        }

        /**
        * {@inheritDoc}
        *
        * @since 2.0
        */
        @Override
        public String getMsgType() {
            return msgType;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isValid(int tag) {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isField(int tag) {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isGroupField(int tag) {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public GroupDictionary getGroupDictionary(int tag) {
            return new GroupDictionaryImpl();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FieldDictionary getFieldDictionary(int tag) {
            return new FieldDictionaryImpl();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isTrailerField(int tag) {
            switch (tag) {
            case FIXTag.SIGNATURE_LENGTH:
            case FIXTag.SIGNATURE:
            case FIXTag.CHECK_SUM:
                return true;
            default:
                return false;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isHeaderField(int tag) {

            switch (tag) {
            case FIXTag.BEGIN_STRING:
            case FIXTag.BODY_LENGTH:
            case FIXTag.MSG_TYPE:
            case FIXTag.SENDER_COMP_ID:
            case FIXTag.TARGET_COMP_ID:
            case FIXTag.ON_BEHALF_OF_COMP_ID:
            case FIXTag.DELIVER_TO_COMP_ID:
            case FIXTag.SECURE_DATA_LENGTH:
            case FIXTag.SECURE_DATA:
            case FIXTag.MSG_SEQ_NUM:
            case FIXTag.SENDER_SUB_ID:
            case FIXTag.SENDER_LOCATION_ID:
            case FIXTag.TARGET_SUB_ID:
            case FIXTag.TARGET_LOCATION_ID:
            case FIXTag.ON_BEHALF_OF_SUB_ID:
            case FIXTag.ON_BEHALF_OF_LOCATION_ID:
            case FIXTag.DELIVER_TO_SUB_ID:
            case FIXTag.DELIVER_TO_LOCATION_ID:
            case FIXTag.POSS_DUP_FLAG:
            case FIXTag.POSS_RESEND:
            case FIXTag.SENDING_TIME:
            case FIXTag.ORIG_SENDING_TIME:
            case FIXTag.XML_DATA_LENGTH:
            case FIXTag.XML_DATA:
            case FIXTag.MESSAGE_ENCODING:
            case FIXTag.LAST_MSG_SEQ_NUM_PROCESSED:
            case FIXTag.APPL_VER_ID:
            case FIXTag.CSTM_APPL_VER_ID:
            case FIXTag.NO_HOPS:
                return true;
            default:
                return false;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Set<Integer> getRequiredFields() {
            return Collections.emptySet();
        }
    }

    private class GroupDictionaryImpl implements GroupDictionary {

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Set<Integer> getRequiredFields() {
            return Collections.emptySet();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isField(int tag) {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isValid(int tag) {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isGroupField(int tag) {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public GroupDictionary getGroupDictionary(int tag) {
            return new GroupDictionaryImpl();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FieldDictionary getFieldDictionary(int tag) {
            return new FieldDictionaryImpl();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public int getDelimiterField() {
            return -1;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public List<Integer> getOrderedFields() {
            return Collections.emptyList();
        }
    }

    private class FieldDictionaryImpl implements FieldDictionary {

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public String getName() {
            return null;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         * @deprecated
         */
        @Deprecated
        @Override
        public boolean isDataField() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Set<String> getFieldValues() {
            return Collections.emptySet();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean allowOtherFieldValues() {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXFieldType getType() {
            return FIXFieldType.UNKNOWN;
        }
    }
}
