package io.github.defective4.rpi.pirocast.props;

import java.util.Locale;
import java.util.Properties;

public class AppProperties extends Properties {
    protected int aprsResamplerPort = 55557;
    protected int controllerPort = 55555;
    protected String dateFormat = "d MMM YY";
    protected String dateTimeLocale = "default";
    protected int rdsPort = 55556;
    protected String receiverExecutablePath = "./src/main/grc/receiver.py";
    protected String timeFormat = "HH:mm:ss";

    protected int ui_aprsScrollSpeed = 1;
    protected int ui_fileNameScrollSpeed = 1;
    protected int ui_longClickLength = 500;
    protected int ui_rdsScrollSpeed = 1;
    protected int ui_standbyDisplayLinger = 5000;

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

    public Locale getDateTimeLocale() {
        return "default".equalsIgnoreCase(dateTimeLocale) ? Locale.getDefault(Locale.Category.FORMAT)
                : new Locale(dateTimeLocale);
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

    public int getStandbyDisplayLinger() {
        return ui_standbyDisplayLinger == 0 ? Integer.MAX_VALUE : ui_standbyDisplayLinger;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

}
