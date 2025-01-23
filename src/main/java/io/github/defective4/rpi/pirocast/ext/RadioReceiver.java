package io.github.defective4.rpi.pirocast.ext;

import java.io.IOException;
import java.util.Objects;

import io.github.defective4.rpi.pirocast.Band;
import io.github.defective4.rpi.pirocast.Demodulator;
import io.github.defective4.rpi.pirocast.settings.Setting;
import io.github.defective4.sdr.msg.MessagePair;
import io.github.defective4.sdr.msg.RawMessageSender;
import io.github.defective4.sdr.rds.RDSListener;
import io.github.defective4.sdr.rds.RDSReceiver;

public class RadioReceiver {
    private RawMessageSender controller;
    private final int controllerPort;
    private Process process;
    private final RDSListener rdsListener;
    private final int rdsPort;
    private RDSReceiver rdsReceiver;
    private Thread rdsThread;
    private final String receiverPath;

    public RadioReceiver(int controllerPort, int rdsPort, String receiverPath, RDSListener rdsListener) {
        Objects.requireNonNull(receiverPath);
        Objects.requireNonNull(rdsListener);
        this.controllerPort = controllerPort;
        this.rdsPort = rdsPort;
        this.receiverPath = receiverPath;
        this.rdsListener = rdsListener;
    }

    public void initDefaultSettings(Band band) {
        setDemodulator(band.getDemodulator());
        setGain((int) band.getSetting(Setting.E_GAIN));
        setRDS((boolean) band.getSetting(Setting.D_RDS));
        setDeemphasis((int) band.getSetting(Setting.F_DEEMP));
        setStereo((boolean) band.getSetting(Setting.B_STEREO));
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public void resetRDS() {
        if (rdsReceiver != null) rdsReceiver.reset();
    }

    public void setAPRS(boolean aprs) {
        if (controller != null) controller.sendMessage(new MessagePair("aprs", aprs ? 1 : 0));
    }

    public void setCenterFrequency(double freq) {
        if (controller != null) controller.sendMessage(new MessagePair("center_freq", freq));
    }

    public void setDeemphasis(int value) {
        if (controller != null) controller.sendMessage(new MessagePair("deemp", value * 25 * 1e-5));
    }

    public void setDemodFrequency(double freq) {
        if (controller != null) controller.sendMessage(new MessagePair("demod_freq", freq));
    }

    public void setDemodulator(Demodulator demodulator) {
        if (controller != null) controller.sendMessage(new MessagePair("demod", demodulator.getId()));
    }

    public void setGain(int value) {
        if (controller != null) controller.sendMessage(new MessagePair("gain", value));
    }

    public void setRDS(boolean enableRDS) {
        if (controller != null) controller.sendMessage(new MessagePair("enable_rds", enableRDS ? 1 : 0));
        if (enableRDS) startRDS();
        else stopRDS();
    }

    public void setStereo(boolean stereo) {
        if (controller != null) controller.sendMessage(new MessagePair("fm_stereo", stereo ? 1 : 0));
    }

    public void start() throws IOException {
        if (isAlive()) return;
        controller = new RawMessageSender("tcp://0.0.0.0:" + controllerPort, true);
        controller.start();
        process = new ProcessBuilder("python3", receiverPath, "-a", "tcp://localhost:" + controllerPort, "-r",
                "tcp://localhost:" + rdsPort).start();
    }

    public void stop() {
        if (controller != null) try {
            controller.close();
            controller = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopRDS();
        if (process != null) process.destroyForcibly();
        process = null;
    }

    private void startRDS() {
        rdsReceiver = new RDSReceiver("tcp://0.0.0.0:" + rdsPort, true);
        rdsReceiver.addListener(rdsListener);
        rdsThread = new Thread(() -> {
            try {
                rdsReceiver.start();
            } catch (Exception e) {}
        });
        rdsThread.start();
    }

    private void stopRDS() {
        if (rdsReceiver != null) try {
            rdsReceiver.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (rdsThread != null) {
            rdsThread.interrupt();
            rdsThread = null;
        }
    }
}
