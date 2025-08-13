package com.keymouseshare.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import com.keymouseshare.input.*;
import com.keymouseshare.core.Controller;
import com.keymouseshare.config.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        }
        
        // 通知控制器有客户端连接
        DeviceConfig.Device device = new DeviceConfig.Device();
        device.setDeviceId(deviceId);
        device.setDeviceName("Client (" + remoteAddress + ")");
        device.setIpAddress(remoteAddress);
        // 设置默认屏幕尺寸
        device.setScreenWidth(1920);
        device.setScreenHeight(1080);
        // 设置默认网络位置，根据设备ID设置不同的位置以避免重叠
        int positionOffset = Math.abs(deviceId.hashCode()) % 100;
        device.setNetworkX(positionOffset * 200);  // 水平错开
        device.setNetworkY(positionOffset * 100);  // 垂直错开
        controller.onClientConnected(device);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        String id = deviceId;
        if (id == null) {
            id = "unknown-" + ctx.channel().id().asShortText();
        }
        logger.info("Client disconnected: {}", id);
        
        // 从管理器中移除客户端通道
        if (controller.getNetworkManager() != null) {
            controller.getNetworkManager().removeClientChannel(id);
        }
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InputEvent event) throws Exception {
        logger.debug("Received event: {}", event.getType());
        
        // 根据事件类型进行处理
        switch (event.getType()) {
            case MOUSE_MOVE:
                controller.onMouseMove((MouseEvent) event);
                break;
            case MOUSE_PRESS:
                controller.onMousePress((MouseEvent) event);
                break;
            case MOUSE_RELEASE:
                controller.onMouseRelease((MouseEvent) event);
                break;
            case MOUSE_WHEEL:
                controller.onMouseWheel((MouseEvent) event);
                break;
            case KEY_PRESS:
                controller.onKeyPress((KeyEvent) event);
                break;
            case KEY_RELEASE:
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