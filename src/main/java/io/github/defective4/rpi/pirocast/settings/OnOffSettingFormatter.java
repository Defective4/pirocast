package io.github.defective4.rpi.pirocast.settings;

public class OnOffSettingFormatter implements SettingFormatter {

    @Override
    public String format(Object obj) {
        return obj instanceof Boolean bool && bool ? "On" : "Off";
    }

}
