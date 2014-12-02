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

package quickfix.examples.ordermatch;

import quickfix.fix42.field.ClOrdID;
import quickfix.fix42.field.OrdType;
import quickfix.fix42.field.Side;
import quickfix.fix42.field.Symbol;

public class Order {

    private long entryTime;

    private ClOrdID clientOrderId;

    private Symbol symbol;

    private String owner;

    private String target;

    private Side side;

    private OrdType type;

    private double price;

    private long quantity;

    private long openQuantity;

    private long executedQuantity;

    private double avgExecutedPrice;

    private double lastExecutedPrice;

    private long lastExecutedQuantity;

    public Order(ClOrdID clientId, Symbol symbol, String owner, String target,
            Side side, OrdType type, double price, long quantity) {

        super();
        this.clientOrderId = clientId;
        this.symbol = symbol;
        this.owner = owner;
        this.target = target;
        this.side = side;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        openQuantity = quantity;
        entryTime = System.currentTimeMillis();
    }

    public double getAvgExecutedPrice() {

        return avgExecutedPrice;
    }

    public ClOrdID getClientOrderId() {

        return clientOrderId;
    }

    public long getExecutedQuantity() {

        return executedQuantity;
    }

    public long getLastExecutedQuantity() {

        return lastExecutedQuantity;
    }

    public long getOpenQuantity() {

        return openQuantity;
    }

    public String getOwner() {

        return owner;
    }

    public double getPrice() {

        return price;
    }

    public long getQuantity() {

        return quantity;
    }

    public Side getSide() {

        return side;
    }

    public Symbol getSymbol() {

        return symbol;
    }

    public String getTarget() {

        return target;
    }

    public OrdType getType() {

        return type;
    }

    public boolean isFilled() {

        return quantity == executedQuantity;
    }

    public void cancel() {

        openQuantity = 0;
    }

    public boolean isClosed() {

        return openQuantity == 0;
    }

    public void execute(double price, long quantity) {

        avgExecutedPrice = ((quantity * price) + (avgExecutedPrice * executedQuantity))
                / (quantity + executedQuantity);

        openQuantity -= quantity;
        executedQuantity += quantity;
        lastExecutedPrice = price;
        lastExecutedQuantity = quantity;
    }

    @Override
    public String toString() {

        return (side == Side.BUY ? "BUY" : "SELL") + " " + quantity + "@$"
                + price + " (" + openQuantity + ")";
    }

    public long getEntryTime() {

        return entryTime;
    }

    public double getLastExecutedPrice() {

        return lastExecutedPrice;
    }
}
