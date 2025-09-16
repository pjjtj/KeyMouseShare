package com.keymouseshare.keyboard;

import com.keymouseshare.util.SlidingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BaseMouseKeyBoard {

    private static final Logger logger = LoggerFactory.getLogger(BaseMouseKeyBoard.class);

    private Robot keyBoardRobot;
    private Robot mouseRobot;

    // 线程池用于处理键盘和鼠标事件
    private final ExecutorService keyboardExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Keyboard-Handler");
        t.setDaemon(true);
        return t;
    });

    private final ExecutorService mouseExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Mouse-Handler");
        t.setDaemon(true);
        return t;
    });

    // 组合键缓存
    SlidingCache<Integer, Integer> sessionCache = new SlidingCache<>(3000);

    public BaseMouseKeyBoard() {
        try {
            mouseRobot = new Robot();
            keyBoardRobot = new Robot();
        } catch (AWTException e) {
            logger.error("无法创建Robot实例 {}", e.getMessage());
        }
    }

    // 键盘事件提交到键盘线程处理
    public void keyPress(int keyCode) {
        keyboardExecutor.submit(() -> {
            if (keyBoardRobot != null) {
                sessionCache.put(keyCode, keyCode);
                if (sessionCache.getKeys().size() > 1) {
                    pressCombination();
                } else {
                    keyBoardRobot.keyPress(keyCode);
                    keyBoardRobot.delay(50);
                }
            }
        });
    }

    public void keyRelease(int keyCode) {
        keyboardExecutor.submit(() -> {
            if (keyBoardRobot != null) {
                keyBoardRobot.keyRelease(keyCode);
                sessionCache.remove(keyCode);
                keyBoardRobot.delay(50);
            }
        });
    }

    // 鼠标事件提交到鼠标线程处理
    public void mouseMove(int x, int y) {
        mouseExecutor.submit(() -> {
            if (mouseRobot != null) {
                mouseRobot.mouseMove(x, y);
            }
        });
    }

    public void mousePress(int button, int x, int y) {
        mouseExecutor.submit(() -> {
            if (mouseRobot != null) {
                if (!sessionCache.isEmpty()) {
                    // 注意：这里可能需要同步机制确保组合键状态一致性
                    pressCombination();
                }
                mouseRobot.mousePress(button);
                mouseRobot.delay(50);
            }
        });
    }

    public void mouseRelease(int button, int x, int y) {
        mouseExecutor.submit(() -> {
            if (mouseRobot != null) {
                mouseRobot.mouseRelease(button);
                mouseRobot.delay(50);
            }
        });
    }

    public void mouseWheel(int wheelAmount) {
        mouseExecutor.submit(() -> {
            if (mouseRobot != null) {
                mouseRobot.mouseWheel(wheelAmount);
                mouseRobot.delay(50);
            }
        });
    }

    private void pressCombination() {
        if (keyBoardRobot != null) {
            for (int keyCode : sessionCache.getValues()) {
                keyBoardRobot.keyPress(keyCode);
                keyBoardRobot.delay(50);
            }
        }
    }

    // 关闭线程池
    public void shutdown() {
        keyboardExecutor.shutdown();
        mouseExecutor.shutdown();
    }

    /**
     * 检查是否按下了Ctrl键（兼容左右Ctrl键）
     *
     * @return 如果按下了Ctrl键返回true，否则返回false
     */
    public boolean isCtrlPressed() {
        return sessionCache.get(KeyEvent.VK_CONTROL) != null  || sessionCache.get(KeyEvent.VK_META) != null;
    }

    /**
     * 检查是否按下了Alt键（兼容左右Alt键）
     *
     * @return 如果按下了Alt键返回true，否则返回false
     */
    public boolean isAltPressed() {
        return sessionCache.get(KeyEvent.VK_ALT) != null;
    }

    /**
     * 检查是否按下了Shift键（兼容左右Shift键）
     *
     * @return 如果按下了Shift键返回true，否则返回false
     */
    public boolean isShiftPressed() {
        return sessionCache.get(KeyEvent.VK_SHIFT) != null;
    }
}