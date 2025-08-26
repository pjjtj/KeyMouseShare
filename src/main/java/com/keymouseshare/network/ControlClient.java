package com.keymouseshare.network;

import com.keymouseshare.bean.ControlEvent;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Netty客户端初始化
 */
public class ControlClient {
    private EventLoopGroup group;
    private Channel channel;
    
    public void connect(String host, int port) throws Exception {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast(new ControlMessageDecoder())
                        .addLast(new ControlMessageEncoder())
                        .addLast(new ControlClientHandler());
                }
            });
        
        channel = bootstrap.connect(host, port).sync().channel();
    }
    
    public void sendMouseEdgeCheckEvent(ControlEvent event) {
        channel.writeAndFlush(event);
    }
    
    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}