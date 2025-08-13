package com.keymouseshare.ui;

import com.keymouseshare.core.Controller;
import com.keymouseshare.screen.ScreenLayoutConfig;
import com.keymouseshare.util.OSUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 主窗口界面，提供图形化配置和控制功能
 */
public class MainWindow extends JFrame {
    private Controller controller;
    private JTextField serverHostField;
    private JTextField serverPortField;
    private JButton connectButton;
    private JButton startServerButton;
    private JButton screenLayoutButton;
    private JTextArea logArea;
    
    public MainWindow(Controller controller) {
        this.controller = controller;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("KeyMouseShare - 跨平台鼠标键盘共享工具");
        setSize(600, 400);
        setLocationRelativeTo(null); // 居中显示
    }
    
    private void initializeComponents() {
        serverHostField = new JTextField("localhost", 15);
        serverPortField = new JTextField("8888", 5);
        connectButton = new JButton("连接到服务器");
        startServerButton = new JButton("启动服务器");
        screenLayoutButton = new JButton("配置屏幕布局");
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
    }
    
    private void setupLayout() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 创建顶部配置面板
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("网络配置"));
        topPanel.add(new JLabel("服务器地址:"));
        topPanel.add(serverHostField);
        topPanel.add(new JLabel("端口:"));
        topPanel.add(serverPortField);
        topPanel.add(connectButton);
        topPanel.add(startServerButton);
        topPanel.add(screenLayoutButton);
        
        // 创建日志面板
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("运行日志"));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        // 添加到主面板
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(logPanel, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private void setupEventHandlers() {
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });
        
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });
        
        screenLayoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                configureScreenLayout();
            }
        });
    }
    
    private void connectToServer() {
        String host = serverHostField.getText().trim();
        String portStr = serverPortField.getText().trim();
        
        if (host.isEmpty()) {
            showError("请输入服务器地址");
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            showError("端口号必须是数字");
            return;
        }
        
        appendLog("正在连接到服务器 " + host + ":" + port);
        new Thread(() -> {
            try {
                controller.getNetworkManager().startClient(host, port);
                appendLog("连接成功");
            } catch (Exception e) {
                appendLog("连接失败: " + e.getMessage());
                showError("连接失败: " + e.getMessage());
            }
        }).start();
    }
    
    private void startServer() {
        String portStr = serverPortField.getText().trim();
        
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            showError("端口号必须是数字");
            return;
        }
        
        appendLog("正在启动服务器，端口:" + port);
        new Thread(() -> {
            try {
                controller.getNetworkManager().startServer(port);
                appendLog("服务器启动成功");
            } catch (Exception e) {
                appendLog("服务器启动失败: " + e.getMessage());
                showError("服务器启动失败: " + e.getMessage());
            }
        }).start();
    }
    
    private void configureScreenLayout() {
        ScreenLayoutConfig layoutConfig = controller.getScreenLayoutManager().getLayoutConfig();
        ScreenLayoutDialog dialog = new ScreenLayoutDialog(this, layoutConfig);
        dialog.setVisible(true);
        
        appendLog("屏幕布局配置完成");
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }
    
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.util.Date() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void showWindow() {
        setVisible(true);
        appendLog("KeyMouseShare 已启动. 操作系统: " + OSUtil.getOSName());
    }
}