package com.keymouseshare.network;

import com.keymouseshare.bean.*;
import com.keymouseshare.util.NetUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
import java.util.logging.Logger;

/**
 * 控制服务端处理器
 */
public class ControlServerHandler extends SimpleChannelInboundHandler<ControlEvent> {
    private static final Logger logger = Logger.getLogger(ControlServerHandler.class.getName());
    
    // 虚拟桌面实例

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("控制服务端连接已激活");
        List<ScreenInfo> screenInfo = DeviceStorage.getInstance().getDeviceScreens(NetUtil.dealRemoteAddress(ctx.channel().remoteAddress().toString()));
        // 添加屏幕到虚拟桌面
        VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
        screenInfo.forEach(virtualDesktopStorage::addScreen);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ControlEvent event) {
        // 处理从客户端接收到的控制事件
        logger.info("接收到控制事件: " + event.getType());
        // 这里可以添加具体的事件处理逻辑
        
        // 示例：将事件回传给客户端
        ctx.writeAndFlush(event);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.severe("控制服务端发生异常: " + cause.getMessage());
        ctx.close();
    }
    
}