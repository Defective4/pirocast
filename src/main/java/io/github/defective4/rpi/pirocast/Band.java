package io.github.defective4.rpi.pirocast;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import io.github.defective4.rpi.pirocast.settings.Setting;

public class Band {
    private final SignalSource demodulator;
    private final String extra;
    private float lastFrequency;
    private final float minFreq, maxFreq, defaultFreq;
    private final String name;
    private final Map<Setting, Object> settings = new LinkedHashMap<>();

    public Band(String name, SignalSource demodulator, float minFreq, float maxFreq, float defaultFreq) {
        this(name, demodulator, minFreq, maxFreq, defaultFreq, null);
    }

    public Band(String name, SignalSource demodulator, float minFreq, float maxFreq, float defaultFreq, String extra) {
        this.defaultFreq = defaultFreq;
        this.name = name;
        this.demodulator = demodulator;
        this.minFreq = minFreq;
        this.maxFreq = maxFreq;
        this.extra = extra;
        lastFrequency = defaultFreq;
        initDefaults();
    }

    public float getDefaultFreq() {
        return defaultFreq;
    }

    public SignalSource getDemodulator() {
        return demodulator;
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

    public String getName() {
        return name;
    }

    public Object getSetting(Setting setting) {
        Objects.requireNonNull(setting);
        Object val = settings.get(setting);
        return val == null ? setting.getDefaultValue() : val;
    }

    public Collection<Setting> getSettings() {
        return settings.keySet();
    }

    public void setLastFrequency(float lastFrequency) {
        this.lastFrequency = lastFrequency;
    }

    public void setSetting(Setting setting, Object value) {
        Objects.requireNonNull(value);
        settings.put(setting, value);
    }

    private void initDefaults() {
        for (Setting set : Setting.values()) if (set.isApplicable(demodulator)) setSetting(set, set.getDefaultValue());
    }

}
