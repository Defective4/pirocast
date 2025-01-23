package io.github.defective4.rpi.pirocast;

import static io.github.defective4.rpi.pirocast.ApplicationState.*;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import io.github.defective4.rpi.pirocast.display.SwingLcdDisplayEmulator;
import io.github.defective4.rpi.pirocast.display.TextDisplay;
import io.github.defective4.rpi.pirocast.ext.RadioReceiver;
import io.github.defective4.rpi.pirocast.input.Button;
import io.github.defective4.rpi.pirocast.input.InputAdapter;
import io.github.defective4.rpi.pirocast.input.InputManager;
import io.github.defective4.rpi.pirocast.input.SwingInputManager;
import io.github.defective4.rpi.pirocast.props.AppProperties;
import io.github.defective4.rpi.pirocast.settings.Setting;
import io.github.defective4.sdr.rds.RDSFlags;
import io.github.defective4.sdr.rds.RDSListener;

public class Pirocast {

    private int bandIndex = 0;
    private final List<Band> bands;
    private float centerFrequency = 0;
    private final TextDisplay display;
    private final InputManager inputManager;
    private float offsetFrequency = 0;
    private final AppProperties properties;
    private String rdsRadiotext, rdsStation;
    private int rdsRadiotextScrollIndex = 0;
    private boolean rdsSignal, ta, tp, rdsStereo;
    private final RadioReceiver receiver;
    private int settingIndex = 0;
    private ApplicationState state = OFF;
    private final Timer uiTimer = new Timer(true);

    public Pirocast(List<Band> bands, AppProperties properties) {
        if (bands.isEmpty()) throw new IllegalArgumentException("Band list cannot be empty");
        Objects.requireNonNull(properties);
        this.bands = bands;
        this.properties = properties;
        receiver = new RadioReceiver(properties.getControllerPort(), properties.getRdsPort(),
                properties.getReceiverExecutablePath(), new RDSListener() {
                    @Override
                    public void clockUpdated(String time) {
                        rdsSignal = true;
                    }

                    @Override
                    public void flagsUpdated(RDSFlags flags) {
                        rdsSignal = true;
                        ta = flags.hasTA();
                        tp = flags.hasTP();
                        rdsStereo = flags.isStereo();
                    }

                    @Override
                    public void programInfoUpdated(String programInfo) {
                        rdsSignal = true;
                    }

                    @Override
                    public void programTypeUpdated(String programType) {
                        rdsSignal = true;
                    }

                    @Override
                    public void radiotextUpdated(String radiotext) {
                        rdsSignal = true;
                        rdsRadiotext = radiotext;
                        rdsRadiotextScrollIndex = 0;
                    }

                    @Override
                    public void stationUpdated(String station) {
                        rdsSignal = true;
                        rdsStation = station;
                    }
                });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> receiver.stop()));
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
        uiTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                updateDisplay();
                if (rdsRadiotext != null) {
                    rdsRadiotextScrollIndex++;
                    if (rdsRadiotext.length() - rdsRadiotextScrollIndex < display.getColumns())
                        rdsRadiotextScrollIndex = 0;
                }
            }
        }, 0, 1000);
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
        return Setting.A_TUNING_STEP.isApplicable(band.getDemodulator())
                ? (int) band.getSetting(Setting.A_TUNING_STEP) * 1e3f
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
        receiver.setDemodFrequency(offsetFrequency);
        receiver.setCenterFrequency(centerFrequency);
        resetTransientData();
        updateDisplay();
    }

    public void start() throws IOException {
        resetTransientData();
        display.setDisplayBacklight(true);
        state = MAIN;
        receiver.start();

        Band band = getCurrentBand();
        receiver.initDefaultSettings(band);
        setFrequency(band.getLastFrequency());
        updateDisplay();
    }

    public void stop() {
        state = OFF;
        receiver.stop();
        updateDisplay();
    }

    private void nextSetting() {
        settingIndex++;
        if (settingIndex > getCurrentBand().getSettings().size()) settingIndex = 0;
    }

    private void resetTransientData() {
        receiver.resetRDS();
        rdsRadiotext = null;
        rdsStation = null;
        rdsSignal = false;
        rdsRadiotextScrollIndex = 0;
        ta = false;
        tp = false;
        rdsStereo = false;
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
                float freq = getCurrentFrequency();
                line1 = freq <= 1e6 ? Double.toString(getCurrentFrequency() / 1e3) + " KHz"
                        : Double.toString(getCurrentFrequency() / 1e6) + " MHz";
                if (getCurrentBand().getDemodulator() == Demodulator.FM && rdsSignal) {
                    line1 += "*";
                    if (rdsStation != null) {
                        StringBuilder lineBuilder = display.generateCenteredText(rdsStation);
                        // TODO custom chars
                        if (ta) lineBuilder.setCharAt(lineBuilder.length() - 2, 'A');
                        if (tp) lineBuilder.setCharAt(lineBuilder.length() - 1, 'B');
                        if (rdsStereo && (int) getCurrentBand().getSetting(Setting.B_STEREO) != 0)
                            lineBuilder.setCharAt(0, 'S');
                        line1 = lineBuilder.toString();
                    }
                    if (rdsRadiotext != null) {
                        display.displayLineOfText(rdsRadiotext.substring(rdsRadiotextScrollIndex), 2);
                    }
                }
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
            Band band = getCurrentBand();
            receiver.initDefaultSettings(band);
            setFrequency(band.getLastFrequency());
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
            } else if (currentVal instanceof Boolean bool) {
                band.setSetting(set, !bool);
            }

            switch (set) {
                case D_GAIN -> receiver.setGain((int) band.getSetting(set));
                case C_RDS -> {
                    resetTransientData();
                    receiver.setRDS((boolean) band.getSetting(set));
                }
                case E_DEEMP -> receiver.setDeemphasis((int) band.getSetting(set));
                default -> {}
            }
        }
        updateDisplay();
    }
}
