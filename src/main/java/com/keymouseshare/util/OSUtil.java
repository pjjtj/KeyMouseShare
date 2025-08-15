package com.keymouseshare.util;

/**
 * 操作系统工具类
 * 提供操作系统相关的工具方法
 */
public class OSUtil {
    
    /**
     * 获取操作系统名称
     */
    public static String getOSName() {
        return System.getProperty("os.name");
    }
    
    /**
     * 获取操作系统版本
     */
    public static String getOSVersion() {
        return System.getProperty("os.version");
    }
    
    /**
     * 获取操作系统架构
     */
    public static String getOSArch() {
        return System.getProperty("os.arch");
    }
    
    /**
     * 判断是否为Windows系统
     */
    public static boolean isWindows() {
        return getOSName().toLowerCase().contains("win");
    }
    
    /**
     * 判断是否为Mac系统
     */
    public static boolean isMac() {
        return getOSName().toLowerCase().contains("mac");
    }
    
    /**
     * 判断是否为Linux系统
     */
    public static boolean isLinux() {
        String osName = getOSName().toLowerCase();
        return osName.contains("nix") || osName.contains("nux") || osName.contains("aix");
    }
}