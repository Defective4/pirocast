package io.github.defective4.rpi.pirocast.display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.InputStream;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

public class SwingLcdDisplayEmulator extends JFrame implements TextDisplay {

    private boolean backlit = true;
    private final Color bg = new Color(25, 64, 255);

    private final int columns;
    private final JLabel[] labels;
    private final JPanel panel;

    public SwingLcdDisplayEmulator(int columns, int rows) {
        this.columns = columns;
        labels = new JLabel[rows];
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("LCD Emulator");
        setResizable(false);
        Font lcdFont;
        try (InputStream in = getClass().getResourceAsStream("/hd44780/hd44780-5x8.ttf")) {
            lcdFont = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(32f);
        } catch (Exception e) {
            e.printStackTrace();
            lcdFont = new Font("Monospaced", Font.PLAIN, 32);
        }
        setFont(lcdFont);
        FontMetrics metrics = getFontMetrics(lcdFont);
        panel = new JPanel();
        panel.setBorder(new EmptyBorder(8, 16, 8, 16));
        panel.setBackground(bg);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        int maxWidth = 0;
        for (int w : metrics.getWidths()) maxWidth = Math.max(maxWidth, w);
        for (int i = 0; i < rows; i++) {
            JLabel label = new JLabel();
            labels[i] = label;
            label.setForeground(Color.white);
            label.setFont(lcdFont);
            label.setPreferredSize(new Dimension(maxWidth * columns, metrics.getHeight()));
            panel.add(label);
        }
        setContentPane(panel);
        pack();
    }

    @Override
    public void clearDisplay() {
        for (int i = 0; i < labels.length; i++) clearLine(i);
    }

    @Override
    public void clearLine(int line) {
        labels[line].setText(" ");
    }

    @Override
    public void createCharacter(int index, byte[] character) {}

    @Override
    public void displayLineOfText(String text, int line) {
        if (text.length() > getColumns()) text = text.substring(0, getColumns());
        labels[line].setText(text);
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
        for (JLabel l : labels) l.setForeground(enabled ? Color.white : new Color(7, 13, 50));
        panel.setBackground(enabled ? bg : new Color(3, 6, 26));
        backlit = enabled;
    }

    @Override
    public void showDisplay() {
        setVisible(true);
    }

}
