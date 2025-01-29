package io.github.defective4.rpi.pirocast.ext;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class AUXLoopback {
    public enum SampleRate {
        F44(44100, "44.1"), F48(48000, "48");

        private final float freq;
        private final String name;

        private SampleRate(float freq, String name) {
            this.name = name;
            this.freq = freq;
        }

        public float getFreq() {
            return freq;
        }

        public String getName() {
            return name;
        }
    }

    private AudioFormat audioFormat;
    private Thread bridgeThread;
    private SourceDataLine sdl;
    private TargetDataLine tdl;

    public void close() {
        if (bridgeThread != null) {
            bridgeThread.interrupt();
            bridgeThread = null;
        }
        if (sdl != null) try {
            sdl.close();
            sdl = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (tdl != null) try {
            tdl.close();
            tdl = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSampleRate(float rate, boolean restart) throws LineUnavailableException {
        audioFormat = new AudioFormat(rate, 16, 2, true, false);
        if (!restart) return;
        close();
        start();
    }

    public void start() throws LineUnavailableException {
        if (bridgeThread != null) return;
        if (audioFormat == null) setSampleRate(44100, false);
        sdl = AudioSystem.getSourceDataLine(audioFormat);
        tdl = AudioSystem.getTargetDataLine(audioFormat);
        sdl.open();
        sdl.start();
        tdl.open();
        tdl.start();
        bridgeThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    if (tdl != null) {
                        int read = tdl.read(buffer, 0, buffer.length);
                        if (read <= 0) break;
                        if (sdl != null) sdl.write(buffer, 0, read);
                    }
                }
            } catch (Exception e) {}
        });
        bridgeThread.start();
    }
}
