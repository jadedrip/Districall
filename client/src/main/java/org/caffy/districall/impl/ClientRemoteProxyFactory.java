package org.caffy.districall.impl;

import org.caffy.districall.exception.UnsupportedProtocol;
import org.caffy.districall.interf.IServicePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ServiceNotFoundException;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <pre>
 * Title: 接口工厂
 * </pre>
 */
@SuppressWarnings("unused")
public class ClientRemoteProxyFactory {
    private static final Logger logger = LoggerFactory.getLogger(ClientRemoteProxyFactory.class);

    private IServicePool servicePool = null;
    private ThreadPoolExecutor threadPoolExecutor = null;

    public ClientRemoteProxyFactory(IServicePool servicePool, ThreadPoolExecutor threadPoolExecutor) {
        this.servicePool = servicePool;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public ClientRemoteProxyFactory(IServicePool servicePool) {
        this.servicePool = servicePool;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    /**
     * 获取一个单例接口
     */
    @SuppressWarnings("unchecked")
    public <T> T getSingleton(Class<T> iClass) throws Exception {
        // DynamicInterface annotation = iClass.getAnnotation(DynamicInterface.class);
        // if (annotation == null) throw new RuntimeException("接口" + iClass.getSimpleName() + "没有被注解。");

        // 使用轮询的方式获取服务器并发送请求
        final Services finalServices = findServices(iClass);
        if (finalServices == null) throw new ServiceNotFoundException();
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getDeclaringClass().getName();
                if ("java.lang.Object".equals(name)) return null;   // 调试的时候 IDE 会调用本方法，这里过滤掉

                String service = finalServices.getService();
                return invokeMethod(method, null, service, args);
            }
        };

        return (T) Proxy.newProxyInstance(ClientRemoteProxyFactory.class.getClassLoader(), new Class[]{iClass}, handler);
    }

    /**
     * 获取一个单例接口
     */
    @SuppressWarnings("unchecked")
    public <T> T getSingleton(String group, Class<T> iClass) throws Exception {
        // 使用一致哈希的方式获取服务器并发送请求
        final String service = findService(group, iClass);
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getDeclaringClass().getName();
                if ("java.lang.Object".equals(name)) return null;   // 调试的时候 IDE 会调用本方法，这里过滤掉
                return invokeMethod(method, null, service, args);
            }
        };

        return (T) Proxy.newProxyInstance(ClientRemoteProxyFactory.class.getClassLoader(), new Class[]{iClass}, handler);
    }

    interface Destroyable {
        void destroy();
    }

    private AtomicLong serial = new AtomicLong(1);

    /**
     * 创建到远程服务的一个“会话”
     *
     * @param group  分组。通过这个参数使用一致性哈希算法来选取服务器（NULL 为随机）
     * @param iClass 远程接口
     * @return 代理接口
     */
    @SuppressWarnings("unchecked")
    public <T> T createObject(
            String group,
            final Class<T> iClass,
            Object[] parameters
    ) throws Throwable {
        final String serviceDetail = findService(group, iClass);
        TcpRemoteClient client;
        try {
            client = getClient(serviceDetail);
        } catch (Exception e) {   // 如果无法创建连接
            removeService(serviceDetail);
            throw e;
        }

        if (client == null) throw new NoSuchMethodException();

        final UUID id = UUID.randomUUID();
        // 先创建反向调用，防止对象创建后立即被反向
        final CountDownLatch latch = new CountDownLatch(1);
        client.createSession(id, iClass.getName(), parameters);

        logger.debug("会话创建：{}", id);
        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                final Method destroy = Destroyable.class.getMethod("destroy");
                if (destroy.equals(method)) {
                    remoteDestroy(id, serviceDetail);
                    return null;
                }

                return invokeMethod(method, id, serviceDetail, args);
            }
        };

        return (T) Proxy.newProxyInstance(iClass.getClassLoader(), new Class[]{iClass, Destroyable.class}, invocationHandler);
    }

    /**
     * 销毁会话
     *
     * @param sessionProxy 会话对象
     */
    public <T> void destroySession(T sessionProxy) {
        ((Destroyable) sessionProxy).destroy();
    }

    private Services findServices(Class<?> className) throws Exception {
        assert servicePool != null;
        return servicePool.queryService(className.getName());
    }

    private void removeService(String serviceDetail) {
        remotes.remove(serviceDetail);
    }

    private String findService(String group, final Class<?> iClass) throws Exception {
        Services s = findServices(iClass);
        if (s == null) throw new ServiceNotFoundException();
        String service = s.getService(group);
        if (service == null) throw new ServiceNotFoundException();
        return service;
    }

    private Map<String, TcpRemoteClient> remotes = new ConcurrentHashMap<String, TcpRemoteClient>();

    private TcpRemoteClient getClient(final String connectString) throws InterruptedException, UnsupportedProtocol {
        TcpRemoteClient client = remotes.get(connectString);
        if (client == null) {
            client = new TcpRemoteClient(connectString, new Closeable() {
                @Override
                public void close() throws IOException {
                    remotes.remove(connectString);  // 先从列表移除
                }
            });
            client.setThreadPoolExecutor(threadPoolExecutor);
            remotes.put(connectString, client);
            client.start();
        }
        return client;
    }

    private Object invokeMethod(Method method, UUID key, String connectString, Object[] args) throws Throwable {
        String name = method.getDeclaringClass().getName();
        // String methodName = ClassScanner.getMethodName(method);
        if ("java.lang.Object".equals(name)) {
            logger.debug("java.lang.Object {}, call by ide?", method);
            return method.toString();
        }
        logger.info("开始调用接口：" + name + ":" + method + " in " + Thread.currentThread().getId());

        TcpRemoteClient client = getClient(connectString);
        return client.remoteMethod(key, method, args);
    }

    private void remoteDestroy(UUID id, String serviceDetail) {
        try {
            TcpRemoteClient client = getClient(serviceDetail);
            client.destroy(id);
        } catch (Throwable e) {
            logger.error("无法销毁会话: {}", id, e);
        }
    }
}
