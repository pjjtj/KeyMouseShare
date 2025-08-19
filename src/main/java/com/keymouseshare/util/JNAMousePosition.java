package com.keymouseshare.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.win32.W32APIOptions;

import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.MouseInfo;
import java.util.List;

/**
 * 使用JNA获取鼠标位置的工具类
 */
public class JNAMousePosition {
    
    /**
     * Windows用户库接口
     */
    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);
        
        /**
         * 获取鼠标位置
         * @param lpPoint POINT结构体指针
         * @return 成功返回非0值
         */
        boolean GetCursorPos(POINT lpPoint);
    }
    
    /**
     * Windows POINT结构体
     */
    @Structure.FieldOrder({"x", "y"})
    public static class POINT extends Structure {
        public int x;
        public int y;
    }
    
    /**
     * 获取当前鼠标位置
     * @return 鼠标位置点，如果获取失败则返回null
     */
    public static Point getMousePosition() {
        if (Platform.isWindows()) {
            return getWindowsMousePosition();
        } else if (Platform.isMac()) {
            return getMacMousePosition();
        } else if (Platform.isLinux()) {
            return getLinuxMousePosition();
        } else {
            // 对于其他平台，使用默认的AWT方法
            return getAWTMousePosition();
        }
    }
    
    /**
     * Windows平台获取鼠标位置
     * @return 鼠标位置点
     */
    private static Point getWindowsMousePosition() {
        try {
            POINT point = new POINT();
            if (User32.INSTANCE.GetCursorPos(point)) {
                return new Point(point.x, point.y);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getAWTMousePosition();
    }
    
    /**
     * macOS平台获取鼠标位置
     * @return 鼠标位置点
     */
    private static Point getMacMousePosition() {
        // 在macOS上，暂时使用AWT方法
        // 实际项目中可以使用更复杂的JNA绑定来直接调用Cocoa API
        return getAWTMousePosition();
    }
    
    /**
     * Linux平台获取鼠标位置
     * @return 鼠标位置点
     */
    private static Point getLinuxMousePosition() {
        // 在Linux上，我们使用现有的AWT方法
        return getAWTMousePosition();
    }
    
    /**
     * 使用AWT获取鼠标位置（后备方法）
     * @return 鼠标位置点
     */
    private static Point getAWTMousePosition() {
        try {
            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            if (pointerInfo != null) {
                return pointerInfo.getLocation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}