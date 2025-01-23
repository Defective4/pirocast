package io.github.defective4.rpi.pirocast.ext;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class AUXLoopback {
    private static final AudioFormat AUX_FORMAT = new AudioFormat(44100, 16, 2, true, false);
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

    public void start() throws LineUnavailableException {
        if (bridgeThread != null) return;
        sdl = AudioSystem.getSourceDataLine(AUX_FORMAT);
        tdl = AudioSystem.getTargetDataLine(AUX_FORMAT);
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
