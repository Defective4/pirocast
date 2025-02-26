package io.github.defective4.rpi.pirocast.ext;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import io.github.defective4.rpi.pirocast.LogManager;
import io.github.defective4.rpi.pirocast.LogManager.LogLevel;

public class FFMpegPlayer {

    public static interface TrackListener {
        void ffmpegTerminated(int code);

        void trackEnded();
    }

    private final TrackListener ls;
    private Process process;
    private Thread readerThread;
    private SourceDataLine sdl;

    public FFMpegPlayer(TrackListener ls) {
        this.ls = ls;
    }

    public void start(File file) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        start(file.toString());
    }

    public void start(URL url) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        start(url.toString());
    }

    public void stop() {
        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
        if (sdl != null) {
            sdl.close();
            sdl = null;
        }
    }

    private void start(String source) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        if (readerThread != null) return;
        ProcessBuilder builder = new ProcessBuilder("ffmpeg", "-i", source, "-f", "wav", "-");
        builder = LogManager.redirectProcess(builder, "ffmpeg", LogLevel.ERRORS);
        process = builder.start();
        AudioInputStream in = AudioSystem.getAudioInputStream(process.getInputStream());
        sdl = AudioSystem.getSourceDataLine(in.getFormat());
        sdl.open();
        sdl.start();
        readerThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[sdl.getBufferSize()];
                int read;
                while (true) {
                    read = in.read(buffer, 0, buffer.length);
                    if (read <= 0) break;
                    sdl.write(buffer, 0, read);
                }
            } catch (IOException e) {}
            if (process != null) try {
                int code = process.waitFor();
                if (code == 0) ls.trackEnded();
                else ls.ffmpegTerminated(code);
            } catch (Exception e) {}
        });
        readerThread.start();
    }
}
