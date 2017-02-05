package org.caffy.districall.transfer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.channel.Channel;
import org.caffy.districall.beans.ExceptionWarp;
import org.caffy.districall.beans.RemoteMethodResponse;
import org.caffy.districall.interf.ICallback;
import org.caffy.districall.utils.ImplementationFactory;
import org.caffy.districall.beans.ExchangeFrame;
import org.caffy.districall.beans.RemoteMethod;
import org.caffy.districall.exception.RemoteException;
import org.caffy.districall.utils.SingletonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 连接的基类
 */
public abstract class ConnectionBase {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionBase.class);
    private ThreadPoolExecutor threadPoolExecutor;
    private Channel channel;
    private int timeout = 5;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public ConnectionBase(Channel channel) {
        this.channel = channel;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    private Object methodInvoke(RemoteMethod c) throws Throwable {
        logger.debug("远程调用 {}", c);

        Method method = c.getMethod();
        replaceByProxy(c);

        Object o;
        if (c.getSession() == null) {
            // 单例调用
            o = SingletonFactory.query(method.getDeclaringClass().getName());
        } else {
            o = interfaceImplementations.getIfPresent(c.getSession());
        }

        if (o == null)
            throw new IllegalAccessException();

        Object r;
        try {
            method.setAccessible(false);
            Object[] parameters = c.getParameters();
            if (parameters == null)
                r = method.invoke(o);
            else
                r = method.invoke(o, parameters);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        return r;
    }

    private void replaceByProxy(RemoteMethod c) {
        Method method = c.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes == null) return;
        int i = 0;
        for (Class<?> aClass : parameterTypes) {
            if (aClass.isInterface()) {
                final UUID u = UUID.fromString((String) c.getParameters()[i]);
                InvocationHandler invocationHandler = new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        logger.info("反向调用：{}", method);
                        return inviteRemoteMethod(u, method, args, timeout);
                    }
                };

                Object o = Proxy.newProxyInstance(aClass.getClassLoader(), new Class[]{aClass}, invocationHandler);
                c.getParameters()[i] = o;
            }
            i++;
        }
    }

    protected void onMethod(final long serial, final RemoteMethod method) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ExchangeFrame frame;
                try {
                    Object o = methodInvoke(method);
                    RemoteMethodResponse response = new RemoteMethodResponse();
                    response.setObject(o);
                    frame = new ExchangeFrame(serial, response);
                } catch (Throwable e) {
                    logger.warn("远程方法抛出异常 {}: {}", method, e.getMessage());
                    ExceptionWarp warp = new ExceptionWarp();
                    warp.setType(e.getClass().getName());
                    warp.setMessage(e.getMessage());
                    frame = new ExchangeFrame(serial, warp);
                }
                channel.writeAndFlush(frame);
            }
        };

        if (threadPoolExecutor != null)
            threadPoolExecutor.execute(runnable);
        else {
            runnable.run();
        }
    }

    protected void onRemoteMethodResponse(RemoteMethodResponse response) {
        ICallback callback = response.getCallback();
        if (callback == null) return;
        Object object = response.getObject();
        if (object instanceof ExceptionWarp) {
            ExceptionWarp warp = (ExceptionWarp) object;
            ClassLoader classLoader = this.getClass().getClassLoader();
            Throwable throwable;
            try {
                Class<?> aClass = classLoader.loadClass(warp.getType());
                Constructor<?> constructor = aClass.getConstructor(String.class);
                throwable = (Throwable) constructor.newInstance(warp.getMessage());
            } catch (Exception e) {
                throwable = new RemoteException(warp.getType(), warp.getMessage());
            }
            callback.except(throwable);
        } else {
            callback.apply(object);
        }
    }

    protected void close() {
        if (channel != null) channel.close();
    }

    protected void onException(long serial, ExceptionWarp warp) {
        RequestMapper.Returned returned = RequestMapper.pop(serial);
        if (returned == null) {
            logger.error("无法找到对应请求。");
            return;
        }

        try {
            Class clz = ConnectionBase.class.getClassLoader().loadClass(warp.getType());
            Constructor constructor = clz.getConstructor(String.class);
            Object o = constructor.newInstance(warp.getMessage());
            returned.callback.except((Throwable) o);
        } catch (Exception e) {
            returned.callback.except(new RemoteException(warp.getMessage(), warp.getType()));
        }
    }

    protected Cache<UUID, Object> interfaceImplementations
            = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();

    /**
     * 执行一个方法（同步）
     *
     * @param session
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    public Object inviteRemoteMethod(UUID session, Method method, Object[] args, int timeout)
            throws Throwable {
        // 如果方法参数里有接口
        int i = 0;
        for (Class<?> aClass : method.getParameterTypes()) {
            if (aClass.isInterface()) { // 接口参数替换位对象序号
                UUID id = UUID.randomUUID();
                interfaceImplementations.put(id, args[i]);
                args[i] = id;
                ImplementationFactory.registerImplementation(aClass);
            }
            i++;
        }

        RemoteMethod remoteMethod = new RemoteMethod(session, method, args);
        return postFrameAndWait(method.getReturnType(), remoteMethod, timeout);
    }

    protected void post(long serial, Object data) {
        ExchangeFrame frame = new ExchangeFrame(serial, data);
        channel.writeAndFlush(frame);
    }

    protected Object postFrameAndWait(Class<?> type, Object data, int timeout)
            throws Throwable {
        final Object[] obj = {null};
        final Throwable[] ex = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        // 把异步操作转为同步
        ICallback<Object> callback = new ICallback<Object>() {
            @Override
            public void apply(Object v) {
                obj[0] = v;
                latch.countDown();
            }

            @Override
            public void except(Throwable e) {
                ex[0] = e;
                latch.countDown();
            }
        };

        long serial = RequestMapper.make(type, callback);
        post(serial, data);
        if (latch.await(timeout, TimeUnit.SECONDS)) {
            if (ex[0] != null)
                throw ex[0];
            else
                return obj[0];
        }
        throw new TimeoutException();
    }
}
