package com.keymouseshare.network;

import com.keymouseshare.bean.ControlEvent;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Netty服务端初始化
 */
public class ControlServer {
    //处理客户端连接请求（通常 1 个线程）
    private EventLoopGroup bossGroup;
    // 处理已建立连接的 I/O 操作（线程数建议与 CPU 核心数匹配）
    private EventLoopGroup workerGroup;
    
    // 保存客户端连接的映射
    private Map<String, ChannelHandlerContext> clientChannels = new ConcurrentHashMap<>();

    public void start(int port) throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class) // 使用 NIO 通道
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast(new ControlMessageDecoder())// 添加自定义处理器
                        .addLast(new ControlMessageEncoder())// 添加自定义处理器
                        .addLast(new ControlServerHandler(clientChannels));// 添加自定义处理器
                }
            });

        bootstrap.bind(port).sync();// 绑定端口 阻塞直到服务器关闭
    }


    public void stop() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        // 关闭所有客户端连接
        for (ChannelHandlerContext ctx : clientChannels.values()) {
            if (ctx != null && ctx.channel().isActive()) {
                ctx.close();
            }
        }
        clientChannels.clear();
    }
    
    /**
     * 向指定IP地址的客户端发送控制事件
     *
     * @param event    控制事件
     */
    public void sendControlEvent(ControlEvent event) {
        ChannelHandlerContext ctx = clientChannels.get(event.getDeviceIp());
        if (ctx != null && ctx.channel().isActive()) {
            ctx.writeAndFlush(event);
        }
    }
    
    /**
     * 获取当前连接的客户端数量
     * 
     * @return 客户端连接数量
     */
    public int getClientCount() {
        return clientChannels.size();
    }
    
    /**
     * 检查指定IP地址的客户端是否已连接
     * 
     * @param ipAddress 客户端IP地址
     * @return 如果已连接返回true，否则返回false
     */
    public boolean isClientConnected(String ipAddress) {
        ChannelHandlerContext ctx = clientChannels.get(ipAddress);
        return ctx != null && ctx.channel().isActive();
    }
}