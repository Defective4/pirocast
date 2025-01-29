package io.github.defective4.rpi.pirocast;

import java.util.Arrays;

import io.github.defective4.rpi.pirocast.props.AppProperties;

public class Main {
    public static void main(String[] args) {
        try {
            AppProperties props = new AppProperties();
            Pirocast cast = new Pirocast(Arrays
                    .asList(new Source("File", SignalMode.FILE, 0, 0, 0, "/home/defective/Muzyka2"),
                            new Source("AUX", SignalMode.AUX, 0, 0, 0)),
                    props);
//            cast.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
