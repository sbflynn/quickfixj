/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 25 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix.examples.banzai;

import org.quickfixj.FIXMessage;
import org.quickfixj.FIXMessageHeader;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.field.GenericField;

import quickfix.FieldNotFound;

/**
 * BanzaiHandler - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public abstract class BanzaiHandler {

    private final BanzaiApplication application;

    protected BanzaiHandler(BanzaiApplication application) {
        this.application = application;
    }

    /**
     * Get the application property.
     *
     * @return Returns the application.
     * @since 2.0
     */
    protected BanzaiApplication getApplication() {
        return application;
    }

    /**
     * Get the orderTableModel property.
     *
     * @return Returns the orderTableModel.
     * @since 2.0
     */
    protected OrderTableModel getOrderTableModel() {
        return application.getOrderTableModel();
    }

    /**
     * Get the executionTableModel property.
     *
     * @return Returns the executionTableModel.
     * @since 2.0
     */
    public ExecutionTableModel getExecutionTableModel() {
        return application.getExecutionTableModel();
    }

    protected final void send(FIXMessage message, FIXSessionID sessionID) {
        try {

            message.getHeader().setField(new GenericField(FIXTag.MSG_TYPE, message.getMsgType()));

            application.getEngine().send(message, sessionID);

        } catch (SessionNotFoundException e) {
            System.out.println(e);
        }
    }

    protected final void reverseRoute(FIXMessage message, FIXMessage reply) throws FieldNotFound {

        FIXMessageHeader header = message.getHeader();

        reply.getHeader()
                .setField(
                        new GenericField(FIXTag.SENDER_COMP_ID, header
                                .getFieldValue(FIXTag.TARGET_COMP_ID)));
        reply.getHeader()
                .setField(
                        new GenericField(FIXTag.TARGET_COMP_ID, header
                                .getFieldValue(FIXTag.SENDER_COMP_ID)));
    }

    protected abstract void send(Order order);

    protected abstract void cancel(Order order);

    protected abstract void replace(Order order, Order newOrder);

    protected abstract void process(FIXMessage message, FIXSessionID sessionID);

    protected abstract void sendBusinessReject(FIXSessionID sessionID, FIXMessage message,
            int rejectReason, String rejectText) throws SessionNotFoundException;
}
