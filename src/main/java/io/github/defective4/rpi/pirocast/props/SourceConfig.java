package io.github.defective4.rpi.pirocast.props;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.github.defective4.rpi.pirocast.SignalMode;
import io.github.defective4.rpi.pirocast.Source;

public class SourceConfig {
    private final boolean allowAPRS, allowRDS;
    private final int minFreq, maxFreq, defaultFreq;
    private final String name, mode, extra;

    public SourceConfig(String name, String mode, String extra, boolean allowAPRS, boolean allowRDS, int minFreq,
            int maxFreq, int defaultFreq) {
        this.name = name;
        this.mode = mode;
        this.extra = extra;
        this.allowAPRS = allowAPRS;
        this.allowRDS = allowRDS;
        this.minFreq = minFreq;
        this.maxFreq = maxFreq;
        this.defaultFreq = defaultFreq;
    }

    public String isValid() {
        if (name == null || name.isBlank()) return "name";
        SignalMode m;
        try {
            m = SignalMode.valueOf(mode.toUpperCase());
        } catch (Exception e) {
            return mode;
        }
        if (m == SignalMode.FILE || m == SignalMode.NETWORK) {
            if (extra == null || extra.isBlank()) return "extra";
        } else if (m.getId() != SignalMode.UNDEFINED_ID && maxFreq == 0) return "maxFreq";
        return null;
    }

    public JsonObject toJSON() {
        return new Gson().toJsonTree(this).getAsJsonObject();
    }

    public Source toSource() {
        return new Source(name, SignalMode.valueOf(mode.toUpperCase()), minFreq, maxFreq, defaultFreq, extra, allowRDS,
                allowAPRS);
    }
}
