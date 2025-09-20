package com.keymouseshare.uifx;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;

/**
 * 自定义标题栏UI组件
 * 提供窗口的移动、最小化、最大化和关闭功能
 */
public class TitleBarUI extends HBox {
    private Stage primaryStage;
    private Label titleLabel;
    private Button minimizeButton;
    private Button maximizeButton;
    private Button closeButton;
    
    // 窗口状态跟踪
    private boolean isMaximized = false;
    private double xOffset = 0;
    private double yOffset = 0;
    
    // 窗口原始位置和尺寸
    private double originalX;
    private double originalY;
    private double originalWidth;
    private double originalHeight;

    public TitleBarUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initializeUI();
    }

    private void initializeUI() {
        this.setPrefHeight(50);
        // 添加边框线和圆角效果
        this.setStyle("-fx-background-color: #2b2b2b; " +
                     "-fx-border-color: #2b2b2b; " +
                     "-fx-border-width: 0 0 0 0; " +
                     "-fx-background-radius: 8 8 0 0; " +
                     "-fx-border-radius: 8 8 0 0;");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(0, 10, 0, 10));
        
        // 标题标签
        titleLabel = new Label("KeyMouseShare - 键盘鼠标共享工具");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        // 创建控制按钮区域
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // 最小化按钮
        minimizeButton = createControlButton("-");
        minimizeButton.setOnAction(e -> primaryStage.setIconified(true));
        
        // 最大化按钮
        maximizeButton = createControlButton("□");
        maximizeButton.setOnAction(e -> toggleMaximize());
        
        // 关闭按钮
        closeButton = createControlButton("×");
        closeButton.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-font-size: 16px;");
        closeButton.setOnAction(e -> Platform.runLater(() -> {
            primaryStage.close();
        }));
        
        // 鼠标事件处理 - 实现窗口拖拽移动
        this.setOnMousePressed(this::handleMousePressed);
        this.setOnMouseDragged(this::handleMouseDragged);
        
        // 添加所有组件
        this.getChildren().addAll(titleLabel, spacer, minimizeButton, maximizeButton, closeButton);
    }
    
    /**
     * 创建控制按钮
     */
    private Button createControlButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(32, 32);
        button.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-font-size: 14px; " +
                       "-fx-background-radius: 4;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-font-size: 14px; " +
                                                     "-fx-background-radius: 4;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-font-size: 14px; " +
                                                    "-fx-background-radius: 4;"));
        return button;
    }
    
    /**
     * 处理鼠标按下事件
     */
    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }
    
    /**
     * 处理鼠标拖拽事件
     */
    private void handleMouseDragged(MouseEvent event) {
        // 只有在非最大化状态下才能拖拽窗口
        if (!isMaximized) {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        }
    }
    
    /**
     * 切换窗口最大化状态
     */
    private void toggleMaximize() {
        if (isMaximized) {
            // 恢复窗口到原始状态
            primaryStage.setX(originalX);
            primaryStage.setY(originalY);
            primaryStage.setWidth(originalWidth);
            primaryStage.setHeight(originalHeight);
            isMaximized = false;
            maximizeButton.setText("□");
            // 恢复圆角效果
            this.setStyle("-fx-background-color: #2b2b2b; " +
                         "-fx-border-color: #444444; " +
                         "-fx-border-width: 0 0 1 0; " +
                         "-fx-background-radius: 8 8 0 0; " +
                         "-fx-border-radius: 8 8 0 0;");
        } else {
            // 保存当前窗口状态
            originalX = primaryStage.getX();
            originalY = primaryStage.getY();
            originalWidth = primaryStage.getWidth();
            originalHeight = primaryStage.getHeight();
            
            // 最大化窗口
            primaryStage.setX(0);
            primaryStage.setY(0);
            primaryStage.setWidth(javafx.stage.Screen.getPrimary().getBounds().getWidth());
            primaryStage.setHeight(javafx.stage.Screen.getPrimary().getBounds().getHeight());
            isMaximized = true;
            maximizeButton.setText("❐");
            // 最大化时移除圆角效果
            this.setStyle("-fx-background-color: #2b2b2b; " +
                         "-fx-border-color: #444444; " +
                         "-fx-border-width: 0 0 1 0;");
        }
    }
    
    /**
     * 更新标题文本
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}