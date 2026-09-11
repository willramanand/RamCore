package dev.willram.ramcore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class TxtUtils {

    private final static String headerTemplate = "<gold><st>                        </st></gold> <aqua><header></aqua> <gold><st>                        </st></gold>";

    public static String coolChars(String input) {
        StringBuilder newString = new StringBuilder();
        char[] chars = input.toCharArray();
        for (char ch : chars) {
            switch (ch) {
                case 'a', 'A' -> newString.append('ᴀ');
                case 'b', 'B' -> newString.append('ʙ');
                case 'c', 'C' -> newString.append('ᴄ');
                case 'd', 'D' -> newString.append('ᴅ');
                case 'e', 'E' -> newString.append('ᴇ');
                case 'f', 'F' -> newString.append('ꜰ');
                case 'g', 'G' -> newString.append('ɢ');
                case 'h', 'H' -> newString.append('ʜ');
                case 'i', 'I' -> newString.append('ɪ');
                case 'j', 'J' -> newString.append('ᴊ');
                case 'k', 'K' -> newString.append('ᴋ');
                case 'l', 'L' -> newString.append('ʟ');
                case 'm', 'M' -> newString.append('ᴍ');
                case 'n', 'N' -> newString.append('ɴ');
                case 'o', 'O' -> newString.append('ᴏ');
                case 'p', 'P' -> newString.append('ᴘ');
                case 'q', 'Q' -> newString.append('ꞯ');
                case 'r', 'R' -> newString.append('ʀ');
                case 's', 'S' -> newString.append('ꜱ');
                case 't', 'T' -> newString.append('ᴛ');
                case 'u', 'U' -> newString.append('ᴜ');
                case 'v', 'V' -> newString.append('ᴠ');
                case 'w', 'W' -> newString.append('ᴡ');
                case 'x', 'X' -> newString.append('х');
                case 'y', 'Y' -> newString.append('ʏ');
                case 'z', 'Z' -> newString.append('ᴢ');
                default -> newString.append(ch);
            }
        }
        return newString.toString();
    }

    public static Component generateHeaderComponent(String headerText) {
        return MiniMessage.miniMessage().deserialize(headerTemplate, Placeholder.parsed("header", coolChars(headerText)));
    }

private TxtUtils() {}
}
