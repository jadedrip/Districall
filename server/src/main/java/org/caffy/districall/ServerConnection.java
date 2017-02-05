package org.caffy.districall;

import io.netty.channel.socket.SocketChannel;
import org.caffy.districall.beans.*;
import org.caffy.districall.interf.IFactory;
import org.caffy.districall.transfer.ConnectionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 一条链接
 */
class ServerConnection extends ConnectionBase {
    private static final Logger logger = LoggerFactory.getLogger(ServerConnection.class);

    private TcpServer server;

    ServerConnection(SocketChannel channel, TcpServer server) {
        super(channel);
        this.server = server;
    }

    void onRead(ExchangeFrame request) {
        try {
            Object data = request.getData();
            if (data instanceof Negotiation) {
                // TODO: Negotiation
            } else if (data instanceof RemoteMethod) {
                onMethod(request.getSerial(), (RemoteMethod) data);
            } else if (data instanceof RemoteMethodResponse) {
                onRemoteMethodResponse((RemoteMethodResponse) data);
            } else if (data instanceof CreateSession) {
                onCreateSession(request.getSerial(), (CreateSession) data);
            } else if (data instanceof DestroySession) {
                DestroySession destroySession = (DestroySession) data;
                onDestroySession(request.getSerial(), destroySession.getSession());
            }
        } catch (Throwable e) {
            logger.error("连接发生异常", e);
            close();
        }
    }

    /**
     * 在本地创建一个会话
     */
    private void onCreateSession(long serial, CreateSession s) {
        try {
            IFactory<?> iFactory = server.objectFactory.get(s.getName());
            if (iFactory == null)
                throw new IllegalAccessException();
            Object o = iFactory.getObject(s.getParameters());
            interfaceImplementations.put(s.getUuid(), o);
            post( serial, new CreateSessionResponse());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 销毁使用 createSession 创建的会话
     */
    private void onDestroySession(long serial, UUID session) {
        interfaceImplementations.invalidate(session);
        logger.debug("销毁会话: {}", session);
        post(serial, null);
    }
}
