package io.github.defective4.rpi.pirocast.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

    private long lastFileDuration;
    private final TrackListener ls;
    private Process process;
    private Thread readerThread, errorReaderThread;

    private SourceDataLine sdl;

    public FFMpegPlayer(TrackListener ls) {
        this.ls = ls;
    }

    public long getLastFileDuration() {
        return lastFileDuration;
    }

    public void start(File file) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        start(file.toString());
    }

    public void start(URL url) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        start(url.toString());
    }

    public void stop() {
        lastFileDuration = -1;
        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
        if (errorReaderThread != null) {
            errorReaderThread.interrupt();
            errorReaderThread = null;
        }
        if (sdl != null) {
            sdl.close();
            sdl = null;
        }
    }

    private void start(String source) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        if (readerThread != null) return;
        process = new ProcessBuilder("ffmpeg", "-i", source, "-f", "wav", "-").start();
        AudioInputStream in = AudioSystem.getAudioInputStream(process.getInputStream());
        sdl = AudioSystem.getSourceDataLine(in.getFormat());
        sdl.open();
        sdl.start();
        lastFileDuration = -1;
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
        errorReaderThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    PrintWriter logWriter = LogManager.prepareLogWriter("ffmpeg", LogLevel.ERRORS)) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    if (line.trim().startsWith("Duration: ") && lastFileDuration == -1) {
                        String durString = line.trim().substring(10);
                        int index = durString.indexOf(',');
                        if (index != -1) {
                            durString = durString.substring(0, index);
                        }
                        String[] parts = durString.split(":");
                        long time = -1;
                        if (parts.length > 1) {
                            String partOne = parts[parts.length - 1];
                            int dotIndex = partOne.indexOf('.');
                            if (dotIndex != -1) partOne = partOne.substring(0, dotIndex);
                            int secs = Integer.parseInt(partOne);
                            int mins = Integer.parseInt(parts[parts.length - 2]);
                            int hrs = 0;
                            if (parts.length > 2) hrs = Integer.parseInt(parts[parts.length - 3]);
                            time = 0;
                            time += secs * 1000;
                            time += mins * 1000 * 60;
                            time += hrs * 1000 * 60 * 60;
                        }
                        lastFileDuration = time;
                    }
                    logWriter.println(line);
                }
            } catch (Exception e) {}
        });
        errorReaderThread.start();
        readerThread.start();
    }

}
