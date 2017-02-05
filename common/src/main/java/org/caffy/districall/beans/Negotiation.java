package org.caffy.districall.beans;

import java.io.Serializable;

/**
 * 握手
 */
public class Negotiation implements Serializable {
    private long version = 0;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
