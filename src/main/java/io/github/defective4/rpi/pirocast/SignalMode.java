package io.github.defective4.rpi.pirocast;

public enum SignalMode {
    AM(0), AUX(-1), FM(1), NETWORK(-1), NFM(2);

    public static final int UNDEFINED_ID = -1;
    private final int id;

    private SignalMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
