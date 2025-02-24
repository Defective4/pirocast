package io.github.defective4.rpi.pirocast.input;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalState;

public class GPIOInputManager extends InputManager {

    public GPIOInputManager(long longClickTime, int prev, int ok, int next, Context ctx) {
        super(longClickTime);
        System.out.println(prev);
        ctx
                .digitalInput()
                .create(prev)
                .addListener(event -> { dispatchClick(Button.PREV, event.state() == DigitalState.HIGH); });
        ctx
                .digitalInput()
                .create(ok)
                .addListener(event -> { dispatchClick(Button.OK, event.state() == DigitalState.HIGH); });
        ctx
                .digitalInput()
                .create(next)
                .addListener(event -> { dispatchClick(Button.NEXT, event.state() == DigitalState.HIGH); });
    }

}
