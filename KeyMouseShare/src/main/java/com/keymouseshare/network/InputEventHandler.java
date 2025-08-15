    @Override
    public void channelRead0(ChannelHandlerContext ctx, DataPacket msg) throws Exception {
        String type = msg.getType();
        String deviceId = msg.getDeviceId();
        String remoteAddress = ctx.channel().remoteAddress().toString();
        
        logger.debug("Received message type: {} from device: {} ({})", type, deviceId, remoteAddress);
        
        if ("CONNECT".equals(type)) {
            // 处理客户端连接请求
            logger.info("Client connected: {} ({})", deviceId, remoteAddress);
            
            // 将客户端通道添加到网络管理器
            if (controller.getNetworkManager() != null) {
                controller.getNetworkManager().addClientChannel(deviceId, ctx.channel());
                logger.debug("Client channel added to NetworkManager: {}", deviceId);
            }
            
            // 通知控制器有客户端连接
            DeviceConfig.Device device = new DeviceConfig.Device();
            device.setDeviceId(deviceId);
            device.setDeviceName(msg.getData()); // 使用客户端发送的设备名称
            device.setIpAddress(remoteAddress);
            
            // 获取客户端发送的屏幕分辨率信息
            String screenData = msg.getExtraData();
            if (screenData != null && !screenData.isEmpty()) {
                try {
                    String[] parts = screenData.split("x");
                    if (parts.length == 2) {
                        int width = Integer.parseInt(parts[0]);
                        int height = Integer.parseInt(parts[1]);
                        device.setScreenWidth(width);
                        device.setScreenHeight(height);
                        logger.info("Client screen resolution: {}x{}", width, height);
                    } else {
                        // 如果解析失败，使用默认值
                        device.setScreenWidth(1920);
                        device.setScreenHeight(1080);
                        logger.warn("Failed to parse client screen data: {}, using default resolution", screenData);
                    }
                } catch (NumberFormatException e) {
                    // 如果解析失败，使用默认值
                    device.setScreenWidth(1920);
                    device.setScreenHeight(1080);
                    logger.warn("Failed to parse client screen resolution: {}, using default resolution", screenData, e);
                }
            } else {
                // 如果没有屏幕数据，使用默认值
                device.setScreenWidth(1920);
                device.setScreenHeight(1080);
                logger.info("No screen data from client, using default resolution 1920x1080");
            }
            
            // 检查设备是否已存在，避免重复添加
            if (controller.getScreenLayoutManager() != null) {
                ScreenLayoutConfig layoutConfig = controller.getScreenLayoutManager().getLayoutConfig();
                DeviceScreen existingScreen = layoutConfig.getScreen(deviceId);
                if (existingScreen != null) {
                    // 更新现有屏幕信息
                    existingScreen.setDeviceName(device.getDeviceName());
                    existingScreen.setWidth(device.getScreenWidth());
                    existingScreen.setHeight(device.getScreenHeight());
                    layoutConfig.updateScreen(existingScreen);
                    logger.info("Updated existing screen: {}", deviceId);
                } else {
                    // 添加新屏幕
                    DeviceScreen newScreen = ScreenLayoutConfig.createFromDeviceConfig(device);
                    layoutConfig.addScreen(newScreen);
                    logger.info("Added new screen: {}", deviceId);
                }
            }
            
            // 设置默认网络位置，根据设备ID设置不同的位置以避免重叠
            int positionOffset = Math.abs(deviceId.hashCode()) % 100;
            device.setNetworkX(positionOffset * 200);  // 水平错开
            device.setNetworkY(positionOffset * 100);  // 垂直错开
            controller.onClientConnected(device);
        } else if ("INPUT".equals(type)) {
            // 处理输入事件
            // 暂时忽略，后续实现
            logger.debug("Received input event from device: {}", deviceId);
        } else if ("FILE_TRANSFER".equals(type)) {
            // 处理文件传输事件
            // 暂时忽略，后续实现
            logger.debug("Received file transfer event from device: {}", deviceId);
        } else {
            logger.warn("Unknown message type: {}", type);
        }
    }