package io.github.defective4.rpi.pirocast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class SoundEffectsPlayer {

    private static final byte[] clickData, longClickData;
    private static byte[] currentSound;
    private static boolean enabled;
    private static final Object lock = new Object();
    private static SourceDataLine sdl;
    private static boolean sdlError;

    private static final AudioFormat SFX_FORMAT = new AudioFormat(44100, 16, 1, true, false);

    static {
        clickData = loadData("/sfx/click.wav");
        longClickData = loadData("/sfx/click_long.wav");
        try {
            new Timer(true).scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    byte[] data;
                    synchronized (lock) {
                        data = currentSound;
                        currentSound = null;
                    }
                    if (data != null) {
                        if (sdl == null && !sdlError && enabled) try {
                            sdl = AudioSystem.getSourceDataLine(SFX_FORMAT);
                            sdl.open();
                            sdl.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                            sdlError = true;
                        }
                        if (sdl != null && sdl.isOpen()) sdl.write(data, 0, data.length);
                    }
                }
            }, 0, 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SoundEffectsPlayer() {}

    public static void playClick() {
        playSound(clickData);
    }

    public static void playLongClick() {
        playSound(longClickData);
    }

    public static void setEnabled(boolean enabled) {
        SoundEffectsPlayer.enabled = enabled;
        if (!enabled && sdl != null) {
            sdl.close();
            sdl = null;
        }
    }

    private static byte[] loadData(String resource) {
        try (InputStream in = AudioSystem.getAudioInputStream(SoundEffectsPlayer.class.getResourceAsStream(resource));
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] tmp = new byte[1024];
            while (true) {
                int read = in.read(tmp);
                if (read <= 0) break;
                buffer.write(tmp, 0, read);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void playSound(byte[] data) {
        if (!enabled) return;
        synchronized (lock) {
            currentSound = data;
        }
    }
}
