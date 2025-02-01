package io.github.defective4.rpi.pirocast.input;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class InputManager {
    private final Map<Button, Long> clickTimes = new HashMap<>();
    private final Map<Button, InputListener> listeners = new HashMap<>();

    private final long longClickTime;

    protected InputManager(long longClickTime) {
        this.longClickTime = longClickTime;
    }

    public void putInputListener(Button btn, InputListener listener) {
        Objects.requireNonNull(listener);
        Objects.requireNonNull(btn);
        listeners.put(btn, listener);
    }

    public void removeInputListener(Button btn) {
        listeners.remove(btn);
    }

    protected void dispatchClick(Button button, boolean down) {
        Objects.requireNonNull(button);
        if (down) {
            clickTimes.put(button, System.currentTimeMillis());
            if (listeners.containsKey(button)) listeners.get(button).buttonPressed();
        } else {
            long diff = System.currentTimeMillis() - clickTimes.getOrDefault(button, System.currentTimeMillis());
            if (listeners.containsKey(button)) {
                InputListener ls = listeners.get(button);
                ls.buttonReleased();
                if (diff >= longClickTime) ls.buttonLongClicked();
                else ls.buttonClicked();
            }
        }
    }
}
