package org.caffy.districall.beans;

import java.io.Serializable;
import java.util.UUID;

/**
 * 创建 ISession 的请求报文
 */
public class CreateSession implements Serializable{
    private UUID uuid;
    private String name;
    private Object[] parameters;

    public CreateSession(UUID uuid, String name, Object[] parameters) {
        this.uuid = uuid;
        this.name = name;
        this.parameters = parameters;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}
