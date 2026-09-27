package dev.grip.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsTest {

    private static final Logger LOG = Logger.getLogger("test");

    private static Settings load(String confirmTime) {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("confirm_time", confirmTime);
        return Settings.load(cfg, LOG);
    }

    @Test
    void confirmTimeOutOfRangeFallsBackTo3Seconds() {
        assertEquals(Duration.ofSeconds(3), load("999999999999999999d").confirmTime());
        assertEquals(Duration.ofSeconds(3), load("5h").confirmTime());
        assertEquals(Duration.ofSeconds(3), load("0s").confirmTime());
        assertEquals(Duration.ofSeconds(3), load("not-a-duration").confirmTime());
    }

    @Test
    void confirmTimeWithinRangeIsKept() {
        assertEquals(Duration.ofSeconds(30), load("30s").confirmTime());
        assertEquals(Duration.ofSeconds(60), load("60s").confirmTime());
    }
}
