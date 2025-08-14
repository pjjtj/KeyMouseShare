    /**
     * 启动客户端模式
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public void startClient(String host, int port) {
        workerGroup = new NioEventLoopGroup();
        
        try {
            logger.info("Attempting to connect to server {}:{}", host, port);
            
            // 检查网络连接性
            checkNetworkConnectivity(host, port);
            
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new InputEventDecoder());
                     ch.pipeline().addLast(new InputEventEncoder());
                     ch.pipeline().addLast(new InputEventHandler(controller));
                 }
             });
            
            // 连接到服务器
            ChannelFuture f = b.connect(new InetSocketAddress(host, port)).sync();
            clientChannel = f.channel();
            logger.info("Connected to server {}:{}", host, port);
            
            // 发送连接消息，包含设备ID、设备名称和屏幕分辨率信息
            int[] screenSize = getScreenSize();
            String screenData = screenSize[0] + "x" + screenSize[1];
            DataPacket connectPacket = new DataPacket("CONNECT", controller.getDeviceConfig().getDeviceId(), 
                controller.getDeviceConfig().getDeviceName(), screenData);
            clientChannel.writeAndFlush(connectPacket);
            
            // 通知控制器客户端已连接
            controller.onClientConnected();
            
            DeviceConfig.Device serverDevice = new DeviceConfig.Device();
            String serverDeviceId = "server-" + host.replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + ":" + port;
            serverDevice.setDeviceId(serverDeviceId != null ? serverDeviceId : UUID.randomUUID().toString());
            serverDevice.setDeviceName("Server (" + host + ":" + port + ")");
            serverDevice.setIpAddress(host);
            // 设置默认屏幕尺寸
            serverDevice.setScreenWidth(1920);
            serverDevice.setScreenHeight(1080);
            // 设置默认网络位置
            serverDevice.setNetworkX(0);
            serverDevice.setNetworkY(0);
            controller.onClientConnected(serverDevice);
            
            // 启动设备发现
            startDeviceDiscovery();
        } catch (InterruptedException e) {
            logger.error("Failed to connect to server {}:{}", host, port, e);
        } catch (Exception e) {
            logger.error("Failed to connect to server {}:{} - {}", host, port, e.getMessage(), e);
        }
    }