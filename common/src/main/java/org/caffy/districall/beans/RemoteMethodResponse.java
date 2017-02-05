package org.caffy.districall.beans;

import org.caffy.districall.interf.ICallback;

import java.io.Serializable;

/**
 * 远程请求的返回
 */
public class RemoteMethodResponse implements Serializable{
    private Object object;
    private transient Class<?> type;
    private transient ICallback<?> callback;

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public ICallback<?> getCallback() {
        return callback;
    }

    public void setCallback(ICallback<?> callback) {
        this.callback = callback;
    }
}
