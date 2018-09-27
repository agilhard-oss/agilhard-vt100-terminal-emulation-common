/**
 *
 */
package net.agilhard.terminal.emulation;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.WeakHashMap;

/**
 * The Class Style.
 */
public class Style implements Cloneable {

    /** The count. */
    private static int count = 1;

    /**
     * The Class ChosenColor.
     */
    static class ChosenColor extends Color {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 7492667732033832704L;

        /**
         * Instantiates a new chosen color.
         *
         * @param def
         *            the def
         */
        public ChosenColor(final Color def) {
            super(def.getRGB());
        }
    }

    /** The Constant FOREGROUND. */
    static final ChosenColor FOREGROUND = new ChosenColor(Color.BLACK);

    /** The Constant BACKGROUND. */
    static final ChosenColor BACKGROUND = new ChosenColor(Color.WHITE);

    /**
     * The Enum Option.
     */
    public static enum Option {

        /** The bold. */
        BOLD,

        /** The blink. */
        BLINK,

        /** The dim. */
        DIM,

        /** The reverse. */
        REVERSE,

        /** The underscore. */
        UNDERSCORE,

        /** The hidden. */
        HIDDEN
    }

    /** The Constant EMPTY. */
    public static final Style EMPTY = new Style();

    /** The Constant styles. */
    private static WeakHashMap<Style, WeakReference<Style>> styles = new WeakHashMap<>();

    /**
     * Gets the canonical style.
     *
     * @param currentStyle
     *            the current style
     * @return the canonical style
     */
    public static Style getCanonicalStyle(final Style currentStyle) {
        final WeakReference<Style> canonRef = styles.get(currentStyle);
        if (canonRef != null) {
            final Style canonStyle = canonRef.get();
            if (canonStyle != null) {
                return canonStyle;
            }
        }
        styles.put(currentStyle, new WeakReference<>(currentStyle));
        return currentStyle;
    }

    /** The foreground. */
    private Color foreground;

    /** The background. */
    private Color background;

    /** The options. */
    private final EnumSet<Option> options;

    /** The number. */
    private final int number;

    /**
     * Instantiates a new style.
     */
    Style() {
        this.number = count++;
        this.foreground = FOREGROUND;
        this.background = BACKGROUND;
        this.options = EnumSet.noneOf(Option.class);
    }

    /**
     * Instantiates a new style.
     *
     * @param foreground
     *            the foreground
     * @param background
     *            the background
     * @param options
     *            the options
     */
    Style(final Color foreground, final Color background, final EnumSet<Option> options) {
        this.number = count++;
        this.foreground = foreground;
        this.background = background;
        this.options = options.clone();
    }

    /**
     * Sets the foreground.
     *
     * @param foreground
     *            the new foreground
     */
    void setForeground(final Color foreground) {
        this.foreground = foreground;
    }

    /**
     * Gets the foreground.
     *
     * @return the foreground
     */
    public Color getForeground() {
        return this.foreground;
    }

    /**
     * Sets the background.
     *
     * @param background
     *            the new background
     */
    void setBackground(final Color background) {
        this.background = background;
    }

    /**
     * Gets the background.
     *
     * @return the background
     */
    public Color getBackground() {
        return this.background;
    }

    /**
     * Sets the option.
     *
     * @param opt
     *            the opt
     * @param val
     *            the val
     */
    public void setOption(final Option opt, final boolean val) {
        if (val) {
            this.options.add(opt);
        } else {
            this.options.remove(opt);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    // CHECKSTYLE:OFF
    public Style clone() {
        return new Style(this.getForeground(), this.getBackground(), this.options);
    }
    // CHECKSTYLE:ON

    /**
     * Gets the number.
     *
     * @return the number
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * Checks for option.
     *
     * @param bold
     *            the bold
     * @return true, if successful
     */
    public boolean hasOption(final Option bold) {
        return this.options.contains(bold);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.background == null ? 0 : this.background.hashCode());
        result = prime * result + (this.foreground == null ? 0 : this.foreground.hashCode());
        result = prime * result + (this.options == null ? 0 : this.options.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Style other = (Style) obj;
        if (this.background == null) {
            if (other.background != null) {
                return false;
            }
        } else if (!this.background.equals(other.background)) {
            return false;
        }
        if (this.foreground == null) {
            if (other.foreground != null) {
                return false;
            }
        } else if (!this.foreground.equals(other.foreground)) {
            return false;
        }
        if (this.options == null) {
            if (other.options != null) {
                return false;
            }
        } else if (!this.options.equals(other.options)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the background for run.
     *
     * @return the background for run
     */
    public Color getBackgroundForRun() {
        return this.options.contains(Option.REVERSE) ? this.foreground : this.background;
    }

    /**
     * Gets the foreground for run.
     *
     * @return the foreground for run
     */
    public Color getForegroundForRun() {
        return this.options.contains(Option.REVERSE) ? this.background : this.foreground;
    }

    /**
     * Clear options.
     */
    public void clearOptions() {
        this.options.clear();
    }

}