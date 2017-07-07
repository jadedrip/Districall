# Districall 分布式远程调用框架

本框架简化了远程调用所需要进行的编码操作。尽量把远程调用，做得和本地调用一样。
本框架底层通过 netty 实现网络数据通讯。

## 接口定义

为了让接口可以被远程调用，唯一需要做的是，在接口上写上 @DynamicInterface 注解。
让框架通过扫描的形式自动发现它，或者通过收到注册的形式注册接口。S

## 服务发现

为了让客户端能够“发现”合适的服务器，我们需要实例化一个服务发现器，相应的接口是 —— IServicePool。  
这个接口推荐的实现是 ZookeeperServicePool，可以通过 Zookeeper 服务器来实现服务发现。  
只要客户端和服务端使用同一个服务发现器，那么他们就可以自动的发现服务，并实现“分布式”的远程调用。

另一个是基于配置的 StaticServicePool 实现。

## 客户端

客户端通过 Client 对象来实现远程调用。
分2种形式，一种是 #单例# 形式，另一种是 #会话# 的形式。

### 单例形式的远程调用

基本代码如下：

    IServicePool pool = new ZookeeperServicePool("x.x.x.x");    // 发现服务的实现
    Client client = new Client(pool);
    IRemoteInterface singleton = client.getSingleton(IRemoteInterface.class);

IRemoteInterface 就是通过 @DynamicInterface 注解定义的远程接口。

@DynamicInterface
public interface IRemoteInterface {
    int doSomething( );
    void doSomething( String text );    // 可以多态
}

通过 Client 来实例化后，就可以和本地类一样使用，框架通过反射的方式，
自动的把 IRemoteInterface 中的方法调用发送到合适的远程服务器，  
并且获得返回值后返回，需要注意的是，这里不支持异步的方法。

getSingleton 可以提供一个 group 参数，如果指定了 group, 会使用一致性哈希算法来确定服务器，
以保证 group 相同的请求，会发送到同一台服务器上。

而如果不指定 group, 框架会把 #每一次方法调用# 依次发送到不同服务器上去执行。

### 会话形式的远程调用

基本代码如下：

    IServicePool pool = new ZookeeperServicePool("x.x.x.x");    // 发现服务的实现
    Client client = new Client(pool);
    ISessionTest session = client.createSession("10", ISessionTest.class, 5000, new IReverseTest{ ... }  );
    // do something ...
    client.destroySession(session); // 销毁会话

ISessionTest 同样是通过 @DynamicInterface 注解定义的远程接口。不同的是，为了实现
服务端反向调用客户端的接口，我们需要提供反向接口的实现，只有提供了实现的接口才可以被服务端调用。
当然，如果不需要反向调用的能力，也可以忽略反向接口。
反向接口就是普通接口，不需要进行注解，只要它在 createSession 的时候提供了实现，就可以被服务端调用。

另外，会话和单例不同，会话每次都会 new 一个会话对象（上面的 session 在服务端会被 new 出来），
因此可以安全的在会话对象里保存会话私有信息。

会话可以等待过期自动删除，但推荐的做法是在会话使用完毕，通过 client.destroySession 方法销毁会话，
以节约服务端资源。

## 服务端

服务同样需要配置发现服务, 并且框架会通过扫描的方式自动的在 classPath 里查找实现。

    public static void main(String[] args) {
        Server server = new Server();
        server.setServicePool(new ZookeeperServicePool("127.0.0.1"));
        try {
            server.start("im.yixin", 6000);     // 扫描包并开始监听端口 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

### 接口实现

我们只需要在服务端简单的实现一个远程接口，就可以被客户端调用了。

比如上面的 IRemoteInterface 接口，只要在工程里写个实现类：

    public class RemoteImplementation implements IRemoteInterface {
        @Override
        public int doSomething() {
            System.out.println("Hello world!");
            return 10;
        }
        ......
     }

不需要做其他工作，这个实现会由框架自动实例化并调用（支持构造函数）。

## 异常

目前，可以有限度的支持异常。方法可以抛出异常，message 和异常类型会被传递，但包括堆栈在内的其他信息会丢失。
