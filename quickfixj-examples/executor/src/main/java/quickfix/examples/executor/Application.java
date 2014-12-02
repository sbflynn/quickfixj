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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.ConfigError;
import quickfix.DataDictionaryProvider;
import quickfix.DoNotSend;
import quickfix.FieldConvertError;
import quickfix.FieldNotFound;
import quickfix.FixTags;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.LogUtil;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.fix40.ExecutionReport;
import quickfix.fix40.NewOrderSingle;
import quickfix.fix40.field.AvgPx;
import quickfix.fix40.field.CumQty;
import quickfix.fix40.field.ExecID;
import quickfix.fix40.field.ExecTransType;
import quickfix.fix40.field.LastPx;
import quickfix.fix40.field.LastShares;
import quickfix.fix40.field.OrdStatus;
import quickfix.fix40.field.OrderID;
import quickfix.fix40.field.OrderQty;
import quickfix.fix40.field.Price;
import quickfix.fix50.field.OrdType;
import quickfix.fix50.field.Side;
import quickfix.fix50.field.Symbol;

public class Application extends quickfix.MessageCracker implements quickfix.Application {
    private static final String DEFAULT_MARKET_PRICE_KEY = "DefaultMarketPrice";
    private static final String ALWAYS_FILL_LIMIT_KEY = "AlwaysFillLimitOrders";
    private static final String VALID_ORDER_TYPES_KEY = "ValidOrderTypes";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final boolean alwaysFillLimitOrders;
    private final HashSet<String> validOrderTypes = new HashSet<String>();
    private MarketDataProvider marketDataProvider;

    public Application(SessionSettings settings) throws ConfigError, FieldConvertError {
        initializeValidOrderTypes(settings);
        initializeMarketDataProvider(settings);

        if (settings.isSetting(ALWAYS_FILL_LIMIT_KEY)) {
            alwaysFillLimitOrders = settings.getBool(ALWAYS_FILL_LIMIT_KEY);
        } else {
            alwaysFillLimitOrders = false;
        }
    }

    private void initializeMarketDataProvider(SessionSettings settings) throws ConfigError,
            FieldConvertError {
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

    private void initializeValidOrderTypes(SessionSettings settings) throws ConfigError,
            FieldConvertError {
        if (settings.isSetting(VALID_ORDER_TYPES_KEY)) {
            List<String> orderTypes = Arrays.asList(settings.getString(VALID_ORDER_TYPES_KEY)
                    .trim().split("\\s*,\\s*"));
            validOrderTypes.addAll(orderTypes);
        } else {
            validOrderTypes.add(OrdType.LIMIT + "");
        }
    }

    @Override
    public void onCreate(SessionID sessionID) {
        Session.lookupSession(sessionID).getLog().onEvent("Valid order types: " + validOrderTypes);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        //no-op
    }

    @Override
    public void onLogout(SessionID sessionID) {
        //no-op
    }

    @Override
    public void toAdmin(quickfix.Message message, SessionID sessionID) {
        //no-op
    }

    @Override
    public void toApp(quickfix.Message message, SessionID sessionID) throws DoNotSend {
        //no-op
    }

    @Override
    public void fromAdmin(quickfix.Message message, SessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        //no-op
    }

    @Override
    public void fromApp(quickfix.Message message, SessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    public void onMessage(NewOrderSingle order, SessionID sessionID) throws FieldNotFound,
            IncorrectTagValue {
        try {

            // earlier FIX versions used string not character values
            validateOrder(new quickfix.fix44.field.OrdType(order.getOrdType().getCharacters()
                    .charAt(0)));

            double price = getPrice(order);

            OrderQty orderQty = order.getOrderQty();

            ExecutionReport accept = new ExecutionReport(new OrderID(genOrderID()), new ExecID(
                    genExecID()), ExecTransType.NEW, OrdStatus.NEW, order.getSymbol(),
                    order.getSide(), orderQty, new LastShares(0), new LastPx(0), new CumQty(0),
                    new AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                ExecutionReport fill = new ExecutionReport(new OrderID(genOrderID()), new ExecID(
                        genExecID()), ExecTransType.NEW, OrdStatus.FILLED, order.getSymbol(),
                        order.getSide(), orderQty, new LastShares(orderQty.getValue()), new LastPx(
                                price), new CumQty(orderQty.getValue()), new AvgPx(price));

                fill.setClOrdID(order.getClOrdID());

                sendMessage(sessionID, fill);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    private boolean isOrderExecutable(Message order, double price) throws FieldNotFound {
        if (order.getChar(OrdType.TAG) == OrdType.LIMIT.getValue()) {
            BigDecimal limitPrice = new BigDecimal(order.getString(Price.TAG));
            char side = order.getChar(Side.TAG);
            BigDecimal thePrice = new BigDecimal(price);

            return (side == Side.BUY.getValue() && thePrice.compareTo(limitPrice) <= 0)
                    || ((side == Side.SELL.getValue() || side == Side.SELL_SHORT.getValue()) && thePrice
                            .compareTo(limitPrice) >= 0);
        }
        return true;
    }

    private double getPrice(Message message) throws FieldNotFound {

        if (message.getChar(OrdType.TAG) == OrdType.LIMIT.getValue() && alwaysFillLimitOrders) {
            return message.getDouble(Price.TAG);
        }

        if (marketDataProvider == null) {
            throw new RuntimeException("No market data provider specified for market order");
        }

        char side = message.getChar(Side.TAG);
        if (side == Side.BUY.getValue()) {
            return marketDataProvider.getAsk(message.getString(Symbol.TAG));
        } else if (side == Side.SELL.getValue() || side == Side.SELL_SHORT.getValue()) {
            return marketDataProvider.getBid(message.getString(Symbol.TAG));
        } else {
            throw new RuntimeException("Invalid order side: " + side);
        }
    }

    private void sendMessage(SessionID sessionID, Message message) {
        try {
            Session session = Session.lookupSession(sessionID);
            if (session == null) {
                throw new SessionNotFound(sessionID.toString());
            }

            message.getHeader().setString(FixTags.MSG_TYPE, message.getMsgType());

            DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
            if (dataDictionaryProvider != null) {
                try {
                    dataDictionaryProvider.getApplicationDataDictionary(getApplVerID(session))
                            .validate(message, true);
                } catch (Exception e) {
                    LogUtil.logThrowable(sessionID,
                            "Outgoing message failed validation: " + e.getMessage(), e);
                    return;
                }
            }

            session.send(message);

        } catch (SessionNotFound e) {
            log.error(e.getMessage(), e);
        }
    }

    private FIXApplication getApplVerID(Session session) {
        FIXBeginString beginString = session.getSessionID().getBeginString();
        if (FIXBeginString.FIXT11.equals(beginString)) {
            return FIXApplication.FIX50;
        }

        return MessageUtils.toApplVerID(beginString);
    }

    public void onMessage(quickfix.fix41.NewOrderSingle order, SessionID sessionID)
            throws FieldNotFound, IncorrectTagValue {
        try {

            // earlier FIX versions used string not character values
            validateOrder(new quickfix.fix44.field.OrdType(order.getOrdType().getCharacters()
                    .charAt(0)));

            double price = getPrice(order);

            quickfix.fix41.field.OrderQty orderQty = order.getOrderQty();

            quickfix.fix41.ExecutionReport accept = new quickfix.fix41.ExecutionReport(
                    new quickfix.fix41.field.OrderID(genOrderID()),
                    new quickfix.fix41.field.ExecID(genExecID()),
                    quickfix.fix41.field.ExecTransType.NEW, quickfix.fix41.field.ExecType.FILL,
                    quickfix.fix41.field.OrdStatus.NEW, order.getSymbol(), order.getSide(),
                    order.getOrderQty(), new quickfix.fix41.field.LastShares(0),
                    new quickfix.fix41.field.LastPx(0), new quickfix.fix41.field.LeavesQty(0),
                    new quickfix.fix41.field.CumQty(0), new quickfix.fix41.field.AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                quickfix.fix41.ExecutionReport executionReport = new quickfix.fix41.ExecutionReport(
                        new quickfix.fix41.field.OrderID(genOrderID()),
                        new quickfix.fix41.field.ExecID(genExecID()),
                        quickfix.fix41.field.ExecTransType.NEW, quickfix.fix41.field.ExecType.FILL,
                        quickfix.fix41.field.OrdStatus.FILLED, order.getSymbol(), order.getSide(),
                        order.getOrderQty(), new quickfix.fix41.field.LastShares(
                                orderQty.getValue()), new quickfix.fix41.field.LastPx(price),
                        new quickfix.fix41.field.LeavesQty(0), new quickfix.fix41.field.CumQty(
                                orderQty.getValue()), new quickfix.fix41.field.AvgPx(price));

                executionReport.setClOrdID(order.getClOrdID());

                sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    public void onMessage(quickfix.fix42.NewOrderSingle order, SessionID sessionID)
            throws FieldNotFound, IncorrectTagValue {
        try {

            // earlier FIX versions used string not character values
            validateOrder(new quickfix.fix44.field.OrdType(order.getOrdType().getCharacters()
                    .charAt(0)));

            double price = getPrice(order);

            quickfix.fix42.field.OrderQty orderQty = order.getOrderQty();

            quickfix.fix42.ExecutionReport accept = new quickfix.fix42.ExecutionReport(
                    new quickfix.fix42.field.OrderID(genOrderID()),
                    new quickfix.fix42.field.ExecID(genExecID()),
                    quickfix.fix42.field.ExecTransType.NEW, quickfix.fix42.field.ExecType.FILL,
                    quickfix.fix42.field.OrdStatus.NEW, order.getSymbol(), order.getSide(),
                    new quickfix.fix42.field.LeavesQty(0), new quickfix.fix42.field.CumQty(0),
                    new quickfix.fix42.field.AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                quickfix.fix42.ExecutionReport executionReport = new quickfix.fix42.ExecutionReport(
                        new quickfix.fix42.field.OrderID(genOrderID()),
                        new quickfix.fix42.field.ExecID(genExecID()),
                        quickfix.fix42.field.ExecTransType.NEW, quickfix.fix42.field.ExecType.FILL,
                        quickfix.fix42.field.OrdStatus.FILLED, order.getSymbol(), order.getSide(),
                        new quickfix.fix42.field.LeavesQty(0), new quickfix.fix42.field.CumQty(
                                orderQty.getValue()), new quickfix.fix42.field.AvgPx(price));

                executionReport.setClOrdID(order.getClOrdID());
                executionReport.setOrderQty(orderQty);
                executionReport.setLastShares(new quickfix.fix42.field.LastShares(orderQty
                        .getValue()));
                executionReport.setLastPx(new quickfix.fix42.field.LastPx(price));

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

    public void onMessage(quickfix.fix43.NewOrderSingle order, SessionID sessionID)
            throws FieldNotFound, IncorrectTagValue {
        try {
            validateOrder(order.getOrdType());

            double price = getPrice(order);

            quickfix.fix43.field.OrderQty orderQty = order.getOrderQty();

            quickfix.fix43.ExecutionReport accept = new quickfix.fix43.ExecutionReport(
                    new quickfix.fix43.field.OrderID(genOrderID()),
                    new quickfix.fix43.field.ExecID(genExecID()),
                    quickfix.fix43.field.ExecType.FILL, quickfix.fix43.field.OrdStatus.NEW,
                    order.getSide(), new quickfix.fix43.field.LeavesQty(order.getOrderQty()
                            .getValue()), new quickfix.fix43.field.CumQty(0),
                    new quickfix.fix43.field.AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            accept.setSymbol(order.getSymbol());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                quickfix.fix43.ExecutionReport executionReport = new quickfix.fix43.ExecutionReport(
                        new quickfix.fix43.field.OrderID(genOrderID()),
                        new quickfix.fix43.field.ExecID(genExecID()),
                        quickfix.fix43.field.ExecType.FILL, quickfix.fix43.field.OrdStatus.FILLED,
                        order.getSide(), new quickfix.fix43.field.LeavesQty(0),
                        new quickfix.fix43.field.CumQty(orderQty.getValue()),
                        new quickfix.fix43.field.AvgPx(price));

                executionReport.setClOrdID(order.getClOrdID());
                executionReport.setSymbol(order.getSymbol());
                executionReport.setOrderQty(orderQty);
                executionReport.setLastQty(new quickfix.fix43.field.LastQty(orderQty.getValue()));
                executionReport.setLastPx(new quickfix.fix43.field.LastPx(price));

                sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    public void onMessage(quickfix.fix44.NewOrderSingle order, SessionID sessionID)
            throws FieldNotFound, IncorrectTagValue {
        try {
            validateOrder(order.getOrdType());

            double price = getPrice(order);

            quickfix.fix44.field.OrderQty orderQty = order.getOrderQty();

            quickfix.fix44.ExecutionReport accept = new quickfix.fix44.ExecutionReport(
                    new quickfix.fix44.field.OrderID(genOrderID()),
                    new quickfix.fix44.field.ExecID(genExecID()),
                    quickfix.fix44.field.ExecType.FILL, quickfix.fix44.field.OrdStatus.NEW,
                    order.getSide(), new quickfix.fix44.field.LeavesQty(order.getOrderQty()
                            .getValue()), new quickfix.fix44.field.CumQty(0),
                    new quickfix.fix44.field.AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            accept.setSymbol(order.getSymbol());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                quickfix.fix44.ExecutionReport executionReport = new quickfix.fix44.ExecutionReport(
                        new quickfix.fix44.field.OrderID(genOrderID()),
                        new quickfix.fix44.field.ExecID(genExecID()),
                        quickfix.fix44.field.ExecType.FILL, quickfix.fix44.field.OrdStatus.FILLED,
                        order.getSide(), new quickfix.fix44.field.LeavesQty(0),
                        new quickfix.fix44.field.CumQty(orderQty.getValue()),
                        new quickfix.fix44.field.AvgPx(price));

                executionReport.setClOrdID(order.getClOrdID());
                executionReport.setSymbol(order.getSymbol());
                executionReport.setOrderQty(orderQty);
                executionReport.setLastQty(new quickfix.fix44.field.LastQty(orderQty.getValue()));
                executionReport.setLastPx(new quickfix.fix44.field.LastPx(price));

                sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    public void onMessage(quickfix.fix50.NewOrderSingle order, SessionID sessionID)
            throws FieldNotFound, IncorrectTagValue {
        try {
            validateOrder(order.getOrdType());

            double price = getPrice(order);

            quickfix.fix50.field.OrderQty orderQty = order.getOrderQty();

            quickfix.fix50.ExecutionReport accept = new quickfix.fix50.ExecutionReport(
                    new quickfix.fix50.field.OrderID(genOrderID()),
                    new quickfix.fix50.field.ExecID(genExecID()),
                    quickfix.fix50.field.ExecType.FILL, quickfix.fix50.field.OrdStatus.NEW,
                    order.getSide(), new quickfix.fix50.field.LeavesQty(order.getOrderQty()
                            .getValue()), new quickfix.fix50.field.CumQty(0));

            accept.setClOrdID(order.getClOrdID());
            accept.setSymbol(order.getSymbol());
            sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                quickfix.fix50.ExecutionReport executionReport = new quickfix.fix50.ExecutionReport(
                        new quickfix.fix50.field.OrderID(genOrderID()),
                        new quickfix.fix50.field.ExecID(genExecID()),
                        quickfix.fix50.field.ExecType.FILL, quickfix.fix50.field.OrdStatus.FILLED,
                        order.getSide(), new quickfix.fix50.field.LeavesQty(0),
                        new quickfix.fix50.field.CumQty(orderQty.getValue()));

                executionReport.setClOrdID(order.getClOrdID());
                executionReport.setSymbol(order.getSymbol());
                executionReport.setOrderQty(orderQty);
                executionReport.setLastQty(new quickfix.fix50.field.LastQty(orderQty.getValue()));
                executionReport.setLastPx(new quickfix.fix50.field.LastPx(price));
                executionReport.setAvgPx(new quickfix.fix50.field.AvgPx(price));

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
