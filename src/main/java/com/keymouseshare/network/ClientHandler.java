package com.keymouseshare.network;

import com.keymouseshare.core.Controller;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javax.swing.*;

/**
 * 客户端处理器
 * 处理与服务器的连接和消息
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private Controller controller;
    
    public ClientHandler(Controller controller) {
        this.controller = controller;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Connected to server: " + ctx.channel().remoteAddress());
        
        // 通知主窗口刷新设备列表
        if (controller.getMainWindow() != null) {
            SwingUtilities.invokeLater(() -> {
                controller.getMainWindow().refreshDeviceList();
            });
        }
        
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Disconnected from server: " + ctx.channel().remoteAddress());
        
        // 通知主窗口刷新设备列表
        if (controller.getMainWindow() != null) {
            SwingUtilities.invokeLater(() -> {
                controller.getMainWindow().refreshDeviceList();
            });
        }
        
        super.channelInactive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DataPacket) {
            DataPacket packet = (DataPacket) msg;
            // 使用更具体的日志记录
            System.out.println("Received data packet from server: " + packet.getType());
            
            // 处理数据包
            controller.getNetworkManager().handleDataPacket(packet, ctx);
        }
        
        super.channelRead(ctx, msg);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 使用更具体的日志记录
        System.err.println("Exception in client handler: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}