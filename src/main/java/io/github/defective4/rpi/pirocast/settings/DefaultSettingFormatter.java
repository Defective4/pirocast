package io.github.defective4.rpi.pirocast.settings;

public class DefaultSettingFormatter implements SettingFormatter {
    @Override
    public String format(Object obj) {
        return obj.toString();
    }
}
