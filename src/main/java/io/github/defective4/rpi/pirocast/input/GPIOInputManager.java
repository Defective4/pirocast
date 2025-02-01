package io.github.defective4.rpi.pirocast.input;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalState;

public class GPIOInputManager extends InputManager {

    public GPIOInputManager(long longClickTime, int prev, int ok, int next, Context ctx) {
        super(longClickTime);
        ctx.digitalInput().create(prev).addListener(event -> {
            System.out.println("Prev " + event.state());
            dispatchClick(Button.PREV, event.state() == DigitalState.HIGH);
        });
        ctx.digitalInput().create(ok).addListener(event -> {
            System.out.println("OK " + event.state());
            dispatchClick(Button.OK, event.state() == DigitalState.HIGH);
        });
        ctx.digitalInput().create(next).addListener(event -> {
            System.out.println("Next " + event.state());
            dispatchClick(Button.NEXT, event.state() == DigitalState.HIGH);
        });
    }

}
