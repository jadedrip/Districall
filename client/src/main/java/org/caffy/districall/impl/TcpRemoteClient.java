package org.caffy.districall.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.caffy.districall.beans.ExchangeFrame;
import org.caffy.districall.codec.JsonDecoder;
import org.caffy.districall.codec.JsonEncoder;
import org.caffy.districall.exception.UnsupportedProtocol;
import org.caffy.districall.interf.ICallback;
import org.caffy.districall.utils.UriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 基于 TCP 的客户端服务
 */
@SuppressWarnings("unused")
class TcpRemoteClient {
    private static final Logger logger = LoggerFactory.getLogger(TcpRemoteClient.class);
    private String host;
    private int port;
    private ClientConnection connection = null;
    private Closeable closed;
    private ThreadPoolExecutor threadPoolExecutor = null;

    int timeout = 5;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    void createSession(UUID uuid, String name, Object[] parameters) throws Throwable {
        connection.createSession(uuid, name, parameters, timeout);
    }

    TcpRemoteClient(String connectString, Closeable closed) {
        try {
            URI uri = new URI(connectString);
            host = uri.getHost();
            port = uri.getPort();
            Map<String, String> map = UriUtils.parseQuery(uri.getQuery());
            String protocol = map.get("protocol");
            // serializer = SerializeFactory.getSerializer(protocol);
        } catch (URISyntaxException e) {
            logger.error("解析异常");
        }

        this.closed = closed;
    }

    void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    private static EventLoopGroup workerGroup = new NioEventLoopGroup();

    private ChannelPipeline pipeline;

    void start() throws InterruptedException, UnsupportedProtocol {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup).channel(NioSocketChannel.class);      // NioSocketChannel is being used to create a client-side Channel.
        //Note that we do not use childOption() here unlike we did with
        // ServerBootstrap because the client-side SocketChannel does not have a parent.
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                pipeline = ch.pipeline();
                connection = new ClientConnection(ch);
                connection.setThreadPoolExecutor(threadPoolExecutor);
                pipeline.addLast(new LengthFieldPrepender(2, false))
                        .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 2, 0, 2))
                        .addLast(new JsonEncoder())
                        .addLast(new JsonDecoder());
                pipeline.addLast("handler", new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        ExchangeFrame frame = (ExchangeFrame) msg;
                        connection.onReceive(frame.getSerial(), frame.getData());
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        ctx.close();
                        connection = null;
                        if (closed != null) closed.close();
                    }
                });
            }
        });

        // Bind and start to accept incoming connections.
        ChannelFuture channelFuture = bootstrap.connect(this.host, this.port);
        channelFuture.sync();
    }

    Object remoteMethod(UUID key, Method method, Object[] args) throws Throwable {
        return connection.inviteRemoteMethod(key, method, args, timeout);
    }

    void destroy(UUID id) throws Throwable {
        connection.destroySession(id);
    }
}
