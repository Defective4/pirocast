package io.github.defective4.rpi.pirocast.display;

public interface TextDisplay {
    default void centerTextInLine(String text, int line) {
        displayLineOfText(generateCenteredText(text).toString(), line);
    }

    void clearDisplay();

    void clearLine(int line);

    void createCharacter(int index, byte[] character);

    void displayLineOfText(String text, int line);

    default StringBuilder generateCenteredText(String text) {
        StringBuilder builder = new StringBuilder(getColumns());
        double freeSpace = (getColumns() - text.length()) / 2d;
        int left = (int) Math.floor(freeSpace);
        int right = (int) Math.ceil(freeSpace);
        for (int i = 0; i < left; i++) builder.append(" ");
        builder.append(text);
        for (int i = 0; i < right; i++) builder.append(" ");
        return builder;
    }

    int getColumns();

    boolean getDisplayBacklight();

    int getRows();

    void setDisplayBacklight(boolean enabled);

    void showDisplay();
}
