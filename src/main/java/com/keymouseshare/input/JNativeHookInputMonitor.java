package com.keymouseshare.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener;
import com.keymouseshare.MainApplication;
import com.keymouseshare.bean.DeviceStorage;
import com.keymouseshare.bean.ScreenCoordinate;
import com.keymouseshare.bean.VirtualDesktopStorage;

import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 基于JNativeHook的键盘鼠标输入监听器
 * 用于监听本地设备的键盘和鼠标事件并打印日志
 */
public class JNativeHookInputMonitor implements NativeKeyListener, NativeMouseListener, NativeMouseMotionListener {
    private static final Logger logger = Logger.getLogger(JNativeHookInputMonitor.class.getName());
    
    private boolean isMonitoring = false;
    private BiConsumer<Integer, Integer> mousePositionListener;
    private MainApplication mainApplication;

    public JNativeHookInputMonitor() {
        logger.info("JNativeHookInputMonitor实例已创建");
    }
    
    /**
     * 设置主应用程序引用，用于调用相关方法
     * @param mainApplication 主应用程序实例
     */
    public void setMainApplication(MainApplication mainApplication) {
        this.mainApplication = mainApplication;
    }
    
    /**
     * 设置鼠标位置更新监听器
     * @param listener 鼠标位置监听器
     */
    public void setMousePositionListener(BiConsumer<Integer, Integer> listener) {
        this.mousePositionListener = listener;
    }
    
    /**
     * 开始监听本地输入事件
     */
    public void startMonitoring() {
        if (isMonitoring) {
            logger.info("JNativeHook监听器已在运行中");
            return;
        }
        
        try {
            // 禁用JNativeHook的默认日志记录器，避免日志过多
            java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.OFF);
            
            // 注册全局屏幕监听器
            GlobalScreen.registerNativeHook();
            
            // 添加事件监听器
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);
            GlobalScreen.addNativeMouseMotionListener(this);
            
            isMonitoring = true;
            logger.info("开始使用JNativeHook监听本地键盘鼠标事件");
            System.out.println("JNativeHook输入监听已启动");
        } catch (NativeHookException e) {
            logger.log(Level.SEVERE, "无法注册JNativeHook", e);
            System.err.println("无法注册JNativeHook: " + e.getMessage());
            isMonitoring = false;
        }
    }
    
    /**
     * 停止监听本地输入事件
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            logger.info("JNativeHook监听器未在运行");
            return;
        }
        
        try {
            // 移除事件监听器
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.removeNativeMouseListener(this);
            GlobalScreen.removeNativeMouseMotionListener(this);
            
            // 注销全局屏幕监听器
            GlobalScreen.unregisterNativeHook();
            
            isMonitoring = false;
            logger.info("停止使用JNativeHook监听本地键盘鼠标事件");
            System.out.println("JNativeHook输入监听已停止");
        } catch (NativeHookException e) {
            logger.log(Level.WARNING, "注销JNativeHook时发生异常", e);
        }
    }
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (!isMonitoring) return;
        
        // 暂停键盘事件日志打印
        // logger.info("键盘事件: 类型=按键按下, 键码=" + e.getKeyCode());
        // System.out.println("键盘事件: 类型=按键按下, 键码=" + e.getKeyCode());
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (!isMonitoring) return;
        
        // 暂停键盘事件日志打印
        // logger.info("键盘事件: 类型=按键释放, 键码=" + e.getKeyCode());
        // System.out.println("键盘事件: 类型=按键释放, 键码=" + e.getKeyCode());
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        if (!isMonitoring) return;
        
        // 暂停键盘事件日志打印
        // logger.info("键盘事件: 类型=按键输入, 字符=" + e.getKeyChar());
        // System.out.println("键盘事件: 类型=按键输入, 字符=" + e.getKeyChar());
    }
    
    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        if (!isMonitoring) return;
        
        // 暂停鼠标事件日志打印
        // logger.info("鼠标事件: 类型=点击, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
        // System.out.println("鼠标事件: 类型=点击, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
    }
    
    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (!isMonitoring) return;
        
        // 暂停鼠标事件日志打印
        // logger.info("鼠标事件: 类型=按下, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
        // System.out.println("鼠标事件: 类型=按下, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
    }
    
    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (!isMonitoring) return;
        
        // 暂停鼠标事件日志打印
        // logger.info("鼠标事件: 类型=释放, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
        // System.out.println("鼠标事件: 类型=释放, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
    }
    
    @Override
    public void nativeMouseMoved(NativeMouseEvent e) {
        if (!isMonitoring) return;
        
        // 暂停鼠标事件日志打印
//         logger.info("鼠标事件: 类型=移动, 位置=(" + e.getX() + "," + e.getY() + ")");
//         System.out.println("鼠标事件: 类型=移动, 位置=(" + e.getX() + "," + e.getY() + ")");
        
        // 通知鼠标位置监听器
        if (mousePositionListener != null) {
            mousePositionListener.accept(e.getX(), e.getY());
        }
    }
    
    @Override
    public void nativeMouseDragged(NativeMouseEvent e) {
        if (!isMonitoring) return;
        
        // 暂停鼠标事件日志打印
        // logger.info("鼠标事件: 类型=拖拽, 位置=(" + e.getX() + "," + e.getY() + ")");
        // System.out.println("鼠标事件: 类型=拖拽, 位置=(" + e.getX() + "," + e.getY() + ")");
        
        // 通知鼠标位置监听器
//        if (mousePositionListener != null) {
//            mousePositionListener.accept(e.getX(), e.getY());
//        }
    }

    /**
     * 检查监听器是否正在运行
     * @return 是否正在监听
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }
}