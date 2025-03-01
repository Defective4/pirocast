package io.github.defective4.rpi.pirocast;

import static io.github.defective4.rpi.pirocast.ApplicationState.*;
import static io.github.defective4.rpi.pirocast.SoundEffectsPlayer.*;
import static io.github.defective4.rpi.pirocast.settings.Setting.BEEP;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.library.pigpio.PiGpio;
import com.pi4j.plugin.linuxfs.provider.i2c.LinuxFsI2CProvider;
import com.pi4j.plugin.pigpio.provider.gpio.digital.PiGpioDigitalInputProvider;
import com.pi4j.plugin.raspberrypi.platform.RaspberryPiPlatform;

import io.github.defective4.rpi.pirocast.FileManager.Mode;
import io.github.defective4.rpi.pirocast.display.I2CLcdDisplay;
import io.github.defective4.rpi.pirocast.display.SwingLcdDisplayEmulator;
import io.github.defective4.rpi.pirocast.display.TextDisplay;
import io.github.defective4.rpi.pirocast.ext.AUXLoopback;
import io.github.defective4.rpi.pirocast.ext.AUXLoopback.SampleRate;
import io.github.defective4.rpi.pirocast.ext.Direwolf;
import io.github.defective4.rpi.pirocast.ext.FFMpegPlayer;
import io.github.defective4.rpi.pirocast.ext.RadioReceiver;
import io.github.defective4.rpi.pirocast.ext.WaveResamplerServer;
import io.github.defective4.rpi.pirocast.input.Button;
import io.github.defective4.rpi.pirocast.input.GPIOInputManager;
import io.github.defective4.rpi.pirocast.input.InputAdapter;
import io.github.defective4.rpi.pirocast.input.InputManager;
import io.github.defective4.rpi.pirocast.input.SwingInputManager;
import io.github.defective4.rpi.pirocast.props.AppProperties;
import io.github.defective4.rpi.pirocast.props.DisplayAdapter;
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
    private final AUXLoopback auxLoopback = new AUXLoopback();
    private int bandIndex = 0;
    private final List<Source> bands;
    private float centerFrequency = 0;
    private final TextDisplay display;
    private final FFMpegPlayer ffmpeg;
    private final FileManager fileManager;
    private long filePlayerStartTime;
    private int fileScrollIndex = 0;
    private final InputManager inputManager;
    private int lastNavButtonDirection;
    private long lastNavButtonPress;
    private long lastUpdated;
    private boolean mediaError;

    private long offLightTime = 0;
    private float offsetFrequency = 0;
    private final AppProperties properties;
    private String rdsRadiotext, rdsStation;
    private int rdsRadiotextScrollIndex = 0;
    private boolean rdsSignal, ta, tp, rdsStereo;
    private final RadioReceiver receiver;
    private int settingIndex = 0;
    private ApplicationState state = OFF;

    private final DateFormat timeFormat, dateFormat;
    private final Timer uiTimer = new Timer(true);

    public Pirocast(List<Source> bands, AppProperties properties) {
        if (bands.isEmpty()) throw new IllegalArgumentException("Band list cannot be empty");
        Objects.requireNonNull(properties);
        Locale loc = properties.getDateTimeLocale();
        timeFormat = new SimpleDateFormat(properties.getTimeFormat(), loc);
        dateFormat = new SimpleDateFormat(properties.getDateFormat(), loc);
        this.bands = bands;
        this.properties = properties;
        ffmpeg = new FFMpegPlayer(new FFMpegPlayer.TrackListener() {

            @Override
            public void ffmpegTerminated(int code) {
                if (getCurrentSource().getMode() == SignalMode.FILE
                        || getCurrentSource().getMode() == SignalMode.NETWORK) {
                    mediaError = true;
                }
                updateDisplay();
            }

            @Override
            public void trackEnded() {
                Source src = getCurrentSource();
                if (src.getMode() == SignalMode.FILE) {
                    FileManager.Mode mode = (FileManager.Mode) src.getSetting(Setting.PLAYER_MODE);
                    if (mode == Mode.SHUFFLE) {
                        fileManager.nextRandomFile();
                    } else {
                        fileManager.nextFile(mode == Mode.REPEAT_ONE ? 0 : 1);
                    }
                    updateDisplay();
                }
            }
        });
        fileManager = new FileManager((index, file) -> {
            fileScrollIndex = 0;
            if (getCurrentSource().getMode() == SignalMode.FILE) {
                ffmpeg.stop();
                mediaError = false;
                if (file != null) {
                    try {
                        filePlayerStartTime = System.currentTimeMillis();
                        ffmpeg.start(file);
                    } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
                        raiseMediaError(e);
                    }
                }
            }
        });
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
                            if ((boolean) getCurrentSource().getSetting(Setting.STEREO)) {
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
            ffmpeg.stop();
        }));
        centerFrequency = bands.get(0).getDefaultFreq();
        Context ctx = null;
        switch (properties.getDisplayAdapter()) {
            default:
            case SWING: {
                display = new SwingLcdDisplayEmulator(properties.getDisplayColumns(), properties.getDisplayRows());
                break;
            }
            case I2C: {
                ctx = createPiContext();
                display = new I2CLcdDisplay(ctx, properties.getDisplayRows(), properties.getDisplayColumns());
                break;
            }
        }
        switch (properties.getInputAdapter()) {
            case GPIO: {
                if (ctx == null) ctx = createPiContext();
                inputManager = new GPIOInputManager(properties.getLongClickLength(), properties.getGpioInputPrev(),
                        properties.getGpioInputOk(), properties.getGpioInputNext(), ctx);
                break;
            }
            default:
            case SWING: {
                if (properties.getDisplayAdapter() != DisplayAdapter.SWING)
                    throw new IllegalStateException("Swing input adapter is only compatible with Swing display");
                inputManager = new SwingInputManager((Window) display, properties.getLongClickLength(),
                        KeyEvent.VK_LEFT, KeyEvent.VK_ENTER, KeyEvent.VK_RIGHT);
                break;
            }
        }
        display.setDisplayBacklight(false);
        display.createCharacter(1, new byte[] {
                0b01110, 0b00100, 0b00100, 0b00000, 0b00100, 0b01010, 0b01110, 0b01010
        });
        display.createCharacter(2, new byte[] {
                0b01110, 0b00100, 0b00100, 0b00000, 0b01100, 0b01010, 0b01100, 0b01000
        });
        display.showDisplay();
        inputManager.putInputListener(Button.NEXT, new InputAdapter() {

            @Override
            public void buttonClicked() {
                if (state == SETTINGS) {
                    updateSettingValue(1);
                    playClick();
                } else if (state == OFF) {
                    offLightTime = System.currentTimeMillis();
                    display.setDisplayBacklight(!display.getDisplayBacklight());
                }
                tune(1);
            }

            @Override
            public void buttonPressed() {
                lastNavButtonDirection = 1;
                lastNavButtonPress = System.currentTimeMillis();
            }

            @Override
            public void buttonReleased() {
                lastNavButtonDirection = 0;
            }
        });
        inputManager.putInputListener(Button.PREV, new InputAdapter() {

            @Override
            public void buttonClicked() {
                if (state == SETTINGS) {
                    updateSettingValue(-1);
                    playClick();
                } else if (state == OFF) {
                    offLightTime = System.currentTimeMillis();
                    display.setDisplayBacklight(!display.getDisplayBacklight());
                }
                tune(-1);
            }

            @Override
            public void buttonPressed() {
                lastNavButtonDirection = -1;
                lastNavButtonPress = System.currentTimeMillis();
            }

            @Override
            public void buttonReleased() {
                lastNavButtonDirection = 0;
            }
        });
        inputManager.putInputListener(Button.OK, new InputAdapter() {
            @Override
            public void buttonClicked() {
                switch (state) {
                    case OFF -> {
                        offLightTime = System.currentTimeMillis();
                        display.setDisplayBacklight(!display.getDisplayBacklight());
                    }
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
                    case MAIN, ERROR -> {
                        stop();
                        playLongClick();
                    }
                    case SETTINGS -> {
                        state = MAIN;
                        updateDisplay();
                        playLongClick();
                    }
                    default -> {}
                }
            }

            @Override
            public void buttonPressed() {
                if (state != OFF) playClick();
            }

        });
        uiTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                Source src = getCurrentSource();
                if (src.getMode() == SignalMode.FILE) {
                    File srcDir = new File(src.getExtra());
                    File current = fileManager.getSelectedFile();
                    if (!srcDir.isDirectory() || fileManager.isMissingDirectory()) fileManager.listAudioFiles(srcDir);
                    if (current != null) {
                        String name = current.getName();
                        int dotIndex = name.lastIndexOf('.');
                        if (dotIndex >= 0) name = name.substring(0, dotIndex);
                        fileScrollIndex += properties.getFileNameScrollSpeed();
                        if (name.length() - fileScrollIndex < display.getColumns() - 4) fileScrollIndex = 0;
                    }
                } else if (src.getMode().getId() != SignalMode.UNDEFINED_ID && !receiver.isAlive()) {
                    raiseMediaError(null);
                    receiver.stop();
                }
                updateDisplay(true);
                if (rdsRadiotext != null) {
                    rdsRadiotextScrollIndex += properties.getRdsScrollSpeed();
                    if (rdsRadiotext.length() - rdsRadiotextScrollIndex < display.getColumns())
                        rdsRadiotextScrollIndex = 0;
                }
                if (!aprsQueue.isEmpty()) {
                    String element = aprsQueue.peek();
                    aprsScrollIndex += properties.getAprsScrollSpeed();
                    if (element.length() - aprsScrollIndex < display.getColumns()) {
                        aprsQueue.poll();
                        aprsScrollIndex = 0;
                    }
                }
            }
        }, 1000, 1000);
        uiTimer.scheduleAtFixedRate(new TimerTask() {

            private boolean tuning;

            @Override
            public void run() {
                if (getCurrentSource().getMode().getId() == SignalMode.UNDEFINED_ID) return;
                if (state == MAIN && lastNavButtonDirection != 0
                        && System.currentTimeMillis() - lastNavButtonPress > properties.getLongClickLength()) {
                    if (!tuning) {
                        tuning = true;
                        playLongClick();
                        receiver.setMuted(true);
                    }
                    tune(lastNavButtonDirection, false);
                } else if (tuning) {
                    tuning = false;
                    receiver.setCenterFrequency(centerFrequency);
                    receiver.setDemodFrequency(offsetFrequency);
                    receiver.setMuted(false);
                }
            }
        }, 0, 80);
    }

    public float getCurrentFrequency() {
        return centerFrequency + offsetFrequency;
    }

    public Setting getCurrentSetting() {
        List<Setting> settings = new ArrayList<>();
        settings.add(Setting.SOURCE);
        settings.addAll(getCurrentSource().getSettings());
        return settings.get(settingIndex % settings.size());
    }

    public Source getCurrentSource() {
        return bands.get(bandIndex);
    }

    public float getTuningStep() {
        Source band = getCurrentSource();
        return Setting.TUNING_STEP.isApplicable(band.getMode()) ? (int) band.getSetting(Setting.TUNING_STEP) * 1e3f
                : 100e3f;
    }

    public void setFrequency(float freq) {
        setFrequency(freq, true);
    }

    public void setFrequency(float freq, boolean commit) {
        Source band = getCurrentSource();
        if (freq < band.getMinFreq()) freq = band.getMaxFreq();
        if (freq > band.getMaxFreq()) freq = band.getMinFreq();
        float diff = Math.abs(freq - centerFrequency);
        if (diff > 1e6) {
            offsetFrequency = 0;
            centerFrequency = freq;
        } else {
            offsetFrequency = freq - centerFrequency;
        }
        if (commit) {
            receiver.setDemodFrequency(offsetFrequency);
            receiver.setCenterFrequency(centerFrequency);
        }
        resetTransientData();
        updateDisplay();
    }

    public void start() {
        try {
            Source band = getCurrentSource();
            display.setDisplayBacklight(true);
            state = MAIN;
            mediaError = false;
            switch (band.getMode()) {
                case FILE -> fileManager.listAudioFiles(new File(band.getExtra()));
                case NETWORK -> {
                    try {
                        ffmpeg.start(new URI(band.getExtra()).toURL());
                    } catch (Exception e) {
                        raiseMediaError(e);
                    }
                }
                case AUX -> {
                    try {
                        auxLoopback.setSampleRate(((SampleRate) band.getSetting(Setting.SAMPLERATE)).getFreq(), false);
                        auxLoopback.start();
                    } catch (Exception e) {
                        raiseMediaError(e);
                    }
                }
                default -> {
                    try {
                        receiver.start();
                        receiver.initDefaultSettings(band);
                    } catch (Exception e) {
                        raiseMediaError(e);
                        receiver.stop();
                    }
                }
            }

            SoundEffectsPlayer.setEnabled((boolean) band.getSetting(BEEP));
            if (band.getMode() == SignalMode.NFM && (boolean) band.getSetting(Setting.APRS)) startAPRS();
            aprsResampler.start();
            resetTransientData();

            setFrequency(band.getLastFrequency());
        } catch (Exception e) {
            raiseError(e);
        }
        updateDisplay();
    }

    public void stop() {
        getCurrentSource().setLastFrequency(getCurrentFrequency());
        state = OFF;
        receiver.stop();
        auxLoopback.close();
        ffmpeg.stop();
        stopAPRS();
        aprsResampler.stop();
        updateDisplay();
    }

    private void nextSetting() {
        settingIndex++;
        Source src = getCurrentSource();
        Setting set = getCurrentSetting();
        switch (set) {
            case APRS:
                if (!src.isAPRSAllowed()) settingIndex++;
                break;
            case RDS:
                if (!src.isRDSAllowed()) settingIndex++;
                break;
            default:
                break;
        }
        if (settingIndex > src.getSettings().size()) settingIndex = 0;
    }

    private void raiseError(Exception e) {
        e.printStackTrace();
        stop();
        state = ERROR;
        display.setDisplayBacklight(true);
    }

    private void raiseMediaError(Exception e) {
        if (e != null) e.printStackTrace();
        mediaError = true;
        updateDisplay();
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
        if ((boolean) getCurrentSource().getSetting(Setting.STEREO)) receiver.setStereo(false);
    }

    private void startAPRS() {
        aprsDecoder.start();
        aprsResampler.setTarget(aprsDecoder.getOutputStream());
        receiver.setAPRS(true);
    }

    private void stopAPRS() {
        try {
            receiver.setAPRS(false);
        } catch (Exception e) {}
        aprsDecoder.stop();
        aprsResampler.setTarget(null);
    }

    private void tune(int direction) {
        tune(direction, true);
    }

    private void tune(int direction, boolean commit) {
        if (state == MAIN) {
            Source src = getCurrentSource();
            SignalMode mode = src.getMode();
            if (mode.getId() != SignalMode.UNDEFINED_ID && !mediaError && receiver.isReady())
                setFrequency(getCurrentFrequency() + getTuningStep() * direction, commit);
            else if (mode == SignalMode.FILE) {
                if ((FileManager.Mode) src.getSetting(Setting.PLAYER_MODE) == Mode.SHUFFLE)
                    fileManager.nextRandomFile();
                else fileManager.nextFile(direction);
                updateDisplay();
            }
            if (commit) playClick();
        }
    }

    private void updateDisplay() {
        updateDisplay(false);
    }

    private void updateDisplay(boolean auto) {
        if (display instanceof I2CLcdDisplay) {
            if (!auto) {
                if (System.currentTimeMillis() - lastUpdated < properties.getDisplayGuardInterval()) return;
            } else if (System.currentTimeMillis() - lastUpdated < 900) return;
        }
        lastUpdated = System.currentTimeMillis();
        switch (state) {
            case ERROR -> {
                display.centerTextInLine("System", 0);
                display.centerTextInLine("Error", 1);
            }
            case SETTINGS -> {
                Setting setting = getCurrentSetting();
                display.centerTextInLine(setting.getName(), 0);
                String value;
                if (setting == Setting.SOURCE) {
                    value = getCurrentSource().getName();
                } else {
                    value = setting.getFormatter().format(getCurrentSource().getSetting(setting));
                }
                StringBuilder builder = display.generateCenteredText(value);
                builder.setCharAt(0, '<');
                builder.setCharAt(builder.length() - 1, '>');
                display.centerTextInLine(setting.getName(), 0);
                display.displayLineOfText(builder.toString(), 1);
            }
            case OFF -> {
                if (display.getDisplayBacklight()
                        && System.currentTimeMillis() - offLightTime > properties.getStandbyDisplayLinger()) {
                    display.clearDisplay();
                    display.setDisplayBacklight(false);
                }
                Date now = new Date(System.currentTimeMillis());
                display.centerTextInLine(timeFormat.format(now), 0);
                display.centerTextInLine(dateFormat.format(now), 1);
            }
            case MAIN -> {
                SignalMode mode = getCurrentSource().getMode();
                float freq = getCurrentFrequency();
                String freqS = freq <= 1e6 ? Double.toString(getCurrentFrequency() / 1e3) + " KHz"
                        : Double.toString(getCurrentFrequency() / 1e6) + " MHz";
                if (rdsSignal && mode == SignalMode.FM) freqS = freqS + "*";
                StringBuilder line2 = display.generateCenteredText(freqS);
                line2.setCharAt(0, '<');
                line2.setCharAt(line2.length() - 1, '>');
                if (mediaError) line2 = display.generateCenteredText("ERROR");
                else if (!receiver.isReady()) line2 = display.generateCenteredText("Preparing...");
                StringBuilder line1 = display.generateCenteredText(mode.name());

                switch (mode) {
                    case NFM -> {
                        if (!aprsQueue.isEmpty()) {
                            line1 = new StringBuilder(aprsQueue.peek().substring(aprsScrollIndex));
                        }
                    }
                    case FM -> {
                        if (rdsSignal) {
                            if (rdsStation != null) {
                                line1 = display.generateCenteredText(rdsStation);
                            }

                            if (rdsRadiotext != null) {
                                StringBuilder rtx = new StringBuilder(rdsRadiotext.substring(rdsRadiotextScrollIndex));
                                if (rdsStation == null) line1 = rtx;
                                else line2 = rtx;
                            }

                            if (rdsStation != null || rdsRadiotext == null) {
                                if (rdsStereo) line1.setCharAt(0, 'S');
                                if (ta) line1.setCharAt(line1.length() - 2, '\1');
                                if (tp) line1.setCharAt(line1.length() - 1, '\2');
                            }
                        }
                    }
                    case FILE -> {
                        if (mediaError) {
                            line2 = display.generateCenteredText("Media Error");
                        } else if (fileManager.hasFiles()) {
                            String fileName = fileManager.getSelectedFile().getName();
                            int dotIndex = fileName.lastIndexOf('.');
                            if (dotIndex >= 0) fileName = fileName.substring(0, dotIndex);
                            line1 = display
                                    .generateCenteredText(fileName
                                            .substring(fileScrollIndex,
                                                    Math
                                                            .min(fileName.length(),
                                                                    fileScrollIndex + display.getColumns() - 2)));

                            switch (properties.getAudioPlayerTimerStyle()) {
                                case TIMER: {
                                    line2 = display
                                            .generateCenteredText(
                                                    formatFileTime(System.currentTimeMillis() - filePlayerStartTime));
                                    break;
                                }
                                case COUNTDOWN: {
                                    String str = "??:??";
                                    if (ffmpeg.getLastFileDuration() != -1) {
                                        long dur = ffmpeg.getLastFileDuration()
                                                - (System.currentTimeMillis() - filePlayerStartTime);
                                        str = formatFileTime(dur);
                                    }
                                    line2 = display.generateCenteredText(str);
                                    break;
                                }
                                default:
                                case CLASSIC: {
                                    long currentDur = System.currentTimeMillis() - filePlayerStartTime;
                                    String seekStr = formatFileTime(currentDur);
                                    String durStr = "??:??";
                                    if (ffmpeg.getLastFileDuration() != -1)
                                        durStr = formatFileTime(ffmpeg.getLastFileDuration());

                                    line2 = display.generateCenteredText(seekStr + " / " + durStr);
                                    break;
                                }
                            }

                            line2.setCharAt(0, '<');
                            line2.setCharAt(line2.length() - 1, '>');
                        } else {
                            line2 = display
                                    .generateCenteredText(fileManager.isMissingDirectory() ? "No Media" : "No File");
                        }
                    }
                    case AUX -> {
                        line2 = mediaError ? display.generateCenteredText("ERROR") : new StringBuilder();
                        line1 = display.generateCenteredText("AUX In");
                    }
                    case NETWORK -> {
                        line1 = display.generateCenteredText("Internet Radio");
                        line2 = display.generateCenteredText(mediaError ? "Media Error" : getCurrentSource().getName());
                    }
                    default -> {}
                }

                display.displayLineOfText(line1.toString(), 0);
                display.displayLineOfText(line2.toString(), 1);
            }
            default -> {}
        }
    }

    private void updateSettingValue(int direction) {
        Setting set = getCurrentSetting();
        if (set == Setting.SOURCE) {
            mediaError = false;
            getCurrentSource().setLastFrequency(getCurrentFrequency());
            bandIndex += direction;
            if (bandIndex < 0) bandIndex = bands.size() - 1;
            if (bandIndex >= bands.size()) bandIndex = 0;
            Source band = getCurrentSource();
            if (band.getMode().getId() == SignalMode.UNDEFINED_ID) {
                receiver.stop();
                ffmpeg.stop();
                auxLoopback.close();
                switch (band.getMode()) {
                    case FILE -> {
                        try {
                            fileManager.listAudioFiles(new File(band.getExtra()));
                        } catch (Exception e) {
                            raiseMediaError(e);
                        }
                    }
                    case NETWORK -> {
                        try {
                            ffmpeg.start(new URI(band.getExtra()).toURL());
                        } catch (Exception e) {
                            raiseMediaError(e);
                        }
                    }
                    case AUX -> {
                        try {
                            auxLoopback
                                    .setSampleRate(((SampleRate) band.getSetting(Setting.SAMPLERATE)).getFreq(), false);
                            auxLoopback.start();
                        } catch (Exception e) {
                            raiseMediaError(e);
                        }
                    }
                    default -> {}
                }

            } else {
                auxLoopback.close();
                ffmpeg.stop();
                try {
                    receiver.start();
                } catch (Exception e) {
                    raiseMediaError(e);
                    receiver.stop();
                    return;
                }
                receiver.initDefaultSettings(band);
                setFrequency(band.getLastFrequency());
                receiver.setRDS(band.getMode() == SignalMode.FM && (boolean) band.getSetting(Setting.RDS));
            }
            SoundEffectsPlayer.setEnabled((boolean) band.getSetting(BEEP));
            if (band.getMode() == SignalMode.NFM && (boolean) band.getSetting(Setting.APRS)) startAPRS();
            else stopAPRS();
            SoundEffectsPlayer.setEnabled((boolean) band.getSetting(Setting.BEEP));
        } else {
            Source band = getCurrentSource();
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
            } else if (currentVal instanceof Enum<?> en) {
                Enum<?>[] consts = en.getDeclaringClass().getEnumConstants();
                int index = -1;
                for (int i = 0; i < consts.length; i++) {
                    if (consts[i] == en) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    index += direction;
                    if (index >= consts.length) index = 0;
                    if (index < 0) index = consts.length - 1;
                    band.setSetting(set, consts[index]);
                }
            }

            switch (set) {
                case SAMPLERATE -> {
                    try {
                        mediaError = false;
                        auxLoopback.setSampleRate(((SampleRate) band.getSetting(set)).getFreq(), true);
                    } catch (Exception e) {
                        raiseMediaError(e);
                    }
                }
                case BEEP -> SoundEffectsPlayer.setEnabled((boolean) band.getSetting(set));
                case GAIN -> receiver.setGain((int) band.getSetting(set));
                case RDS -> {
                    resetTransientData();
                    receiver.setRDS((boolean) band.getSetting(set));
                }
                case DEEMP -> receiver.setDeemphasis((int) band.getSetting(set));
                case STEREO -> receiver.setStereo((boolean) band.getSetting(set));
                case APRS -> {
                    resetTransientData();
                    if ((boolean) band.getSetting(Setting.APRS)) startAPRS();
                    else stopAPRS();
                }
                default -> {}
            }
        }
        updateDisplay();
    }

    private static Context createPiContext() {
        PiGpio pigpio = PiGpio.newNativeInstance();
        return Pi4J.newContextBuilder().noAutoDetect().add(new RaspberryPiPlatform() {
            @Override
            protected String[] getProviders() {
                return new String[] {};
            }
        }).add(PiGpioDigitalInputProvider.newInstance(pigpio), LinuxFsI2CProvider.newInstance()).build();
    }

    private static String formatFileTime(long dur) {
        int min = (int) Math.floor(dur / 1000 / 60);
        int sec = (int) Math.floor(dur / 1000) % 60;

        String minStr = Integer.toString(min);
        String secStr = Integer.toString(sec);

        if (minStr.length() < 2) minStr = "0" + minStr;
        if (secStr.length() < 2) secStr = "0" + secStr;

        return minStr + ":" + secStr;
    }
}
