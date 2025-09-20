package com.keymouseshare.network;

import com.keymouseshare.bean.*;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.NetUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 控制服务端处理器
 */
public class ControlServerHandler extends SimpleChannelInboundHandler<ControlEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ControlServerHandler.class);

    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    
    // 客户端连接映射
    private Map<String, ChannelHandlerContext> clientChannels;
    
    public ControlServerHandler(Map<String, ChannelHandlerContext> clientChannels) {
        logger.debug("创建ControlServerHandler实例");
        this.clientChannels = clientChannels;
    }
    
    // 虚拟桌面实例

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientIp = NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString());
        logger.info("控制服务端连接已激活，客户端IP: {}", clientIp);
        
        // 保存客户端连接
        clientChannels.put(clientIp, ctx);
        logger.debug("客户端 {} 已添加到连接映射中，当前客户端数量: {}", clientIp, clientChannels.size());
        
        List<ScreenInfo> screenInfo = DeviceStorage.getInstance().getDeviceScreens(clientIp);
        logger.debug("获取到客户端 {} 的屏幕信息数量: {}", clientIp, screenInfo != null ? screenInfo.size() : 0);
        // 添加屏幕到虚拟桌面
        if (screenInfo != null) {
            screenInfo.forEach(virtualDesktopStorage::addScreen);
            logger.debug("已将客户端 {} 的屏幕信息添加到虚拟桌面", clientIp);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientIp = NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString());
        logger.info("控制服务端连接已断开，客户端IP: {}", clientIp);
        
        // 移除客户端连接
        clientChannels.remove(clientIp);
        logger.debug("客户端 {} 已从连接映射中移除，剩余客户端数量: {}", clientIp, clientChannels.size());
        
        // 从虚拟桌面中移除该客户端的屏幕信息
        VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
        int removedScreens = virtualDesktopStorage.getScreens().entrySet().removeIf(entry -> 
            entry.getValue().getDeviceIp().equals(clientIp)) ? 1 : 0;
        logger.debug("已从虚拟桌面中移除客户端 {} 的屏幕信息，移除数量: {}", clientIp, removedScreens);
        virtualDesktopStorage.virtualDesktopChanged();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ControlEvent event) {
        String clientIp = NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString());
        // 处理从客户端接收到的控制事件
        logger.debug("接收到控制事件: {} 来自客户端: {}", event.getType(), clientIp);
        // 这里可以添加具体的事件处理逻辑
        
        // 示例：将事件回传给客户端
        ctx.writeAndFlush(event);
        logger.debug("控制事件已处理并回传给客户端: {}", clientIp);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String clientIp = NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString());
        logger.error("控制服务端发生异常，客户端IP: {}，异常信息: {}", clientIp, cause.getMessage(), cause);
        ctx.close();
        
        // 移除客户端连接
        clientChannels.remove(clientIp);
        logger.debug("由于异常，客户端 {} 已从连接映射中移除", clientIp);
    }
    
}