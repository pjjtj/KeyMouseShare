package com.keymouseshare.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     */
    private void createDefaultConfig() {
        config = new DeviceConfig();
        
        // 生成唯一设备ID
        config.setDeviceId(UUID.randomUUID().toString());
        
        // 获取主机名作为设备名
        try {
            config.setDeviceName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            config.setDeviceName("UnknownDevice");
            logger.warn("Failed to get hostname, using default device name", e);
        }
        
        // 设置默认屏幕尺寸（需要根据实际屏幕尺寸调整）
        config.setScreenWidth(1920);
        config.setScreenHeight(1080);
        
        // 设置默认网络位置
        config.setNetworkX(0);
        config.setNetworkY(0);
        
        saveConfig();
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