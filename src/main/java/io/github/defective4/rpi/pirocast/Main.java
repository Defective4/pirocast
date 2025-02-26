package io.github.defective4.rpi.pirocast;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.defective4.rpi.pirocast.props.AppProperties;
import io.github.defective4.rpi.pirocast.props.SourceConfig;

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
