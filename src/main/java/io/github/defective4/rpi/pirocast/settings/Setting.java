package io.github.defective4.rpi.pirocast.settings;

import io.github.defective4.rpi.pirocast.SignalMode;

public enum Setting {
    A_BEEP("Beep", SignalMode.values(), true, null, null, new OnOffSettingFormatter()),
    B_STEREO("Stereo", SignalMode.FM, true, null, null, new OnOffSettingFormatter()),
    B_TUNING_STEP("Tuning Step", new SignalMode[] {
            SignalMode.AM, SignalMode.NFM
    }, 10, 1, 100, val -> val + " KHz"),
    C_APRS("APRS", SignalMode.NFM, true, null, null, new OnOffSettingFormatter()),
    D_RDS("RDS", SignalMode.FM, true, null, null, new OnOffSettingFormatter()),
    E_GAIN("RF Gain", new SignalMode[] {
            SignalMode.AM, SignalMode.FM, SignalMode.NFM
    }, 10, 0, 49, null),
    F_DEEMP("Deemphasis", new SignalMode[] {
            SignalMode.FM, SignalMode.NFM
    }, 2, 0, 3, val -> {
        int v = (int) val;
        if (v == 0) return "Off";
        return v * 25 + "u";
    }),
    SOURCE("Source", new SignalMode[0], null, null, null, null);

    private final SignalMode[] applicableModes;
    private final Object defaultValue;
    private final SettingFormatter formatter;
    private final Object minValue, maxValue;
    private final String name;

    private Setting(String name, SignalMode applicableMode, Object defaultValue, Object minValue, Object maxValue,
            SettingFormatter formatter) {
        this(name, new SignalMode[] {
                applicableMode
        }, defaultValue, minValue, maxValue, formatter);
    }

    private Setting(String name, SignalMode[] applicableModes, Object defaultValue, Object minValue, Object maxValue,
            SettingFormatter formatter) {
        this.applicableModes = applicableModes;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.name = name;
        this.formatter = formatter == null ? new DefaultSettingFormatter() : formatter;
    }

    public SignalMode[] getApplicableModes() {
        return applicableModes;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public SettingFormatter getFormatter() {
        return formatter;
    }

    public Object getMaxValue() {
        return maxValue;
    }

    public Object getMinValue() {
        return minValue;
    }

    public String getName() {
        return name;
    }

    public boolean isApplicable(SignalMode demod) {
        for (SignalMode d : getApplicableModes()) if (d == demod) return true;
        return false;
    }

}
