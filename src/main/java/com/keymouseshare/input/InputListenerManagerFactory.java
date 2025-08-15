package com.keymouseshare.input;

import com.keymouseshare.util.OSUtil;

/**
 * 输入监听管理器工厂类
 * 根据操作系统类型创建相应的输入监听管理器
 */
public class InputListenerManagerFactory {
    
    /**
     * 创建适合当前操作系统的输入监听管理器
     */
    public static AbstractInputListenerManager createInputListenerManager() {
        String osName = OSUtil.getOSName().toLowerCase();
        
        if (osName.contains("win")) {
            // Windows系统
            try {
                return new BasicInputListenerManager(); // 实际项目中应使用Windows专用实现
            } catch (Exception e) {
                System.err.println("Failed to create Windows input listener manager: " + e.getMessage());
                return new BasicInputListenerManager();
            }
        } else if (osName.contains("mac")) {
            // Mac系统
            try {
                return new BasicInputListenerManager(); // 实际项目中应使用Mac专用实现
            } catch (Exception e) {
                System.err.println("Failed to create Mac input listener manager: " + e.getMessage());
                return new BasicInputListenerManager();
            }
        } else {
            // Linux或其他系统
            try {
                return new BasicInputListenerManager(); // 实际项目中应使用Linux专用实现
            } catch (Exception e) {
                System.err.println("Failed to create Linux input listener manager: " + e.getMessage());
                return new BasicInputListenerManager();
            }
        }
    }
}