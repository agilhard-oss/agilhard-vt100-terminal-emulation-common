package net.agilhard.terminal.emulation;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class BackBuffer.
 */
public class BackBuffer {

    /** The Constant NL. */
    private static final String NL = "\n";

    /** The Logger. */
    private final Logger log = LoggerFactory.getLogger(BackBuffer.class);

    /** The Constant EMPTY_CHAR. */
    private static final char EMPTY_CHAR = ' '; // (char) 0x0;

    /** The buf. */
    private char[] buf;

    /** The style buf. */
    private Style[] styleBuf;

    /** The damage. */
    private BitSet damage;

    /** The style state. */
    private final StyleState styleState;

    /** The width. */
    private int width;

    /** The height. */
    private int height;

    /** The lock. */
    private final Lock lock = new ReentrantLock();

    /**
     * Instantiates a new back buffer.
     *
     * @param width
     *            the width
     * @param height
     *            the height
     * @param styleState
     *            the style state
     */
    public BackBuffer(final int width, final int height, final StyleState styleState) {
        this.styleState = styleState;
        this.allocateBuffers(width, height);
    }

    /**
     * Allocate buffers.
     *
     * @param csWidth
     *            the cs_width
     * @param csHeight
     *            the cs_height
     */
    private void allocateBuffers(final int csWidth, final int csHeight) {
        this.width = csWidth;
        this.height = csHeight;

        this.buf = new char[csWidth * csHeight];
        Arrays.fill(this.buf, EMPTY_CHAR);

        this.styleBuf = new Style[csWidth * csHeight];
        Arrays.fill(this.styleBuf, Style.EMPTY);

        this.damage = new BitSet(csWidth * csHeight);
    }

    /**
     * Do resize.
     *
     * @param pendingResize
     *            the pending resize
     * @param origin
     *            the origin
     * @return the dimension
     */
    @SuppressWarnings("unused")
    public Dimension doResize(final Dimension pendingResize, final RequestOrigin origin) {
        final char[] oldBuf = this.buf;
        final Style[] oldStyleBuf = this.styleBuf;
        final int oldHeight = this.height;
        final int oldWidth = this.width;
        this.allocateBuffers(pendingResize.width, pendingResize.height);
        this.clear();

        // copying lines...
        final int copyWidth = Math.min(oldWidth, this.width);
        final int copyHeight = Math.min(oldHeight, this.height);

        final int oldStart = oldHeight - copyHeight;
        final int start = this.height - copyHeight;

        for (int i = 0; i < copyHeight; i++) {
            System.arraycopy(oldBuf, (oldStart + i) * oldWidth, this.buf, (start + i) * this.width, copyWidth);
            System.arraycopy(oldStyleBuf, (oldStart + i) * oldWidth, this.styleBuf, (start + i) * this.width,
                copyWidth);
        }

        this.damage.set(0, this.width * this.height - 1, true);

        return pendingResize;
    }

    /**
     * Clear.
     */
    public void clear() {
        Arrays.fill(this.buf, EMPTY_CHAR);
        this.damage.set(0, this.width * this.height, true);
    }

    /**
     * Clear area.
     *
     * @param leftX
     *            the left x
     * @param topY
     *            the top y
     * @param rightX
     *            the right x
     * @param bottomY
     *            the bottom y
     */
    public void clearArea(final int leftX, final int topY, final int rightX, final int bottomY) {
        if (topY > bottomY) {
            this.log.error("Attempt to clear upside down area: top:" + topY + " > bottom:" + bottomY);
            return;
        }
        for (int y = topY; y < bottomY; y++) {
            if (y > this.height - 1 || y < 0) {
                this.log.error("attempt to clear line" + y + NL + "args were x1:" + leftX + " y1:" + topY + " x2:"
                    + rightX + "y2:" + bottomY);
            } else if (leftX > rightX) {
                this.log.error("Attempt to clear backwards area: left:" + leftX + " > right:" + rightX);
            } else {
                Arrays.fill(this.buf, y * this.width + leftX, y * this.width + rightX, EMPTY_CHAR);
                Arrays.fill(this.styleBuf, y * this.width + leftX, y * this.width + rightX, Style.EMPTY);
                this.damage.set(y * this.width + leftX, y * this.width + rightX, true);
            }
        }
    }

    /**
     * Draw bytes.
     *
     * @param bytes
     *            the bytes
     * @param s
     *            the s
     * @param len
     *            the len
     * @param x
     *            the x
     * @param y
     *            the y
     */
    public void drawBytes(final byte[] bytes, final int s, final int len, final int x, final int y) {
        final int adjY = y - 1;
        if (adjY >= this.height || adjY < 0) {
            if (this.log.isDebugEnabled()) {
                final StringBuffer sb = new StringBuffer("Attempt to draw line ").append(adjY).append(" at (").append(x)
                    .append(",").append(y).append(")");

                CharacterUtils.appendBuf(sb, bytes, s, len);
                this.log.debug(sb.toString());
            }
            return;
        }

        for (int i = 0; i < len; i++) {
            final int location = adjY * this.width + x + i;
            this.buf[location] = (char) bytes[s + i]; // Arraycopy does not convert
            this.styleBuf[location] = this.styleState.getCurrent();
        }
        this.damage.set(adjY * this.width + x, adjY * this.width + x + len);
    }

    /**
     * Draw string.
     *
     * @param str
     *            the str
     * @param x
     *            the x
     * @param y
     *            the y
     */
    public void drawString(final String str, final int x, final int y) {
        final int adjY = y - 1;
        if (adjY >= this.height || adjY < 0) {
            if (this.log.isDebugEnabled()) {
                this.log.debug("Attempt to draw line out of bounds: " + adjY + " at (" + x + "," + y + ")");
            }
            return;
        }
        str.getChars(0, str.length(), this.buf, adjY * this.width + x);
        for (int i = 0; i < str.length(); i++) {
            final int location = adjY * this.width + x + i;
            this.styleBuf[location] = this.styleState.getCurrent();
        }
        this.damage.set(adjY * this.width + x, adjY * this.width + x + str.length());
    }

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
    public void scrollArea(final int y, final int h, final int dy) {
        final int lastLine = y + h;
        if (dy > 0) {
            // Moving lines down
            for (int line = lastLine - dy; line >= y; line--) {
                if (line < 0) {
                    this.log.error("Attempt to scroll line from above top of screen:" + line);
                    continue;
                }
                if (line + dy + 1 > this.height) {
                    this.log.error("Attempt to scroll line off bottom of screen:" + (line + dy));
                    continue;
                }
                System.arraycopy(this.buf, line * this.width, this.buf, (line + dy) * this.width, this.width);
                System.arraycopy(this.styleBuf, line * this.width, this.styleBuf, (line + dy) * this.width, this.width);
                Util.bitsetCopy(this.damage, line * this.width, this.damage, (line + dy) * this.width, this.width);
                //damage.set( (line + dy) * width, (line + dy + 1) * width );
            }
        } else {
            // Moving lines up
            for (int line = y + dy + 1; line < lastLine; line++) {
                if (line > this.height - 1) {
                    this.log.error("Attempt to scroll line from below bottom of screen:" + line);
                    continue;
                }
                if (line + dy < 0) {
                    this.log.error("Attempt to scroll to line off top of screen" + (line + dy));
                    continue;
                }

                System.arraycopy(this.buf, line * this.width, this.buf, (line + dy) * this.width, this.width);
                System.arraycopy(this.styleBuf, line * this.width, this.styleBuf, (line + dy) * this.width, this.width);
                Util.bitsetCopy(this.damage, line * this.width, this.damage, (line + dy) * this.width, this.width);
                //damage.set( (line + dy) * width, (line + dy + 1) * width );
            }
        }
    }

    /**
     * Gets the style lines.
     *
     * @return the style lines
     */
    // CHECKSTYLE:OFF
    @SuppressWarnings("boxing")
    public String getStyleLines() {

        this.lock.lock();
        try {
            final StringBuilder sb = new StringBuilder();
            for (int row = 0; row < this.height; row++) {
                for (int col = 0; col < this.width; col++) {
                    final Style style = this.styleBuf[row * this.width + col];
                    int styleNum = style == null ? styleNum = 0 : style.getNumber();
                    sb.append(String.format("%03d ", styleNum));
                }
                sb.append(NL);
            }
            return sb.toString();
        } finally {
            this.lock.unlock();
        }
    }
    // CHECKSTYLE:ON

    /**
     * Gets the lines.
     *
     * @return the lines
     */
    public String getLines() {
        this.lock.lock();
        try {
            final StringBuffer sb = new StringBuffer();
            for (int row = 0; row < this.height; row++) {
                sb.append(this.buf, row * this.width, this.width);
                sb.append('\n');
            }
            return sb.toString();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Gets the damage lines.
     *
     * @return the damage lines
     */
    public String getDamageLines() {
        this.lock.lock();
        try {
            final StringBuffer sb = new StringBuffer();
            for (int row = 0; row < this.height; row++) {
                for (int col = 0; col < this.width; col++) {
                    final boolean isDamaged = this.damage.get(row * this.width + col);
                    sb.append(isDamaged ? 'X' : '-');
                }
                sb.append(NL);
            }
            return sb.toString();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Reset damage.
     */
    public void resetDamage() {
        this.lock.lock();
        try {
            this.damage.clear();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Pump runs.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param w
     *            the w
     * @param h
     *            the h
     * @param consumer
     *            the consumer
     */
    public void pumpRuns(final int x, final int y, final int w, final int h, final StyledRunConsumer consumer) {

        final int startRow = y;
        final int endRow = y + h;
        final int startCol = x;
        final int endCol = x + w;

        this.lock.lock();
        try {
            for (int row = startRow; row < endRow; row++) {
                Style lastStyle = null;
                int beginRun = startCol;
                for (int col = startCol; col < endCol; col++) {
                    final int location = row * this.width + col;
                    //if(location < 0 || location > styleBuf.length ){
                    if (location < 0 || location >= this.styleBuf.length) {
                        this.log.error("Requested out of bounds runs:" + "x:" + x + " y:" + y + " w:" + w + " h:" + h);
                        continue;
                    }
                    final Style cellStyle = this.styleBuf[location];
                    if (lastStyle == null) {
                        //begin line
                        lastStyle = cellStyle;
                    } else if (!cellStyle.equals(lastStyle)) {
                        //start of new run
                        consumer.consumeRun(beginRun, row, lastStyle, this.buf, row * this.width + beginRun,
                            col - beginRun);
                        beginRun = col;
                        lastStyle = cellStyle;
                    }
                }
                //end row
                if (lastStyle == null) {
                    this.log.error(
                        "Style is null for run supposed to be from " + beginRun + " to " + endCol + "on row " + row);
                } else {
                    consumer.consumeRun(beginRun, row, lastStyle, this.buf, row * this.width + beginRun,
                        endCol - beginRun);
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Pump runs from damage.
     *
     * @param consumer
     *            the consumer
     */
    public void pumpRunsFromDamage(final StyledRunConsumer consumer) {
        final int startRow = 0;
        final int endRow = this.height;
        final int startCol = 0;
        final int endCol = this.width;
        this.lock.lock();
        try {
            for (int row = startRow; row < endRow; row++) {

                Style lastStyle = null;
                int beginRun = startCol;
                for (int col = startCol; col < endCol; col++) {
                    final int location = row * this.width + col;
                    if (location < 0 || location > this.styleBuf.length) {
                        this.log.error("Requested out of bounds runs: pumpFromDamage");
                        continue;
                    }
                    final Style cellStyle = this.styleBuf[location];
                    final boolean isDamaged = this.damage.get(location);
                    if (!isDamaged) {
                        if (lastStyle != null) {
                            consumer.consumeRun(beginRun, row, lastStyle, this.buf, row * this.width + beginRun,
                                col - beginRun);
                        }
                        beginRun = col;
                        lastStyle = null;
                    } else if (lastStyle == null) {
                        //begin damaged run
                        lastStyle = cellStyle;
                    } else if (!cellStyle.equals(lastStyle)) {
                        //start of new run
                        consumer.consumeRun(beginRun, row, lastStyle, this.buf, row * this.width + beginRun,
                            col - beginRun);
                        beginRun = col;
                        lastStyle = cellStyle;
                    }
                }
                //end row
                if (lastStyle != null) {
                    consumer.consumeRun(beginRun, row, lastStyle, this.buf, row * this.width + beginRun,
                        endCol - beginRun);
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Checks for damage.
     *
     * @return true, if successful
     */
    public boolean hasDamage() {
        return this.damage.nextSetBit(0) != -1;
    }

    /**
     * Lock.
     */
    public void lock() {
        this.lock.lock();

    }

    /**
     * Unlock.
     */
    public void unlock() {
        this.lock.unlock();
    }

    /**
     * Try lock.
     *
     * @return true, if successful
     */
    public boolean tryLock() {
        return this.lock.tryLock();
    }

}
