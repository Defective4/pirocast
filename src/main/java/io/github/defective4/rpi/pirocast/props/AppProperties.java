package io.github.defective4.rpi.pirocast.props;

import java.util.Properties;

public class AppProperties extends Properties {
    protected int aprsResamplerPort = 55557;
    protected int controllerPort = 55555;
    protected int rdsPort = 55556;
    protected String receiverExecutablePath = "./src/main/grc/receiver.py";

    public int getAprsResamplerPort() {
        return aprsResamplerPort;
    }

    public int getControllerPort() {
        return controllerPort;
    }

    public int getRdsPort() {
        return rdsPort;
    }

    public String getReceiverExecutablePath() {
        return receiverExecutablePath;
    }

}
