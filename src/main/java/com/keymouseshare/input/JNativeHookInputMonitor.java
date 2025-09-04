package com.keymouseshare.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.*;
import com.keymouseshare.MainApplication;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 基于JNativeHook的键盘鼠标输入监听器
 * 用于监听本地设备的键盘和鼠标事件并打印日志
 */
public class JNativeHookInputMonitor implements NativeKeyListener, NativeMouseListener, NativeMouseMotionListener, NativeMouseWheelListener {
    private static final Logger logger = Logger.getLogger(JNativeHookInputMonitor.class.getName());
    
    // 用于跟踪当前按下的键
    private final Set<Integer> pressedKeys = new HashSet<>();
    
    // 定义组合键
    private static final int CTRL_KEY = NativeKeyEvent.VC_CONTROL;
    private static final int ALT_KEY = NativeKeyEvent.VC_ALT;
    private static final int ESC_KEY = NativeKeyEvent.VC_ESCAPE;
    
    // 鼠标键盘事件监听器
    private MouseKeyBoardEventListener mouseKeyBoardEventListener;

    private boolean isMonitoring = false;
    private MainApplication mainApplication;

    // 鼠标事件监听器接口
    public interface MouseKeyBoardEventListener {
        void onMouseMove(int x, int y);
        void onMousePress(int button, int x, int y);
        void onMouseRelease(int button, int x, int y);
        void onMouseWheel(int rotation, int x, int y); // 添加滚轮事件
        void onKeyPress(char keyCode);
        void onKeyRelease(char keyCode);
    }

    public interface KeyBoardEventListener {

    }

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
     * 设置鼠标事件监听器
     * @param listener 鼠标事件监听器
     */
    public void setMouseEventListener(MouseKeyBoardEventListener listener) {
        this.mouseKeyBoardEventListener = listener;
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
            GlobalScreen.addNativeMouseWheelListener(this); // 添加滚轮事件监听器
            
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
            // 清空按键状态
            pressedKeys.clear();
            
            // 移除事件监听器
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.removeNativeMouseListener(this);
            GlobalScreen.removeNativeMouseMotionListener(this);
            GlobalScreen.removeNativeMouseWheelListener(this); // 移除滚轮事件监听器
            
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
        
        int keyCode = e.getKeyCode();
        pressedKeys.add(keyCode);

        System.out.println("Sent nativeKeyPressed event: " + e.getKeyCode());

        // 检查是否按下了Ctrl+Alt+Esc组合键
        if (isCtrlAltEscPressed()) {
            handleCtrlAltEscCombination();
            return;
        }
        
        // 转发键盘按下事件
        if (mouseKeyBoardEventListener != null) {
            mouseKeyBoardEventListener.onKeyPress(e.getKeyChar());
        }

        // 暂停键盘事件日志打印
        // logger.info("键盘事件: 类型=按键按下, 键码=" + e.getKeyCode());
        // System.out.println("键盘事件: 类型=按键按下, 键码=" + e.getKeyCode());
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (!isMonitoring) return;
        
        int keyCode = e.getKeyCode();
        pressedKeys.remove(keyCode);

        System.out.println("Sent nativeKeyReleased event: " + keyCode);

        // 转发键盘释放事件
        if (mouseKeyBoardEventListener != null) {
            mouseKeyBoardEventListener.onKeyRelease(e.getKeyChar());
        }
        
        // 暂停键盘事件日志打印
        // logger.info("键盘事件: 类型=按键释放, 键码=" + e.getKeyCode());
        // System.out.println("键盘事件: 类型=按键释放, 键码=" + e.getKeyCode());
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        if (!isMonitoring) return;
        
        // 转发键盘输入事件
        if (mainApplication != null) {
            // 可以在这里添加键盘事件转发逻辑
        }
        
        // 暂停键盘事件日志打印
        // logger.info("键盘事件: 类型=按键输入, 字符=" + e.getKeyChar());
        // System.out.println("键盘事件: 类型=按键输入, 字符=" + e.getKeyChar());
    }
    
    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (!isMonitoring) return;
        
        // 转发鼠标按下事件
        if (mouseKeyBoardEventListener != null) {
            mouseKeyBoardEventListener.onMousePress(e.getButton(), e.getX(), e.getY());
        }
        
        // 暂停鼠标事件日志打印
        // logger.info("鼠标事件: 类型=按下, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
        // System.out.println("鼠标事件: 类型=按下, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
    }
    
    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (!isMonitoring) return;
        
        // 转发鼠标释放事件
        if (mouseKeyBoardEventListener != null) {
            mouseKeyBoardEventListener.onMouseRelease(e.getButton(), e.getX(), e.getY());
        }
        
        // 暂停鼠标事件日志打印
        // logger.info("鼠标事件: 类型=释放, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
        // System.out.println("鼠标事件: 类型=释放, 按钮=" + e.getButton() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
    }
    
    @Override
    public void nativeMouseMoved(NativeMouseEvent e) {
        if (!isMonitoring) return;
        
        // 转发鼠标移动事件
        if (mouseKeyBoardEventListener != null) {
            mouseKeyBoardEventListener.onMouseMove(e.getX(), e.getY());
        }
        
        // 暂停鼠标事件日志打印
//         logger.info("鼠标事件: 类型=移动, 位置=(" + e.getX() + "," + e.getY() + ")");
//         System.out.println("鼠标事件: 类型=移动, 位置=(" + e.getX() + "," + e.getY() + ")");

    }
    
    @Override
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        if (!isMonitoring) return;
        
        // 转发鼠标滚轮事件
        if (mouseKeyBoardEventListener != null) {
            mouseKeyBoardEventListener.onMouseWheel(e.getWheelRotation(), e.getX(), e.getY());
        }
        
        // 暂停鼠标滚轮事件日志打印
        // logger.info("鼠标滚轮事件: 旋转=" + e.getWheelRotation() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
        // System.out.println("鼠标滚轮事件: 旋转=" + e.getWheelRotation() + ", 位置=(" + e.getX() + "," + e.getY() + ")");
    }

    /**
     * 检查是否按下了Ctrl+Alt+Esc组合键
     * @return 如果按下了组合键返回true，否则返回false
     */
    private boolean isCtrlAltEscPressed() {
        return pressedKeys.contains(CTRL_KEY) && 
               pressedKeys.contains(ALT_KEY) && 
               pressedKeys.contains(ESC_KEY);
    }
    
    /**
     * 检查是否按下了Ctrl键
     * @return 如果按下了Ctrl键返回true，否则返回false
     */
    private boolean isCtrlPressed() {
        return pressedKeys.contains(CTRL_KEY);
    }
    
    /**
     * 处理Ctrl+Alt+Esc组合键事件
     */
    private void handleCtrlAltEscCombination() {
        System.out.println("[HOTKEY] Ctrl+Alt+Esc pressed - 触发紧急停止");
        logger.info("检测到Ctrl+Alt+Esc组合键按下，触发紧急停止");
        
        // 如果主应用程序引用存在，调用紧急停止方法
        if (mainApplication != null) {
            // 这里可以调用主应用程序的紧急停止方法
            mainApplication.cancelKeyMouseShare();
        }
    }
    
    /**
     * 处理Ctrl+鼠标左键点击事件
     */
//    private void handleCtrlLeftClick() {
//        System.out.println("[HOTKEY] Ctrl+Left Click detected");
//        logger.info("检测到Ctrl+鼠标左键点击");
//
//        // 可以在这里添加特定的处理逻辑
//        // 例如：切换控制模式、标记屏幕区域等
//    }

    /**
     * 检查监听器是否正在运行
     * @return 是否正在监听
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }
}