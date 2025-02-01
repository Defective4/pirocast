package io.github.defective4.rpi.pirocast.input;

public interface InputListener {
    void buttonClicked();

    void buttonLongClicked();

    void buttonPressed();

    void buttonReleased();
}
