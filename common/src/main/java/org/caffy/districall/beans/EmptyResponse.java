package org.caffy.districall.beans;

import org.caffy.districall.interf.ICallback;

/**
 * 空的命令回应
 */
public class EmptyResponse {
    private transient ICallback<?> callback;

    public EmptyResponse(ICallback<?> callback) {
        this.callback = callback;
    }

    public ICallback<?> getCallback() {
        return callback;
    }

    public void setCallback(ICallback<?> callback) {
        this.callback = callback;
    }
}
