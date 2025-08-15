package com.keymouseshare.network;

import com.keymouseshare.core.Controller;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 服务器端处理器
 * 处理来自客户端的连接和消息
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private Controller controller;
    
    public ServerHandler(Controller controller) {
        this.controller = controller;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 处理从客户端接收到的消息
        System.out.println("Received message from client: " + msg);
        super.channelRead(ctx, msg);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("Server handler exception: " + cause.getMessage());
        ctx.close();
    }
}