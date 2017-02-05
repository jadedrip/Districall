package org.caffy.districall.impl;

import io.netty.channel.Channel;
import org.caffy.districall.beans.*;
import org.caffy.districall.interf.ICallback;
import org.caffy.districall.transfer.ConnectionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    class SessionCache {
        Map<UUID, Object> reflexObjects = new TreeMap<UUID, Object>();

        void destroy() {
            Set<UUID> set = reflexObjects.keySet();
            interfaceImplementations.invalidateAll(set);
            reflexObjects.clear();
        }
    }

    private Map<UUID, SessionCache> sessionMap = new ConcurrentHashMap<UUID, SessionCache>();

    void createSession(UUID uuid, String name, Object[] parameters, int timeout) throws Throwable {
        CreateSession create = new CreateSession(uuid, name, parameters);
        sessionMap.put(uuid, new SessionCache());
        postFrameAndWait(Void.class, create, timeout);
    }

    void destroySession(UUID id) throws Throwable {
        DestroySession session = new DestroySession(id);
        SessionCache remove = sessionMap.remove(id);
        if (remove != null) remove.destroy();
        postFrameAndWait(Void.class, session, timeout);
    }

    @Override
    protected void onSessionInterface(UUID session, UUID id, Object arg) {
        // 在请求参数中包含接口时，将它和 Session 绑定起来，以便销毁 Session 时可以回收这些对象。
        SessionCache cache = sessionMap.get(session);
        assert cache != null;
        cache.reflexObjects.put(id, arg);
    }

    void onReceive(long serial, Object request) {
        try {
            if (request instanceof Negotiation) {

            } else if (request instanceof RemoteMethod) {
                onMethod(serial, (RemoteMethod) request);
            } else if (request instanceof RemoteMethodResponse) {
                onRemoteMethodResponse((RemoteMethodResponse) request);
            } else if (request instanceof EmptyResponse) {
                onEmptyResponse((EmptyResponse) request);
            } else if (request instanceof ExceptionWarp) {
                onException(serial, (ExceptionWarp) request);
            }
        } catch (Throwable e) {
            logger.error("连接发生异常", e);
            close();
        }
    }

    private void onEmptyResponse(EmptyResponse request) {
        ICallback<?> callback = request.getCallback();
        callback.apply(null);
    }
}
