package io.github.defective4.rpi.pirocast;

import java.util.Arrays;

import io.github.defective4.rpi.pirocast.props.AppProperties;

public class Main {
    public static void main(String[] args) {
        try {
            AppProperties props = new AppProperties();
            Pirocast cast = new Pirocast(Arrays
                    .asList(new Band("FM", Demodulator.FM, 87e6f, 108e6f, 88e6f),
                            new Band("AM", Demodulator.AM, 0, 27e6f, 95e5f),
                            new Band("NFM", Demodulator.NFM, 144e6f, 146e6f, 1448e5f)),
                    props);
            cast.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
