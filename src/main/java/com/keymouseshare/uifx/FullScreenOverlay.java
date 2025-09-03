package com.keymouseshare.uifx;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.image.WritableImage;

/**
 * 全屏遮罩类，用于创建带遮罩效果的全屏覆盖层
 */
public class FullScreenOverlay {

    private static final Stage stage = new Stage();
    private static Pane root;
    private static Rectangle overlay;
    private static Cursor transparentCursor;

    // 创建透明光标（推荐 >=16x16 避免黑方块）
    public static Cursor createTransparentCursor() {
        WritableImage cursorImage = new WritableImage(1, 1);
        cursorImage.getPixelWriter().setColor(0, 0, Color.rgb(0,0,0,0.01));

        // 用透明图像生成光标
        return new ImageCursor(cursorImage, 0, 0);
    }

    // 绑定场景，确保光标不会被系统恢复
    public static void applyTransparentCursor(Scene scene) {
        // 设置完全透明的自定义光标
        if (transparentCursor == null) {
            transparentCursor = createTransparentCursor();
        }
        scene.setCursor(transparentCursor);

        // 确保光标不会因事件恢复
        scene.setOnMouseMoved(e -> scene.setCursor(transparentCursor));
        scene.setOnMouseEntered(e -> scene.setCursor(transparentCursor));
        scene.setOnMouseExited(e -> scene.setCursor(transparentCursor));
    }
    /**
     * 创建一个全屏遮罩窗口
     *
     * @param overlayColor 遮罩颜色
     * @param opacity      遮罩透明度 (0.0-1.0)
     */
    public static void openFullScreenOverlay(Color overlayColor, double opacity) {
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        
        // 初始化根面板
        root = new Pane();
        root.setPrefSize(bounds.getWidth(), bounds.getHeight());
        
        // 创建遮罩矩形
        overlay = new Rectangle(bounds.getWidth(), bounds.getHeight());
        overlay.setFill(overlayColor);
        overlay.setOpacity(opacity);
        
        // 将遮罩添加到根面板
        root.getChildren().add(overlay);

        // 设置舞台属性
        stage.setAlwaysOnTop(true);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        
        // 创建场景，使用透明背景
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);
        
        // 设置透明光标
        applyTransparentCursor(scene);

        stage.setScene(scene);
        stage.show();

        System.out.println("Full screen overlay opened.");
    }



    /**
     * 创建一个默认的半透明黑色遮罩
     */
    public static void openFullScreenOverlay() {
        openFullScreenOverlay(Color.BLACK, 0.5);
    }

    /**
     * 更新遮罩颜色和透明度
     *
     * @param overlayColor 遮罩颜色
     * @param opacity      遮罩透明度 (0.0-1.0)
     */
    public static void updateOverlay(Color overlayColor, double opacity) {
        // 如果当前没有遮罩，则创建一个
        if (overlay == null) {
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            overlay = new Rectangle(bounds.getWidth(), bounds.getHeight());
            overlay.setFill(overlayColor);
            overlay.setOpacity(opacity);
            root.getChildren().add(overlay);
        } else {
            overlay.setFill(overlayColor);
            overlay.setOpacity(opacity);
        }
    }

    /**
     * 关闭遮罩窗口并恢复鼠标光标
     */
    public static void closeFullScreenOverlay() {
        // 恢复光标为默认
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.setCursor(Cursor.DEFAULT);
        }
        // 关闭窗口
        stage.close();
        System.out.println("Full screen overlay closed.");
    }

    /**
     * 检查遮罩窗口是否正在显示
     *
     * @return 如果遮罩窗口正在显示则返回true，否则返回false
     */
    public static boolean isShowing() {
        return stage.isShowing();
    }
}