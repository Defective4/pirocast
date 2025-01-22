package io.github.defective4.rpi.pirocast.ext;

import java.io.IOException;

import io.github.defective4.rpi.pirocast.Band;
import io.github.defective4.rpi.pirocast.Demodulator;
import io.github.defective4.rpi.pirocast.settings.Setting;
import io.github.defective4.sdr.msg.MessagePair;
import io.github.defective4.sdr.msg.RawMessageSender;

public class RadioReceiver {
    private final RawMessageSender controller;
    private final int controllerPort;
    private Process process;
    private final String receiverPath;

    public RadioReceiver(int controllerPort, String receiverPath) {
        this.controllerPort = controllerPort;
        this.receiverPath = receiverPath;
        controller = new RawMessageSender("tcp://0.0.0.0:" + controllerPort, true);
    }

    public void initDefaultSettings(Band band) {
        setDemodulator(band.getDemodulator());
        setGain((int) band.getSetting(Setting.D_GAIN));
        setRDS((boolean) band.getSetting(Setting.C_RDS));
        setDeemphasis((int) band.getSetting(Setting.E_DEEMP));
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public void setCenterFrequency(double freq) {
        controller.sendMessage(new MessagePair("center_freq", freq));
    }

    public void setDeemphasis(int value) {
        controller.sendMessage(new MessagePair("deemp", value * 25 * 1e-5));
    }

    public void setDemodFrequency(double freq) {
        controller.sendMessage(new MessagePair("demod_freq", freq));
    }

    public void setDemodulator(Demodulator demodulator) {
        controller.sendMessage(new MessagePair("demod", demodulator.getId()));
    }

    public void setGain(int value) {
        controller.sendMessage(new MessagePair("gain", value));
    }

    public void setRDS(boolean enableRDS) {
        controller.sendMessage(new MessagePair("enable_rds", enableRDS ? 1 : 0));
    }

    public void start() throws IOException {
        controller.start();
        process = new ProcessBuilder("python3", receiverPath, "-a", "tcp://localhost:" + controllerPort).start();
    }

    public void stop() {
        try {
            controller.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (process != null) process.destroyForcibly();
        process = null;
    }
}
