package com.keymouseshare.ui;

import com.keymouseshare.core.Controller;
import com.keymouseshare.core.DeviceControlManager;
import com.keymouseshare.network.DeviceInfo;
import com.keymouseshare.screen.ScreenInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * 主窗口界面
 * 提供图形用户界面用于设备管理和屏幕布局配置
 */
public class MainWindow extends JFrame {
    private Controller controller;
    private JPanel mainPanel;
    private JButton serverButton;
    private JButton configLayoutButton;
    private JPanel devicePanel;
    private DefaultListModel<DeviceItem> deviceListModel;
    private JList<DeviceItem> deviceList;
    
    public MainWindow(Controller controller) {
        this.controller = controller;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        setTitle("KeyMouseShare - 跨平台鼠标键盘共享工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // 居中显示
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        mainPanel = new JPanel(new BorderLayout());
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        serverButton = new JButton("启动服务器");
        configLayoutButton = new JButton("配置屏幕布局");
        JButton refreshButton = new JButton("刷新设备");
        buttonPanel.add(serverButton);
        buttonPanel.add(configLayoutButton);
        buttonPanel.add(refreshButton);
        
        // 创建设备列表
        deviceListModel = new DefaultListModel<>();
        deviceList = new JList<>(deviceListModel);
        deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceList.setCellRenderer(new DeviceListCellRenderer());
        JScrollPane scrollPane = new JScrollPane(deviceList);
        
        devicePanel = new JPanel(new BorderLayout());
        devicePanel.setBorder(BorderFactory.createTitledBorder("发现的设备"));
        devicePanel.add(scrollPane, BorderLayout.CENTER);
        
        // 添加本地设备信息（本地设备默认为允许控制）
        deviceListModel.addElement(new DeviceItem("local", "本地设备", DeviceControlManager.ControlPermission.ALLOWED));
        
        // 添加刷新按钮事件处理
        refreshButton.addActionListener(e -> refreshDeviceList());
    }
    
    /**
     * 添加本地设备到列表
     */
    private void addLocalDeviceToList() {
        // 本地设备默认显示简单名称，权限默认为允许
        deviceListModel.addElement(new DeviceItem("local", "本地设备", DeviceControlManager.ControlPermission.ALLOWED));
    }
    
    /**
     * 获取本地IP地址
     */
    private String getLocalIpAddress() {
        try {
            // 尝试从NetworkManager获取本地设备信息
            if (controller.getNetworkManager() != null) {
                DeviceInfo localDeviceInfo = controller.getNetworkManager().getLocalDeviceInfo();
                if (localDeviceInfo != null) {
                    return localDeviceInfo.getIpAddress();
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting local IP from NetworkManager: " + e.getMessage());
        }
        
        // 备用方法：使用系统属性
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.err.println("Error getting local IP from system: " + e.getMessage());
        }
        
        return "127.0.0.1"; // 默认值
    }
    
    /**
     * 刷新设备列表
     */
    public void refreshDeviceList() {
        refreshDeviceListInternal();
    }
    
    /**
     * 刷新设备列表
     */
    private void refreshDeviceListInternal() {
        // 清空当前列表（除了本地设备）
        deviceListModel.clear();
        deviceListModel.addElement(new DeviceItem("local", "本地设备", DeviceControlManager.ControlPermission.ALLOWED));
        
        // 添加发现的设备
        try {
            if (controller.getNetworkManager() != null) {
                // 添加已连接的设备
                for (DeviceInfo deviceInfo : controller.getNetworkManager().getConnectedDevices()) {
                    if (deviceInfo.isOnline()) {
                        int screenCount = 0;
                        if (deviceInfo.getScreens() != null) {
                            screenCount = deviceInfo.getScreens().size();
                        }
                        
                        String displayName = deviceInfo.getDeviceName() + " (" + deviceInfo.getIpAddress() + ") [" + screenCount + "个屏幕] [已连接]";
                        deviceListModel.addElement(new DeviceItem(
                            deviceInfo.getDeviceId(),
                            displayName,
                            DeviceControlManager.ControlPermission.ALLOWED
                        ));
                    }
                }
                
                // 添加发现但未连接的设备
                for (DeviceInfo deviceInfo : controller.getNetworkManager().getDiscoveredDevices()) {
                    if (deviceInfo.isOnline()) {
                        // 检查是否已经在已连接设备列表中
                        boolean isConnected = false;
                        for (DeviceInfo connectedDevice : controller.getNetworkManager().getConnectedDevices()) {
                            if (connectedDevice.getDeviceId().equals(deviceInfo.getDeviceId())) {
                                isConnected = true;
                                break;
                            }
                        }
                        
                        // 如果未连接，则添加到列表中
                        if (!isConnected) {
                            int screenCount = 0;
                            if (deviceInfo.getScreens() != null) {
                                screenCount = deviceInfo.getScreens().size();
                            }
                            
                            // 获取设备的控制权限状态
                            DeviceControlManager.ControlPermission permission = controller.getDeviceControlManager().getDevicePermission(deviceInfo.getDeviceId());
                            
                            String status = "[未连接]";
                            if (permission == null || permission == DeviceControlManager.ControlPermission.DELAY_5_MINUTES ||
                                permission == DeviceControlManager.ControlPermission.DELAY_10_MINUTES ||
                                permission == DeviceControlManager.ControlPermission.DELAY_30_MINUTES) {
                                status = "[等待授权]";
                            }
                            
                            String displayName = deviceInfo.getDeviceName() + " (" + deviceInfo.getIpAddress() + ") [" + screenCount + "个屏幕] " + status;
                            deviceListModel.addElement(new DeviceItem(
                                deviceInfo.getDeviceId(),
                                displayName,
                                permission
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error refreshing device list: " + e.getMessage());
        }
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        mainPanel.add(devicePanel, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JLabel("状态: 就绪"), BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(serverButton);
        buttonPanel.add(configLayoutButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        serverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleServerMode();
            }
        });
        
        configLayoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openScreenLayoutConfig();
            }
        });
        
        // 添加设备列表双击事件
        deviceList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = deviceList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        DeviceItem item = deviceListModel.getElementAt(index);
                        showPermissionDialog(item);
                    }
                }
            }
        });
    }
    
    /**
     * 切换服务器模式
     */
    private void toggleServerMode() {
        if (controller.isServerMode()) {
            controller.stopServer();
            serverButton.setText("启动服务器");
        } else {
            controller.startServer();
            serverButton.setText("停止服务器");
        }
    }
    
    /**
     * 打开屏幕布局配置窗口
     */
    private void openScreenLayoutConfig() {
        ScreenLayoutConfigDialog dialog = new ScreenLayoutConfigDialog(this, controller);
        dialog.setVisible(true);
    }
    
    /**
     * 当服务器启动时调用
     */
    public void onServerStarted() {
        serverButton.setText("停止服务器");
        // 更新UI状态
    }
    
    /**
     * 当服务器停止时调用
     */
    public void onServerStopped() {
        serverButton.setText("启动服务器");
        // 更新UI状态
    }
    
    /**
     * 当发现新设备时调用
     */
    public void onDeviceDiscovered(String deviceIp) {
        SwingUtilities.invokeLater(() -> {
            // 检查设备是否已存在于列表中
            boolean exists = false;
            for (int i = 0; i < deviceListModel.size(); i++) {
                DeviceItem item = deviceListModel.getElementAt(i);
                if (item.getDisplayName().contains(deviceIp)) {
                    exists = true;
                    break;
                }
            }
            
            // 如果不存在，则添加到列表
            if (!exists) {
                // 获取屏幕数量
                int screenCount = 1;
                try {
                    // 从已发现的设备中查找对应的设备信息
                    if (controller.getNetworkManager() != null) {
                        for (DeviceInfo deviceInfo : controller.getNetworkManager().getDiscoveredDevices()) {
                            if (deviceIp.equals(deviceInfo.getIpAddress()) && deviceInfo.isOnline()) {
                                if (deviceInfo.getScreens() != null) {
                                    screenCount = deviceInfo.getScreens().size();
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error getting screen count for new device: " + e.getMessage());
                }
                
                String displayName = "新设备 (" + deviceIp + ") [" + screenCount + "个屏幕]";
                // 默认权限状态为未允许
                deviceListModel.addElement(new DeviceItem(
                    deviceIp, 
                    displayName, 
                    null)); // null表示使用默认的未允许状态
            }
        });
    }
    
    /**
     * 显示控制授权请求对话框
     */
    public void showControlAuthorizationRequest(DeviceInfo requestingDevice) {
        // 检查设备是否已经连接或者已经授权，避免重复提示
        String deviceId = requestingDevice.getDeviceId();
        
        // 检查设备是否已经连接
        boolean isConnected = false;
        try {
            isConnected = controller.getNetworkManager().getConnectedDevices().stream()
                .anyMatch(device -> device.getDeviceId().equals(deviceId));
        } catch (Exception e) {
            System.err.println("Error checking connected devices: " + e.getMessage());
        }
        
        // 检查设备是否已经被授权
        boolean isAllowed = false;
        try {
            isAllowed = controller.getDeviceControlManager().isDeviceAllowed(deviceId);
        } catch (Exception e) {
            System.err.println("Error checking device permission: " + e.getMessage());
        }
        
        // 如果设备已经连接或已授权，则不显示提示框
        if (isConnected || isAllowed) {
            System.out.println("Device " + deviceId + " is already connected or allowed, skipping authorization dialog");
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            String message = String.format(
                "<html><b>%s (%s)</b> 请求控制您的设备<br><br>请选择操作：</html>",
                requestingDevice.getDeviceName(),
                requestingDevice.getIpAddress()
            );
            
            String[] options = {"允许", "5分钟后提醒", "10分钟后提醒", "30分钟后提醒", "拒绝"};
            int choice = JOptionPane.showOptionDialog(
                this,
                message,
                "控制授权请求",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            // 确保在处理用户选择前设备仍然未连接
            boolean stillNotConnected = true;
            try {
                stillNotConnected = !controller.getNetworkManager().getConnectedDevices().stream()
                    .anyMatch(device -> device.getDeviceId().equals(deviceId));
            } catch (Exception e) {
                System.err.println("Error re-checking connected devices: " + e.getMessage());
            }
            
            if (stillNotConnected) {
                switch (choice) {
                    case 0: // 允许
                        controller.getNetworkManager().handleControlAuthorizationResponse(requestingDevice, true);
                        controller.getDeviceControlManager().setDevicePermission(requestingDevice.getDeviceId(), 
                            DeviceControlManager.ControlPermission.ALLOWED);
                        break;
                    case 1: // 5分钟后提醒
                        controller.getDeviceControlManager().setDevicePermission(requestingDevice.getDeviceId(), 
                            DeviceControlManager.ControlPermission.DELAY_5_MINUTES);
                        controller.getNetworkManager().handleControlAuthorizationResponse(requestingDevice, false);
                        break;
                    case 2: // 10分钟后提醒
                        controller.getDeviceControlManager().setDevicePermission(requestingDevice.getDeviceId(), 
                            DeviceControlManager.ControlPermission.DELAY_10_MINUTES);
                        controller.getNetworkManager().handleControlAuthorizationResponse(requestingDevice, false);
                        break;
                    case 3: // 30分钟后提醒
                        controller.getDeviceControlManager().setDevicePermission(requestingDevice.getDeviceId(), 
                            DeviceControlManager.ControlPermission.DELAY_30_MINUTES);
                        controller.getNetworkManager().handleControlAuthorizationResponse(requestingDevice, false);
                        break;
                    case 4: // 拒绝
                    default:
                        controller.getNetworkManager().handleControlAuthorizationResponse(requestingDevice, false);
                        break;
                }
            } else {
                System.out.println("Device " + deviceId + " got connected while showing dialog, skipping action");
            }
        });
    }
    
    /**
     * 显示权限设置对话框
     */
    private void showPermissionDialog(DeviceItem item) {
        String[] options = {"允许", "5分钟后提醒", "10分钟后提醒", "30分钟后提醒"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "设置设备 " + item.getDisplayName() + " 的控制权限:",
            "控制权限设置",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (choice >= 0) {
            DeviceControlManager.ControlPermission permission = DeviceControlManager.ControlPermission.ALLOWED;
            switch (choice) {
                case 0:
                    permission = DeviceControlManager.ControlPermission.ALLOWED;
                    break;
                case 1:
                    permission = DeviceControlManager.ControlPermission.DELAY_5_MINUTES;
                    break;
                case 2:
                    permission = DeviceControlManager.ControlPermission.DELAY_10_MINUTES;
                    break;
                case 3:
                    permission = DeviceControlManager.ControlPermission.DELAY_30_MINUTES;
                    break;
            }
            
            item.setPermission(permission);
            controller.getDeviceControlManager().setDevicePermission(item.getDeviceId(), permission);
            deviceList.repaint();
        }
    }
    
    /**
     * 设备列表项类
     */
    private static class DeviceItem {
        private String deviceId;
        private String displayName;
        private DeviceControlManager.ControlPermission permission;
        
        public DeviceItem(String deviceId, String displayName, DeviceControlManager.ControlPermission permission) {
            this.deviceId = deviceId;
            this.displayName = displayName;
            this.permission = permission;
        }
        
        // Getters and Setters
        public String getDeviceId() {
            return deviceId;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public DeviceControlManager.ControlPermission getPermission() {
            return permission;
        }
        
        public void setPermission(DeviceControlManager.ControlPermission permission) {
            this.permission = permission;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    /**
     * 设备列表单元格渲染器
     */
    private static class DeviceListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof DeviceItem) {
                DeviceItem item = (DeviceItem) value;
                setText(item.toString());
                
                // 根据权限状态和连接状态设置颜色
                DeviceControlManager.ControlPermission permission = item.getPermission();
                
                // 检查是否包含已连接状态
                String displayName = item.getDisplayName();
                if (displayName.contains("[已连接]")) {
                    // 已连接设备使用绿色
                    setForeground(Color.GREEN);
                } else if (permission == DeviceControlManager.ControlPermission.ALLOWED) {
                    setForeground(Color.BLACK);
                } else if (permission == DeviceControlManager.ControlPermission.DELAY_5_MINUTES ||
                           permission == DeviceControlManager.ControlPermission.DELAY_10_MINUTES ||
                           permission == DeviceControlManager.ControlPermission.DELAY_30_MINUTES) {
                    // 等待授权的设备使用灰色
                    setForeground(Color.GRAY);
                } else {
                    // 默认状态（未允许）使用红色显示
                    setForeground(Color.RED);
                }
                
                // 本地设备使用特殊颜色
                if ("local".equals(item.getDeviceId())) {
                    setBackground(Color.CYAN);
                }
            }
            
            return this;
        }
    }
}