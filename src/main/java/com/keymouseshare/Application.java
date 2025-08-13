package com.keymouseshare;

import com.keymouseshare.core.Controller;
import com.keymouseshare.input.InputListenerManager;
import com.keymouseshare.input.InputListenerManagerFactory;
import com.keymouseshare.ui.MainWindow;
import com.keymouseshare.util.OSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 跨平台鼠标键盘共享应用主入口
 * 支持Windows和Mac平台的鼠标键盘共享及文件拖拽功能
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    public static void main(String[] args) {
        logger.info("KeyMouseShare Application Starting...");
        
        // 记录系统和网络信息
        logSystemInfo();
        
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Failed to set system look and feel", e);
        }
        
        // 创建并初始化控制器
        Controller controller = new Controller();
        controller.initialize();
        
        // 创建输入监听管理器
        InputListenerManager inputListenerManager = InputListenerManagerFactory.createInputListenerManager();
        controller.setInputListenerManager(inputListenerManager);
        
        // 根据参数决定启动模式
        if (args.length > 0 && "--server".equals(args[0])) {
            logger.info("Starting in server mode");
            controller.getNetworkManager().startServer(8888);
        } else if (args.length > 0 && "--client".equals(args[0])) {
            logger.info("Starting in client mode");
            controller.getNetworkManager().startClient("localhost", 8888);
        } else {
            logger.info("Starting in GUI mode");
            // 启动GUI界面
            SwingUtilities.invokeLater(() -> {
                MainWindow mainWindow = new MainWindow(controller);
                mainWindow.showWindow();
            });
        }
        
        // 启动输入监听
        if (inputListenerManager != null) {
            inputListenerManager.startListening();
        }
        
        // 启动控制器
        controller.start();
        
        // 添加关闭钩子，确保程序正常退出
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application...");
            controller.stop();
        }));
    }
    
    /**
     * 记录系统和网络信息
     */
    private static void logSystemInfo() {
        logger.info("Operating System: {}", System.getProperty("os.name"));
        logger.info("OS Version: {}", System.getProperty("os.version"));
        logger.info("Architecture: {}", System.getProperty("os.arch"));
        logger.info("Java Version: {}", System.getProperty("java.version"));
        logger.info("Java Vendor: {}", System.getProperty("java.vendor"));
        
        try {
            logger.info("Host Name: {}", InetAddress.getLocalHost().getHostName());
            logger.info("Host Address: {}", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            logger.warn("Failed to get host info", e);
        }
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            logger.info("Network interfaces:");
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    logger.info("  {}: {}", networkInterface.getName(), networkInterface.getDisplayName());
                    networkInterface.getInterfaceAddresses().stream()
                        .filter(addr -> addr.getAddress() instanceof java.net.Inet4Address)
                        .forEach(addr -> {
                            logger.info("    IP: {}", addr.getAddress().getHostAddress());
                        });
                }
            }
        } catch (SocketException e) {
            logger.warn("Failed to get network interfaces", e);
        }
        
        // 检查防火墙可能的影响
        if (OSUtil.isMac()) {
            logger.info("Running on macOS - please ensure the app has accessibility permissions");
            logger.info("Also check if firewall is blocking connections on ports 8888 and 8889");
        }
    }
}