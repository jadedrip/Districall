package districall;

import io.netty.channel.ChannelFuture;
import org.caffy.districall.TcpServer;
import org.caffy.districall.interf.IFactory;

public class ListenerTest {
    /**
     * 测试代码
     */
    public static void main(String[] argvs) {
        TcpServer server = new TcpServer(6000);
        //server.setServicePool( new BroadcastServicePool() );

        try {
            // 注册公开接口
            server.registerSingleton(new RemoteImplementation(), IRemoteInterface.class);
            server.registerImplement(new IFactory<ISessionTest>() {
                @Override
                public ISessionTest getObject(Object[] parameters) {
                    return new SessionTest();
                }
            }, ISessionTest.class);

            ChannelFuture start = server.start();

//            // 把本服务器注册到 Zookeeper 服务发现器上
//            ZookeeperServicePool servicePool = new ZookeeperServicePool("192.168.21.202");
//            Service serviceDetail = server.getDetail();
//            for (String interfaceName : serviceDetail.interfaceNames) {
//                servicePool.setService(
//                        serviceDetail.serviceKey,
//                        interfaceName,
//                        serviceDetail.connectString
//                );
//            }
//
//            server.setServicePool(servicePool);

            start.sync();   // 等待服务结束
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}