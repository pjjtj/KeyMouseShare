package com.keymouseshare.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.DisplayMode;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * 配置管理器，用于管理设备配置信息
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private DeviceConfig config;
    
    public ConfigManager() {
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, DeviceConfig.class);
                logger.info("Configuration loaded from {}", CONFIG_FILE);
            } catch (IOException e) {
                logger.error("Failed to load configuration from file", e);
                createDefaultConfig();
            }
        } else {
            createDefaultConfig();
        }
    }
    
    /**
     * 创建默认配置
     * @return 默认设备配置
     */
    private DeviceConfig createDefaultConfig() {
        DeviceConfig config = new DeviceConfig();
        config.setDeviceId("device-" + System.currentTimeMillis());
        config.setDeviceName("DefaultDevice");
        
        // 获取实际屏幕分辨率，如果获取不到则使用默认值1920x1080
        int[] screenSize = getScreenSize();
        config.setScreenWidth(screenSize[0]);
        config.setScreenHeight(screenSize[1]);
        
        logger.info("Created default config with screen resolution: {}x{}", 
            config.getScreenWidth(), config.getScreenHeight());
        
        return config;
    }
    
    /**
     * 获取实际屏幕分辨率
     * @return 包含宽度和高度的数组，如果获取失败则返回默认值1920x1080
     */
    private int[] getScreenSize() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            
            // 遍历所有屏幕设备，找到主屏幕或选择最大屏幕
            GraphicsDevice primaryDevice = ge.getDefaultScreenDevice();
            DisplayMode dm = primaryDevice.getDisplayMode();
            
            int width = dm.getWidth();
            int height = dm.getHeight();
            
            // 确保获取到的尺寸是有效的
            if (width > 0 && height > 0) {
                logger.info("Primary screen size detected: {}x{}", width, height);
                return new int[]{width, height};
            }
            
            // 如果主屏幕无效，尝试其他屏幕
            for (GraphicsDevice screen : screens) {
                dm = screen.getDisplayMode();
                width = dm.getWidth();
                height = dm.getHeight();
                
                if (width > 0 && height > 0) {
                    logger.info("Screen size detected from device {}: {}x{}", 
                        screen.getIDstring(), width, height);
                    return new int[]{width, height};
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get screen size, using default 1920x1080", e);
        }
        
        // 默认返回1920x1080
        logger.info("Using default screen size: 1920x1080");
        return new int[]{1920, 1080};
    }
    
    /**
     * 保存配置到文件
     */
    public void saveConfig() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(config, writer);
            logger.info("Configuration saved to {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Failed to save configuration to file", e);
        }
    }
    
    /**
     * 获取当前设备配置
     * @return 设备配置
     */
    public DeviceConfig getConfig() {
        return config;
    }
    
    /**
     * 更新设备配置
     * @param config 新的设备配置
     */
    public void updateConfig(DeviceConfig config) {
        this.config = config;
        saveConfig();
    }
}