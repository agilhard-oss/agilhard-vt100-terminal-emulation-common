package net.agilhard.terminal.emulation;

/**
 * The Interface StyledRunConsumer.
 */
public interface StyledRunConsumer {

    /**
     * Consume run.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param style
     *            the style
     * @param buf
     *            the buf
     * @param start
     *            the start
     * @param len
     *            the len
     */
    void consumeRun(int x, int y, Style style, char[] buf, int start, int len);
}
