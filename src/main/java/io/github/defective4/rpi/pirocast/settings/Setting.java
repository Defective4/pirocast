package io.github.defective4.rpi.pirocast.settings;

import io.github.defective4.rpi.pirocast.SignalSource;

public enum Setting {
    A_BEEP("Beep", SignalSource.values(), true, null, null, new OnOffSettingFormatter()),
    B_STEREO("Stereo", SignalSource.FM, true, null, null, new OnOffSettingFormatter()),
    B_TUNING_STEP("Tuning Step", new SignalSource[] {
            SignalSource.AM, SignalSource.NFM
    }, 10, 1, 100, val -> val + " KHz"),
    C_APRS("APRS", SignalSource.NFM, true, null, null, new OnOffSettingFormatter()),
    D_RDS("RDS", SignalSource.FM, true, null, null, new OnOffSettingFormatter()),
    E_GAIN("RF Gain", new SignalSource[] {
            SignalSource.AM, SignalSource.FM, SignalSource.NFM
    }, 10, 0, 49, null),
    F_DEEMP("Deemphasis", new SignalSource[] {
            SignalSource.FM, SignalSource.NFM
    }, 2, 0, 3, val -> {
        int v = (int) val;
        if (v == 0) return "Off";
        return v * 25 + "u";
    }),
    MODE("Mode", new SignalSource[0], null, null, null, null);

    private final SignalSource[] applicableModes;
    private final Object defaultValue;
    private final SettingFormatter formatter;
    private final Object minValue, maxValue;
    private final String name;

    private Setting(String name, SignalSource applicableMode, Object defaultValue, Object minValue, Object maxValue,
            SettingFormatter formatter) {
        this(name, new SignalSource[] {
                applicableMode
        }, defaultValue, minValue, maxValue, formatter);
    }

    private Setting(String name, SignalSource[] applicableModes, Object defaultValue, Object minValue, Object maxValue,
            SettingFormatter formatter) {
        this.applicableModes = applicableModes;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.name = name;
        this.formatter = formatter == null ? new DefaultSettingFormatter() : formatter;
    }

    public SignalSource[] getApplicableModes() {
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

    public boolean isApplicable(SignalSource demod) {
        for (SignalSource d : getApplicableModes()) if (d == demod) return true;
        return false;
    }

}
