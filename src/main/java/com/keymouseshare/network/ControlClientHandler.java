package com.keymouseshare.network;

import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.keymouseshare.bean.ControlEvent;
import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.keyboard.MouseKeyBoardFactory;
import com.keymouseshare.util.NativeToAwtKeyEventMapper;
import com.keymouseshare.util.NativeToAwtMouseEventMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Logger;

/**
 * 控制客户端处理器
 */
public class ControlClientHandler extends SimpleChannelInboundHandler<ControlEvent> {
    private static final Logger logger = Logger.getLogger(ControlClientHandler.class.getName());
    private final MouseKeyBoard mouseKeyBoard = MouseKeyBoardFactory.getFactory();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("控制客户端连接已激活");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ControlEvent event) {
        // 处理从服务器接收到的控制事件
//        logger.info("接收到控制事件: " + event.getType());

        // 根据事件类型调用相应的MouseKeyBoard方法
        switch (event.getType()) {

            case "MousePressed":
                logger.info("鼠标按下: 按钮=" + event.getButton() + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                logger.info("转换后: 按钮=" + NativeToAwtMouseEventMapper.toInputEventButton(event.getButton()) + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                mouseKeyBoard.mousePress(NativeToAwtMouseEventMapper.toInputEventButton(event.getButton()));
                break;

            case "MouseReleased":
                logger.info("鼠标释放: 按钮=" + event.getButton() + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                logger.info("转换后: 按钮=" + NativeToAwtMouseEventMapper.toInputEventButton(event.getButton()) + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                mouseKeyBoard.mouseRelease(NativeToAwtMouseEventMapper.toInputEventButton(event.getButton()));
                break;

            case "MouseMoved":
//                logger.info("鼠标移动到: " + event.getX() + ", " + event.getY());
                mouseKeyBoard.mouseMove(event.getX(), event.getY());
                break;

            case "MouseWheel":
                logger.info("鼠标滚轮: 旋转=" + event.getButton() + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                mouseKeyBoard.mouseWheel(event.getButton()); // button字段存储滚轮旋转值
                break;

            case "KeyPressed":
                logger.info("键盘按下: 键码=" + NativeToAwtKeyEventMapper.toAwtKeyCode(event.getKeyCode()));
                mouseKeyBoard.keyPress(NativeToAwtKeyEventMapper.toAwtKeyCode(event.getKeyCode()));
                break;

            case "KeyReleased":
                logger.info("键盘释放: 键码=" +NativeToAwtKeyEventMapper.toAwtKeyCode(event.getKeyCode()));
                mouseKeyBoard.keyRelease(NativeToAwtKeyEventMapper.toAwtKeyCode(event.getKeyCode()));
                break;

            default:
                logger.warning("未知的控制事件类型: " + event.getType());
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.severe("控制客户端发生异常: " + cause.getMessage());
        ctx.close();
    }
}