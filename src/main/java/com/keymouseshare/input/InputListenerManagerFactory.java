package com.keymouseshare.input;

import com.keymouseshare.util.OSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 输入监听管理器工厂类，根据操作系统创建相应的输入监听管理器
 */
public class InputListenerManagerFactory {
    private static final Logger logger = LoggerFactory.getLogger(InputListenerManagerFactory.class);
    
    /**
     * 创建适用于当前操作系统的输入监听管理器
     * @return 输入监听管理器
     */
    public static InputListenerManager createInputListenerManager() {
        OSUtil.OSType osType = OSUtil.getOSType();
        
        switch (osType) {
            case WINDOWS:
                logger.info("Creating Windows input listener manager");
                try {
                    // 在实际项目中使用完整的Windows实现
                    // 目前使用示例实现进行演示
                    return new WindowsInputListenerExample();
                } catch (Exception e) {
                    logger.error("Failed to create Windows input listener manager, using basic implementation", e);
                    // 如果无法创建特定平台的实现，则使用基本实现
                    return new BasicInputListenerManager();
                }
                
            case MAC:
                logger.info("Creating Mac input listener manager");
                try {
                    // 尝试创建Mac输入监听管理器
                    Class<?> macManagerClass = Class.forName("com.keymouseshare.input.MacInputListenerManager");
                    return (InputListenerManager) macManagerClass.newInstance();
                } catch (Exception e) {
                    logger.error("Failed to create Mac input listener manager, using basic implementation", e);
                    // 如果无法创建特定平台的实现，则使用基本实现
                    return new BasicInputListenerManager();
                }
                
            default:
                logger.info("Creating basic input listener manager for unknown OS");
                return new BasicInputListenerManager();
        }
    }
}