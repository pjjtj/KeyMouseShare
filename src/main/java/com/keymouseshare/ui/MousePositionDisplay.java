package com.keymouseshare.ui;

import com.keymouseshare.util.JNAMousePosition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import java.awt.*;
import java.util.List;

/**
 * 鼠标位置显示器
 * 用于实时显示鼠标在屏幕中的位置
 */
public class MousePositionDisplay extends HBox {
    
    private Label xPositionLabel;
    private Label yPositionLabel;
    private Label screenPositionLabel;
    
    public MousePositionDisplay() {
        initializeUI();
    }
    
    private void initializeUI() {
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(10);
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setStyle("-fx-border-color: lightgray; -fx-border-width: 1 0 0 0; -fx-border-radius: 0;");
        
        Label mouseLabel = new Label("鼠标位置:");
        mouseLabel.setFont(Font.font(12));
        
        xPositionLabel = new Label("X: 0");
        xPositionLabel.setFont(Font.font(12));
        xPositionLabel.setStyle("-fx-text-fill: blue;");
        
        yPositionLabel = new Label("Y: 0");
        yPositionLabel.setFont(Font.font(12));
        yPositionLabel.setStyle("-fx-text-fill: blue;");
        
        screenPositionLabel = new Label("(屏幕坐标)");
        screenPositionLabel.setFont(Font.font(12));
        screenPositionLabel.setStyle("-fx-text-fill: gray;");
        
        this.getChildren().addAll(mouseLabel, xPositionLabel, yPositionLabel, screenPositionLabel);
    }
    
    /**
     * 更新鼠标位置显示
     * @param x X坐标
     * @param y Y坐标
     */
    public void updateMousePosition(double x, double y) {
        xPositionLabel.setText("X: " + String.format("%.0f", x));
        yPositionLabel.setText("Y: " + String.format("%.0f", y));
        
        // 使用JavaFX获取更准确的鼠标位置和屏幕信息
        updateScreenInfo(x, y);
    }
    
    /**
     * 使用JavaFX获取并更新屏幕信息
     */
    public void updateScreenInfo(double x, double y) {
        // 获取所有屏幕信息
        List<javafx.stage.Screen> screens = javafx.stage.Screen.getScreens();
        javafx.stage.Screen targetScreen = null;
        int targetScreenIndex = 0;
        
        // 查找鼠标所在的屏幕
        for (int i = 0; i < screens.size(); i++) {
            javafx.stage.Screen screen = screens.get(i);
            Rectangle2D bounds = screen.getBounds();
            // 检查鼠标位置是否在屏幕边界内（包含边界）
            if (x >= bounds.getMinX() && x <= bounds.getMaxX() && 
                y >= bounds.getMinY() && y <= bounds.getMaxY()) {
                targetScreen = screen;
                targetScreenIndex = i;
                break;
            }
        }
        
        // 如果没有找到特定屏幕，则使用主屏幕
        if (targetScreen == null) {
            targetScreen = javafx.stage.Screen.getPrimary();
            targetScreenIndex = screens.indexOf(targetScreen);
        }
        
        Rectangle2D bounds = targetScreen.getBounds();
        Rectangle2D visualBounds = targetScreen.getVisualBounds();
        
        // 计算相对于屏幕的位置
        double screenX = x - bounds.getMinX();
        double screenY = y - bounds.getMinY();
        
        // 尝试获取真实的屏幕分辨率
        String resolutionInfo = getRealScreenResolution(targetScreenIndex);
        
        // 显示更详细的信息，包括屏幕分辨率和完整坐标空间信息
        screenPositionLabel.setText(String.format("(屏幕%d: %.0f, %.0f of %s)", 
            targetScreenIndex + 1, screenX, screenY, resolutionInfo));
    }
    
    /**
     * 获取真实的屏幕分辨率信息
     * @param screenIndex 屏幕索引
     * @return 分辨率信息字符串
     */
    private String getRealScreenResolution(int screenIndex) {
        try {
            // 使用Toolkit获取真实的屏幕分辨率
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            
            if (screenIndex < screens.length) {
                DisplayMode dm = screens[screenIndex].getDisplayMode();
                int width = dm.getWidth();
                int height = dm.getHeight();
                return width + "x" + height;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 如果无法获取准确信息，回退到使用JavaFX的信息
        try {
            List<javafx.stage.Screen> fxScreens = javafx.stage.Screen.getScreens();
            if (screenIndex < fxScreens.size()) {
                javafx.stage.Screen fxScreen = fxScreens.get(screenIndex);
                Rectangle2D visualBounds = fxScreen.getVisualBounds();
                return (int)visualBounds.getWidth() + "x" + (int)visualBounds.getHeight();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "未知分辨率";
    }
    
    /**
     * 更新鼠标位置显示（带屏幕名称）
     * @param x X坐标
     * @param y Y坐标
     * @param screenName 屏幕名称
     */
    public void updateMousePosition(double x, double y, String screenName) {
        xPositionLabel.setText("X: " + String.format("%.0f", x));
        yPositionLabel.setText("Y: " + String.format("%.0f", y));
        screenPositionLabel.setText(String.format("(屏幕: %s)", screenName));
    }
}