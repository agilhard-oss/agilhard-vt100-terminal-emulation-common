package net.agilhard.terminal.emulation;

import java.awt.Color;

/**
 * The Class StyleState.
 */
public class StyleState {

    /** The current style. */
    private Style currentStyle = Style.EMPTY;

    /**
     * Roll style.
     */
    private void rollStyle() {
        this.currentStyle = this.currentStyle.clone();
    }

    /**
     * Gets the current.
     *
     * @return the current
     */
    public Style getCurrent() {
        return Style.getCanonicalStyle(this.currentStyle);
    }

    /**
     * Sets the current background.
     *
     * @param bg
     *            the new current background
     */
    public void setCurrentBackground(final Color bg) {
        this.rollStyle();
        this.currentStyle.setBackground(bg);
    }

    /**
     * Sets the current foreground.
     *
     * @param fg
     *            the new current foreground
     */
    public void setCurrentForeground(final Color fg) {
        this.rollStyle();
        this.currentStyle.setForeground(fg);
    }

    /**
     * Sets the option.
     *
     * @param opt
     *            the opt
     * @param val
     *            the val
     */
    public void setOption(final Style.Option opt, final boolean val) {
        this.rollStyle();
        this.currentStyle.setOption(opt, val);
    }

    /**
     * Reset.
     */
    public void reset() {
        this.rollStyle();
        this.currentStyle.setForeground(Style.FOREGROUND);
        this.currentStyle.setBackground(Style.BACKGROUND);
        this.currentStyle.clearOptions();

    }

}
