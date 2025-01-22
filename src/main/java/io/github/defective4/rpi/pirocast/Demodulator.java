package io.github.defective4.rpi.pirocast;

public enum Demodulator {
    AM(0), FM(1), NFM(2);

    private final int id;

    private Demodulator(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
