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

import java.net.SocketAddress;
import java.util.Map;

import org.quickfixj.engine.FIXSession.FIXSessionID;

/**
 * Accepts connections from FIX clients and manages the associated sessions.
 */
public interface Acceptor extends Connector {

    /**
     * Acceptor setting specifying the socket protocol used to accept connections.
     */
    public static final String SETTING_SOCKET_ACCEPT_PROTOCOL = "SocketAcceptProtocol";

    /**
     * Acceptor setting specifying port for accepting FIX client connections.
     */
    public static final String SETTING_SOCKET_ACCEPT_PORT = "SocketAcceptPort";

    /**
     * Acceptor setting specifying local IP interface address for accepting connections.
     */
    public static final String SETTING_SOCKET_ACCEPT_ADDRESS = "SocketAcceptAddress";

    /**
     * Acceptor setting specifying local IP interface address for accepting connections.
     */
    public static final String SETTING_ACCEPTOR_TEMPLATE = "AcceptorTemplate";

    /**
     * The size of the underlying event queue
     *
     * @return size of the queue; if this counters grows, the application does not consume FIX message quick enough
     */
    int getQueueSize();

    /**
     * @since 2.0
     */
    Map<FIXSessionID, SocketAddress> getAcceptorAddresses();
}
