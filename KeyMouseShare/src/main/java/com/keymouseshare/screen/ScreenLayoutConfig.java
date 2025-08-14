    /**
     * 根据设备配置创建设备屏幕对象
     * @param deviceConfig 设备配置
     * @return 设备屏幕对象
     */
    public static DeviceScreen createFromDeviceConfig(DeviceConfig deviceConfig) {
        DeviceScreen screen = new DeviceScreen();
        screen.setDeviceId(deviceConfig.getDeviceId());
        screen.setDeviceName(deviceConfig.getDeviceName());
        
        // 使用实际屏幕分辨率，如果获取不到则使用默认值1920x1080
        int screenWidth = deviceConfig.getScreenWidth();
        int screenHeight = deviceConfig.getScreenHeight();
        
        // 如果配置中的屏幕尺寸无效，则尝试重新获取实际屏幕分辨率
        if (screenWidth <= 0 || screenHeight <= 0) {
            try {
                java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                java.awt.GraphicsDevice[] screens = ge.getScreenDevices();
                
                // 遍历所有屏幕设备，找到主屏幕或选择最大屏幕
                java.awt.GraphicsDevice primaryDevice = ge.getDefaultScreenDevice();
                java.awt.DisplayMode dm = primaryDevice.getDisplayMode();
                
                screenWidth = dm.getWidth();
                screenHeight = dm.getHeight();
                
                // 确保获取到的尺寸是有效的
                if (screenWidth <= 0 || screenHeight <= 0) {
                    // 如果主屏幕无效，尝试其他屏幕
                    for (java.awt.GraphicsDevice screenDevice : screens) {
                        dm = screenDevice.getDisplayMode();
                        screenWidth = dm.getWidth();
                        screenHeight = dm.getHeight();
                        
                        if (screenWidth > 0 && screenHeight > 0) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // 出现异常时使用默认值
                screenWidth = 1920;
                screenHeight = 1080;
            }
            
            // 如果仍然无效，使用默认值
            if (screenWidth <= 0 || screenHeight <= 0) {
                screenWidth = 1920;
                screenHeight = 1080;
            }
        }
        
        screen.setWidth(screenWidth);
        screen.setHeight(screenHeight);
        
        screen.setX(deviceConfig.getNetworkX());
        screen.setY(deviceConfig.getNetworkY());
        return screen;
    }