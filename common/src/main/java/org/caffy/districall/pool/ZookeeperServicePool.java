package org.caffy.districall.pool;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.caffy.districall.impl.Services;
import org.caffy.districall.interf.IServicePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程接口服务器管理器, 通过 Zookeeper 来查找接口对应的连接串
 */
@SuppressWarnings("unused")
public class ZookeeperServicePool implements IServicePool {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperServicePool.class);

    private static Charset utf8 = Charset.forName("utf-8");
    private static final int SESSION_TIMEOUT = 10000;
    private final String ZK_PATH;
    private CuratorFramework client;

    /**
     * 构造
     *
     * @param serverConn Zookeeper 服务端连接字符串（Zookeeper 服务器列表）
     */
    public ZookeeperServicePool(String serverConn, String zkPath) {
        this.ZK_PATH = zkPath.endsWith("/") ? zkPath : zkPath + "/";
        init(serverConn);
    }

    public ZookeeperServicePool(String serverConn) {
        this.ZK_PATH = "/services/";
        init(serverConn);
    }

    private void init(String serverConn) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        client = CuratorFrameworkFactory.newClient(serverConn, retryPolicy);
        client.start();
    }

    public void setService(String serviceId, String queryName, String connectString) throws Exception {
        ZooKeeper zooKeeper = client.getZookeeperClient().getZooKeeper();

        String path, s;
        // 注册接口
        path = ZK_PATH + queryName + "/" + serviceId;
        // 创建自身节点
        s = client.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                .forPath(path);
        logger.debug("创建节点：" + s);
        client.setData().forPath(s, connectString.getBytes(utf8));
    }

    private ConcurrentHashMap<String, Services> servicesCache = new ConcurrentHashMap<String, Services>();

    @Override
    public Services queryService(String name) throws Exception {
        Services services = servicesCache.get(name);
        if (services != null) return services;

        final String path = ZK_PATH + name;

        final Services finalServices = new Services();
        List<String> strings = client.getChildren()
                .usingWatcher(new CuratorWatcher() {
                    @Override
                    public void process(WatchedEvent event) throws Exception {
                        List<String> strings = client.getChildren().forPath(path);
                        List<String> nodes = queryChild(path, strings);
                        finalServices.onReset(nodes);
                    }
                }).forPath(path);
        List<String> nodes = queryChild(path, strings);
        finalServices.onReset(nodes);
        servicesCache.putIfAbsent(name, finalServices);
        return finalServices;
    }

    private List<String> queryChild(String path, Collection<String> children) throws Exception {
        ArrayList<String> tmp = new ArrayList<String>(children.size());
        for (String string : children) {
            byte[] bytes = client.getData().forPath(path + "/" + string);
            tmp.add(new String(bytes, utf8));
        }
        return tmp;
    }
}
