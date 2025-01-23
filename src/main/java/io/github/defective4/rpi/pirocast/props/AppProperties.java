package io.github.defective4.rpi.pirocast.props;

import java.util.Properties;

public class AppProperties extends Properties {
    protected String controllerPort = "55555";
    protected String rdsPort = "55556";
    protected String receiverExecutablePath = "./src/main/grc/receiver.py";

    public int getControllerPort() {
        return Integer.parseInt(controllerPort);
    }

    public int getRdsPort() {
        return Integer.parseInt(rdsPort);
    }

    public String getReceiverExecutablePath() {
        return receiverExecutablePath;
    }

}
