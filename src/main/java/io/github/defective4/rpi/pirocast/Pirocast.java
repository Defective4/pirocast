package io.github.defective4.rpi.pirocast;

import static io.github.defective4.rpi.pirocast.ApplicationState.*;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import io.github.defective4.rpi.pirocast.display.SwingLcdDisplayEmulator;
import io.github.defective4.rpi.pirocast.display.TextDisplay;
import io.github.defective4.rpi.pirocast.input.Button;
import io.github.defective4.rpi.pirocast.input.InputAdapter;
import io.github.defective4.rpi.pirocast.input.InputManager;
import io.github.defective4.rpi.pirocast.input.SwingInputManager;
import io.github.defective4.rpi.pirocast.settings.Setting;

public class Pirocast {

    private int bandIndex = 0;
    private final List<Band> bands;
    private float centerFrequency = 0;
    private final TextDisplay display;
    private final InputManager inputManager;
    private float offsetFrequency = 0;
    private int settingIndex = 0;
    private ApplicationState state = OFF;

    public Pirocast(List<Band> bands) {
        if (bands.isEmpty()) throw new IllegalArgumentException("Band list cannot be empty");
        this.bands = bands;
        centerFrequency = bands.get(0).getDefaultFreq();
        display = new SwingLcdDisplayEmulator(16, 2); // TODO configuration
        inputManager = new SwingInputManager((Window) display, 500, KeyEvent.VK_LEFT, KeyEvent.VK_ENTER,
                KeyEvent.VK_RIGHT);
        display.showDisplay();
        inputManager.putInputListener(Button.NEXT, new InputAdapter() {

            @Override
            public void buttonClicked() {
                if (state == SETTINGS) {
                    updateSettingValue(1);
                }
            }

            @Override
            public void buttonPressed() {
                if (state == MAIN) setFrequency(getCurrentFrequency() + getTuningStep());
            }
        });
        inputManager.putInputListener(Button.PREV, new InputAdapter() {

            @Override
            public void buttonClicked() {
                if (state == SETTINGS) {
                    updateSettingValue(-1);
                }
            }

            @Override
            public void buttonPressed() {
                if (state == MAIN) setFrequency(getCurrentFrequency() - getTuningStep());
            }
        });
        inputManager.putInputListener(Button.OK, new InputAdapter() {
            @Override
            public void buttonClicked() {
                switch (state) {
                    case SETTINGS -> {
                        nextSetting();
                        updateDisplay();
                    }
                    case MAIN -> {
                        state = SETTINGS;
                        updateDisplay();
                    }
                    default -> {}
                }
            }

            @Override
            public void buttonLongClicked() {
                switch (state) {
                    case SETTINGS -> {
                        state = MAIN;
                        updateDisplay();
                    }
                    default -> {}
                }
            }
        });
    }

    public Band getCurrentBand() {
        return bands.get(bandIndex);
    }

    public float getCurrentFrequency() {
        return centerFrequency + offsetFrequency;
    }

    public Setting getCurrentSetting() {
        List<Setting> settings = new ArrayList<>();
        settings.add(Setting.MODE);
        settings.addAll(getCurrentBand().getSettings());
        return settings.get(settingIndex % settings.size());
    }

    public float getTuningStep() {
        Band band = getCurrentBand();
        return Setting.TUNING_STEP.isApplicable(band.getDemodulator())
                ? (int) band.getSetting(Setting.TUNING_STEP) * 1e3f
                : 100e3f;
    }

    public void setFrequency(float freq) {
        Band band = getCurrentBand();
        if (freq < band.getMinFreq()) freq = band.getMinFreq();
        if (freq > band.getMaxFreq()) freq = band.getMaxFreq();
        float diff = Math.abs(freq - centerFrequency);
        if (diff > 1e6) {
            offsetFrequency = 0;
            centerFrequency = freq;
        } else {
            offsetFrequency = freq - centerFrequency;
        }
        updateDisplay();
    }

    public void start() {
        display.setDisplayBacklight(true);
        state = MAIN;
        setFrequency(getCurrentBand().getLastFrequency());
        updateDisplay();
    }

    public void stop() {
        state = OFF;
        updateDisplay();
    }

    private void nextSetting() {
        settingIndex++;
        if (settingIndex > getCurrentBand().getSettings().size()) settingIndex = 0;
    }

    private void updateDisplay() {
        switch (state) {
            case SETTINGS -> {
                display.clearDisplay();
                Setting setting = getCurrentSetting();
                display.centerTextInLine(setting.getName(), 1);
                String value;
                if (setting == Setting.MODE) {
                    value = getCurrentBand().getName();
                } else {
                    value = setting.getFormatter().format(getCurrentBand().getSetting(setting));
                }
                StringBuilder builder = display.generateCenteredText(value);
                builder.setCharAt(0, '<');
                builder.setCharAt(builder.length() - 1, '>');
                display.displayLineOfText(builder.toString(), 2);
            }
            case OFF -> {
                if (display.getDisplayBacklight()) {
                    display.clearDisplay();
                    display.setDisplayBacklight(false);
                }
            }
            case MAIN -> {
                display.clearDisplay();
                String line1;
                line1 = Double.toString(getCurrentFrequency() / 1e6) + " MHz";
                display.centerTextInLine(line1, 1);
            }
            default -> {

            }
        }
    }

    private void updateSettingValue(int direction) {
        Setting set = getCurrentSetting();
        if (set == Setting.MODE) {
            getCurrentBand().setLastFrequency(getCurrentFrequency());
            bandIndex += direction;
            if (bandIndex < 0) bandIndex = bands.size() - 1;
            if (bandIndex >= bands.size()) bandIndex = 0;
            setFrequency(getCurrentBand().getLastFrequency());
        } else {
            Band band = getCurrentBand();
            Object currentVal = band.getSetting(set);
            if (currentVal instanceof Integer i) {
                int newVal = i + direction;
                if (set.getMinValue() instanceof Integer min && set.getMaxValue() instanceof Integer max) {
                    if (newVal < min) newVal = max;
                    else if (newVal > max) newVal = min;
                }
                band.setSetting(set, newVal);
            }
        }
        updateDisplay();
    }
}
