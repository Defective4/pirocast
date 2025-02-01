package io.github.defective4.rpi.pirocast.props;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
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

    private final File file;

    public AppProperties(File file) {
        this.file = file;
    }

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

    public void load() throws IOException {
        if (file.isFile()) {
            try (Reader reader = new FileReader(file)) {
                load(reader);
            }
            for (Field field : getClass().getDeclaredFields()) {
                try {
                    String val = getProperty(field.getName().replace('_', '.'));
                    if (val != null) {
                        if (field.getType() == String.class) field.set(this, val);
                        else if (field.getType() == int.class) field.set(this, Integer.parseInt(val));
                    }
                } catch (Exception e) {}
            }
        }
    }

    public void save() throws IOException {
        for (Field field : getClass().getDeclaredFields()) try {
            String newVal = null;
            Object fVal = field.get(this);
            if (fVal instanceof String str) newVal = str;
            else if (fVal instanceof Integer i) newVal = Integer.toString(i);
            if (newVal != null) setProperty(field.getName().replace('_', '.'), newVal);
        } catch (Exception e) {}

        try (Writer writer = new FileWriter(file)) {
            store(writer, null);
        }
    }

}
