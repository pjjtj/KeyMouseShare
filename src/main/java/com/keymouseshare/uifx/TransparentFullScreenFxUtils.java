package com.keymouseshare.uifx;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class TransparentFullScreenFxUtils {


    private static final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private static Pane root;
    private static Rectangle overlay;
    private static Cursor transparentCursor;

    // 创建透明光标（推荐 >=16x16 避免黑方块）
    public static Cursor createTransparentCursor() {
        // 创建一个1x1像素的透明图像
        int size = 1;
        WritableImage transparentImage = new WritableImage(size, size);
        PixelWriter pixelWriter = transparentImage.getPixelWriter();

        // 将唯一的像素设置为完全透明
        Color transparent = Color.rgb(0, 0, 0, 0.01); // 或者 Color.TRANSPARENT
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                pixelWriter.setColor(x, y, transparent);
            }
        }

        // 使用透明图像创建光标，热点设置为(0,0)
        return new ImageCursor(transparentImage, 0, 0);
    }

    // 绑定场景，确保光标不会被系统恢复
    public static void applyTransparentCursor(Scene scene) {
        // 设置完全透明的自定义光标
        if (transparentCursor == null) {
            transparentCursor = createTransparentCursor();
        }
        scene.setCursor(transparentCursor);
//        scene.setCursor(Cursor.NONE);

        // 确保光标不会因事件恢复
        scene.setOnMouseMoved(e -> scene.setCursor(transparentCursor));
        scene.setOnMouseEntered(e -> scene.setCursor(transparentCursor));
        scene.setOnMouseExited(e -> scene.setCursor(transparentCursor));
    }

    /**
     * 创建一个完全透明的遮罩
     */
    public static void openTransparentOverlayHiddenCursor() {
        Rectangle2D bounds = Screen.getPrimary().getBounds();

        // 初始化根面板
        root = new Pane();
        root.setPrefSize(bounds.getWidth(), bounds.getHeight());

        // 设置舞台属性
        stage.setAlwaysOnTop(true);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        // 创建完全透明的场景
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        // 确保面板背景也是透明的
        root.setBackground(null);
        root.setStyle("-fx-background-color: rgb(0,0,0,0.01);");

        applyTransparentCursor(scene);

        stage.setScene(scene);
        stage.show();

        System.out.println("Transparent overlay opened.");

        // 确保清除任何可能存在的遮罩
        overlay = null;
    }

    /**
     * 关闭窗口并恢复鼠标光标
     */
    public static void closeFullScreenAndRestoreCursor() {
        // 恢复光标为默认
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.setCursor(Cursor.DEFAULT);
        }
        // 关闭窗口
        stage.close();
        System.out.println("Full screen window close.");
    }

}
