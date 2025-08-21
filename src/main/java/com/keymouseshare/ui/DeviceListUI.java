package com.keymouseshare.ui;

import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.network.DeviceDiscovery;
import com.keymouseshare.network.ControlRequestManager;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 设备列表UI组件
 */
public class DeviceListUI extends VBox {
    private static final Logger logger = Logger.getLogger(DeviceListUI.class.getName());
    
    private ListView<HBox> deviceListView;
    private DeviceDiscovery deviceDiscovery;
    private ControlRequestManager controlRequestManager;
    private Map<String, DeviceInfo> deviceInfoMap = new ConcurrentHashMap<>();
    
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
    }
    
    public DeviceListUI(DeviceDiscovery deviceDiscovery) {
        this.deviceDiscovery = deviceDiscovery;
        // 初始化界面
        initializeUI();
        // 初始化设备列表
        initializeDeviceList();
    }
    
    /**
     * 设置设备发现服务
     * @param deviceDiscovery 设备发现服务
     */
    public void setDeviceDiscovery(DeviceDiscovery deviceDiscovery) {
        this.deviceDiscovery = deviceDiscovery;
        // 初始化设备列表
        initializeDeviceList();
    }
    
    /**
     * 设置控制请求管理器
     * @param controlRequestManager 控制请求管理器
     */
    public void setControlRequestManager(ControlRequestManager controlRequestManager) {
        this.controlRequestManager = controlRequestManager;
    }
    
    /**
     * 获取控制请求管理器
     * @return controlRequestManager 控制请求管理器
     */
    public ControlRequestManager getControlRequestManager() {
        return controlRequestManager;
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
                    String text = ipLabel.getText();
                    String ipAddress = text.contains(" (本地)") ? text.replace(" (本地)", "") : text; // 移除本地标识
                    
                    // 触发设备选中回调
                    if (onDeviceSelected != null) {
                        onDeviceSelected.run();
                    }
                    
                    // 触发带IP的设备选中回调
                    if (onDeviceSelectedWithIP != null) {
                        onDeviceSelectedWithIP.accept(ipAddress);
                    }
                    
                    // 如果是鼠标右键点击，则发起控制请求
                    if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        // 发起控制请求
                        if (controlRequestManager != null && deviceDiscovery != null) {
                            DeviceInfo localDevice = deviceDiscovery.getLocalDevice();
                            if (localDevice != null && !localDevice.getIpAddress().equals(ipAddress)) {
                                requestControl(ipAddress);
                            }
                        }
                    }
                    // 如果是鼠标左键点击，则选中设备（原有逻辑）
                    else if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                        // 如果选中的不是本地设备，则尝试发起控制请求
                        if (controlRequestManager != null && deviceDiscovery != null) {
                            DeviceInfo localDevice = deviceDiscovery.getLocalDevice();
                            if (localDevice != null && !localDevice.getIpAddress().equals(ipAddress)) {
                                // 发起控制请求
                                requestControl(ipAddress);
                            }
                        }
                    }
                }
            }
        });

        // 创建启动服务器按钮（底部）
        Button startServerButton = new Button("启动服务器");
        startServerButton.setOnAction(event -> {
            if (controlRequestManager != null) {
                boolean isCurrentlyServer = controlRequestManager.isServerMode();
                controlRequestManager.setServerMode(!isCurrentlyServer);
                startServerButton.setText(isCurrentlyServer ? "启动服务器" : "停止服务器");
                deviceDiscovery.getLocalDevice().setDeviceType("S");
                deviceDiscovery.getLocalDevice().setConnectionStatus("CONNECTED");
                updateLocalDevice();
                deviceDiscovery.notifyDeviceUpdate(deviceDiscovery.getLocalDevice());
            }
        });
        
        VBox bottomBox = new VBox();
        bottomBox.getChildren().add(startServerButton);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);
        
        this.getChildren().addAll(titleLabel, deviceListView, bottomBox);
    }
    
    /**
     * 发起控制请求
     * @param targetDeviceIp 目标设备IP
     */
    private void requestControl(String targetDeviceIp) {
        if (controlRequestManager != null) {
            // 异步发送控制请求
            controlRequestManager.sendControlRequest(targetDeviceIp)
                .thenAccept(permissionGranted -> {
                    if (permissionGranted) {
                        // 权限已授予，建立TCP连接
                        try {
                            controlRequestManager.establishConnection(targetDeviceIp);
                            logger.info("已成功连接到设备: " + targetDeviceIp);
                        } catch (Exception e) {
                            logger.severe("建立连接失败: " + e.getMessage());
                        }
                    } else {
                        logger.info("控制请求被拒绝: " + targetDeviceIp);
                    }
                })
                .exceptionally(throwable -> {
                    logger.severe("发送控制请求时发生错误: " + throwable.getMessage());
                    return null;
                });
        }
    }
    
    /**
     * 初始化设备列表
     */
    private void initializeDeviceList() {
        // 添加本地设备到列表顶部
        if (deviceDiscovery != null) {
            DeviceInfo localDevice = deviceDiscovery.getLocalDevice();
            if (localDevice != null) {
                HBox localDeviceItem = createDeviceItem(localDevice.getIpAddress(), localDevice.getDeviceType(), localDevice.getConnectionStatus(), true);
                deviceListView.getItems().add(0, localDeviceItem); // 添加到列表顶部
                deviceInfoMap.put(localDevice.getIpAddress(), localDevice);
            }
        }
    }
    
    /**
     * 更新设备列表
     * @param devices 设备列表
     */
    public void updateDeviceList(List<DeviceInfo> devices) {
        // 清空除本地设备外的所有设备
        if (!deviceListView.getItems().isEmpty()) {
            // 保留第一个项目（本地设备）
            HBox localDeviceItem = deviceListView.getItems().get(0);
            deviceListView.getItems().clear();
            deviceListView.getItems().add(localDeviceItem);
        } else {
            deviceListView.getItems().clear();
        }
        
        // 添加所有发现的设备（除了本地设备）
        if (deviceDiscovery != null) {
            DeviceInfo localDevice = deviceDiscovery.getLocalDevice();
            String localIpAddress = localDevice != null ? localDevice.getIpAddress() : null;
            
            for (DeviceInfo device : devices) {
                String ipAddress = device.getIpAddress();
                // 跳过本地设备，因为我们已经在列表顶部添加了它
                if (localIpAddress != null && localIpAddress.equals(ipAddress)) {
                    continue;
                }
                
                // 创建设备项并添加到列表
                HBox deviceItem = createDeviceItem(ipAddress, device.getDeviceType(), device.getConnectionStatus(), false);
                deviceListView.getItems().add(deviceItem);
                deviceInfoMap.put(ipAddress, device);
            }
        }
    }
    
    private HBox createDeviceItem(String ipAddress, String role, String connectStatus, boolean isLocal) {
        HBox item = new HBox();
        item.setSpacing(10);
        item.setPadding(new Insets(5));
        item.setAlignment(Pos.CENTER_LEFT);
        
        // 创建状态指示器
        Circle statusIndicator = new Circle(8);
        if (connectStatus.equals("CONNECTED")) {
            statusIndicator.setFill(Color.GREEN);
        } else if(connectStatus.equals("DISCONNECTED")){
            statusIndicator.setFill(Color.GRAY);
        } else if(connectStatus.equals("PENDING_AUTHORIZATION")){
            statusIndicator.setFill(Color.ORANGE);
        } else {
            // 默认状态
            statusIndicator.setFill(Color.GRAY);
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
        Label ipLabel = new Label(ipAddress + (isLocal ? " (本地)" : ""));
        ipLabel.setStyle("-fx-font-size: 12px;");
        
        item.getChildren().addAll(indicatorContainer, ipLabel);
        
        // 设置容器可扩展性
        HBox.setHgrow(ipLabel, Priority.ALWAYS);
        
        return item;
    }

    /**
     * 更新本地设备显示
     */
    public void updateLocalDevice() {
        if (deviceDiscovery != null) {
            DeviceInfo localDevice = deviceDiscovery.getLocalDevice();
            if (localDevice != null) {
                // 更新本地设备项或创建新的本地设备项
                HBox localDeviceItem = createDeviceItem(localDevice.getIpAddress(), localDevice.getDeviceType(), localDevice.getConnectionStatus(), true);
                
                // 如果列表为空或第一个项目不是本地设备，则添加本地设备到顶部
                if (deviceListView.getItems().isEmpty() || 
                    deviceListView.getItems().size() == 0 ||
                    !isLocalDeviceItem(deviceListView.getItems().get(0))) {
                    deviceListView.getItems().add(0, localDeviceItem);
                } else {
                    // 替换现有的本地设备项
                    deviceListView.getItems().set(0, localDeviceItem);
                }
                
                deviceInfoMap.put(localDevice.getIpAddress(), localDevice);
            }
        }
    }
    
    /**
     * 检查HBox是否为本地设备项
     */
    private boolean isLocalDeviceItem(HBox item) {
        if (item != null && !item.getChildren().isEmpty() && item.getChildren().size() > 1) {
            if (item.getChildren().get(1) instanceof Label) {
                Label label = (Label) item.getChildren().get(1);
                return label.getText().contains(" (本地)");
            }
        }
        return false;
    }
}