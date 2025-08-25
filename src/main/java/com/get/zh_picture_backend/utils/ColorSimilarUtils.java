package com.get.zh_picture_backend.utils;

import java.awt.*;

public class ColorSimilarUtils {

    public static double calculateSimilarity(Color color1,Color color2){
        //比较两个color的rgb欧几里得距离
        int color1Red = color1.getRed();
        int color1Green = color1.getGreen();
        int color1Blue = color1.getBlue();

        int color2Red = color2.getRed();
        int color2Green = color2.getGreen();
        int color2Blue = color2.getBlue();

        //计算欧氏距离
        double distance=Math.sqrt(Math.pow((color1Red-color2Red),2)+Math.pow((color1Green-color2Green),2)+Math.pow((color1Blue-color2Blue),2));

        //返回的值是越大越好
        return 1-distance/Math.sqrt(3*Math.pow(255,2));
    }

    public static double calculateSimilarity(String hexColor1,String hexColor2){
        Color color1=Color.decode(hexColor1);
        Color color2=Color.decode(hexColor2);
        return calculateSimilarity(color1,color2);
    }

    /**
     * 处理cos存储桶返回的颜色
     * @param input
     * @return
     */
    public static String toHexColor(String input) {
        // 去掉 0x 前缀
        String colorValue;
        if (input.startsWith("0x")) {
            colorValue = input.substring(2);
        } else {
            colorValue = input;
        }

        // 将剩余部分解析为十六进制数，再转成 6 位十六进制字符串
        int intValue = Integer.parseInt(colorValue, 16);
        String hexColor = String.format("%06x", intValue);

        // 返回标准 #RRGGBB 格式
        return "#" + hexColor;
    }

}
