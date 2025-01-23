package io.github.defective4.rpi.pirocast.ext;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class WaveResamplerServer {
    private Thread connectionThread;
    private final AudioFormat from, to;
    private final int port;
    private ServerSocket server;
    private OutputStream target;

    public WaveResamplerServer(int port, AudioFormat from, AudioFormat to) {
        this.port = port;
        this.from = from;
        this.to = to;
    }

    public void setTarget(OutputStream target) {
        this.target = target;
    }

    public void start() throws IOException {
        server = new ServerSocket(port);
        connectionThread = new Thread(() -> {
            while (!connectionThread.isInterrupted() && server != null && !server.isClosed()) {
                try (Socket client = server.accept();
                        AudioInputStream in = AudioSystem
                                .getAudioInputStream(to, new AudioInputStream(client.getInputStream(), from,
                                        AudioSystem.NOT_SPECIFIED))) {
                    byte[] buffer = new byte[1024];
                    while (!client.isClosed()) {
                        int read = in.read(buffer);
                        if (read <= 0) break;
                        if (target == null) continue;
                        target.write(buffer, 0, read);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        connectionThread.start();
    }

    public void stop() {
        if (server != null) try {
            server.close();
            server = null;
        } catch (Exception e) {}
        if (connectionThread != null) connectionThread.interrupt();
    }
}
