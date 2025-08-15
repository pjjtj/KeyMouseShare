package com.keymouseshare.input;

import com.keymouseshare.core.Controller;
import com.keymouseshare.screen.ScreenInfo;
import com.keymouseshare.screen.ScreenLayoutManager;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;

/**
 * 鼠标移动管理器
 * 处理鼠标边缘检测和屏幕切换逻辑
 */
public class MouseMovementManager implements MouseMotionListener {
    private Controller controller;
    private AbstractInputListenerManager inputListenerManager;
    private String activeDeviceId;
    private boolean isLocalActive = true;
    
    public MouseMovementManager(Controller controller) {
        this.controller = controller;
        this.inputListenerManager = InputListenerManagerFactory.createInputListenerManager();
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        handleMouseMovement(e.getX(), e.getY());
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        handleMouseMovement(e.getX(), e.getY());
    }
    
    /**
     * 处理鼠标移动事件
     */
    private void handleMouseMovement(int x, int y) {
        // 如果当前不是本地活动状态，不处理本地鼠标移动
        if (!isLocalActive) {
            return;
        }
        
        ScreenLayoutManager layoutManager = controller.getScreenLayoutManager();
        List<ScreenInfo> screens = layoutManager.getAllScreens();
        
        // 查找当前鼠标所在的屏幕
        ScreenInfo currentScreen = null;
        for (ScreenInfo screen : screens) {
            if (isPointInScreen(screen, x, y)) {
                currentScreen = screen;
                break;
            }
        }
        
        // 如果没有找到当前屏幕，返回
        if (currentScreen == null) {
            return;
        }
        
        // 检查是否需要切换到相邻屏幕
        checkScreenTransition(currentScreen, x, y);
    }
    
    /**
     * 检查屏幕切换条件
     */
    private void checkScreenTransition(ScreenInfo currentScreen, int x, int y) {
        ScreenLayoutManager layoutManager = controller.getScreenLayoutManager();
        List<ScreenInfo> screens = layoutManager.getAllScreens();
        
        // 检查左边缘
        if (x <= currentScreen.getX() + 5) {
            ScreenInfo targetScreen = findScreenAtEdge(screens, currentScreen, Edge.LEFT);
            if (targetScreen != null) {
                transitionToScreen(targetScreen, targetScreen.getWidth() - 5, y - currentScreen.getY() + targetScreen.getY());
            }
        }
        // 检查右边缘
        else if (x >= currentScreen.getX() + currentScreen.getWidth() - 5) {
            ScreenInfo targetScreen = findScreenAtEdge(screens, currentScreen, Edge.RIGHT);
            if (targetScreen != null) {
                transitionToScreen(targetScreen, 5, y - currentScreen.getY() + targetScreen.getY());
            }
        }
        // 检查上边缘
        else if (y <= currentScreen.getY() + 5) {
            ScreenInfo targetScreen = findScreenAtEdge(screens, currentScreen, Edge.TOP);
            if (targetScreen != null) {
                transitionToScreen(targetScreen, x - currentScreen.getX() + targetScreen.getX(), targetScreen.getHeight() - 5);
            }
        }
        // 检查下边缘
        else if (y >= currentScreen.getY() + currentScreen.getHeight() - 5) {
            ScreenInfo targetScreen = findScreenAtEdge(screens, currentScreen, Edge.BOTTOM);
            if (targetScreen != null) {
                transitionToScreen(targetScreen, x - currentScreen.getX() + targetScreen.getX(), 5);
            }
        }
    }
    
    /**
     * 查找边缘相邻的屏幕
     */
    private ScreenInfo findScreenAtEdge(List<ScreenInfo> screens, ScreenInfo currentScreen, Edge edge) {
        for (ScreenInfo screen : screens) {
            if (screen == currentScreen) {
                continue;
            }
            
            switch (edge) {
                case LEFT:
                    // 当前屏幕的左边缘与目标屏幕的右边缘对齐
                    if (Math.abs(currentScreen.getX() - (screen.getX() + screen.getWidth())) < 10 &&
                        isOverlapping(currentScreen.getY(), currentScreen.getY() + currentScreen.getHeight(),
                                     screen.getY(), screen.getY() + screen.getHeight())) {
                        return screen;
                    }
                    break;
                case RIGHT:
                    // 当前屏幕的右边缘与目标屏幕的左边缘对齐
                    if (Math.abs((currentScreen.getX() + currentScreen.getWidth()) - screen.getX()) < 10 &&
                        isOverlapping(currentScreen.getY(), currentScreen.getY() + currentScreen.getHeight(),
                                     screen.getY(), screen.getY() + screen.getHeight())) {
                        return screen;
                    }
                    break;
                case TOP:
                    // 当前屏幕的上边缘与目标屏幕的下边缘对齐
                    if (Math.abs(currentScreen.getY() - (screen.getY() + screen.getHeight())) < 10 &&
                        isOverlapping(currentScreen.getX(), currentScreen.getX() + currentScreen.getWidth(),
                                     screen.getX(), screen.getX() + screen.getWidth())) {
                        return screen;
                    }
                    break;
                case BOTTOM:
                    // 当前屏幕的下边缘与目标屏幕的上边缘对齐
                    if (Math.abs((currentScreen.getY() + currentScreen.getHeight()) - screen.getY()) < 10 &&
                        isOverlapping(currentScreen.getX(), currentScreen.getX() + currentScreen.getWidth(),
                                     screen.getX(), screen.getX() + screen.getWidth())) {
                        return screen;
                    }
                    break;
            }
        }
        return null;
    }
    
    /**
     * 检查两个范围是否重叠
     */
    private boolean isOverlapping(int start1, int end1, int start2, int end2) {
        return Math.max(start1, start2) < Math.min(end1, end2);
    }
    
    /**
     * 切换到目标屏幕
     */
    private void transitionToScreen(ScreenInfo targetScreen, int x, int y) {
        System.out.println("Transitioning to screen: " + targetScreen.getId() + " at (" + x + ", " + y + ")");
        
        // 如果目标屏幕是远程设备，需要切换到远程控制模式
        if (targetScreen.getDeviceType() == ScreenInfo.DeviceType.CLIENT ||
            targetScreen.getDeviceType() == ScreenInfo.DeviceType.SERVER) {
            
            // 停止本地输入监听
            inputListenerManager.stopListening();
            isLocalActive = false;
            activeDeviceId = targetScreen.getId();
            
            // 发送鼠标位置到远程设备
            sendMouseMoveToRemoteDevice(x, y);
        } else {
            // 本地屏幕切换，直接移动鼠标
            inputListenerManager.moveMouse(x, y);
        }
    }
    
    /**
     * 发送鼠标移动到远程设备
     */
    private void sendMouseMoveToRemoteDevice(int x, int y) {
        // 实现发送鼠标移动事件到远程设备的逻辑
        System.out.println("Sending mouse move to remote device: " + activeDeviceId + " at (" + x + ", " + y + ")");
        
        // 这里应该通过网络管理器发送数据包
        // controller.getNetworkManager().sendMouseMoveEvent(x, y, activeDeviceId);
    }
    
    /**
     * 检查点是否在屏幕内
     */
    private boolean isPointInScreen(ScreenInfo screen, int x, int y) {
        return x >= screen.getX() && x <= screen.getX() + screen.getWidth() &&
               y >= screen.getY() && y <= screen.getY() + screen.getHeight();
    }
    
    /**
     * 边缘枚举
     */
    private enum Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }
    
    /**
     * 设置本地活动状态
     */
    public void setLocalActive(boolean localActive) {
        this.isLocalActive = localActive;
        if (localActive) {
            // 恢复本地输入监听
            inputListenerManager.startListening();
        }
    }
    
    /**
     * 获取当前活动设备ID
     */
    public String getActiveDeviceId() {
        return activeDeviceId;
    }
}