package com.keymouseshare.network;

import com.keymouseshare.bean.ControlEvent;
import com.keymouseshare.bean.ControlEventType;
import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.keyboard.MouseKeyBoardFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.awt.Robot;
import java.awt.event.InputEvent;
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
        logger.info("接收到控制事件: " + event.getType());
        
        // 根据事件类型调用相应的MouseKeyBoard方法
        switch (event.getType()) {
            case "MouseClicked":
                logger.info("鼠标点击: 按钮=" + event.getButton() + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                mouseKeyBoard.mouseClick(event.getX(), event.getY());
                break;
                
            case "MousePressed":
                logger.info("鼠标按下: 按钮=" + event.getButton() + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                mouseKeyBoard.mousePress(event.getButton());
                break;
                
            case "MouseReleased":
                logger.info("鼠标释放: 按钮=" + event.getButton() + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                mouseKeyBoard.mouseRelease(event.getButton());
                break;
                
            case "MouseMoved":
                logger.info("鼠标移动到: " + event.getX() + ", " + event.getY());
                mouseKeyBoard.mouseMove(event.getX(), event.getY());
                break;
                
            case "MouseDragged":
                logger.info("鼠标拖拽到: " + event.getX() + ", " + event.getY());
                mouseKeyBoard.mouseDragged();
                // 在拖拽过程中通常也会伴随鼠标移动
                mouseKeyBoard.mouseMove(event.getX(), event.getY());
                break;
                
            case "MouseWheel":
                logger.info("鼠标滚轮: 旋转=" + event.getButton() + ", 位置=(" + event.getX() + ", " + event.getY() + ")");
                // 使用Robot类处理鼠标滚轮事件，因为MouseKeyBoard接口没有滚轮方法
                try {
                    Robot robot = new Robot();
                    robot.mouseWheel(event.getButton()); // button字段存储滚轮旋转值
                } catch (Exception e) {
                    logger.severe("处理鼠标滚轮事件时出错: " + e.getMessage());
                }
                break;
                
            case "KeyPressed":
                logger.info("键盘按下: 键码=" + event.getKeyCode());
                mouseKeyBoard.keyPress(event.getKeyCode());
                break;
                
            case "KeyReleased":
                logger.info("键盘释放: 键码=" + event.getKeyCode());
                mouseKeyBoard.keyRelease(event.getKeyCode());
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