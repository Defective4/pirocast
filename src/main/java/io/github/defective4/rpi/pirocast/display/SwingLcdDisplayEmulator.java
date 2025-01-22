package io.github.defective4.rpi.pirocast.display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

public class SwingLcdDisplayEmulator extends JFrame implements TextDisplay {

    private boolean backlit = true;
    private final int columns;

    private final JLabel[] labels;

    public SwingLcdDisplayEmulator(int columns, int rows) {
        this.columns = columns;
        labels = new JLabel[rows];
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("LCD Emulator");
        setResizable(false);
        Font lcdFont = new Font("Monospaced", Font.PLAIN, 32);
        setFont(lcdFont);
        FontMetrics metrics = getFontMetrics(lcdFont);
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 16, 0, 16));
        panel.setBackground(Color.black);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < rows; i++) {
            JLabel label = new JLabel();
            labels[i] = label;
            label.setForeground(Color.cyan);
            label.setFont(lcdFont);
            label.setPreferredSize(new Dimension(metrics.stringWidth("0") * columns, metrics.getHeight()));
            panel.add(label);
        }
        setContentPane(panel);
        pack();
    }

    @Override
    public void clearDisplay() {
        for (int i = 0; i < labels.length; i++) clearLine(i + 1);
    }

    @Override
    public void clearLine(int line) {
        labels[line - 1].setText(" ");
    }

    @Override
    public void createCharacter(int index, byte[] character) {}

    @Override
    public void displayLineOfText(String text, int line) {
        if (text.length() > getColumns()) text = text.substring(0, getColumns());
        labels[line - 1].setText(text);
    }

    @Override
    public int getColumns() {
        return columns;
    }

    @Override
    public boolean getDisplayBacklight() {
        return backlit;
    }

    @Override
    public int getRows() {
        return labels.length;
    }

    @Override
    public void setDisplayBacklight(boolean enabled) {
        for (JLabel l : labels) l.setForeground(enabled ? Color.cyan : new Color(0, 0, 0, 0));
        backlit = enabled;
    }

    @Override
    public void showDisplay() {
        setVisible(true);
    }

}
