package dev.willram.ramcore.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.TreeMap;

public class Formatter {

    private final static TreeMap<Integer, String> romanMap = new TreeMap<>();

    static {
        romanMap.put(1000, "M");
        romanMap.put(900, "CM");
        romanMap.put(500, "D");
        romanMap.put(400, "CD");
        romanMap.put(100, "C");
        romanMap.put(90, "XC");
        romanMap.put(50, "L");
        romanMap.put(40, "XL");
        romanMap.put(10, "X");
        romanMap.put(9, "IX");
        romanMap.put(5, "V");
        romanMap.put(4, "IV");
        romanMap.put(1, "I");
    }

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

    public static String romanNumeral(int input) {
        int l =  romanMap.floorKey(input);
        if (input == l) {
            return romanMap.get(input);
        }
        return romanMap.get(l) + romanNumeral(input-l);
    }

    public static String bigNumber(long count) {
        if (count < 1000) return "" + count;
        int exp = (int) (Math.log(count) / Math.log(1000));
        return String.format("%.1f%c",
                count / Math.pow(1000, exp),
                "KMBTQU".charAt(exp-1));
    }
}