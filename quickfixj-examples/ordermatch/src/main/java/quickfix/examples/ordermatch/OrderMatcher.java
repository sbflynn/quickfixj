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

import java.util.ArrayList;
import java.util.HashMap;

import quickfix.fix42.field.Side;
import quickfix.fix42.field.Symbol;

public class OrderMatcher {

    private HashMap<Symbol, Market> markets = new HashMap<Symbol, Market>();

    private Market getMarket(Symbol symbol) {

        Market m = markets.get(symbol);
        if (m == null) {
            m = new Market();
            markets.put(symbol, m);
        }
        return m;
    }

    public boolean insert(Order order) {

        return getMarket(order.getSymbol()).insert(order);
    }

    public void match(Symbol symbol, ArrayList<Order> orders) {

        getMarket(symbol).match(symbol, orders);
    }

    public Order find(Symbol symbol, Side side, String id) {

        return getMarket(symbol).find(symbol, side, id);
    }

    public void erase(Order order) {

        getMarket(order.getSymbol()).erase(order);
    }

    public void display() {

        for (Symbol symbol : markets.keySet()) {
            System.out.println("MARKET: " + symbol);
            display(symbol);
        }
    }

    public void display(Symbol symbol) {

        getMarket(symbol).display();
    }
}
