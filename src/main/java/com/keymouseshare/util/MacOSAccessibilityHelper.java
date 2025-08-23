package com.keymouseshare.util;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * macOS辅助功能授权助手类
 * 用于检查和请求macOS系统的辅助功能权限
 */
public class MacOSAccessibilityHelper {
    
    private static final Logger logger = Logger.getLogger(MacOSAccessibilityHelper.class.getName());
    
    /**
     * 检查当前应用是否具有辅助功能权限
     * @return 如果已授权返回true，否则返回false
     */
    public static boolean isAccessibilityEnabled() {
        if (!isMacOS()) {
            return true; // 非macOS系统默认返回true
        }
        
        try {
            // 尝试使用JNA方式检查权限
            return checkAccessibilityWithJNA();
        } catch (Exception e) {
            logger.log(Level.WARNING, "使用JNA检查辅助功能权限时发生异常，尝试使用AppleScript方式", e);
            try {
                // 回退到AppleScript方式
                return checkAccessibilityWithAppleScript();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "检查辅助功能权限时发生异常", ex);
                return false;
            }
        }
    }
    
    /**
     * 使用JNA检查辅助功能权限
     * @return 如果已授权返回true，否则返回false
     * @throws Exception 检查过程中可能发生的异常
     */
    private static boolean checkAccessibilityWithJNA() throws Exception {
        // 使用反射加载JNA相关类，避免直接依赖
        Class<?> axuicoreClass = Class.forName("com.sun.jna.platform.mac.AXUIElement");
        Class<?> coreFoundationClass = Class.forName("com.sun.jna.platform.mac.CoreFoundation");
        Class<?> cfDictionaryClass = Class.forName("com.sun.jna.platform.mac.CoreFoundation$CFDictionaryRef");
        Class<?> cfTypeClass = Class.forName("com.sun.jna.platform.mac.CoreFoundation$CFTypeRef");
        Class<?> cfStringClass = Class.forName("com.sun.jna.platform.mac.CoreFoundation$CFStringRef");
        
        // 获取相关方法
        Method axIsProcessTrustedWithOptions = axuicoreClass.getMethod("AXIsProcessTrustedWithOptions", cfDictionaryClass);
        Method cfStringCreateWithCString = coreFoundationClass.getMethod("CFStringCreateWithCString", 
            Class.forName("com.sun.jna.Pointer"), String.class, int.class);
        Method cfDictionaryCreate = coreFoundationClass.getMethod("CFDictionaryCreate",
            Class.forName("com.sun.jna.Pointer"),
            Class.forName("com.sun.jna.PointerByReference"),
            Class.forName("com.sun.jna.PointerByReference"),
            int.class,
            Class.forName("com.sun.jna.platform.mac.CoreFoundation$CFDictionaryKeyCallBacks"),
            Class.forName("com.sun.jna.platform.mac.CoreFoundation$CFDictionaryValueCallBacks"));
        
        // 创建参数
        Object kAXTrustedCheckOptionPrompt = cfStringCreateWithCString.invoke(null, null, "AXTrustedCheckOptionPrompt", 0x08000100);
        Object kCFBooleanTrue = coreFoundationClass.getField("kCFBooleanTrue").get(null);
        
        // 创建字典
        Map<String, Object> options = new HashMap<>();
        options.put("AXTrustedCheckOptionPrompt", kCFBooleanTrue);
        
        // 调用检查方法
        Object result = axIsProcessTrustedWithOptions.invoke(null, options);
        return (Boolean) result;
    }
    
    /**
     * 使用AppleScript检查辅助功能权限
     * @return 如果已授权返回true，否则返回false
     * @throws Exception 执行命令时可能发生的异常
     */
    private static boolean checkAccessibilityWithAppleScript() throws Exception {
        // 使用AppleScript检查辅助功能权限
        String[] cmd = {
            "osascript", "-e",
            "tell application \"System Events\" to authorization status of bundle identifier \"" +
            System.getProperty("user.dir") + "\""
        };
        
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String result = reader.readLine();
        
        // 如果返回"authorized"表示已授权
        return "authorized".equals(result);
    }
    
    /**
     * 显示授权提示对话框
     */
    public static void showAccessibilityPrompt() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                null,
                "为了正常捕获键盘和鼠标事件，需要授予辅助功能权限。\n" +
                "请前往「系统偏好设置」->「安全性与隐私」->「隐私」标签页->「辅助功能」，\n" +
                "然后将本应用添加到允许列表中。",
                "需要辅助功能权限",
                JOptionPane.WARNING_MESSAGE
            );
        });
    }
    
    /**
     * 打开系统偏好设置中的辅助功能页面
     */
    public static void openAccessibilityPreferences() {
        try {
            String[] cmd = {"open", "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility"};
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            logger.log(Level.WARNING, "无法直接打开辅助功能设置页面", e);
            // 如果无法直接打开特定页面，则打开系统偏好设置
            try {
                String[] cmd = {"open", "-a", "System Preferences"};
                Runtime.getRuntime().exec(cmd);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "无法打开系统偏好设置", ex);
            }
        }
    }
    
    /**
     * 检查是否在macOS系统上运行
     * @return 如果在macOS上运行返回true，否则返回false
     */
    public static boolean isMacOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac");
    }
    
    /**
     * 检查并引导用户设置辅助功能权限
     */
    public static void checkAndPromptAccessibilityPermission() {
        if (!isMacOS()) {
            return;
        }
        
        logger.info("检测到运行在macOS系统上，检查辅助功能权限...");
        System.out.println("检测到运行在macOS系统上，检查辅助功能权限...");
        
        // 实际检查权限
        if (!isAccessibilityEnabled()) {
            showAccessibilityPromptWithGuide();
        }
    }
    
    /**
     * 显示带引导的授权提示对话框
     */
    private static void showAccessibilityPromptWithGuide() {
        SwingUtilities.invokeLater(() -> {
            String message = "<html>" +
                    "<h3>需要辅助功能权限</h3>" +
                    "<p>为了正常捕获键盘和鼠标事件，需要授予辅助功能权限。</p>" +
                    "<p>请按以下步骤操作：</p>" +
                    "<ol>" +
                    "<li>点击下方「打开系统偏好设置」按钮</li>" +
                    "<li>在「安全性与隐私」窗口中点击「隐私」标签</li>" +
                    "<li>在左侧列表中选择「辅助功能」</li>" +
                    "<li>点击左下角的锁图标并输入管理员密码</li>" +
                    "<li>点击「+」按钮添加本应用程序</li>" +
                    "<li>确保本应用程序在列表中被勾选</li>" +
                    "</ol>" +
                    "</html>";
            
            Object[] options = {"打开系统偏好设置", "稍后设置"};
            int choice = JOptionPane.showOptionDialog(
                null,
                message,
                "辅助功能权限",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (choice == 0) {
                openAccessibilityPreferences();
            }
        });
    }
}