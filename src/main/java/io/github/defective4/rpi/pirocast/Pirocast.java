package io.github.defective4.rpi.pirocast;

import static io.github.defective4.rpi.pirocast.ApplicationState.*;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import io.github.defective4.rpi.pirocast.display.SwingLcdDisplayEmulator;
import io.github.defective4.rpi.pirocast.display.TextDisplay;
import io.github.defective4.rpi.pirocast.ext.Direwolf;
import io.github.defective4.rpi.pirocast.ext.RadioReceiver;
import io.github.defective4.rpi.pirocast.ext.WaveResamplerServer;
import io.github.defective4.rpi.pirocast.input.Button;
import io.github.defective4.rpi.pirocast.input.InputAdapter;
import io.github.defective4.rpi.pirocast.input.InputManager;
import io.github.defective4.rpi.pirocast.input.SwingInputManager;
import io.github.defective4.rpi.pirocast.props.AppProperties;
import io.github.defective4.rpi.pirocast.settings.Setting;
import io.github.defective4.sdr.rds.RDSFlags;
import io.github.defective4.sdr.rds.RDSListener;

public class Pirocast {

    private static final AudioFormat FLOAT_AUDIO_FORMAT = new AudioFormat(Encoding.PCM_FLOAT, 48000, 32, 1, 4, 48000,
            false);
    private static final AudioFormat SIGNED_AUDIO_FORMAT = new AudioFormat(Encoding.PCM_SIGNED, 48000, 16, 1, 2, 48000,
            false);
    private final Direwolf aprsDecoder;
    private final Queue<String> aprsQueue = new ConcurrentLinkedQueue<>();
    private final WaveResamplerServer aprsResampler;
    private int aprsScrollIndex = 0;
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
        aprsDecoder = new Direwolf(line -> aprsQueue.add(line));
        aprsResampler = new WaveResamplerServer(properties.getAprsResamplerPort(), FLOAT_AUDIO_FORMAT,
                SIGNED_AUDIO_FORMAT);
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
                        if (flags.isStereo() != rdsStereo) {
                            rdsStereo = flags.isStereo();
                            if ((boolean) getCurrentBand().getSetting(Setting.B_STEREO)) {
                                receiver.setStereo(rdsStereo);
                            }
                        }
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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                receiver.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            stopAPRS();
        }));
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
                    case OFF -> start();
                    case MAIN -> stop();
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
                if (!aprsQueue.isEmpty()) {
                    String element = aprsQueue.peek();
                    aprsScrollIndex += 2;
                    if (element.length() - aprsScrollIndex < display.getColumns()) {
                        aprsQueue.poll();
                        aprsScrollIndex = 0;
                    }
                }
            }
        }, 1000, 1000);
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

    public void start() {
        try {
            Band band = getCurrentBand();
            display.setDisplayBacklight(true);
            state = MAIN;
            receiver.start();
            if (band.getDemodulator() == Demodulator.NFM && (boolean) band.getSetting(Setting.C_APRS)) startAPRS();
            aprsResampler.start();
            resetTransientData();

            receiver.initDefaultSettings(band);
            setFrequency(band.getLastFrequency());
        } catch (Exception e) {
            e.printStackTrace();
            raiseError();
        }
        updateDisplay();
    }

    public void stop() {
        getCurrentBand().setLastFrequency(getCurrentFrequency());
        state = OFF;
        receiver.stop();
        stopAPRS();
        aprsResampler.stop();
        updateDisplay();
    }

    private void nextSetting() {
        settingIndex++;
        if (settingIndex > getCurrentBand().getSettings().size()) settingIndex = 0;
    }

    private void raiseError() {
        stop();
        state = ERROR;
        display.setDisplayBacklight(true);
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
        aprsQueue.clear();
        aprsScrollIndex = 0;
        if ((boolean) getCurrentBand().getSetting(Setting.B_STEREO)) receiver.setStereo(false);
    }

    private void startAPRS() {
        aprsDecoder.start();
        aprsResampler.setTarget(aprsDecoder.getOutputStream());
    }

    private void stopAPRS() {
        aprsDecoder.stop();
        aprsResampler.setTarget(null);
    }

    private void updateDisplay() {
        switch (state) {
            case ERROR -> {
                display.clearDisplay();
                display.centerTextInLine("System", 1);
                display.centerTextInLine("Error", 2);
            }
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
                Demodulator mode = getCurrentBand().getDemodulator();
                if (mode == Demodulator.FM && rdsSignal) {
                    line1 += "*";
                    if (rdsStation != null) {
                        StringBuilder lineBuilder = display.generateCenteredText(rdsStation);
                        // TODO custom chars
                        if (ta) lineBuilder.setCharAt(lineBuilder.length() - 2, 'A');
                        if (tp) lineBuilder.setCharAt(lineBuilder.length() - 1, 'B');
                        if (rdsStereo && (boolean) getCurrentBand().getSetting(Setting.B_STEREO))
                            lineBuilder.setCharAt(0, 'S');
                        line1 = lineBuilder.toString();
                    }
                    if (rdsRadiotext != null) {
                        display.displayLineOfText(rdsRadiotext.substring(rdsRadiotextScrollIndex), 2);
                    }
                } else if (mode == Demodulator.NFM && !aprsQueue.isEmpty()) {
                    String element = aprsQueue.peek().substring(aprsScrollIndex);
                    display.displayLineOfText(element, 2);
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
            receiver.setRDS(band.getDemodulator() == Demodulator.FM && (boolean) band.getSetting(Setting.D_RDS));
            if (band.getDemodulator() == Demodulator.NFM && (boolean) band.getSetting(Setting.C_APRS)) startAPRS();
            else stopAPRS();
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
                case E_GAIN -> receiver.setGain((int) band.getSetting(set));
                case D_RDS -> {
                    resetTransientData();
                    receiver.setRDS((boolean) band.getSetting(set));
                }
                case F_DEEMP -> receiver.setDeemphasis((int) band.getSetting(set));
                case B_STEREO -> receiver.setStereo((boolean) band.getSetting(set));
                case C_APRS -> {
                    resetTransientData();
                    if ((boolean) band.getSetting(Setting.C_APRS)) startAPRS();
                    else stopAPRS();
                }
                default -> {}
            }
        }
        updateDisplay();
    }
}
