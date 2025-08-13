package com.keymouseshare.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import com.keymouseshare.input.*;
import com.keymouseshare.core.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 输入事件处理器，处理从网络接收到的输入事件
 */
public class InputEventHandler extends SimpleChannelInboundHandler<InputEvent> {
    private static final Logger logger = LoggerFactory.getLogger(InputEventHandler.class);
    
    private Controller controller;
    
    public InputEventHandler(Controller controller) {
        this.controller = controller;
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