public class TestDeviceConfig2 {
    public static void main(String[] args) {
        com.keymouseshare.config.DeviceConfig config = new com.keymouseshare.config.DeviceConfig();
        com.keymouseshare.core.Controller controller = new com.keymouseshare.core.Controller();
        System.out.println("Config Screen width: " + config.getScreenWidth());
        System.out.println("Config Screen height: " + config.getScreenHeight());
        System.out.println("Controller Screen width: " + controller.getDeviceConfig().getScreenWidth());
        System.out.println("Controller Screen height: " + controller.getDeviceConfig().getScreenHeight());
    }
}