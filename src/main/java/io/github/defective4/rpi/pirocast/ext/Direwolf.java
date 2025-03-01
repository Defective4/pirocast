package io.github.defective4.rpi.pirocast.ext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import io.github.defective4.rpi.pirocast.LogManager;
import io.github.defective4.rpi.pirocast.LogManager.LogLevel;

public class Direwolf {

    public static interface APRSListener {
        void received(String line);
    }

    private boolean kissReady, agwReady;
    private final APRSListener listener;

    private Process process;
    private Thread readerThread;

    public Direwolf(APRSListener listener) {
        this.listener = listener;
    }

    public OutputStream getOutputStream() {
        if (!isAlive()) return null;
        return process.getOutputStream();
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public void start() {
        if (isAlive()) return;
        kissReady = false;
        agwReady = false;
        try {
            ProcessBuilder builder = new ProcessBuilder("direwolf", "-c", "/dev/null", "-qhx", "-t", "0", "-a", "0",
                    "-r", "48000", "-n", "1", "-b", "16", "-");
            builder = LogManager.redirectProcess(builder, "direwolf", LogLevel.ERRORS);
            process = builder.start();
            readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        PrintWriter logWriter = LogManager.prepareLogWriter("direwolf", LogLevel.ALL)) {
                    while (process.isAlive()) {
                        String line = reader.readLine();
                        if (line == null) break;
                        logWriter.println(line);
                        if (!line.isBlank()) {
                            if (kissReady && agwReady) {
                                if (!line.startsWith("[")) {
                                    listener.received(line);
                                }
                            } else if (line.startsWith("Ready to accept KISS TCP client application")) {
                                kissReady = true;
                            } else if (line.startsWith("Ready to accept AGW client application")) {
                                agwReady = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            readerThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (process != null) process.destroyForcibly();
        if (readerThread != null) readerThread.interrupt();
        process = null;
        readerThread = null;
    }
}
