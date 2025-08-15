package com.keymouseshare.network;

import com.keymouseshare.core.Controller;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器端处理器
 * 处理来自客户端的连接和消息
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private Controller controller;
    private static final Map<String, ChannelHandlerContext> clientChannels = new ConcurrentHashMap<>();
    
    public ServerHandler(Controller controller) {
        this.controller = controller;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
        // 将客户端通道添加到映射中
        clientChannels.put(ctx.channel().remoteAddress().toString(), ctx);
        
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
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
        // 从映射中移除客户端通道
        clientChannels.remove(ctx.channel().remoteAddress().toString());
        
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
            System.out.println("Received data packet from client: " + packet.getType());
            
            // 处理数据包
            controller.getNetworkManager().handleDataPacket(packet, ctx);
        }
        
        super.channelRead(ctx, msg);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("Exception in server handler: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
    
    /**
     * 获取所有客户端通道
     */
    public static Map<String, ChannelHandlerContext> getClientChannels() {
        return new ConcurrentHashMap<>(clientChannels);
    }
}