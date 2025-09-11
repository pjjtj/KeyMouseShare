package com.keymouseshare.network;

import com.keymouseshare.bean.*;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 控制请求管理器
 * 处理权限控制与连接建立流程
 */
public class ControlRequestManager {
    private static final Logger logger = LoggerFactory.getLogger(ControlRequestManager.class);

    private ControlServer controlServer;
    private ControlClient controlClient;
    private boolean isServerMode = false;
    private Window parentWindow;
    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();


    public ControlRequestManager() {

    }

    /**
     * 设置父窗口，用于显示权限对话框
     *
     * @param window 父窗口
     */
    public void setParentWindow(Window window) {
        this.parentWindow = window;
    }

    /**
     * 设置服务器模式
     *
     * @param isServer 是否为服务器模式
     */
    public void setServerMode(boolean isServer) {
        this.isServerMode = isServer;

        if (isServer) {
            // 启动Netty服务端
            startServer();
        } else {
            // 停止Netty服务端
            stopServer();
        }
    }

    /**
     * 启动Netty服务端
     */
    private void startServer() {
        try {
            if (controlServer == null) {
                controlServer = new ControlServer();
            }

            // 在新线程中启动服务端，避免阻塞UI线程
            new Thread(() -> {
                try {
                    controlServer.start(8889); // 使用8889端口进行控制连接
                    logger.info("控制服务端已启动，端口: 8889");
                } catch (Exception e) {
                    logger.error("启动控制服务端失败: {}", e.getMessage());
                }
            }).start();

            deviceStorage.getLocalDevice().getScreens().forEach(virtualDesktopStorage::addScreen);

        } catch (Exception e) {
            logger.error("创建控制服务端失败: {}", e.getMessage());
        }
    }

    /**
     * 停止Netty服务端
     */
    private void stopServer() {
        if (controlServer != null) {
            try {
                controlServer.stop();
                logger.info("控制服务端已停止");
            } catch (Exception e) {
                logger.error("停止控制服务端失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 显示权限对话框
     *
     * @param requesterIp 请求方IP
     * @return 是否授权
     */
    public CompletableFuture<Boolean> showPermissionDialog(String requesterIp) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // 更新目标设备状态为PENDING_AUTHORIZATION
        DeviceStorage.getInstance().getLocalDevice().setConnectionStatus(ConnectType.PENDING_AUTHORIZATION.name());
        // TODO 是否要即刻通知

        // 创建权限对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("控制请求");
        alert.setHeaderText("设备控制请求");
        alert.setContentText("设备 (" + requesterIp + ") 请求控制您的计算机，是否允许？");

        // 设置对话框属性
        if (parentWindow != null) {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.initOwner(parentWindow);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            alert.initModality(Modality.APPLICATION_MODAL);
        }

        // 显示对话框并等待用户响应
        alert.showAndWait();

        // 返回用户选择结果
        future.complete(alert.getResult() == ButtonType.OK);
        alert.close();
        return future;
    }

    /**
     * 建立TCP连接
     *
     * @param targetDeviceIp 目标设备IP
     * @throws Exception 连接异常
     */
    public void establishConnection(String targetDeviceIp) throws Exception {
        if (controlClient == null) {
            controlClient = new ControlClient();
        }

        // 连接到目标设备的控制服务端
        controlClient.connect(targetDeviceIp, 8889);
        logger.info("已连接到设备: " + targetDeviceIp);

        // 更新目标设备状态为CONNECTED
        DeviceStorage.getInstance().getLocalDevice().setConnectionStatus(ConnectType.CONNECTED.name());
        // TODO 是否要即刻通知

    }

    /**
     * 拒绝来自指定IP地址的控制连接请求
     * 
     * @param requesterIpAddress 请求控制连接的设备IP地址
     */
    public void rejectConnection(String requesterIpAddress) {
        DeviceStorage.getInstance().getLocalDevice().setConnectionStatus(ConnectType.DISCONNECTED.name());
        // TODO 是否要即刻通知
    }

    /**
     * netty 服务器发送消息给客户端
     *
     * @param event 控制事件
     */
    public void sendControlRequest( ControlEvent event) {
        if (controlServer != null) {
            controlServer.sendControlEvent(event);
            logger.info("已发送控制请求到客户端: {}, 事件类型: {}, 数据: ({},{})", event.getDeviceIp(), event.getType(), event.getX(), event.getY());
        } else {
            logger.error("控制服务器未启动，无法发送控制请求到客户端: {}", event.getDeviceIp());
        }
    }

    /**
     * 断开TCP连接
     */
    public void disconnect() {
        if (controlClient != null) {
            try {
                controlClient.disconnect();
                logger.info("控制客户端已断开连接");
            } catch (Exception e) {
                logger.error("断开控制客户端连接失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查是否为服务器模式
     *
     * @return 是否为服务器模式
     */
    public boolean isServerMode() {
        return isServerMode;
    }


}