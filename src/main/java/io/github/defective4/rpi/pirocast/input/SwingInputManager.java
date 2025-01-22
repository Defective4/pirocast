package io.github.defective4.rpi.pirocast.input;

import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SwingInputManager extends InputManager {

    private final int prev, ok, next;

    public SwingInputManager(Window win, long longClickTime, int prev, int ok, int next) {
        super(longClickTime);
        this.prev = prev;
        this.ok = ok;
        this.next = next;
        win.addKeyListener(new KeyAdapter() {

            boolean held = false;

            @Override
            public void keyPressed(KeyEvent e) {
                if (held) return;
                held = true;
                handle(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                held = false;
                handle(e.getKeyCode(), false);
            }

            private void handle(int id, boolean down) {
                Button btn;
                if (id == prev) btn = Button.PREV;
                else if (id == ok) btn = Button.OK;
                else if (id == next) btn = Button.NEXT;
                else btn = null;
                if (btn != null) dispatchClick(btn, down);
            }
        });
    }

}
