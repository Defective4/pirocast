package io.github.defective4.rpi.pirocast;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import io.github.defective4.rpi.pirocast.settings.Setting;

public class Source {
    private final boolean allowAPRS, allowRDS;
    private final String extra;
    private float lastFrequency;
    private final float minFreq, maxFreq, defaultFreq;
    private final SignalMode mode;
    private final String name;
    private final Map<Setting, Object> settings = new LinkedHashMap<>();

    public Source(String name, SignalMode mode, float minFreq, float maxFreq, float defaultFreq, String extra,
            boolean allowRDS, boolean allowAPRS) {
        this.defaultFreq = defaultFreq;
        this.name = name;
        this.mode = mode;
        this.minFreq = minFreq;
        this.maxFreq = maxFreq;
        this.extra = extra;
        this.allowAPRS = allowAPRS;
        this.allowRDS = allowRDS;
        lastFrequency = defaultFreq;
        initDefaults();
    }

    public float getDefaultFreq() {
        return defaultFreq;
    }

    public String getExtra() {
        return extra;
    }

    public float getLastFrequency() {
        return lastFrequency;
    }

    public float getMaxFreq() {
        return maxFreq;
    }

    public float getMinFreq() {
        return minFreq;
    }

    public SignalMode getMode() {
        return mode;
    }

    public String getName() {
        return name;
    }

    public Object getSetting(Setting setting) {
        Objects.requireNonNull(setting);
        switch (setting) {
            case D_RDS -> { if (!allowRDS) return false; }
            case C_APRS -> { if (!allowAPRS) return false; }
            default -> {}
        }
        Object val = settings.get(setting);
        return val == null ? setting.getDefaultValue() : val;
    }

    public Collection<Setting> getSettings() {
        return settings.keySet();
    }

    public boolean isAPRSAllowed() {
        return allowAPRS;
    }

    public boolean isRDSAllowed() {
        return allowRDS;
    }

    public void setLastFrequency(float lastFrequency) {
        this.lastFrequency = lastFrequency;
    }

    public void setSetting(Setting setting, Object value) {
        Objects.requireNonNull(value);
        settings.put(setting, value);
    }

    private void initDefaults() {
        for (Setting set : Setting.values()) if (set.isApplicable(mode)) setSetting(set, set.getDefaultValue());
    }

}
