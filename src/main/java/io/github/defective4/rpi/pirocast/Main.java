package io.github.defective4.rpi.pirocast;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.defective4.rpi.pirocast.props.AppProperties;
import io.github.defective4.rpi.pirocast.props.SourceConfig;
import io.github.defective4.rpi.pirocast.settings.Setting;

public class Main {
    public static void main(String[] args) {
        try {
            File sourcesFile = new File("sources.json");
            File propsFile = new File("pirocast.properties");
            AppProperties props = new AppProperties(propsFile);

            if (!sourcesFile.exists()) {
                try (InputStream in = Main.class.getResourceAsStream("/sources.json.default")) {
                    Files.copy(in, sourcesFile.toPath());
                    System.err.println("Default sources saved to " + sourcesFile);
                }
            }

            List<Source> sources = new ArrayList<>();

            if (sourcesFile.isFile()) try (Reader reader = new FileReader(sourcesFile)) {
                Gson gson = new Gson();
                JsonArray array = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("sources");
                for (JsonElement el : array) {
                    if (el instanceof JsonObject obj) {
                        try {
                            SourceConfig srcCfg = gson.fromJson(obj, SourceConfig.class);
                            String ivField = srcCfg.isValid();
                            if (ivField != null) throw new IOException("Field " + ivField + " is missing or invalid");
                            sources.add(srcCfg.toSource());
                        } catch (Exception e) {
                            System.err.println("Skipped one of sources from config file: " + e.getMessage());
                        }
                    }
                }
            }

            if (sources.isEmpty()) {
                System.err.println("Sources list is empty");
                System.exit(2);
                return;
            }

            if (!propsFile.isFile()) {
                props.save();
                System.err
                        .println(
                                "Default properties saved to \"" + propsFile + "\". Review it and run Pirocast again.");
                return;
            }

            props.load();

            if (props.persistSettings()) {
                File settingsFile = new File("pcsettings.json");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try (Writer writer = new FileWriter(settingsFile)) {
                        JsonObject settings = new JsonObject();
                        Gson gson = new Gson();
                        for (Source src : sources) {
                            JsonObject obj = new JsonObject();
                            for (Setting set : src.getSettings())
                                obj.add(set.name(), gson.toJsonTree(src.getSetting(set)));
                            settings.add(src.getName(), obj);
                        }
                        gson.toJson(settings, writer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
                if (settingsFile.isFile()) {
                    try (Reader reader = new FileReader(settingsFile)) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : root.entrySet())
                            if (entry.getValue() instanceof JsonObject obj) {
                                String srcName = entry.getKey();
                                Source src = null;
                                for (Source s : sources) if (s.getName().equals(srcName)) {
                                    src = s;
                                    break;
                                }
                                if (src == null) continue;
                                Map<String, JsonElement> settings = new HashMap<>();
                                obj.entrySet().forEach(e -> settings.put(e.getKey(), e.getValue()));
                                src.initSettings(settings);
                            }
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }

            LogManager.setLogDir(props.getLoggingDirectory());
            LogManager.setLogLevel(props.getLogLevel());
            LogManager.setLogArchiving(props.isLogArchivingEnabled());

            Pirocast cast = new Pirocast(sources, props);
//            cast.start();
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
