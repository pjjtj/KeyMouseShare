public class TestDeviceConfig {
    public static void main(String[] args) {
        com.keymouseshare.config.DeviceConfig config = new com.keymouseshare.config.DeviceConfig();
        System.out.println("Screen width: " + config.getScreenWidth());
        System.out.println("Screen height: " + config.getScreenHeight());
    }
}