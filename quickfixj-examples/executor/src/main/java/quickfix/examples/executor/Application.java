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

package quickfix.examples.executor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.field.GenericField;
import org.quickfixj.messages.bd.fix40.ExecutionReport;
import org.quickfixj.messages.bd.fix40.NewOrderSingle;
import org.quickfixj.messages.bd.fix40.field.AvgPx;
import org.quickfixj.messages.bd.fix40.field.CumQty;
import org.quickfixj.messages.bd.fix40.field.ExecID;
import org.quickfixj.messages.bd.fix40.field.ExecTransType;
import org.quickfixj.messages.bd.fix40.field.LastPx;
import org.quickfixj.messages.bd.fix40.field.LastShares;
import org.quickfixj.messages.bd.fix40.field.OrdStatus;
import org.quickfixj.messages.bd.fix40.field.OrderID;
import org.quickfixj.messages.bd.fix40.field.OrderQty;
import org.quickfixj.messages.bd.fix40.field.Price;
import org.quickfixj.messages.bd.fix50.field.OrdType;
import org.quickfixj.messages.bd.fix50.field.Side;
import org.quickfixj.messages.bd.fix50.field.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.ConfigError;
import quickfix.DefaultValidator;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.LogUtil;
import quickfix.MessageCracker;
import quickfix.MessageUtils;
import quickfix.Session;
import quickfix.SessionSettings;

public class Application extends MessageCracker implements quickfix.Application {
    private static final String DEFAULT_MARKET_PRICE_KEY = "DefaultMarketPrice";
    private static final String ALWAYS_FILL_LIMIT_KEY = "AlwaysFillLimitOrders";
    private static final String VALID_ORDER_TYPES_KEY = "ValidOrderTypes";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final boolean alwaysFillLimitOrders;
    private final HashSet<String> validOrderTypes = new HashSet<String>();
    private MarketDataProvider marketDataProvider;

    public Application(SessionSettings settings) throws ConfigError {
        initializeValidOrderTypes(settings);
        initializeMarketDataProvider(settings);

        if (settings.isSetting(ALWAYS_FILL_LIMIT_KEY)) {
            alwaysFillLimitOrders = settings.getBool(ALWAYS_FILL_LIMIT_KEY);
        } else {
            alwaysFillLimitOrders = false;
        }
    }

    private void initializeMarketDataProvider(SessionSettings settings) throws ConfigError {
        if (settings.isSetting(DEFAULT_MARKET_PRICE_KEY)) {
            if (marketDataProvider == null) {
                final double defaultMarketPrice = settings.getDouble(DEFAULT_MARKET_PRICE_KEY);
                marketDataProvider = new MarketDataProvider() {
                    @Override
                    public double getAsk(String symbol) {
                        return defaultMarketPrice;
                    }

                    @Override
                    public double getBid(String symbol) {
                        return defaultMarketPrice;
                    }
                };
            } else {
                log.warn("Ignoring " + DEFAULT_MARKET_PRICE_KEY
                        + " since provider is already defined.");
            }
        }
    }

    private void initializeValidOrderTypes(SessionSettings settings) throws ConfigError {
        if (settings.isSetting(VALID_ORDER_TYPES_KEY)) {
            List<String> orderTypes = Arrays.asList(settings.getString(VALID_ORDER_TYPES_KEY)
                    .trim().split("\\s*,\\s*"));
            validOrderTypes.addAll(orderTypes);
        } else {
            validOrderTypes.add(OrdType.LIMIT + "");
        }
    }

    @Override
    public void onCreate(FIXSessionID sessionID) {
        Session.lookupSession(sessionID).getLog().onEvent("Valid order types: " + validOrderTypes);
    }

    @Override
    public void onLogon(FIXSessionID sessionID) {
        //no-op
    }

    @Override
    public void onLogout(FIXSessionID sessionID) {
        //no-op
    }

    @Override
    public void toAdmin(FIXMessage message, FIXSessionID sessionID) {
        //no-op
    }

    @Override
    public void toApp(FIXMessage message, FIXSessionID sessionID) throws DoNotSend {
        //no-op
    }

    @Override
    public void fromAdmin(FIXMessage message, FIXSessionID sessionID) {
        //no-op
    }

    @Override
    public void fromApp(FIXMessage message, FIXSessionID sessionID) {
        crack(message, sessionID);
    }

    public void onMessage(NewOrderSingle order, FIXSessionID sessionID) {
        try {

            // earlier FIX versions used string not character values
            validateOrder(new org.quickfixj.messages.bd.fix44.field.OrdType(order.getOrdType()
                    .getCharacters().charAt(0)));

            double price = getPrice(order);

            OrderQty orderQty = order.getOrderQty();

            ExecutionReport accept = new ExecutionReport(new OrderID(genOrderID()), new ExecID(
                    genExecIDAsInt()), ExecTransType.NEW, OrdStatus.NEW, order.getSymbol(),
                    order.getSide(), orderQty, new LastShares(0), new LastPx(0), new CumQty(0),
                    new AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                ExecutionReport fill = new ExecutionReport(new OrderID(genOrderID()), new ExecID(
                        genExecIDAsInt()), ExecTransType.NEW, OrdStatus.FILLED, order.getSymbol(),
                        order.getSide(), orderQty, new LastShares(orderQty.getValue()), new LastPx(
                                price), new CumQty(orderQty.getValue()), new AvgPx(price));

                fill.setClOrdID(order.getClOrdID());

                sendMessage(sessionID, fill);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    private boolean isOrderExecutable(FIXMessage order, double price) throws FieldNotFound {
        if (MessageUtils.coerceToChar(order, OrdType.TAG) == OrdType.LIMIT.getValue()) {
            BigDecimal limitPrice = new BigDecimal(order.getFieldValue(Price.TAG));
            char side = MessageUtils.coerceToChar(order, Side.TAG);
            BigDecimal thePrice = new BigDecimal(price);

            return (side == Side.BUY.getValue() && thePrice.compareTo(limitPrice) <= 0)
                    || ((side == Side.SELL.getValue() || side == Side.SELL_SHORT.getValue()) && thePrice
                            .compareTo(limitPrice) >= 0);
        }
        return true;
    }

    private double getPrice(FIXMessage message) throws FieldNotFound {

        if (MessageUtils.coerceToChar(message, OrdType.TAG) == OrdType.LIMIT.getValue()
                && alwaysFillLimitOrders) {
            return MessageUtils.coerceToDouble(message, Price.TAG);
        }

        if (marketDataProvider == null) {
            throw new RuntimeException("No market data provider specified for market order");
        }

        char side = MessageUtils.coerceToChar(message, OrdType.TAG);
        if (side == Side.BUY.getValue()) {
            return marketDataProvider.getAsk(message.getFieldValue(Symbol.TAG));
        } else if (side == Side.SELL.getValue() || side == Side.SELL_SHORT.getValue()) {
            return marketDataProvider.getBid(message.getFieldValue(Symbol.TAG));
        } else {
            throw new RuntimeException("Invalid order side: " + side);
        }
    }

    void sendMessage(FIXSessionID sessionID, FIXMessage message) {
        try {
            Session session = Session.lookupSession(sessionID);
            if (session == null) {
                throw new SessionNotFoundException(sessionID.toString());
            }

            message.getHeader().setField(new GenericField(FIXTag.MSG_TYPE, message.getMsgType()));

            FIXMessageDictionaryFactory dictionaryFactory = session.getDataDictionary();

            if (dictionaryFactory != null) {
                try {
                    FIXMessageDictionary messageDictionary = dictionaryFactory
                            .getMessageDictionary(getApplVerID(session), message.getMsgType());
                    new DefaultValidator(sessionID.getBeginString()).validate(messageDictionary,
                            message, true);
                } catch (Exception e) {
                    LogUtil.logThrowable(sessionID,
                            "Outgoing message failed validation: " + e.getMessage(), e);
                    return;
                }
            }

            session.send(message);

        } catch (SessionNotFoundException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Get the marketDataProvider property.
     *
     * @return Returns the marketDataProvider.
     * @since 2.0
     */
    public MarketDataProvider getMarketDataProvider() {
        return marketDataProvider;
    }

    public double getBid(String symbol) {

        if (getMarketDataProvider() == null) {
            throw new RuntimeException("No market data provider specified for market order");
        }

        return getMarketDataProvider().getBid(symbol);
    }

    public double getAsk(String symbol) {

        if (getMarketDataProvider() == null) {
            throw new RuntimeException("No market data provider specified for market order");
        }

        return getMarketDataProvider().getAsk(symbol);
    }

    /**
     * Get the validOrderTypes property.
     *
     * @return Returns the validOrderTypes.
     * @since 2.0
     */
    public HashSet<String> getValidOrderTypes() {
        return validOrderTypes;
    }

    /**
     * Get the log property.
     *
     * @return Returns the log.
     * @since 2.0
     */
    public Logger getLog() {
        return log;
    }

    /**
     * Get the alwaysFillLimitOrders property.
     *
     * @return Returns the alwaysFillLimitOrders.
     * @since 2.0
     */
    public boolean isAlwaysFillLimitOrders() {
        return alwaysFillLimitOrders;
    }

    private FIXApplication getApplVerID(Session session) {
        FIXBeginString beginString = session.getSessionID().getBeginString();
        if (FIXBeginString.FIXT11.equals(beginString)) {
            return FIXApplication.FIX50;
        }

        return MessageUtils.toApplVerID(beginString);
    }

    public void onMessage(org.quickfixj.messages.bd.fix41.NewOrderSingle order,
            FIXSessionID sessionID) throws FieldNotFound, IncorrectTagValue {
        try {

            // earlier FIX versions used string not character values
            validateOrder(new org.quickfixj.messages.bd.fix44.field.OrdType(order.getOrdType()
                    .getCharacters().charAt(0)));

            double price = getPrice(order);

            org.quickfixj.messages.bd.fix41.field.OrderQty orderQty = order.getOrderQty();

            org.quickfixj.messages.bd.fix41.ExecutionReport accept = new org.quickfixj.messages.bd.fix41.ExecutionReport(
                    new org.quickfixj.messages.bd.fix41.field.OrderID(genOrderID()),
                    new org.quickfixj.messages.bd.fix41.field.ExecID(genExecID()),
                    org.quickfixj.messages.bd.fix41.field.ExecTransType.NEW,
                    org.quickfixj.messages.bd.fix41.field.ExecType.FILL,
                    org.quickfixj.messages.bd.fix41.field.OrdStatus.NEW, order.getSymbol(),
                    order.getSide(), order.getOrderQty(),
                    new org.quickfixj.messages.bd.fix41.field.LastShares(0),
                    new org.quickfixj.messages.bd.fix41.field.LastPx(0),
                    new org.quickfixj.messages.bd.fix41.field.LeavesQty(0),
                    new org.quickfixj.messages.bd.fix41.field.CumQty(0),
                    new org.quickfixj.messages.bd.fix41.field.AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                org.quickfixj.messages.bd.fix41.ExecutionReport executionReport = new org.quickfixj.messages.bd.fix41.ExecutionReport(
                        new org.quickfixj.messages.bd.fix41.field.OrderID(genOrderID()),
                        new org.quickfixj.messages.bd.fix41.field.ExecID(genExecID()),
                        org.quickfixj.messages.bd.fix41.field.ExecTransType.NEW,
                        org.quickfixj.messages.bd.fix41.field.ExecType.FILL,
                        org.quickfixj.messages.bd.fix41.field.OrdStatus.FILLED, order.getSymbol(),
                        order.getSide(), order.getOrderQty(),
                        new org.quickfixj.messages.bd.fix41.field.LastShares(orderQty.getValue()),
                        new org.quickfixj.messages.bd.fix41.field.LastPx(price),
                        new org.quickfixj.messages.bd.fix41.field.LeavesQty(0),
                        new org.quickfixj.messages.bd.fix41.field.CumQty(orderQty.getValue()),
                        new org.quickfixj.messages.bd.fix41.field.AvgPx(price));

                executionReport.setClOrdID(order.getClOrdID());

                sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix42.NewOrderSingle order,
            FIXSessionID sessionID) throws FieldNotFound, IncorrectTagValue {
        try {

            // earlier FIX versions used string not character values
            validateOrder(new org.quickfixj.messages.bd.fix44.field.OrdType(order.getOrdType()
                    .getCharacters().charAt(0)));

            double price = getPrice(order);

            org.quickfixj.messages.bd.fix42.field.OrderQty orderQty = order.getOrderQty();

            org.quickfixj.messages.bd.fix42.ExecutionReport accept = new org.quickfixj.messages.bd.fix42.ExecutionReport(
                    new org.quickfixj.messages.bd.fix42.field.OrderID(genOrderID()),
                    new org.quickfixj.messages.bd.fix42.field.ExecID(genExecID()),
                    org.quickfixj.messages.bd.fix42.field.ExecTransType.NEW,
                    org.quickfixj.messages.bd.fix42.field.ExecType.FILL,
                    org.quickfixj.messages.bd.fix42.field.OrdStatus.NEW, order.getSymbol(),
                    order.getSide(), new org.quickfixj.messages.bd.fix42.field.LeavesQty(0),
                    new org.quickfixj.messages.bd.fix42.field.CumQty(0),
                    new org.quickfixj.messages.bd.fix42.field.AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                org.quickfixj.messages.bd.fix42.ExecutionReport executionReport = new org.quickfixj.messages.bd.fix42.ExecutionReport(
                        new org.quickfixj.messages.bd.fix42.field.OrderID(genOrderID()),
                        new org.quickfixj.messages.bd.fix42.field.ExecID(genExecID()),
                        org.quickfixj.messages.bd.fix42.field.ExecTransType.NEW,
                        org.quickfixj.messages.bd.fix42.field.ExecType.FILL,
                        org.quickfixj.messages.bd.fix42.field.OrdStatus.FILLED, order.getSymbol(),
                        order.getSide(), new org.quickfixj.messages.bd.fix42.field.LeavesQty(0),
                        new org.quickfixj.messages.bd.fix42.field.CumQty(orderQty.getValue()),
                        new org.quickfixj.messages.bd.fix42.field.AvgPx(price));

                executionReport.setClOrdID(order.getClOrdID());
                executionReport.setOrderQty(orderQty);
                executionReport.setLastShares(new org.quickfixj.messages.bd.fix42.field.LastShares(
                        orderQty.getValue()));
                executionReport.setLastPx(new org.quickfixj.messages.bd.fix42.field.LastPx(price));

                sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    private void validateOrder(FIXField<Character> ordType) throws IncorrectTagValue, FieldNotFound {

        if (!validOrderTypes.contains(Character.toString(ordType.getValue()))) {
            log.error("Order type not in ValidOrderTypes setting");
            throw new IncorrectTagValue(ordType.getTag());
        }
        if (ordType.getValue() == OrdType.MARKET.getValue() && marketDataProvider == null) {
            log.error("DefaultMarketPrice setting not specified for market order");
            throw new IncorrectTagValue(ordType.getTag());
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix43.NewOrderSingle order,
            FIXSessionID sessionID) throws FieldNotFound, IncorrectTagValue {
        try {
            validateOrder(order.getOrdType());

            double price = getPrice(order);

            org.quickfixj.messages.bd.fix43.field.OrderQty orderQty = order.getOrderQty();

            org.quickfixj.messages.bd.fix43.ExecutionReport accept = new org.quickfixj.messages.bd.fix43.ExecutionReport(
                    new org.quickfixj.messages.bd.fix43.field.OrderID(genOrderID()),
                    new org.quickfixj.messages.bd.fix43.field.ExecID(genExecID()),
                    org.quickfixj.messages.bd.fix43.field.ExecType.FILL,
                    org.quickfixj.messages.bd.fix43.field.OrdStatus.NEW, order.getSide(),
                    new org.quickfixj.messages.bd.fix43.field.LeavesQty(order.getOrderQty()
                            .getValue()), new org.quickfixj.messages.bd.fix43.field.CumQty(0),
                    new org.quickfixj.messages.bd.fix43.field.AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            accept.setSymbol(order.getSymbol());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                org.quickfixj.messages.bd.fix43.ExecutionReport executionReport = new org.quickfixj.messages.bd.fix43.ExecutionReport(
                        new org.quickfixj.messages.bd.fix43.field.OrderID(genOrderID()),
                        new org.quickfixj.messages.bd.fix43.field.ExecID(genExecID()),
                        org.quickfixj.messages.bd.fix43.field.ExecType.FILL,
                        org.quickfixj.messages.bd.fix43.field.OrdStatus.FILLED, order.getSide(),
                        new org.quickfixj.messages.bd.fix43.field.LeavesQty(0),
                        new org.quickfixj.messages.bd.fix43.field.CumQty(orderQty.getValue()),
                        new org.quickfixj.messages.bd.fix43.field.AvgPx(price));

                executionReport.setClOrdID(order.getClOrdID());
                executionReport.setSymbol(order.getSymbol());
                executionReport.setOrderQty(orderQty);
                executionReport.setLastQty(new org.quickfixj.messages.bd.fix43.field.LastQty(
                        orderQty.getValue()));
                executionReport.setLastPx(new org.quickfixj.messages.bd.fix43.field.LastPx(price));

                sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix44.NewOrderSingle order,
            FIXSessionID sessionID) throws FieldNotFound, IncorrectTagValue {
        try {
            validateOrder(order.getOrdType());

            double price = getPrice(order);

            org.quickfixj.messages.bd.fix44.field.OrderQty orderQty = order.getOrderQty();

            org.quickfixj.messages.bd.fix44.ExecutionReport accept = new org.quickfixj.messages.bd.fix44.ExecutionReport(
                    new org.quickfixj.messages.bd.fix44.field.OrderID(genOrderID()),
                    new org.quickfixj.messages.bd.fix44.field.ExecID(genExecID()),
                    org.quickfixj.messages.bd.fix44.field.ExecType.FILL,
                    org.quickfixj.messages.bd.fix44.field.OrdStatus.NEW, order.getSide(),
                    new org.quickfixj.messages.bd.fix44.field.LeavesQty(order.getOrderQty()
                            .getValue()), new org.quickfixj.messages.bd.fix44.field.CumQty(0),
                    new org.quickfixj.messages.bd.fix44.field.AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            accept.setSymbol(order.getSymbol());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                org.quickfixj.messages.bd.fix44.ExecutionReport executionReport = new org.quickfixj.messages.bd.fix44.ExecutionReport(
                        new org.quickfixj.messages.bd.fix44.field.OrderID(genOrderID()),
                        new org.quickfixj.messages.bd.fix44.field.ExecID(genExecID()),
                        org.quickfixj.messages.bd.fix44.field.ExecType.FILL,
                        org.quickfixj.messages.bd.fix44.field.OrdStatus.FILLED, order.getSide(),
                        new org.quickfixj.messages.bd.fix44.field.LeavesQty(0),
                        new org.quickfixj.messages.bd.fix44.field.CumQty(orderQty.getValue()),
                        new org.quickfixj.messages.bd.fix44.field.AvgPx(price));

                executionReport.setClOrdID(order.getClOrdID());
                executionReport.setSymbol(order.getSymbol());
                executionReport.setOrderQty(orderQty);
                executionReport.setLastQty(new org.quickfixj.messages.bd.fix44.field.LastQty(
                        orderQty.getValue()));
                executionReport.setLastPx(new org.quickfixj.messages.bd.fix44.field.LastPx(price));

                sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix50.NewOrderSingle order,
            FIXSessionID sessionID) throws FieldNotFound, IncorrectTagValue {
        try {
            validateOrder(order.getOrdType());

            double price = getPrice(order);

            org.quickfixj.messages.bd.fix50.field.OrderQty orderQty = order.getOrderQty();

            org.quickfixj.messages.bd.fix50.ExecutionReport accept = new org.quickfixj.messages.bd.fix50.ExecutionReport(
                    new org.quickfixj.messages.bd.fix50.field.OrderID(genOrderID()),
                    new org.quickfixj.messages.bd.fix50.field.ExecID(genExecID()),
                    org.quickfixj.messages.bd.fix50.field.ExecType.FILL,
                    org.quickfixj.messages.bd.fix50.field.OrdStatus.NEW, order.getSide(),
                    new org.quickfixj.messages.bd.fix50.field.LeavesQty(order.getOrderQty()
                            .getValue()), new org.quickfixj.messages.bd.fix50.field.CumQty(0));

            accept.setClOrdID(order.getClOrdID());
            accept.setSymbol(order.getSymbol());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                org.quickfixj.messages.bd.fix50.ExecutionReport executionReport = new org.quickfixj.messages.bd.fix50.ExecutionReport(
                        new org.quickfixj.messages.bd.fix50.field.OrderID(genOrderID()),
                        new org.quickfixj.messages.bd.fix50.field.ExecID(genExecID()),
                        org.quickfixj.messages.bd.fix50.field.ExecType.FILL,
                        org.quickfixj.messages.bd.fix50.field.OrdStatus.FILLED, order.getSide(),
                        new org.quickfixj.messages.bd.fix50.field.LeavesQty(0),
                        new org.quickfixj.messages.bd.fix50.field.CumQty(orderQty.getValue()));

                executionReport.setClOrdID(order.getClOrdID());
                executionReport.setSymbol(order.getSymbol());
                executionReport.setOrderQty(orderQty);
                executionReport.setLastQty(new org.quickfixj.messages.bd.fix50.field.LastQty(
                        orderQty.getValue()));
                executionReport.setLastPx(new org.quickfixj.messages.bd.fix50.field.LastPx(price));
                executionReport.setAvgPx(new org.quickfixj.messages.bd.fix50.field.AvgPx(price));

                sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    public String genOrderID() {
        return Integer.valueOf(++m_orderID).toString();
    }

    public String genExecID() {
        return Integer.valueOf(++m_execID).toString();
    }

    public int genExecIDAsInt() {
        return ++m_execID;
    }

    /**
     * Allows a custom market data provider to be specified.
     *
     * @param marketDataProvider
     */
    public void setMarketDataProvider(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    private int m_orderID = 0;
    private int m_execID = 0;
}
