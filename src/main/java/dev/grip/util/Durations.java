package dev.grip.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads durations written the way admins type them: {@code 30s}, {@code 10m}, {@code 1h30m}.
 * A bare number counts as seconds.
 */
public final class Durations {

    private static final Pattern PART = Pattern.compile("(\\d+)([dhms])");

    private Durations() {
    }

    public static Duration parse(String text) {
        final String s = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty duration");
        }
        if (s.chars().allMatch(Character::isDigit)) {
            return Duration.ofSeconds(Long.parseLong(s));
        }

        final Matcher matcher = PART.matcher(s);
        long seconds = 0;
        int end = 0;
        try {
            while (matcher.find()) {
                if (matcher.start() != end) {
                    break;
                }
                end = matcher.end();
                seconds = Math.addExact(seconds, Math.multiplyExact(Long.parseLong(matcher.group(1)), unitSeconds(matcher.group(2).charAt(0))));
            }
        } catch (ArithmeticException e) {
            // A silently-overflowed value would land as either a huge or negative Duration,
            // and either one breaks confirm_time (see Settings.confirmTime).
            throw new IllegalArgumentException("Duration too large: " + text, e);
        }
        if (end == 0 || end != s.length()) {
            throw new IllegalArgumentException("Not a duration: " + text);
        }
        return Duration.ofSeconds(seconds);
    }

    private static long unitSeconds(char unit) {
        return switch (unit) {
            case 'd' -> 86_400;
            case 'h' -> 3_600;
            case 'm' -> 60;
            default -> 1;
        };
    }
}
