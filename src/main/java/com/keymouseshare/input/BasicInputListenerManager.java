package com.keymouseshare.input;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基本输入监听管理器实现，用于演示和测试
 * 在实际应用中，需要针对不同平台实现具体的输入监听功能
 */
public class BasicInputListenerManager extends AbstractInputListenerManager {
    private ScheduledExecutorService scheduler;
    
    @Override
    public void startListening() {
        if (isListening) {
            logger.warn("Input listener is already running");
            return;
        }
        
        isListening = true;
        scheduler = Executors.newScheduledThreadPool(1);
        
        // 模拟鼠标移动事件
        scheduler.scheduleAtFixedRate(() -> {
            if (eventListener != null) {
                // 生成随机鼠标位置
                MouseEvent event = new MouseEvent(InputEvent.EventType.MOUSE_MOVE);
                event.setX(Math.random() * 1920);
                event.setY(Math.random() * 1080);
                eventListener.onMouseMove(event);
            }
        }, 0, 100, TimeUnit.MILLISECONDS); // 每100毫秒一次
        
        // 模拟键盘按键事件
        scheduler.scheduleAtFixedRate(() -> {
            if (eventListener != null && Math.random() > 0.95) { // 5%概率触发
                KeyEvent event = new KeyEvent(Math.random() > 0.5 ? 
                    InputEvent.EventType.KEY_PRESS : InputEvent.EventType.KEY_RELEASE);
                event.setKeyCode((int) (Math.random() * 128));
                event.setKeyChar((char) (Math.random() * 26 + 'a'));
                eventListener.onKeyPress(event);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS); // 每秒一次，延迟1秒开始
        
        logger.info("Basic input listener started");
    }
    
    @Override
    public void stopListening() {
        if (!isListening) {
            logger.warn("Input listener is not running");
            return;
        }
        
        isListening = false;
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Basic input listener stopped");
    }
}