package org.caffy.districall.impl;

import org.caffy.districall.utils.ConsistentHash;

import javax.management.ServiceNotFoundException;
import java.util.List;

/**
 * 服务器集合
 */
public class Services {
    private int count = 0;
    private List<String> servers;
    private ConsistentHash<String> consistentHash;

    public synchronized String getService() throws ServiceNotFoundException {
        if (servers.isEmpty()) return null;
        int i = count++;
        return servers.get(i % servers.size());
    }

    public String getService(Object group) throws ServiceNotFoundException {
        if (group == null) return getService();
        if (servers.isEmpty()) return null;

        ConsistentHash<String> hash = consistentHash;
        synchronized (this) {
            return hash.get(group);
        }
    }

    public void onReset(List<String> servers) {
        ConsistentHash<String> hash = new ConsistentHash<String>(servers);
        synchronized (this) {
            this.servers = servers;
            consistentHash = hash;
        }
    }

    synchronized void remove(String service) {
        servers.remove(service);
        consistentHash.remove(service);
    }
}
