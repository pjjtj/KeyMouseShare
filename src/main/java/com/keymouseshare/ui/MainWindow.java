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
        
        // 添加本地设备示例
        deviceListModel.addElement(new DeviceItem("local", "本地设备", DeviceControlManager.ControlPermission.ALLOWED));
        
        // 添加刷新按钮事件处理
        refreshButton.addActionListener(e -> refreshDeviceList());
    }
    
    /**
     * 刷新设备列表
     */
    private void refreshDeviceList() {
        // 清空当前列表（除了本地设备）
        deviceListModel.clear();
        deviceListModel.addElement(new DeviceItem("local", "本地设备", DeviceControlManager.ControlPermission.ALLOWED));
        
        // 添加发现的设备
        for (DeviceInfo deviceInfo : controller.getNetworkManager().getDiscoveredDevices()) {
            if (deviceInfo.isOnline()) {
                deviceListModel.addElement(new DeviceItem(
                    deviceInfo.getDeviceId(),
                    deviceInfo.getDeviceName() + " (" + deviceInfo.getIpAddress() + ")",
                    DeviceControlManager.ControlPermission.ALLOWED
                ));
            }
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
                deviceListModel.addElement(new DeviceItem(
                    deviceIp, 
                    "新设备 (" + deviceIp + ")", 
                    DeviceControlManager.ControlPermission.ALLOWED));
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
            String permissionStr = "";
            switch (permission) {
                case ALLOWED:
                    permissionStr = " [允许]";
                    break;
                case DELAY_5_MINUTES:
                    permissionStr = " [5分钟后提醒]";
                    break;
                case DELAY_10_MINUTES:
                    permissionStr = " [10分钟后提醒]";
                    break;
                case DELAY_30_MINUTES:
                    permissionStr = " [30分钟后提醒]";
                    break;
            }
            return displayName + permissionStr;
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
                
                // 根据权限状态设置颜色
                switch (item.getPermission()) {
                    case ALLOWED:
                        setForeground(Color.BLACK);
                        break;
                    case DELAY_5_MINUTES:
                    case DELAY_10_MINUTES:
                    case DELAY_30_MINUTES:
                        setForeground(Color.GRAY);
                        break;
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