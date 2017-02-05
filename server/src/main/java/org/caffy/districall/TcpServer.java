package org.caffy.districall;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.caffy.districall.annotation.DynamicInterface;
import org.caffy.districall.codec.JsonEncoder;
import org.caffy.districall.utils.ImplementationFactory;
import org.caffy.districall.beans.ExchangeFrame;
import org.caffy.districall.codec.JsonDecoder;
import org.caffy.districall.interf.IFactory;
import org.caffy.districall.interf.IServicePool;
import org.caffy.districall.utils.ClassScanner;
import org.caffy.districall.utils.SingletonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于TCP协议的服务端
 */
@SuppressWarnings("unused")
public class TcpServer {
    private static Logger logger = LoggerFactory.getLogger(TcpServer.class);

    private IServicePool servicePool;
    private String localIp = getLocalIP();
    // 默认的线程池定义
    private ThreadPoolExecutor threadPoolExecutor = null;

    public IServicePool getServicePool() {
        return servicePool;
    }

    public void setServicePool(IServicePool pool) {
        this.servicePool = pool;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public ChannelFuture start(String packagePath) throws Exception {
        scanPackage(packagePath);
        return start();
    }

    private int port;
    private String protocol = "Json";

    /**
     * @param port 监听的端口
     */
    public TcpServer(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    private final EventLoopGroup group = new NioEventLoopGroup();
    private final ServerBootstrap bootstrap = new ServerBootstrap();

    /**
     * 开始一个服务器
     *
     * @throws Exception
     */
    public ChannelFuture start() throws Exception {
        if (threadPoolExecutor == null)
            threadPoolExecutor = new ThreadPoolExecutor(4, 200, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(2), new ThreadPoolExecutor.AbortPolicy());

        final TcpServer server = this;
        bootstrap.group(group).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        final ServerConnection connection = new ServerConnection(channel, server);
                        connection.setThreadPoolExecutor(threadPoolExecutor);
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 2, 0, 2));
                        pipeline.addLast(new LengthFieldPrepender(2, false));
                        pipeline.addLast(new JsonDecoder());
                        pipeline.addLast(new JsonEncoder());
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                if (msg instanceof ExchangeFrame) {
                                    ExchangeFrame byteBuf = (ExchangeFrame) msg;
                                    connection.onRead(byteBuf);
                                }
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                ctx.close();
                            }
                        });
                    }
                });

        logger.info("在 {} : {} 创建服务。", localIp, port);
        serviceDetail.connectString = makeConnectionString();
        ChannelFuture bind = bootstrap.bind(port);
        Channel channel = bind.syncUninterruptibly().channel();
        return channel.closeFuture();
    }

    /**
     * 手动设置实现类，并注册它实现的所有接口
     *
     * @param tClass 实现的类型
     */
    public void registerImplement(Class<?> tClass) {
        Class[] i = tClass.getInterfaces();
        if (i != null) for (Class<?> a : i) {
            ImplementationFactory.registerImplementation(tClass, a);
            serviceDetail.interfaceNames.add(a.getName());
        }
    }

    ConcurrentHashMap<String, IFactory<?>> objectFactory
            = new ConcurrentHashMap<String, IFactory<?>>();

    /**
     * 手动设置接口(不需要注解）及对应实现类
     * 不扫描时可以用
     *
     * @param interfaceClasses 接口类
     */
    public void registerImplement(IFactory<?> factory, Class<?>... interfaceClasses) {
        for (Class<?> a : interfaceClasses) {
            ImplementationFactory.registerImplementation(a);
            serviceDetail.interfaceNames.add(a.getName());
            objectFactory.put(a.getName(), factory);
        }
    }

    static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Service getDetail() {
        return serviceDetail;
    }

    private String makeConnectionString() {
        return "tcp://" + getLocalIP() + ":" + port
                // + "/" + serviceDetail.serviceKey
                + "?frame=LengthField&protocol="
                + protocol;
    }

    // 本服务的描述
    private Service serviceDetail = new Service();

    public Object getSingleton(String interfaceName) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        Class aClass = ImplementationFactory.queryImplementation(interfaceName);
        if (aClass == null) throw new ClassNotFoundException();
        return SingletonFactory.query(aClass);
    }

    /**
     * 强制注册一个单例类，这允许你创建需要配置的单例
     *
     * @param object 单例对象
     */
    @SuppressWarnings("unused")
    public void registerSingleton(Object object) {
        Class<?> c = object.getClass();

        Class[] i = c.getInterfaces();
        if (i != null) for (Class<?> a : i) {
            SingletonFactory.setSingleton(a, object);
            serviceDetail.interfaceNames.add(a.getName());
        }
    }

    /**
     * 强制设置一个单例类，并注册列表中的接口（不需要注解）。
     * 这允许你创建需要配置的单例，注册非注解接口，或者避免扫描。
     *
     * @param singleton 单例对象
     */
    public void registerSingleton(Object singleton, Class<?>... interfaceClasses) {
        Class<?> c = singleton.getClass();

        for (Class a : interfaceClasses) {
            SingletonFactory.setSingleton(a, singleton);
            serviceDetail.interfaceNames.add(a.getName());
        }
    }

    // 下面允许获取纯接口的方法
    static ConcurrentHashMap<String, TreeMap<String, Method>> methods = new ConcurrentHashMap<String, TreeMap<String, Method>>();

    public static synchronized Method getSessionMethod(String interfaceName, String methodName) throws ClassNotFoundException {
        Method method = ImplementationFactory.queryMethodByName(interfaceName, methodName);
        if (method != null) return method;

        TreeMap<String, Method> methodTreeMap = methods.get(interfaceName);
        if (methodTreeMap == null) {
            Class<?> a = TcpServer.class.getClassLoader().loadClass(interfaceName);
            methodTreeMap = new TreeMap<String, Method>();
            Method[] allMethods = a.getMethods();
            if (methods != null) for (Method m : allMethods) {
                String name = ImplementationFactory.getMethodName(m);
                methodTreeMap.put(name, m);
            }
            methods.put(interfaceName, methodTreeMap);
        }
        return methodTreeMap.get(methodName);
    }

    private synchronized void scanPackage(String path) {
        List<Class> classes = ClassScanner.scan(path);
        for (Class c : classes) {
            Class[] i = c.getInterfaces();
            if (i != null) for (Class a : i) {
                DynamicInterface x = (DynamicInterface) a.getAnnotation(DynamicInterface.class);
                if (x == null) continue;

                // 如果这个类实现了远程接口
                ImplementationFactory.registerImplementation(a, c);
                serviceDetail.interfaceNames.add(a.getName());
            }
        }
    }
}
