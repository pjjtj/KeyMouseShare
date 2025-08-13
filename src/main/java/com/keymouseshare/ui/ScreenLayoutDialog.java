package com.keymouseshare.ui;

import com.keymouseshare.screen.DeviceScreen;
import com.keymouseshare.screen.ScreenLayoutConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 屏幕布局配置对话框
 */
public class ScreenLayoutDialog extends JDialog {
    private ScreenLayoutPanel screenLayoutPanel;
    private ScreenLayoutConfig layoutConfig;
    private JTextField deviceNameField;
    private JTextField screenWidthField;
    private JTextField screenHeightField;
    private JTextField positionXField;
    private JTextField positionYField;
    private JButton addButton;
    private JButton updateButton;
    private JButton removeButton;
    private JButton closeButton;
    private DeviceScreen selectedScreen;
    
    public ScreenLayoutDialog(Frame parent, ScreenLayoutConfig layoutConfig) {
        super(parent, "屏幕布局配置", true);
        this.layoutConfig = layoutConfig;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        // 设置选中屏幕变化监听器
        screenLayoutPanel.addPropertyChangeListener("selectedScreen", evt -> {
            selectedScreen = (DeviceScreen) evt.getNewValue();
            updateScreenDetails();
        });
        
        // 初始化时刷新显示
        refreshDisplay();
        
        pack();
        setResizable(true);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        screenLayoutPanel = new ScreenLayoutPanel(layoutConfig);
        
        deviceNameField = new JTextField(15);
        screenWidthField = new JTextField(10);
        screenHeightField = new JTextField(10);
        positionXField = new JTextField(10);
        positionYField = new JTextField(10);
        
        addButton = new JButton("添加屏幕");
        updateButton = new JButton("更新屏幕");
        updateButton.setEnabled(false);
        removeButton = new JButton("移除屏幕");
        removeButton.setEnabled(false);
        closeButton = new JButton("关闭");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 屏幕布局面板
        JPanel layoutPanel = new JPanel(new BorderLayout());
        layoutPanel.setBorder(BorderFactory.createTitledBorder("屏幕布局"));
        layoutPanel.add(new JScrollPane(screenLayoutPanel), BorderLayout.CENTER);
        add(layoutPanel, BorderLayout.CENTER);
        
        // 屏幕详细信息面板
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("屏幕详细信息"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 设备名称
        gbc.gridx = 0; gbc.gridy = 0;
        detailsPanel.add(new JLabel("设备名称:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(deviceNameField, gbc);
        
        // 屏幕宽度
        gbc.gridx = 0; gbc.gridy = 1;
        detailsPanel.add(new JLabel("屏幕宽度:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(screenWidthField, gbc);
        
        // 屏幕高度
        gbc.gridx = 0; gbc.gridy = 2;
        detailsPanel.add(new JLabel("屏幕高度:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(screenHeightField, gbc);
        
        // X坐标
        gbc.gridx = 0; gbc.gridy = 3;
        detailsPanel.add(new JLabel("X坐标:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(positionXField, gbc);
        
        // Y坐标
        gbc.gridx = 0; gbc.gridy = 4;
        detailsPanel.add(new JLabel("Y坐标:"), gbc);
        gbc.gridx = 1;
        detailsPanel.add(positionYField, gbc);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(closeButton);
        
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        detailsPanel.add(buttonPanel, gbc);
        
        add(detailsPanel, BorderLayout.EAST);
    }
    
    private void setupEventHandlers() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addScreen();
            }
        });
        
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateScreen();
            }
        });
        
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeScreen();
            }
        });
        
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
    
    private void addScreen() {
        try {
            String deviceId = java.util.UUID.randomUUID().toString();
            String deviceName = deviceNameField.getText().trim();
            int width = Integer.parseInt(screenWidthField.getText().trim());
            int height = Integer.parseInt(screenHeightField.getText().trim());
            int x = Integer.parseInt(positionXField.getText().trim());
            int y = Integer.parseInt(positionYField.getText().trim());
            
            if (deviceName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "设备名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (width <= 0 || height <= 0) {
                JOptionPane.showMessageDialog(this, "屏幕宽度和高度必须大于0", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            DeviceScreen screen = new DeviceScreen(deviceId, deviceName, width, height, x, y);
            layoutConfig.addScreen(screen);
            refreshDisplay();
            
            clearScreenDetails();
            
            JOptionPane.showMessageDialog(this, "屏幕添加成功", "信息", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateScreen() {
        if (selectedScreen == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个屏幕", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String deviceName = deviceNameField.getText().trim();
            int width = Integer.parseInt(screenWidthField.getText().trim());
            int height = Integer.parseInt(screenHeightField.getText().trim());
            int x = Integer.parseInt(positionXField.getText().trim());
            int y = Integer.parseInt(positionYField.getText().trim());
            
            if (deviceName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "设备名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (width <= 0 || height <= 0) {
                JOptionPane.showMessageDialog(this, "屏幕宽度和高度必须大于0", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            selectedScreen.setDeviceName(deviceName);
            selectedScreen.setWidth(width);
            selectedScreen.setHeight(height);
            selectedScreen.setX(x);
            selectedScreen.setY(y);
            
            layoutConfig.updateScreen(selectedScreen);
            refreshDisplay();
            
            JOptionPane.showMessageDialog(this, "屏幕更新成功", "信息", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void removeScreen() {
        if (selectedScreen == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个屏幕", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要移除屏幕 \"" + selectedScreen.getDeviceName() + "\" 吗？", 
            "确认", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            layoutConfig.removeScreen(selectedScreen.getDeviceId());
            refreshDisplay();
            clearScreenDetails();
            selectedScreen = null;
            updateButton.setEnabled(false);
            removeButton.setEnabled(false);
            
            JOptionPane.showMessageDialog(this, "屏幕移除成功", "信息", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void updateScreenDetails() {
        if (selectedScreen != null) {
            deviceNameField.setText(selectedScreen.getDeviceName());
            screenWidthField.setText(String.valueOf(selectedScreen.getWidth()));
            screenHeightField.setText(String.valueOf(selectedScreen.getHeight()));
            positionXField.setText(String.valueOf(selectedScreen.getX()));
            positionYField.setText(String.valueOf(selectedScreen.getY()));
            
            updateButton.setEnabled(true);
            removeButton.setEnabled(true);
        } else {
            clearScreenDetails();
            updateButton.setEnabled(false);
            removeButton.setEnabled(false);
        }
    }
    
    private void clearScreenDetails() {
        deviceNameField.setText("");
        screenWidthField.setText("");
        screenHeightField.setText("");
        positionXField.setText("");
        positionYField.setText("");
    }
    
    /**
     * 刷新显示
     */
    private void refreshDisplay() {
        screenLayoutPanel.refresh();
    }
    
    public ScreenLayoutConfig getLayoutConfig() {
        return layoutConfig;
    }
}