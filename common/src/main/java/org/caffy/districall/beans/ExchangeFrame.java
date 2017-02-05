package org.caffy.districall.beans;

import java.io.Serializable;

/**
 * 请求报文
 */
public class ExchangeFrame implements Serializable {
    private long serial;
    private Object data;

    public ExchangeFrame(long serial, Object data) {
        this.serial = serial;
        this.data = data;
    }

    public long getSerial() {
        return serial;
    }

    public void setSerial(long serial) {
        this.serial = serial;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
