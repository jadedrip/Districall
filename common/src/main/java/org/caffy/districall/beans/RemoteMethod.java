package org.caffy.districall.beans;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 定义远程呼叫要传递的信息
 */

public class RemoteMethod implements Serializable {
    private UUID session = null;
    private Method method;
    private Object[] parameters;

    public RemoteMethod() {
    }

    public RemoteMethod(UUID session, Method method, Object[] parameters) {
        this.session = session;
        this.method = method;
        this.parameters = parameters;
    }

    public UUID getSession() {
        return session;
    }

    public void setSession(UUID session) {
        this.session = session;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        if (method == null)
            return "[RemoteCall] null";
        return "[RemoteCall] " + method.toString();
    }
}
