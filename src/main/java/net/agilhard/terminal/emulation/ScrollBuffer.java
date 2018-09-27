package net.agilhard.terminal.emulation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ScrollBuffer.
 */
public class ScrollBuffer implements StyledRunConsumer {

    /** The Logger. */
    private final Logger log = LoggerFactory.getLogger(ScrollBuffer.class);

    /** The Constant BUF_SIZE. */
    private static final int BUF_SIZE = 8192;

    /** The Constant RUN_SIZE. */
    private static final int RUN_SIZE = 128;

    /**
     * The Class Section.
     */
    static class Section {

        /** The width. */
        //private int width;

        /** The buf. */
        private char[] buf = new char[BUF_SIZE];

        /** The run starts. */
        private int[] runStarts = new int[RUN_SIZE];

        /** The run styles. */
        private Style[] runStyles = new Style[RUN_SIZE];

        /** The line starts. */
        private BitSet lineStarts = new BitSet(RUN_SIZE);

        /**
         * Sets the line start.
         *
         * @param currentRun
         *            the new line start
         */
        public void setLineStart(@SuppressWarnings("unused") final int currentRun) {
            //.
        }

        /**
         * Put run.
         *
         * @param currentRun
         *            the current run
         * @param bufPos
         *            the buf pos
         * @param isNewLine
         *            the is new line
         * @param style
         *            the style
         * @param otherBuf
         *            the other buf
         * @param start
         *            the start
         * @param len
         *            the len
         * @return the int
         */
        public int putRun(final int currentRun, final int bufPos, final boolean isNewLine, final Style style,
            final char[] otherBuf, final int start, final int len) {
            if (bufPos + len >= this.buf.length) {
                this.complete(currentRun, bufPos);
                return -1;
            }
            this.ensureArrays(currentRun);
            this.lineStarts.set(currentRun, isNewLine);
            this.runStarts[currentRun] = bufPos;
            this.runStyles[currentRun] = style;
            System.arraycopy(otherBuf, start, this.buf, bufPos, len);

            return bufPos + len;
        }

        /**
         * Ensure arrays.
         *
         * @param currentRun
         *            the current run
         */
        private void ensureArrays(final int currentRun) {
            if (currentRun >= this.runStarts.length) {
                this.runStarts = Util.copyOf(this.runStarts, this.runStarts.length * 2);
                this.runStyles = Util.copyOf(this.runStyles, this.runStyles.length * 2);
            }
        }

        /**
         * Complete.
         *
         * @param currentRun
         *            the current run
         * @param bufPos
         *            the buf pos
         */
        public void complete(final int currentRun, final int bufPos) {
            this.runStarts = Util.copyOf(this.runStarts, currentRun);
            this.runStyles = Util.copyOf(this.runStyles, currentRun);
            this.buf = Util.copyOf(this.buf, bufPos);
            this.lineStarts = this.lineStarts.get(0, currentRun);
        }

        // for a complete section
        /**
         * Pump runs complete.
         *
         * @param firstLine
         *            the first line
         * @param startLine
         *            the start line
         * @param endLine
         *            the end line
         * @param consumer
         *            the consumer
         * @return the int
         */
        public int pumpRunsComplete(final int firstLine, final int startLine, final int endLine,
            final StyledRunConsumer consumer) {
            return this.pumpRunsImpl(firstLine, startLine, endLine, consumer, this.buf.length);
        }

        // for a current section
        /**
         * Pump runs current.
         *
         * @param firstLine
         *            the first line
         * @param startLine
         *            the start line
         * @param endLine
         *            the end line
         * @param consumer
         *            the consumer
         * @param bufPos
         *            the buf pos
         * @return the int
         */
        public int pumpRunsCurrent(final int firstLine, final int startLine, final int endLine,
            final StyledRunConsumer consumer, final int bufPos) {
            return this.pumpRunsImpl(firstLine, startLine, endLine, consumer, bufPos);
        }

        /**
         * Pump runs impl.
         *
         * @param firstLine
         *            the first line
         * @param startLine
         *            the start line
         * @param endLine
         *            the end line
         * @param consumer
         *            the consumer
         * @param bufferEnd
         *            the buffer end
         * @return the int
         */
        private int pumpRunsImpl(final int firstLine, final int startLine, final int endLine,
            final StyledRunConsumer consumer, final int bufferEnd) {
            int x = 0;
            int y = firstLine - 1;
            for (int i = 0; i < this.runStarts.length; i++) {
                if (this.lineStarts.get(i)) {
                    x = 0;
                    y++;
                }
                if (y < startLine) {
                    continue;
                }
                if (y > endLine) {
                    break;
                }
                final int runStart = this.runStarts[i];
                int runEnd;
                boolean last = false;
                // if we are at the end of the array, or the next runstart is 0 ( ie unfilled),
                // this is the last run.
                if (i == this.runStarts.length - 1 || this.runStarts[i + 1] == 0) {
                    runEnd = bufferEnd;
                    last = true;
                } else {
                    runEnd = this.runStarts[i + 1];
                }

                consumer.consumeRun(x, y, this.runStyles[i], this.buf, runStart, runEnd - runStart);
                x += runEnd - runStart;
                if (last) {
                    break;
                }
            }
            return y;
        }

        /**
         * Gets the line count.
         *
         * @return the line count
         */
        int getLineCount() {
            return this.lineStarts.cardinality();
        }
    }

    /** The complete sections. */
    private final List<Section> completeSections = new ArrayList<>();

    /** The current section. */
    private Section currentSection;

    /** The current run. */
    private int currentRun;

    /** The buf pos. */
    private int bufPos;

    /** The total lines. */
    private int totalLines;

    /**
     * Instantiates a new scroll buffer.
     */
    public ScrollBuffer() {
        this.newSection();
    }

    /**
     * New section.
     */
    private void newSection() {
        this.currentSection = new Section();
        this.currentRun = -1;
        this.bufPos = 0;
    }

    /**
     * Gets the lines.
     *
     * @return the lines
     */
    public synchronized String getLines() {
        final StringBuffer sb = new StringBuffer();

        final StyledRunConsumer consumer = new StyledRunConsumer() {

            @SuppressWarnings("unused")
            @Override
            public void consumeRun(final int x, final int y, final Style style, final char[] buf, final int start,
                final int len) {
                if (x == 0) {
                    sb.append('\n');
                }
                sb.append(buf, start, len);
            }
        };
        int currentLine = -this.totalLines;
        for (final Section s : this.completeSections) {
            currentLine = s.pumpRunsComplete(currentLine, currentLine, 0, consumer);
        }

        this.currentSection.pumpRunsCurrent(currentLine, currentLine, 0, consumer, this.bufPos);

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.StyledRunConsumer#consumeRun(int, int, net.agilhard.terminal.emulation.Style, char[], int, int)
     */
    @SuppressWarnings("unused")
    @Override
    public synchronized void consumeRun(final int x, final int y, final Style style, final char[] buf, final int start,
        final int len) {
        this.currentRun++;
        final boolean isNewLine = x == 0;
        if (isNewLine) {
            this.totalLines++;
        }
        this.bufPos = this.currentSection.putRun(this.currentRun, this.bufPos, isNewLine, style, buf, start, len);
        if (this.bufPos < 0) {

            this.completeSections.add(this.currentSection);
            this.newSection();
            this.currentRun++;
            this.bufPos = this.currentSection.putRun(this.currentRun, this.bufPos, isNewLine, style, buf, start, len);
            if (this.bufPos < 0) {
                this.log.error("Can not put run in new section, bailing out");
            }
        }
    }

    /**
     * Gets the line count.
     *
     * @return the line count
     */
    public int getLineCount() {
        return this.totalLines;
    }

    /**
     * Pump runs.
     *
     * @param firstLine
     *            the first line
     * @param height
     *            the height
     * @param consumer
     *            the consumer
     */
    public void pumpRuns(final int firstLine, final int height, final StyledRunConsumer consumer) {
        // firstLine is negative . 0 is the first line in the back buffer.
        // Find start Section
        int currentLine = -this.currentSection.getLineCount();
        final int lastLine = firstLine + height;
        if (currentLine > firstLine) {
            //Need to look at past sections
            //Look back through them to find the one that contains our first line.
            int i = this.completeSections.size() - 1;
            for (; i >= 0; i--) {
                currentLine -= this.completeSections.get(i).getLineCount();
                if (currentLine <= firstLine) {
                    // This section contains our first line.
                    break;
                }
            }
            i = Math.max(i, 0); // if they requested before this scroll buffer return as much as possible.
            for (; i < this.completeSections.size(); i++) {
                final int startLine = Math.max(firstLine, currentLine);
                final Section s = this.completeSections.get(i);
                currentLine = s.pumpRunsComplete(currentLine, startLine, lastLine, consumer);
                if (currentLine >= lastLine) {
                    break;
                }
            }
        }
        if (currentLine < lastLine) {
            this.currentSection.pumpRunsCurrent(currentLine, Math.max(firstLine, currentLine), lastLine, consumer,
                this.bufPos);
        }
    }

}
