package com.keymouseshare.network;

import com.keymouseshare.bean.*;
import com.keymouseshare.util.NetUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 控制服务端处理器
 */
public class ControlServerHandler extends SimpleChannelInboundHandler<ControlEvent> {
    private static final Logger logger = Logger.getLogger(ControlServerHandler.class.getName());

    private final  VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    
    // 客户端连接映射
    private Map<String, ChannelHandlerContext> clientChannels;
    
    public ControlServerHandler(Map<String, ChannelHandlerContext> clientChannels) {
        this.clientChannels = clientChannels;
    }
    
    // 虚拟桌面实例

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientIp = NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString());
        logger.info("控制服务端连接已激活，客户端IP: " + clientIp);
        
        // 保存客户端连接
        clientChannels.put(clientIp, ctx);
        
        List<ScreenInfo> screenInfo = DeviceStorage.getInstance().getDeviceScreens(clientIp);
        // 添加屏幕到虚拟桌面
        if (screenInfo != null) {
            screenInfo.forEach(virtualDesktopStorage::addScreen);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientIp = NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString());
        logger.info("控制服务端连接已断开，客户端IP: " + clientIp);
        
        // 移除客户端连接
        clientChannels.remove(clientIp);
        
        // 从虚拟桌面中移除该客户端的屏幕信息
        VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
        // 移除该设备的所有屏幕信息
        virtualDesktopStorage.getScreens().entrySet().removeIf(entry -> 
            entry.getValue().getDeviceIp().equals(clientIp));
        virtualDesktopStorage.virtualDesktopChanged();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ControlEvent event) {
        String clientIp = NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString());
        // 处理从客户端接收到的控制事件
        logger.info("接收到控制事件: " + event.getType() + " 来自客户端: " + clientIp);
        // 这里可以添加具体的事件处理逻辑
        
        // 示例：将事件回传给客户端
        ctx.writeAndFlush(event);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientIp = NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString());
        logger.severe("控制服务端发生异常: " + cause.getMessage() + " 客户端IP: " + clientIp);
        ctx.close();
        
        // 移除客户端连接
        clientChannels.remove(clientIp);
    }
    
}