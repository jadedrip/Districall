package org.caffy.districall.beans;

import java.io.Serializable;
import java.util.UUID;

/**
 * 销毁 Session
 */
public class DestroySession implements Serializable {
    UUID session;

    public DestroySession(UUID session) {
        this.session = session;
    }

    public UUID getSession() {
        return session;
    }

    public void setSession(UUID session) {
        this.session = session;
    }
}
