package io.github.defective4.rpi.pirocast.props;

import java.util.Properties;

public class AppProperties extends Properties {
    protected int aprsResamplerPort = 55557;
    protected int controllerPort = 55555;
    protected String dateFormat = "dd.MM.yyyy";
    protected int rdsPort = 55556;
    protected String receiverExecutablePath = "./src/main/grc/receiver.py";
    protected String timeFormat = "HH:mm:ss";

    protected int ui_aprsScrollSpeed = 1;
    protected int ui_fileNameScrollSpeed = 1;
    protected int ui_longClickLength = 500;
    protected int ui_rdsScrollSpeed = 1;

    public int getAprsResamplerPort() {
        return aprsResamplerPort;
    }

    public int getAprsScrollSpeed() {
        return ui_aprsScrollSpeed;
    }

    public int getControllerPort() {
        return controllerPort;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public int getFileNameScrollSpeed() {
        return ui_fileNameScrollSpeed;
    }

    public int getLongClickLength() {
        return ui_longClickLength;
    }

    public int getRdsPort() {
        return rdsPort;
    }

    public int getRdsScrollSpeed() {
        return ui_rdsScrollSpeed;
    }

    public String getReceiverExecutablePath() {
        return receiverExecutablePath;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

}
