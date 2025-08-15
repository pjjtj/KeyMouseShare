package com.keymouseshare.network;

import com.google.gson.Gson;
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
        System.out.println("[ClientHandler] Connected to server: " + ctx.channel().remoteAddress());
        
        // 发送本地设备信息给服务器
        System.out.println("[ClientHandler] Attempting to send device info to server...");
        sendDeviceInfoToServer(ctx);
        
        // 通知主窗口刷新设备列表
        if (controller.getMainWindow() != null) {
            SwingUtilities.invokeLater(() -> {
                System.out.println("[ClientHandler] Refreshing device list in main window...");
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
            System.out.println("Received data packet from server: " + packet.getType());
            
            // 处理数据包
            controller.getNetworkManager().handleDataPacket(packet, ctx);
        }
        
        super.channelRead(ctx, msg);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("Exception in client handler: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
    
    /**
     * 发送设备信息给服务器
     */
    private void sendDeviceInfoToServer(ChannelHandlerContext ctx) {
        try {
            // 获取本地设备信息
            DeviceInfo localDeviceInfo = controller.getNetworkManager().getLocalDeviceInfo();
            if (localDeviceInfo != null) {
                // 创建设备信息数据包
                String deviceInfoJson = new Gson().toJson(localDeviceInfo);
                DataPacket packet = new DataPacket(
                    DataPacket.TYPE_DEVICE_INFO,
                    localDeviceInfo.getDeviceId(),
                    deviceInfoJson
                );
                
                // 发送数据包
                ctx.writeAndFlush(packet);
                System.out.println("Sent device info to server: " + localDeviceInfo.getDeviceName());
            }
        } catch (Exception e) {
            System.err.println("Error sending device info to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}