package org.caffy.districall;

import org.caffy.districall.impl.ClientRemoteProxyFactory;
import org.caffy.districall.interf.IServicePool;

import javax.management.ServiceNotFoundException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 远程接口客户端
 */
@SuppressWarnings("unused")
public class Client {
    private ClientRemoteProxyFactory proxyFactory;

    public Client(IServicePool servicePool) {
        proxyFactory = new ClientRemoteProxyFactory(servicePool);
    }

    public Client(IServicePool servicePool, ThreadPoolExecutor threadPoolExecutor) {
        proxyFactory = new ClientRemoteProxyFactory(servicePool, threadPoolExecutor);
    }

    /**
     * 获取一个单例接口
     * 注意，接口中的每个方法调用，都会顺序的发送到不同服务器上。
     *
     * @return 接口代理
     */
    public <T> T getSingleton(Class<T> iClass) throws Exception {
        return proxyFactory.getSingleton(iClass);
    }

    /**
     * 获取一个单例接口。
     * 提供服务的远程服务器通过一致性哈希来确定，返回的接口里所有的请求都将发送到同一台服务上。
     *
     * @param group 一致性哈希使用的分组值
     * @return 接口代理
     * @throws ServiceNotFoundException
     */
    public <T> T getSingleton(String group, Class<T> iClass) throws Exception {
        return proxyFactory.getSingleton(group, iClass);
    }

    /**
     * 创建一个远程对象
     *
     * @param group  分组。通过这个参数使用一致性哈希算法来选取服务器（NULL 为随机）
     * @param iClass 远程接口
     * @return 代理接口
     */
    @SuppressWarnings("unchecked")
    public <T> T createObject(String group, final Class<T> iClass, Object ... parameters) throws Throwable {
        return proxyFactory.createObject(group, iClass, parameters);
    }

    /**
     * 销毁会话
     *
     * @param sessionProxy 接口对象
     */
    public <T> void destroySession(T sessionProxy) {
        proxyFactory.destroySession(sessionProxy);
    }
}