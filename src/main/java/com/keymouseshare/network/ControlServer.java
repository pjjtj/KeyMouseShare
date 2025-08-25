package com.keymouseshare.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Netty服务端初始化
 */
public class ControlServer {
    //处理客户端连接请求（通常 1 个线程）
    private EventLoopGroup bossGroup;
    // 处理已建立连接的 I/O 操作（线程数建议与 CPU 核心数匹配）
    private EventLoopGroup workerGroup;
    
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
                        .addLast(new ControlServerHandler());// 添加自定义处理器
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
    }
}