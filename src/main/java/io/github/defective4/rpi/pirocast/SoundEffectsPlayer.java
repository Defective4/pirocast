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
    private static final Object lock = new Object();
    private static final AudioFormat SFX_FORMAT = new AudioFormat(44100, 16, 1, true, false);

    static {
        clickData = loadData("/sfx/click.wav");
        longClickData = loadData("/sfx/click_long.wav");
        try {
            SourceDataLine sdl = AudioSystem.getSourceDataLine(SFX_FORMAT);
            sdl.open();
            sdl.start();
            new Timer(true).scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    byte[] data;
                    synchronized (lock) {
                        data = currentSound;
                        currentSound = null;
                    }
                    if (data != null) {
                        sdl.write(data, 0, data.length);
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
        synchronized (lock) {
            currentSound = data;
        }
    }
}
