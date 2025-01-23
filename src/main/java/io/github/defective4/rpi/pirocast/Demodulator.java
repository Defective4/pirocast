package io.github.defective4.rpi.pirocast;

public enum Demodulator {
    AM(0), AUX(-1), FM(1), NFM(2);

    public static final int UNDEFINED_ID = -1;
    private final int id;

    private Demodulator(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
