package com.keymouseshare.util;

import com.sun.jna.Library;
import com.sun.jna.Memory;  // 添加Memory类导入
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.W32APIOptions;

import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.MouseInfo;
import java.util.Arrays;
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
     * macOS CoreGraphics库接口（暂不需要直接使用，保留以备将来需要）
     */
    public interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);
        
        /**
         * 获取当前鼠标位置
         * @param event 事件指针，通常传入null以获取当前位置
         * @return 位置指针
         */
        Pointer CGEventGetLocation(Pointer event);
        
        /**
         * 释放CoreFoundation对象
         * @param cf CoreFoundation对象指针
         */
        void CFRelease(Pointer cf);
    }
    
    /**
     * Windows POINT结构体
     */
    @Structure.FieldOrder({"x", "y"})
    public static class POINT extends Structure {
        public int x;
        public int y;
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("x", "y");
        }
    }
    
    /**
     * macOS CGPoint结构体
     */
    @Structure.FieldOrder({"x", "y"})
    public static class CGPoint extends Structure {
        public double x;
        public double y;
        
        public CGPoint() {
            super();
        }
        
        public CGPoint(double x, double y) {
            super();
            this.x = x;
            this.y = y;
        }
        
        public CGPoint(Pointer p) {
            super(p);
            read();
        }
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
        try {
            // 在macOS上，我们使用AWT方法作为主要方法，因为直接使用CGEventGetLocation(null)可能不工作
            return getAWTMousePosition();
        } catch (Exception e) {
            System.err.println("获取macOS鼠标位置时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
        // 如果AWT方法失败，回退到另一种方法
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