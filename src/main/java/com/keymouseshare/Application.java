package com.keymouseshare;

import com.keymouseshare.core.Controller;

import javax.swing.*;

/**
 * KeyMouseShare - 跨平台鼠标键盘共享工具主程序入口
 * 支持Windows、Mac和Linux系统
 */
public class Application {
    public static void main(String[] args) {
        System.out.println("Starting KeyMouseShare...");
        
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set system look and feel: " + e.getMessage());
        }
        
        // 解析命令行参数
        boolean isServer = false;
        boolean isClient = false;
        
        for (String arg : args) {
            if ("--server".equals(arg)) {
                isServer = true;
            } else if ("--client".equals(arg)) {
                isClient = true;
            }
        }
        
        // 启动应用
        if (isServer) {
            System.out.println("Running in server mode");
            // 启动服务器模式
        } else if (isClient) {
            System.out.println("Running in client mode");
            // 启动客户端模式
        } else {
            System.out.println("Running in GUI mode");
            // 启动图形界面模式
            startGUI();
        }
    }
    
    /**
     * 启动图形界面模式
     */
    private static void startGUI() {
        SwingUtilities.invokeLater(() -> {
            try {
                Controller controller = new Controller();
                controller.start();
            } catch (Exception e) {
                System.err.println("Failed to start GUI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}