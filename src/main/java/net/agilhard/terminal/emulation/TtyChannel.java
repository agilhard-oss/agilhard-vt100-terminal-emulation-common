/**
 *
 */
package net.agilhard.terminal.emulation;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * The Class TtyChannel.
 */
public class TtyChannel {

    /** The tty. */
    private final Tty tty;

    // CHECKSTYLE:OFF
    /** The buf. */
    byte[] buf = new byte[1024];

    /** The offset. */
    int offset;

    /** The length. */
    int length;

    /** The serial. */
    int serial;
    // CHECKSTYLE:ON

    /**
     * Instantiates a new tty channel.
     *
     * @param tty
     *            the tty
     */
    public TtyChannel(final Tty tty) {
        this.tty = tty;
        this.serial = 0;
    }

    /**
     * Gets the char.
     *
     * @return the char
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public byte getChar() throws java.io.IOException {
        if (this.length == 0) {
            this.fillBuf();
        }
        this.length--;

        return this.buf[this.offset++];
    }

    /**
     * Append buf.
     *
     * @param sb
     *            the sb
     * @param begin
     *            the begin
     * @param csLength
     *            the cs_length
     */
    public void appendBuf(final StringBuffer sb, final int begin, final int csLength) {
        CharacterUtils.appendBuf(sb, this.buf, begin, csLength);
    }

    /**
     * Fill buf.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void fillBuf() throws java.io.IOException {
        // CHECKSTYLE:OFF
        this.length = this.offset = 0;
        // CHECKSTYLE:ON

        this.length = this.tty.read(this.buf, this.offset, this.buf.length - this.offset);
        this.serial++;

        if (this.length <= 0) {
            this.length = 0;
            throw new InterruptedIOException("fillBuf");
        }
    }

    /**
     * Push char.
     *
     * @param b
     *            the b
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void pushChar(final byte b) throws java.io.IOException {
        if (this.offset == 0) {
            // Pushed back too many... shift it up to the end.
            this.offset = this.buf.length - this.length;
            System.arraycopy(this.buf, 0, this.buf, this.offset, this.length);
        }

        this.length++;
        this.buf[--this.offset] = b;
    }

    /**
     * Advance through ascii.
     *
     * @param toLineEnd
     *            the to line end
     * @return the int
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    int advanceThroughASCII(final int toLineEnd) throws java.io.IOException {
        if (this.length == 0) {
            this.fillBuf();
        }

        int len = toLineEnd > this.length ? this.length : toLineEnd;

        final int origLen = len;
        byte tmp;
        while (len > 0) {
            tmp = this.buf[this.offset++];
            if (0x20 <= tmp && tmp <= 0x7f) {
                this.length--;
                len--;
                continue;
            }
            this.offset--;
            break;
        }
        return origLen - len;
    }

    /**
     * Send bytes.
     *
     * @param bytes
     *            the bytes
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void sendBytes(final byte[] bytes) throws IOException {
        this.tty.write(bytes);
    }

    /**
     * Post resize.
     *
     * @param termSize
     *            the term size
     * @param pixelSize
     *            the pixel size
     */
    public void postResize(final Dimension termSize, final Dimension pixelSize) {
        this.tty.resize(termSize, pixelSize);
    }

    /**
     * Push back buffer.
     *
     * @param bytes
     *            the bytes
     * @param len
     *            the len
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void pushBackBuffer(final byte[] bytes, final int len) throws IOException {
        for (int i = len - 1; i >= 0; i--) {
            this.pushChar(bytes[i]);
        }
    }

    /**
     * Gets the tty.
     *
     * @return the tty
     */
    public Tty getTty() {
        return this.tty;
    }

}