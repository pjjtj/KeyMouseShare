package com.keymouseshare.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象输入监听管理器基类
 */
public abstract class AbstractInputListenerManager implements InputListenerManager {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractInputListenerManager.class);
    
    protected InputListener eventListener;
    protected boolean isListening = false;
    
    @Override
    public void setEventListener(InputListener listener) {
        this.eventListener = listener;
    }
    
    @Override
    public boolean isListening() {
        return isListening;
    }
}