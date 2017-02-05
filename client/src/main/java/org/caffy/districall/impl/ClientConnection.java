package org.caffy.districall.impl;

import io.netty.channel.Channel;
import org.caffy.districall.beans.*;
import org.caffy.districall.transfer.ConnectionBase;
import org.caffy.districall.transfer.RequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 客户端到服务器的一条链接
 */
class ClientConnection extends ConnectionBase {
    private static final Logger logger = LoggerFactory.getLogger(ClientConnection.class);

    ClientConnection(Channel channel) {
        super(channel);
    }

    private int timeout = 5;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    void destroySession(UUID id) throws Throwable {
        DestroySession session = new DestroySession(id);
        postFrameAndWait(Void.class, session, timeout);
    }

    void onReceive(long serial, Object request) {
        try {
            if (request instanceof Negotiation) {

            } else if (request instanceof RemoteMethod) {
                onMethod(serial, (RemoteMethod) request);
            } else if (request instanceof RemoteMethodResponse) {
                onRemoteMethodResponse((RemoteMethodResponse) request);
            } else if (request instanceof CreateSessionResponse) {
                onCreateSessionReturn(serial);
            } else if (request instanceof ExceptionWarp) {
                onException(serial, (ExceptionWarp) request);
            }
        } catch (Throwable e) {
            logger.error("连接发生异常", e);
            close();
        }
    }

    private void onCreateSessionReturn(long serial) {
        RequestMapper.Returned pop = RequestMapper.pop(serial);
        pop.callback.apply(null);
    }

    void createSession(UUID uuid, String name, Object[] parameters, int timeout) throws Throwable {
        CreateSession create = new CreateSession(uuid, name, parameters);
        postFrameAndWait(Void.class, create, timeout);
    }
}
