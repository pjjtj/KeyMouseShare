package com.keymouseshare.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备列表UI组件
 */
public class DeviceListUI extends VBox {
    
    private ListView<HBox> deviceListView;
    
    private Runnable onDeviceSelected; // 设备选中回调
    private java.util.function.Consumer<String> onDeviceSelectedWithIP; // 带IP参数的设备选中回调

    public void setOnDeviceSelected(Runnable callback) {
        this.onDeviceSelected = callback;
    }
    
    public void setOnDeviceSelectedWithIP(java.util.function.Consumer<String> callback) {
        this.onDeviceSelectedWithIP = callback;
    }
    
    public DeviceListUI() {
        // 初始化界面
        initializeUI();
        
        // 加载模拟数据
        loadMockData();
    }
    
    private void initializeUI() {
        this.setPrefWidth(200);
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        
        Label titleLabel = new Label("设备列表");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        deviceListView = new ListView<>();
        deviceListView.setPrefHeight(500);

        // 添加点击事件监听器
        deviceListView.setOnMouseClicked(event -> {
            HBox selectedItem = deviceListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                // 清除之前选中项的样式
                deviceListView.getItems().forEach(item -> {
                    item.setStyle(""); // 清除自定义样式
                });

                // 为当前选中项添加高亮样式
//                selectedItem.setStyle("-fx-background-color: #dbefff;");

                // 获取选中项的IP地址
                if (!selectedItem.getChildren().isEmpty() && selectedItem.getChildren().get(1) instanceof Label) {
                    Label ipLabel = (Label) selectedItem.getChildren().get(1);
                    String ipAddress = ipLabel.getText();
                    
                    // 触发设备选中回调
                    if (onDeviceSelected != null) {
                        onDeviceSelected.run();
                    }
                    
                    // 触发带IP的设备选中回调
                    if (onDeviceSelectedWithIP != null) {
                        onDeviceSelectedWithIP.accept(ipAddress);
                    }
                }
            }
        });

        // 创建启动服务器按钮（底部）
        Button startServerButton = new Button("启动服务器");
        VBox bottomBox = new VBox();
        bottomBox.getChildren().add(startServerButton);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);
        
        this.getChildren().addAll(titleLabel, deviceListView, bottomBox);
    }
    
    private void loadMockData() {
        List<HBox> deviceItems = new ArrayList<>();
        
        // 添加模拟设备项
        deviceItems.add(createDeviceItem("IPAddr1", "C", true));   // 客户端，已连接
        deviceItems.add(createDeviceItem("IPAddr2", "S", true));   // 服务器，已连接（选中状态）
        deviceItems.add(createDeviceItem("IPAddr3", "C", true));   // 客户端，已连接
        deviceItems.add(createDeviceItem("IPAddr4", "C", false));  // 客户端，未连接
        
        deviceListView.getItems().addAll(deviceItems);
    }
    
    private HBox createDeviceItem(String ipAddress, String role, boolean connected) {
        HBox item = new HBox();
        item.setSpacing(10);
        item.setPadding(new Insets(5));
        item.setAlignment(Pos.CENTER_LEFT);
        
        // 创建状态指示器
        Circle statusIndicator = new Circle(8);
        if (connected) {
            statusIndicator.setFill(Color.GREEN);
        } else {
            statusIndicator.setFill(Color.ORANGE);
        }
        
        // 添加角色标识
        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        roleLabel.setTranslateY(-1);
        roleLabel.setTranslateX(-11);
        
        // 创建状态指示器容器
        HBox indicatorContainer = new HBox();
        indicatorContainer.setAlignment(Pos.CENTER);
        indicatorContainer.getChildren().addAll(statusIndicator, roleLabel);
        
        // 创建IP地址标签
        Label ipLabel = new Label(ipAddress);
        ipLabel.setStyle("-fx-font-size: 12px;");
        
        item.getChildren().addAll(indicatorContainer, ipLabel);
        
        // 设置容器可扩展性
        HBox.setHgrow(ipLabel, Priority.ALWAYS);
        
        return item;
    }
}