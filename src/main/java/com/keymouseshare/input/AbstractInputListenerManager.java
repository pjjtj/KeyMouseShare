package com.keymouseshare.input;

import java.awt.*;

/**
 * 抽象输入监听管理器
 * 定义跨平台输入监听的通用接口
 */
public abstract class AbstractInputListenerManager {
    
    /**
     * 启动输入监听
     */
    public abstract void startListening();
    
    /**
     * 停止输入监听
     */
    public abstract void stopListening();
    
    /**
     * 模拟鼠标移动
     */
    public abstract void moveMouse(int x, int y);
    
    /**
     * 模拟鼠标点击
     */
    public abstract void mousePress(int button);
    
    /**
     * 模拟鼠标释放
     */
    public abstract void mouseRelease(int button);
    
    /**
     * 模拟鼠标滚轮
     */
    public abstract void mouseWheel(int wheelAmt);
    
    /**
     * 模拟按键按下
     */
    public abstract void keyPress(int keycode);
    
    /**
     * 模拟按键释放
     */
    public abstract void keyRelease(int keycode);
    
    /**
     * 获取鼠标当前位置
     */
    public abstract Point getMousePosition();
    
    /**
     * 检查是否正在监听输入事件
     */
    public abstract boolean isListening();
}