/**
 *
 */
package net.agilhard.terminal.emulation;

import java.awt.Point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class SelectionRunConsumer.
 */
public class SelectionRunConsumer implements StyledRunConsumer {

    /** The Logger. */
    private final Logger log = LoggerFactory.getLogger(SelectionRunConsumer.class);

    /** The selection. */
    private final StringBuffer selection;

    /** The begin. */
    private final Point begin;

    /** The end. */
    private final Point end;

    /** The first. */
    private boolean first = true;

    /**
     * Instantiates a new selection run consumer.
     *
     * @param selection
     *            the selection
     * @param begin
     *            the begin
     * @param end
     *            the end
     */
    public SelectionRunConsumer(final StringBuffer selection, final Point begin, final Point end) {
        this.selection = selection;
        this.end = end;
        this.begin = begin;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.agilhard.terminal.emulation.StyledRunConsumer#consumeRun(int, int,
     * net.agilhard.terminal.emulation.Style, char[], int, int)
     */
    @SuppressWarnings("unused")
    @Override
    public void consumeRun(final int x, final int y, final Style style, final char[] buf, final int start,
        final int len) {
        int startPos = start;
        int extent = len;

        if (y == this.end.y) {
            extent = Math.min(this.end.x - x, extent);

        }
        if (y == this.begin.y) {
            final int xAdj = Math.max(0, this.begin.x - x);
            startPos += xAdj;
            extent -= xAdj;
            if (extent < 0) {
                return;
            }
        }
        if (extent < 0) {
            return; // The run is off the left edge of the selection on the
                   // first line,
        }
        // or off the right edge on the last line.
        if (len > 0) {
            if (!this.first && x == 0) {
                this.selection.append('\n');
            }
            this.first = false;
            if (startPos < 0) {
                this.log.error("Attempt to copy to selection from before start of buffer");
            } else if (startPos + extent >= buf.length) {
                this.log.error("Attempt to copy to selection from after end of buffer");
            } else {
                this.selection.append(buf, startPos, extent);
            }
        }
    }
}