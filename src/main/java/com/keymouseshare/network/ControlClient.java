package com.keymouseshare.network;

import com.keymouseshare.bean.ControlEvent;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty客户端初始化
 */
public class ControlClient {
    private static final Logger logger = LoggerFactory.getLogger(ControlClient.class);
    
    private EventLoopGroup group;
    private Channel channel;
    
    public void connect(String host, int port) throws Exception {
        logger.debug("正在初始化ControlClient...");
        group = new NioEventLoopGroup();
        logger.debug("ControlClient EventLoopGroup已创建");
        
        Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    logger.debug("正在初始化客户端SocketChannel管道...");
                    ch.pipeline()
                        .addLast(new ControlMessageDecoder())
                        .addLast(new ControlMessageEncoder())
                        .addLast(new ControlClientHandler());
                    logger.debug("客户端SocketChannel管道初始化完成");
                }
            });
        
        logger.info("正在连接到服务器 {}:{}", host, port);
        channel = bootstrap.connect(host, port).sync().channel();
        logger.info("ControlClient已成功连接到服务器 {}:{}", host, port);
    }
    
    public void sendMouseEdgeCheckEvent(ControlEvent event) {
        logger.debug("正在发送鼠标边缘检查事件: {}", event);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(event);
            logger.debug("鼠标边缘检查事件已发送");
        } else {
            logger.warn("无法发送事件，连接未激活");
        }
    }
    
    public void disconnect() {
        logger.info("正在断开ControlClient连接...");
        if (channel != null) {
            channel.close();
            logger.debug("Channel已关闭");
        }
        if (group != null) {
            group.shutdownGracefully();
            logger.debug("EventLoopGroup已关闭");
        }
        logger.info("ControlClient连接已完全断开");
    }
}