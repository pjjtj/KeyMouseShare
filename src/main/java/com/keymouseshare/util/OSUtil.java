package com.keymouseshare.util;

/**
 * 操作系统工具类，用于检测和处理不同操作系统的特定功能
 */
public class OSUtil {
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    
    /**
     * 判断是否为Windows系统
     * @return true表示是Windows系统，false表示不是
     */
    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }
    
    /**
     * 判断是否为Mac系统
     * @return true表示是Mac系统，false表示不是
     */
    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }
    
    /**
     * 判断是否为Linux系统
     * @return true表示是Linux系统，false表示不是
     */
    public static boolean isLinux() {
        return OS_NAME.contains("nux");
    }
    
    /**
     * 获取操作系统名称
     * @return 操作系统名称
     */
    public static String getOSName() {
        return OS_NAME;
    }
    
    /**
     * 获取当前操作系统类型
     * @return 操作系统类型枚举
     */
    public static OSType getOSType() {
        if (isWindows()) {
            return OSType.WINDOWS;
        } else if (isMac()) {
            return OSType.MAC;
        } else if (isLinux()) {
            return OSType.LINUX;
        } else {
            return OSType.UNKNOWN;
        }
    }
    
    /**
     * 操作系统类型枚举
     */
    public enum OSType {
        WINDOWS, MAC, LINUX, UNKNOWN
    }
}