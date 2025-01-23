package io.github.defective4.rpi.pirocast.settings;

import io.github.defective4.rpi.pirocast.Demodulator;

public enum Setting {
    A_TUNING_STEP("Tuning Step", new Demodulator[] {
            Demodulator.AM, Demodulator.NFM
    }, 10, 1, 100, val -> val + " KHz"), B_STEREO("Stereo", Demodulator.FM, 2, 0, 2, val -> switch ((int) val) {
        case 1 -> "On";
        case 2 -> "Auto";
        default -> "Off";
    }),
    C_RDS("RDS", Demodulator.FM, true, null, null, new OnOffSettingFormatter()),
    E_GAIN("RF Gain", Demodulator.values(), 10, 0, 49, null),
    F_DEEMP("Deemphasis", new Demodulator[] {
            Demodulator.FM, Demodulator.NFM
    }, 2, 0, 3, val -> {
        int v = (int) val;
        if (v == 0) return "Off";
        return v * 25 + "u";
    }),
    MODE("Mode", new Demodulator[0], null, null, null, null);

    private final Demodulator[] applicableModes;
    private final Object defaultValue;
    private final SettingFormatter formatter;
    private final Object minValue, maxValue;
    private final String name;

    private Setting(String name, Demodulator applicableMode, Object defaultValue, Object minValue, Object maxValue,
            SettingFormatter formatter) {
        this(name, new Demodulator[] {
                applicableMode
        }, defaultValue, minValue, maxValue, formatter);
    }

    private Setting(String name, Demodulator[] applicableModes, Object defaultValue, Object minValue, Object maxValue,
            SettingFormatter formatter) {
        this.applicableModes = applicableModes;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.name = name;
        this.formatter = formatter == null ? new DefaultSettingFormatter() : formatter;
    }

    public Demodulator[] getApplicableModes() {
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

    public boolean isApplicable(Demodulator demod) {
        for (Demodulator d : getApplicableModes()) if (d == demod) return true;
        return false;
    }

}
