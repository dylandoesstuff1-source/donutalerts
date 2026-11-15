package com.donutalerts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmountParser {
    /**
     * Matches chat like:  "Notch paid you $10M"  /  "Notch paid you $1,500,000"
     * If the server's wording differs, change this regex (group 1 = name, 2 = number, 3 = suffix).
     */
    public static final Pattern PAID_YOU = Pattern.compile(
        "([A-Za-z0-9_.]{2,17})\\s+(?:paid|sent|gave)\\s+you\\s+\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\s*([KkMmBbTt]?)",
        Pattern.CASE_INSENSITIVE);

    public record Result(String player, double amount) {}

    public static Result parse(String message) {
        Matcher m = PAID_YOU.matcher(message);
        if (!m.find()) return null;
        return new Result(m.group(1), toNumber(m.group(2), m.group(3)));
    }

    public static double toNumber(String num, String suffix) {
        double v = Double.parseDouble(num.replace(",", ""));
        return switch (suffix.toLowerCase()) {
            case "k" -> v * 1_000d;
            case "m" -> v * 1_000_000d;
            case "b" -> v * 1_000_000_000d;
            case "t" -> v * 1_000_000_000_000d;
            default -> v;
        };
    }

    public static String format(double v) {
        if (v >= 1e12) return trim(v / 1e12) + "T";
        if (v >= 1e9) return trim(v / 1e9) + "B";
        if (v >= 1e6) return trim(v / 1e6) + "M";
        if (v >= 1e3) return trim(v / 1e3) + "K";
        return trim(v);
    }

    private static String trim(double d) {
        return (d == Math.floor(d)) ? String.valueOf((long) d) : String.format("%.2f", d);
    }
}
