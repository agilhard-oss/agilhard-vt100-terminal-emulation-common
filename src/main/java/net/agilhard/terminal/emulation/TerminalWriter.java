/**
 *
 */
package net.agilhard.terminal.emulation;

import java.awt.Color;
import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class TerminalWriter.
 */
public class TerminalWriter {

    /** The Logger. */
    private final Logger log = LoggerFactory.getLogger(TerminalWriter.class);

    /** The tab. */
    private final int tab = 8;

    /** The scroll region top. */
    private int scrollRegionTop;

    /** The scroll region bottom. */
    private int scrollRegionBottom;

    /** The cursor x. */
    private int cursorX;

    /** The cursor y. */
    private int cursorY = 1;

    /** The term width. */
    private int termWidth = 80;

    /** The term height. */
    private int termHeight = 24;

    /** The display. */
    private final TerminalDisplay display;

    /** The back buffer. */
    private final BackBuffer backBuffer;

    /** The style state. */
    private final StyleState styleState;

    /** The modes. */
    private final EnumSet<Mode> modes = EnumSet.of(Mode.ANSI);

    /**
     * Instantiates a new terminal writer.
     *
     * @param term
     *            the term
     * @param buf
     *            the buf
     * @param styleState
     *            the style state
     */
    public TerminalWriter(final TerminalDisplay term, final BackBuffer buf, final StyleState styleState) {
        this.display = term;
        this.backBuffer = buf;
        this.styleState = styleState;

        this.termWidth = term.getColumnCount();
        this.termHeight = term.getRowCount();

        this.scrollRegionTop = 1;
        this.scrollRegionBottom = this.termHeight;
    }

    /**
     * Sets the mode.
     *
     * @param mode
     *            the new mode
     */
    public void setMode(final Mode mode) {
        this.modes.add(mode);
        switch (mode) {
        case WideColumn:
            this.resize(new Dimension(132, 24), RequestOrigin.Remote);
            this.clearScreen();
            this.restoreCursor(null);
            break;
        default:
            //TODO implement modes
            break;
        }
    }

    /**
     * Unset mode.
     *
     * @param mode
     *            the mode
     */
    public void unsetMode(final Mode mode) {
        this.modes.remove(mode);
        switch (mode) {
        case WideColumn:
            this.resize(new Dimension(80, 24), RequestOrigin.Remote);
            this.clearScreen();
            this.restoreCursor(null);
            break;
        default:
            //TODO implement modes
            break;
        }
    }

    /**
     * Wrap lines.
     */
    private void wrapLines() {
        if (this.cursorX >= this.termWidth) {
            this.cursorX = 0;
            this.cursorY += 1;
        }
    }

    /**
     * Finish text.
     */
    private void finishText() {
        this.display.setCursor(this.cursorX, this.cursorY);
        this.scrollY();
    }

    /**
     * Write ascii.
     *
     * @param chosenBuffer
     *            the chosen buffer
     * @param start
     *            the start
     * @param length
     *            the length
     */
    public void writeASCII(final byte[] chosenBuffer, final int start, final int length) {
        this.backBuffer.lock();
        try {
            this.wrapLines();
            if (length != 0) {
                this.backBuffer.clearArea(this.cursorX, this.cursorY - 1, this.cursorX + length, this.cursorY);
                this.backBuffer.drawBytes(chosenBuffer, start, length, this.cursorX, this.cursorY);
            }
            this.cursorX += length;
            this.finishText();
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Write double byte.
     *
     * @param bytesOfChar
     *            the bytes of char
     * @throws UnsupportedEncodingException
     *             the unsupported encoding exception
     */
    public void writeDoubleByte(final byte[] bytesOfChar) throws UnsupportedEncodingException {
        this.writeString(new String(bytesOfChar, 0, 2, "EUC-JP"));
    }

    /**
     * Write char.
     *
     * @param c
     *            the c
     */
    public void writeChar(final char c) {
        final char[] buf = new char[1];
        buf[0] = c;
        this.writeString(new String(buf));
    }

    /**
     * Write string.
     *
     * @param string
     *            the string
     */
    public void writeString(final String string) {
        this.backBuffer.lock();
        try {
            this.wrapLines();
            this.backBuffer.clearArea(this.cursorX, this.cursorY - 1, this.cursorX + string.length(), this.cursorY);
            this.backBuffer.drawString(string, this.cursorX, this.cursorY);
            this.cursorX += string.length();
            this.finishText();
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Write unwrapped string.
     *
     * @param string
     *            the string
     */
    public void writeUnwrappedString(final String string) {
        final int length = string.length();
        int off = 0;
        while (off < length) {
            final int amountInLine = Math.min(this.distanceToLineEnd(), length - off);
            this.writeString(string.substring(off, off + amountInLine));
            this.wrapLines();
            this.scrollY();
            off += amountInLine;
        }
    }

    /**
     * Scroll y.
     */
    public void scrollY() {
        this.backBuffer.lock();
        try {
            if (this.cursorY > this.scrollRegionBottom) {
                final int dy = this.scrollRegionBottom - this.cursorY;
                this.cursorY = this.scrollRegionBottom;
                this.scrollArea(this.scrollRegionTop, this.scrollRegionBottom - this.scrollRegionTop, dy);
                this.backBuffer.clearArea(0, this.cursorY - 1, this.termWidth, this.cursorY);
                this.display.setCursor(this.cursorX, this.cursorY);
            }
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * New line.
     */
    public void newLine() {
        this.cursorY += 1;
        this.display.setCursor(this.cursorX, this.cursorY);
        this.scrollY();
    }

    /**
     * Backspace.
     */
    public void backspace() {
        this.cursorX -= 1;
        if (this.cursorX < 0) {
            this.cursorY -= 1;
            this.cursorX = this.termWidth - 1;
        }
        this.display.setCursor(this.cursorX, this.cursorY);
    }

    /**
     * Carriage return.
     */
    public void carriageReturn() {
        this.cursorX = 0;
        this.display.setCursor(this.cursorX, this.cursorY);
    }

    /**
     * Horizontal tab.
     */
    public void horizontalTab() {
        this.cursorX = (this.cursorX / this.tab + 1) * this.tab;
        if (this.cursorX >= this.termWidth) {
            this.cursorX = 0;
            this.cursorY += 1;
        }
        this.display.setCursor(this.cursorX, this.cursorY);
    }

    /**
     * Erase in display.
     *
     * @param args
     *            the args
     */
    public void eraseInDisplay(final ControlSequence args) {
        // ESC [ Ps J
        this.backBuffer.lock();
        try {
            final int arg = args.getArg(0, 0);
            int beginY;
            int endY;

            switch (arg) {
            case 0:
                // Initial line
                if (this.cursorX < this.termWidth) {
                    this.backBuffer.clearArea(this.cursorX, this.cursorY - 1, this.termWidth, this.cursorY);
                }
                // Rest
                beginY = this.cursorY;
                endY = this.termHeight;

                break;
            case 1:
                // initial line
                this.backBuffer.clearArea(0, this.cursorY - 1, this.cursorX + 1, this.cursorY);

                beginY = 0;
                endY = this.cursorY - 1;
                break;
            case 2:
                beginY = 0;
                endY = this.termHeight;
                break;
            default:
                this.log.error("Unsupported erase in display mode:" + arg);
                beginY = 1;
                endY = 1;
                break;
            }
            // Rest of lines
            if (beginY != endY) {
                this.clearLines(beginY, endY);
            }
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Clear lines.
     *
     * @param beginY
     *            the begin y
     * @param endY
     *            the end y
     */
    public void clearLines(final int beginY, final int endY) {
        this.backBuffer.lock();
        try {
            this.backBuffer.clearArea(0, beginY, this.termWidth, endY);
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Clear screen.
     */
    public void clearScreen() {
        this.clearLines(0, this.termHeight);
    }

    /**
     * Erase in line.
     *
     * @param args
     *            the args
     */
    public void eraseInLine(final ControlSequence args) {
        // ESC [ Ps K
        final int arg = args.getArg(0, 0);
        this.eraseInLine(arg);
    }

    /**
     * Erase in line.
     *
     * @param arg
     *            the arg
     */
    public void eraseInLine(final int arg) {
        this.backBuffer.lock();
        try {
            switch (arg) {
            case 0:
                if (this.cursorX < this.termWidth) {
                    this.backBuffer.clearArea(this.cursorX, this.cursorY - 1, this.termWidth, this.cursorY);
                }
                break;
            case 1:
                final int extent = Math.min(this.cursorX + 1, this.termWidth);
                this.backBuffer.clearArea(0, this.cursorY - 1, extent, this.cursorY);
                break;
            case 2:
                this.backBuffer.clearArea(0, this.cursorY - 1, this.termWidth, this.cursorY);
                break;
            default:
                this.log.error("Unsupported erase in line mode:" + arg);
                break;
            }
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Cursor up.
     *
     * @param args
     *            the args
     */
    public void cursorUp(final ControlSequence args) {
        this.backBuffer.lock();
        try {
            int arg = args.getArg(0, 0);
            arg = arg == 0 ? 1 : arg;

            this.cursorY -= arg;
            this.cursorY = Math.max(this.cursorY, 1);
            this.display.setCursor(this.cursorX, this.cursorY);
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Cursor down.
     *
     * @param args
     *            the args
     */
    public void cursorDown(final ControlSequence args) {
        this.backBuffer.lock();
        try {
            int arg = args.getArg(0, 0);
            arg = arg == 0 ? 1 : arg;
            this.cursorY += arg;
            this.cursorY = Math.min(this.cursorY, this.termHeight);
            this.display.setCursor(this.cursorX, this.cursorY);
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Index.
     */
    public void index() {
        this.backBuffer.lock();
        try {
            if (this.cursorY == this.termHeight) {
                this.scrollArea(this.scrollRegionTop, this.scrollRegionBottom - this.scrollRegionTop, -1);
                this.backBuffer.clearArea(0, this.scrollRegionBottom - 1, this.termWidth, this.scrollRegionBottom);
            } else {
                this.cursorY += 1;
                this.display.setCursor(this.cursorX, this.cursorY);
            }
        } finally {
            this.backBuffer.unlock();
        }
    }

    // Dodgy ?
    /**
     * Scroll area.
     *
     * @param y
     *            the y
     * @param h
     *            the h
     * @param dy
     *            the dy
     */
    private void scrollArea(final int y, final int h, final int dy) {
        this.display.scrollArea(y, h, dy);
        this.backBuffer.scrollArea(y, h, dy);
    }

    /**
     * Next line.
     */
    public void nextLine() {
        this.backBuffer.lock();
        try {
            this.cursorX = 0;
            if (this.cursorY == this.termHeight) {
                this.scrollArea(this.scrollRegionTop, this.scrollRegionBottom - this.scrollRegionTop, -1);
                this.backBuffer.clearArea(0, this.scrollRegionBottom - 1, this.termWidth, this.scrollRegionBottom);
            } else {
                this.cursorY += 1;
            }
            this.display.setCursor(this.cursorX, this.cursorY);
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Reverse index.
     */
    public void reverseIndex() {
        this.backBuffer.lock();
        try {
            if (this.cursorY == 1) {
                this.scrollArea(this.scrollRegionTop - 1, this.scrollRegionBottom - this.scrollRegionTop, 1);
                this.backBuffer.clearArea(this.cursorX, this.cursorY - 1, this.termWidth, this.cursorY);
            } else {
                this.cursorY -= 1;
                this.display.setCursor(this.cursorX, this.cursorY);
            }
        } finally {
            this.backBuffer.unlock();
        }
    }

    /**
     * Cursor forward.
     *
     * @param args
     *            the args
     */
    public void cursorForward(final ControlSequence args) {
        int arg = args.getArg(0, 1);
        arg = arg == 0 ? 1 : arg;
        this.cursorX += arg;
        this.cursorX = Math.min(this.cursorX, this.termWidth - 1);
        this.display.setCursor(this.cursorX, this.cursorY);
    }

    /**
     * Cursor backward.
     *
     * @param args
     *            the args
     */
    public void cursorBackward(final ControlSequence args) {
        int arg = args.getArg(0, 1);
        arg = arg == 0 ? 1 : arg;
        this.cursorX -= arg;
        this.cursorX = Math.max(this.cursorX, 0);
        this.display.setCursor(this.cursorX, this.cursorY);
    }

    /**
     * Cursor position.
     *
     * @param args
     *            the args
     */
    public void cursorPosition(final ControlSequence args) {
        final int argy = args.getArg(0, 1);
        final int argx = args.getArg(1, 1);
        this.cursorX = argx - 1;
        this.cursorY = argy;
        this.display.setCursor(this.cursorX, this.cursorY);
    }

    /**
     * Sets the scrolling region.
     *
     * @param args
     *            the new scrolling region
     */
    public void setScrollingRegion(final ControlSequence args) {
        final int y1 = args.getArg(0, 1);
        final int y2 = args.getArg(1, this.termHeight);

        this.scrollRegionTop = y1;
        this.scrollRegionBottom = y2;
    }

    /*
     * Character Attributes
     *
     * ESC [ Ps;Ps;Ps;...;Ps m
     *
     * Ps refers to a selective parameter. Multiple parameters are separated by
     * the semicolon character (0738). The parameters are executed in order and
     * have the following meanings: 0 or None All Attributes Off 1 Bold on 4
     * Underscore on 5 Blink on 7 Reverse video on
     *
     * Any other parameter values are ignored.
     */

    /** The colors. */
    private static Color[] colors =
        { Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE };

    /*
     * (non-Javadoc)
     *
     * @see net.agilhard.jcterm.ITerminalWriter#setCharacterAttributes(int[], int)
     */
    /**
     * Sets the character attributes.
     *
     * @param args
     *            the new character attributes
     */
    public void setCharacterAttributes(final ControlSequence args) {
        final int argCount = args.getCount();
        if (argCount == 0) {
            this.styleState.reset();
        }

        for (int i = 0; i < argCount; i++) {
            final int arg = args.getArg(i, -1);
            if (arg == -1) {
                this.log.error("Error in processing char attributes, arg " + i);
                continue;
            }

            switch (arg) {
            case 0:
                this.styleState.reset();
                break;
            case 1:// Bright
                this.styleState.setOption(Style.Option.BOLD, true);
                break;
            case 2:// Dim
                this.styleState.setOption(Style.Option.DIM, true);
                break;
            case 4:// Underscore on
                this.styleState.setOption(Style.Option.UNDERSCORE, true);
                break;
            case 5:// Blink on
                this.styleState.setOption(Style.Option.BLINK, true);
                break;
            case 7:// Reverse video on
                this.styleState.setOption(Style.Option.REVERSE, true);
                break;
            case 8: // Hidden
                this.styleState.setOption(Style.Option.HIDDEN, true);
                break;
            default:
                if (arg >= 30 && arg <= 37) {
                    this.styleState.setCurrentForeground(colors[arg - 30]);
                } else if (arg >= 40 && arg <= 47) {
                    this.styleState.setCurrentBackground(colors[arg - 40]);
                } else {
                    this.log.error("Unknown character attribute:" + arg);
                }
            }
        }
    }

    /**
     * Beep.
     */
    public void beep() {
        this.display.beep();
    }

    /**
     * Distance to line end.
     *
     * @return the int
     */
    public int distanceToLineEnd() {
        return this.termWidth - this.cursorX;
    }

    /**
     * Store cursor.
     *
     * @param storedCursor
     *            the stored cursor
     */
    public void storeCursor(final StoredCursor storedCursor) {
        storedCursor.x = this.cursorX;
        storedCursor.y = this.cursorY;
    }

    /**
     * Restore cursor.
     *
     * @param storedCursor
     *            the stored cursor
     */
    public void restoreCursor(final StoredCursor storedCursor) {
        this.cursorX = 0;
        this.cursorY = 1;
        if (storedCursor != null) {
            // TODO: something with origin modes
            this.cursorX = storedCursor.x;
            this.cursorY = storedCursor.y;
        }
        this.display.setCursor(this.cursorX, this.cursorY);
    }

    /**
     * Resize.
     *
     * @param pendingResize
     *            the pending resize
     * @param origin
     *            the origin
     * @return the dimension
     */
    public Dimension resize(final Dimension pendingResize, final RequestOrigin origin) {
        final int oldHeight = this.termHeight;
        final Dimension pixelSize = this.display.doResize(pendingResize, origin);

        this.termWidth = this.display.getColumnCount();
        this.termHeight = this.display.getRowCount();

        this.scrollRegionBottom += this.termHeight - oldHeight;
        this.cursorY += this.termHeight - oldHeight;
        this.cursorY = Math.max(1, this.cursorY);
        return pixelSize;
    }

    /**
     * Fill screen.
     *
     * @param c
     *            the c
     */
    public void fillScreen(final char c) {
        this.backBuffer.lock();
        try {
            final char[] chars = new char[this.termWidth];
            Arrays.fill(chars, c);
            final String str = new String(chars);

            for (int row = 1; row <= this.termHeight; row++) {
                this.backBuffer.drawString(str, 0, row);
            }
        } finally {
            this.backBuffer.unlock();
        }
    }

}