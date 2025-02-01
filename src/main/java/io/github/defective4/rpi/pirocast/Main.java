package io.github.defective4.rpi.pirocast;

import java.io.File;
import java.util.Arrays;

import io.github.defective4.rpi.pirocast.props.AppProperties;

public class Main {
    public static void main(String[] args) {
        try {
            File propsFile = new File("pirocast.properties");
            AppProperties props = new AppProperties(propsFile);

            if (!propsFile.isFile()) {
                props.save();
                System.err
                        .println(
                                "Default properties saved to \"" + propsFile + "\". Review it and run Pirocast again.");
                return;
            }

            props.load();

            Pirocast cast = new Pirocast(Arrays
                    .asList(new Source("FM", SignalMode.FM, 87e6f, 108e6f, 88e6f, null, true, false),
                            new Source("AUX", SignalMode.AUX, 0, 0, 0, null, false, false)),
                    props);
//            cast.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
