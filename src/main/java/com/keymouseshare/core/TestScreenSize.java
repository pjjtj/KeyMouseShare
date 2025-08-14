package com.keymouseshare.core;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.DisplayMode;

public class TestScreenSize {
//    public static void main(String[] args) {
//        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
//        java.awt.GraphicsDevice gd = ge.getDefaultScreenDevice();
//        java.awt.DisplayMode dm = gd.getDisplayMode();
//        int width = dm.getWidth();
//        int height = dm.getHeight();
//        System.out.println("Screen size: " + width + "x" + height);
//
//
//    }

    public static void main(String[] args) {
        com.keymouseshare.config.DeviceConfig config = new com.keymouseshare.config.DeviceConfig();
        System.out.println("Screen width: " + config.getScreenWidth());
        System.out.println("Screen height: " + config.getScreenHeight());
    }

//    public static void main(String[] args) {
//        try {
//            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//            GraphicsDevice[] screens = ge.getScreenDevices();
//            System.out.println("Number of screens: " + screens.length);
//
//            GraphicsDevice primaryDevice = ge.getDefaultScreenDevice();
//            DisplayMode dm = primaryDevice.getDisplayMode();
//            int width = dm.getWidth();
//            int height = dm.getHeight();
//            System.out.println("Primary screen size: " + width + "x" + height);
//
//            for (GraphicsDevice screen : screens) {
//                dm = screen.getDisplayMode();
//                width = dm.getWidth();
//                height = dm.getHeight();
//                System.out.println("Screen " + screen.getIDstring() + " size: " + width + "x" + height);
//            }
//        } catch (Exception e) {
//            System.out.println("Error: " + e.getMessage());
//        }
//    }


}
