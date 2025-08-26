package com.keymouseshare.network;

import com.keymouseshare.bean.ControlEvent;
import com.keymouseshare.bean.ControlEventType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Logger;

/**
 * 控制客户端处理器
 */
public class ControlClientHandler extends SimpleChannelInboundHandler<ControlEvent> {
    private static final Logger logger = Logger.getLogger(ControlClientHandler.class.getName());

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("控制客户端连接已激活");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ControlEvent event) {
        // 处理从服务器接收到的控制事件
        logger.info("接收到控制事件: " + event.getType());
        // 这里可以添加具体的事件处理逻辑
        if(event.getType().equals(ControlEventType.MouseMoved.name())){
            logger.info("鼠标移动到: " + event.getX() + ", " + event.getY());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.severe("控制客户端发生异常: " + cause.getMessage());
        ctx.close();
    }
}