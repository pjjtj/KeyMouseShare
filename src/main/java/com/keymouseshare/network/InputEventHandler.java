package com.keymouseshare.network;

import com.keymouseshare.config.DeviceConfig;
import com.keymouseshare.core.Controller;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * 输入事件处理器，处理从网络接收到的输入事件
 */
public class InputEventHandler extends SimpleChannelInboundHandler<DataPacket> {
    private static final Logger logger = LoggerFactory.getLogger(InputEventHandler.class);
    
    private Controller controller;
    private String deviceId;
    
    public InputEventHandler(Controller controller) {
        this.controller = controller;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // 生成设备ID，基于远程地址和通道ID确保唯一性
        String remoteAddress = ctx.channel().remoteAddress().toString();
        String channelId = ctx.channel().id().asShortText();
        deviceId = "client-" + remoteAddress.replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + "-" + channelId;
        
        logger.info("Client connected: {} (remote: {})", deviceId, remoteAddress);
        
        // 添加客户端通道到管理器
        if (controller.getNetworkManager() != null) {
            controller.getNetworkManager().addClientChannel(deviceId, ctx.channel());
            logger.debug("Client channel added to NetworkManager: {}", deviceId);
        }
        
        // 通知控制器有客户端连接
        DeviceConfig.Device device = new DeviceConfig.Device();
        device.setDeviceId(deviceId);
        device.setDeviceName("Client (" + remoteAddress + ")");
        device.setIpAddress(remoteAddress);
        // 获取实际屏幕分辨率，如果获取不到则使用默认值1920x1080
        int[] screenSize = getScreenSize();
        device.setScreenWidth(screenSize[0]);
        device.setScreenHeight(screenSize[1]);
        // 设置默认网络位置，根据设备ID设置不同的位置以避免重叠
        int positionOffset = Math.abs(deviceId.hashCode()) % 100;
        device.setNetworkX(positionOffset * 200);  // 水平错开
        device.setNetworkY(positionOffset * 100);  // 垂直错开
        controller.onClientConnected(device);
    }
    
    /**
     * 获取实际屏幕分辨率
     * @return 包含宽度和高度的数组，如果获取失败则返回默认值1920x1080
     */
    private int[] getScreenSize() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            
            // 遍历所有屏幕设备，找到主屏幕或选择最大屏幕
            GraphicsDevice primaryDevice = ge.getDefaultScreenDevice();
            DisplayMode dm = primaryDevice.getDisplayMode();
            
            int width = dm.getWidth();
            int height = dm.getHeight();
            
            // 确保获取到的尺寸是有效的
            if (width > 0 && height > 0) {
                logger.info("Primary screen size detected: {}x{}", width, height);
                return new int[]{width, height};
            }
            
            // 如果主屏幕无效，尝试其他屏幕
            for (GraphicsDevice screen : screens) {
                dm = screen.getDisplayMode();
                width = dm.getWidth();
                height = dm.getHeight();
                
                if (width > 0 && height > 0) {
                    logger.info("Screen size detected from device {}: {}x{}", 
                        screen.getIDstring(), width, height);
                    return new int[]{width, height};
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get screen size, using default 1920x1080", e);
        }
        
        // 默认返回1920x1080
        logger.info("Using default screen size: 1920x1080");
        return new int[]{1920, 1080};
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        String id = deviceId != null ? deviceId : "unknown-" + ctx.channel().id().asShortText();
        logger.info("Client disconnected: {}", id);
        
        // 从管理器中移除客户端通道
        if (controller.getNetworkManager() != null) {
            controller.getNetworkManager().removeClientChannel(id);
            logger.debug("Client channel removed from NetworkManager: {}", id);
        }
    }
    
    @Override
    public void channelRead0(ChannelHandlerContext ctx, DataPacket msg) throws Exception {
        String type = msg.getType();
        String deviceId = msg.getDeviceId();
        String remoteAddress = ctx.channel().remoteAddress().toString();
        
        logger.debug("Received message type: {} from device: {} ({})", type, deviceId, remoteAddress);
        
        if ("CONNECT".equals(type)) {
            // 处理客户端连接请求
            logger.info("Client connected: {} ({})", deviceId, remoteAddress);
            
            // 将客户端通道添加到网络管理器
            if (controller.getNetworkManager() != null) {
                controller.getNetworkManager().addClientChannel(deviceId, ctx.channel());
                logger.debug("Client channel added to NetworkManager: {}", deviceId);
            }
            
            // 通知控制器有客户端连接
            DeviceConfig.Device device = new DeviceConfig.Device();
            device.setDeviceId(deviceId);
            device.setDeviceName(msg.getData()); // 使用客户端发送的设备名称
            device.setIpAddress(remoteAddress);
            
            // 获取客户端发送的屏幕分辨率信息
            String screenData = msg.getExtraData();
            if (screenData != null && !screenData.isEmpty()) {
                try {
                    String[] parts = screenData.split("x");
                    if (parts.length == 2) {
                        int width = Integer.parseInt(parts[0]);
                        int height = Integer.parseInt(parts[1]);
                        device.setScreenWidth(width);
                        device.setScreenHeight(height);
                        logger.info("Client screen resolution: {}x{}", width, height);
                    } else {
                        // 如果解析失败，使用默认值
                        device.setScreenWidth(1920);
                        device.setScreenHeight(1080);
                        logger.warn("Failed to parse client screen data: {}, using default resolution", screenData);
                    }
                } catch (NumberFormatException e) {
                    // 如果解析失败，使用默认值
                    device.setScreenWidth(1920);
                    device.setScreenHeight(1080);
                    logger.warn("Failed to parse client screen resolution: {}, using default resolution", screenData, e);
                }
            } else {
                // 如果没有屏幕数据，使用默认值
                device.setScreenWidth(1920);
                device.setScreenHeight(1080);
                logger.info("No screen data from client, using default resolution 1920x1080");
            }
            
            // 设置默认网络位置，根据设备ID设置不同的位置以避免重叠
            int positionOffset = Math.abs(deviceId.hashCode()) % 100;
            device.setNetworkX(positionOffset * 200);  // 水平错开
            device.setNetworkY(positionOffset * 100);  // 垂直错开
            controller.onClientConnected(device);
        } else if ("INPUT".equals(type)) {
            // 处理输入事件
            // 暂时忽略，后续实现
            logger.debug("Received input event from device: {}", deviceId);
        } else if ("FILE_TRANSFER".equals(type)) {
            // 处理文件传输事件
            // 暂时忽略，后续实现
            logger.debug("Received file transfer event from device: {}", deviceId);
        } else {
            logger.warn("Unknown message type: {}", type);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception in InputEventHandler", cause);
        ctx.close();
    }
}