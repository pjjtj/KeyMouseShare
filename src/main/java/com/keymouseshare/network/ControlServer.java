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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty服务端初始化
 */
public class ControlServer {
    private static final Logger logger = LoggerFactory.getLogger(ControlServer.class);
    
    //处理客户端连接请求（通常 1 个线程）
    private EventLoopGroup bossGroup;
    // 处理已建立连接的 I/O 操作（线程数建议与 CPU 核心数匹配）
    private EventLoopGroup workerGroup;
    
    // 保存客户端连接的映射
    private Map<String, ChannelHandlerContext> clientChannels = new ConcurrentHashMap<>();

    public void start(int port) throws Exception {
        logger.debug("正在初始化ControlServer...");
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        logger.info("ControlServer的EventLoopGroup已创建");
        
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class) // 使用 NIO 通道
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    logger.debug("正在初始化SocketChannel管道...");
                    ch.pipeline()
                        .addLast(new ControlMessageDecoder())// 添加自定义处理器
                        .addLast(new ControlMessageEncoder())// 添加自定义处理器
                        .addLast(new ControlServerHandler(clientChannels));// 添加自定义处理器
                    logger.debug("SocketChannel管道初始化完成");
                }
            });

        logger.info("正在绑定端口 {}...", port);
        bootstrap.bind(port).sync();// 绑定端口 阻塞直到服务器关闭
        logger.info("ControlServer已成功启动并绑定到端口 {}", port);
    }


    public void stop() {
        logger.info("正在停止ControlServer...");
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            logger.debug("workerGroup已关闭");
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            logger.debug("bossGroup已关闭");
        }
        
        // 关闭所有客户端连接
        logger.debug("正在关闭所有客户端连接，当前客户端数量: {}", clientChannels.size());
        for (ChannelHandlerContext ctx : clientChannels.values()) {
            if (ctx != null && ctx.channel().isActive()) {
                ctx.close();
            }
        }
        clientChannels.clear();
        logger.info("ControlServer已完全停止");
    }
    
    /**
     * 向指定IP地址的客户端发送控制事件
     *
     * @param event    控制事件
     */
    public void sendControlEvent(ControlEvent event) {
        logger.debug("准备发送控制事件到客户端: {}, 事件类型: {}", event.getDeviceIp(), event.getType());
        ChannelHandlerContext ctx = clientChannels.get(event.getDeviceIp());
        if (ctx != null && ctx.channel().isActive()) {
            ctx.writeAndFlush(event);
            logger.debug("控制事件已发送到客户端: {}", event.getDeviceIp());
        } else {
            logger.warn("无法发送控制事件到客户端: {}，连接可能已断开", event.getDeviceIp());
        }
    }
    
    /**
     * 获取当前连接的客户端数量
     * 
     * @return 客户端连接数量
     */
    public int getClientCount() {
        logger.debug("当前客户端连接数量: {}", clientChannels.size());
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
        boolean connected = ctx != null && ctx.channel().isActive();
        logger.debug("检查客户端 {} 是否连接: {}", ipAddress, connected);
        return connected;
    }
}