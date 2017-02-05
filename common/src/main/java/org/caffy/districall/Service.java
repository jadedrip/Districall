package org.caffy.districall;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 定义服务器信息
 */
@SuppressWarnings("unused")
public class Service {
    // tcp://127.0.0.1:8080/{uuid}?frame=LengthField&protocol=Json
    public String connectString;

    // 本服务的 唯一名称
    public String serviceKey;
    public Set<String> interfaceNames = new HashSet<String>(); // 本机支持的接口列表

    public Service() {
        serviceKey = UUID.randomUUID().toString();
    }

    public Service(String key) {
        serviceKey = key;
    }

    @Override
    public int hashCode() {
        return serviceKey.hashCode();
    }

    @Override
    public String toString() {
        return serviceKey + " - " + connectString;
    }
}
