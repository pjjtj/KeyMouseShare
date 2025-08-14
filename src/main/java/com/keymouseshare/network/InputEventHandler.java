package com.keymouseshare.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import com.keymouseshare.input.*;
import com.keymouseshare.core.Controller;
import com.keymouseshare.config.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.DisplayMode;
import java.util.UUID;

/**
 * 输入事件处理器，处理从网络接收到的输入事件
 */
public class InputEventHandler extends SimpleChannelInboundHandler<InputEvent> {
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
    protected void channelRead0(ChannelHandlerContext ctx, InputEvent event) throws Exception {
        logger.debug("Received event: {}", event.getType());
        
        // 根据事件类型进行处理
        switch (event.getType()) {
            case MOUSE_MOVE:
                logger.info("Received mouse wake event - MOUSE_MOVE: ({}, {})", 
                    ((MouseEvent) event).getX(), ((MouseEvent) event).getY());
                controller.onMouseMove((MouseEvent) event);
                break;
            case MOUSE_PRESS:
                logger.info("Received mouse wake event - MOUSE_PRESS: button={}", 
                    ((MouseEvent) event).getButton());
                controller.onMousePress((MouseEvent) event);
                break;
            case MOUSE_RELEASE:
                logger.info("Received mouse wake event - MOUSE_RELEASE: button={}", 
                    ((MouseEvent) event).getButton());
                controller.onMouseRelease((MouseEvent) event);
                break;
            case MOUSE_WHEEL:
                logger.info("Received mouse wake event - MOUSE_WHEEL: amount={}", 
                    ((MouseEvent) event).getWheelAmount());
                controller.onMouseWheel((MouseEvent) event);
                break;
            case KEY_PRESS:
                logger.info("Received keyboard wake event - KEY_PRESS: keyCode={}", 
                    ((KeyEvent) event).getKeyCode());
                controller.onKeyPress((KeyEvent) event);
                break;
            case KEY_RELEASE:
                logger.info("Received keyboard wake event - KEY_RELEASE: keyCode={}", 
                    ((KeyEvent) event).getKeyCode());
                controller.onKeyRelease((KeyEvent) event);
                break;
            case FILE_DRAG_START:
                controller.onFileDragStart((FileDragEvent) event);
                break;
            case FILE_DRAG_END:
                controller.onFileDragEnd((FileDragEvent) event);
                break;
            default:
                logger.warn("Unknown event type: {}", event.getType());
                break;
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception in InputEventHandler", cause);
        ctx.close();
    }
}