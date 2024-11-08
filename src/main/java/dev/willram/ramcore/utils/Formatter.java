package dev.willram.ramcore.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Formatter {

    public static String formatMoney(double value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        return formatter.format(value);
    }

    public static String decimalFormat(double input, int maxPlaces) {
        // Creates DecimalFormat pattern based on maxPlaces
        StringBuilder pattern = new StringBuilder("#");
        if (maxPlaces > 0) {
            pattern.append(".");
        }
        pattern.append(StringUtils.repeat("#", maxPlaces));
        NumberFormat numberFormat = new DecimalFormat(pattern.toString());

        return numberFormat.format(input);
    }

    public static String bigNumber(long count) {
        if (count < 1000) return "" + count;
        int exp = (int) (Math.log(count) / Math.log(1000));
        return String.format("%.1f%c",
                count / Math.pow(1000, exp),
                "KMBTQU".charAt(exp-1));
    }
}