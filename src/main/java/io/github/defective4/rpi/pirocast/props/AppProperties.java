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

import io.github.defective4.rpi.pirocast.LogManager.LogLevel;

public class AppProperties extends Properties {
    protected String date_format = "d MMM YY";
    protected String date_timeLocale = "default";
    protected String display_adapter = "i2c";
    protected int display_columns = 16;
    protected int display_rows = 2;
    protected int gpio_input_next = 0;
    protected int gpio_input_ok = 0;
    protected int gpio_input_prev = 0;

    protected int hardware_displayGuardInterval = 500;
    protected String input_adapter = "gpio";
    protected boolean logging_archive = true;
    protected String logging_directory = "logs";
    protected String logging_level = "Errors";

    protected int receiver_aprsResamplerPort = 55557;
    protected int receiver_controllerPort = 55555;
    protected String receiver_deviceArguments = "";
    protected String receiver_path = "./receiver.py";
    protected int receiver_rdsPort = 55556;

    protected boolean settings_persist = true;
    protected String time_format = "HH:mm:ss";
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
        return receiver_aprsResamplerPort;
    }

    public int getAprsScrollSpeed() {
        return ui_aprsScrollSpeed;
    }

    public int getControllerPort() {
        return receiver_controllerPort;
    }

    public String getDateFormat() {
        return date_format;
    }

    public Locale getDateTimeLocale() {
        return "default".equalsIgnoreCase(date_timeLocale) ? Locale.getDefault(Locale.Category.FORMAT)
                : new Locale(date_timeLocale);
    }

    public String getDeviceArguments() {
        return receiver_deviceArguments;
    }

    public DisplayAdapter getDisplayAdapter() {
        try {
            return DisplayAdapter.valueOf(display_adapter.toUpperCase());
        } catch (Exception e) {
            return DisplayAdapter.SWING;
        }
    }

    public int getDisplayColumns() {
        return display_columns;
    }

    public int getDisplayGuardInterval() {
        return hardware_displayGuardInterval;
    }

    public int getDisplayRows() {
        return display_rows;
    }

    public int getFileNameScrollSpeed() {
        return ui_fileNameScrollSpeed;
    }

    public int getGpioInputNext() {
        return gpio_input_next;
    }

    public int getGpioInputOk() {
        return gpio_input_ok;
    }

    public int getGpioInputPrev() {
        return gpio_input_prev;
    }

    public InputAdapter getInputAdapter() {
        try {
            return InputAdapter.valueOf(input_adapter.toUpperCase());
        } catch (Exception e) {
            return InputAdapter.SWING;
        }
    }

    public File getLoggingDirectory() {
        return new File(logging_directory);
    }

    public LogLevel getLogLevel() {
        try {
            return LogLevel.valueOf(logging_level.toUpperCase());
        } catch (Exception e) {
            return LogLevel.ERRORS;
        }
    }

    public int getLongClickLength() {
        return ui_longClickLength;
    }

    public int getRdsPort() {
        return receiver_rdsPort;
    }

    public int getRdsScrollSpeed() {
        return ui_rdsScrollSpeed;
    }

    public String getReceiverExecutablePath() {
        return receiver_path;
    }

    public int getStandbyDisplayLinger() {
        return ui_standbyDisplayLinger == 0 ? Integer.MAX_VALUE : ui_standbyDisplayLinger;
    }

    public String getTimeFormat() {
        return time_format;
    }

    public boolean isLogArchivingEnabled() {
        return logging_archive;
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
                        else if (field.getType() == boolean.class) field.set(this, Boolean.parseBoolean(val));
                    }
                } catch (Exception e) {}
            }
        }
    }

    public boolean persistSettings() {
        return settings_persist;
    }

    public void save() throws IOException {
        for (Field field : getClass().getDeclaredFields()) try {
            String newVal = null;
            Object fVal = field.get(this);
            if (fVal instanceof String str) newVal = str;
            else if (fVal instanceof Integer i) newVal = Integer.toString(i);
            else if (fVal instanceof Boolean b) newVal = Boolean.toString(b);
            if (newVal != null) setProperty(field.getName().replace('_', '.'), newVal);
        } catch (Exception e) {}

        try (Writer writer = new FileWriter(file)) {
            store(writer, null);
        }
    }

}
