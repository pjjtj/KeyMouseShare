package com.keymouseshare.input;

import java.awt.*;

/**
 * 基本输入监听管理器
 * 提供基础的输入监听功能实现
 */
public class BasicInputListenerManager extends AbstractInputListenerManager {
    private boolean listening = false;
    private Robot robot;
    
    public BasicInputListenerManager() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.err.println("Failed to create Robot instance: " + e.getMessage());
        }
    }
    
    @Override
    public void startListening() {
        listening = true;
        System.out.println("Started listening for input events");
    }
    
    @Override
    public void stopListening() {
        listening = false;
        System.out.println("Stopped listening for input events");
    }
    
    @Override
    public void moveMouse(int x, int y) {
        if (robot != null) {
            robot.mouseMove(x, y);
        }
    }
    
    @Override
    public void mousePress(int button) {
        if (robot != null) {
            robot.mousePress(button);
        }
    }
    
    @Override
    public void mouseRelease(int button) {
        if (robot != null) {
            robot.mouseRelease(button);
        }
    }
    
    @Override
    public void mouseWheel(int wheelAmt) {
        if (robot != null) {
            robot.mouseWheel(wheelAmt);
        }
    }
    
    @Override
    public void keyPress(int keycode) {
        if (robot != null) {
            robot.keyPress(keycode);
        }
    }
    
    @Override
    public void keyRelease(int keycode) {
        if (robot != null) {
            robot.keyRelease(keycode);
        }
    }
    
    @Override
    public Point getMousePosition() {
        return MouseInfo.getPointerInfo().getLocation();
    }
    
    @Override
    public boolean isListening() {
        return listening;
    }
}