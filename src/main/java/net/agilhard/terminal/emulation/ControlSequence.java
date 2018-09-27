/**
 *
 */
package net.agilhard.terminal.emulation;

import java.io.IOException;
import java.util.ArrayList;

import net.agilhard.terminal.emulation.CharacterUtils.CharacterType;

/**
 * The Class ControlSequence.
 */
public class ControlSequence {

    /** The argc. */
    private int argc;

    /** The argv. */
    private final int[] argv;

    /** The mode table. */
    private Mode[] modeTable;

    /** The final char. */
    private byte finalChar;

    /** The start in buf. */
    private int startInBuf;

    /** The length in buf. */
    private int lengthInBuf;

    /** The buffer version. */
    private int bufferVersion;

    /** The normal modes. */
    private static Mode[] normalModes = {

    };

    /** The question mark modes. */
    private static Mode[] questionMarkModes = { Mode.Null, Mode.CursorKey, Mode.ANSI, Mode.WideColumn,
        Mode.SmoothScroll, Mode.ReverseScreen, Mode.RelativeOrigin, Mode.WrapAround, Mode.AutoRepeat, Mode.Interlace };

    /** The unhandled chars. */
    private ArrayList<Byte> unhandledChars;

    /**
     * Instantiates a new control sequence.
     *
     * @param channel
     *            the channel
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    ControlSequence(final TtyChannel channel) throws IOException {
        this.argv = new int[10];
        this.argc = 0;
        this.modeTable = normalModes;
        this.readControlSequence(channel);
    }

    /**
     * Read control sequence.
     *
     * @param channel
     *            the channel
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void readControlSequence(final TtyChannel channel) throws IOException {
        this.argc = 0;
        // Read integer arguments
        int digit = 0;
        int seenDigit = 0;
        int pos = -1;

        this.bufferVersion = channel.serial;
        this.startInBuf = channel.offset;

        while (true) {
            final byte b = channel.getChar();
            pos++;
            if (b == '?' && pos == 0) {
                this.modeTable = questionMarkModes;
            } else if (b == ';') {
                if (digit > 0) {
                    this.argc++;
                    this.argv[this.argc] = 0;
                    digit = 0;
                }
            } else if ('0' <= b && b <= '9') {
                this.argv[this.argc] = this.argv[this.argc] * 10 + b - '0';
                digit++;
                seenDigit = 1;
                continue;
            } else if (':' <= b && b <= '?') {
                this.addUnhandled(b);
            } else if (0x40 <= b && b <= 0x7E) {
                this.finalChar = b;
                break;
            } else {
                this.addUnhandled(b);
            }
        }
        if (this.bufferVersion == channel.serial) {
            this.lengthInBuf = channel.offset - this.startInBuf;
        } else {
            this.lengthInBuf = -1;
        }
        this.argc += seenDigit;
    }

    /**
     * Adds the unhandled.
     *
     * @param b
     *            the b
     */
    @SuppressWarnings("boxing")
    private void addUnhandled(final byte b) {
        if (this.unhandledChars == null) {
            this.unhandledChars = new ArrayList<>();
        }
        this.unhandledChars.add(b);
    }

    /**
     * Push back reordered.
     *
     * @param channel
     *            the channel
     * @return true, if successful
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public boolean pushBackReordered(final TtyChannel channel) throws IOException {
        if (this.unhandledChars == null) {
            return false;
        }
        final byte[] bytes = new byte[1024]; // can't be more than the whole buffer...
        int i = 0;
        for (final byte b : this.unhandledChars) {
            bytes[i++] = b;
        }
        bytes[i++] = (byte) CharacterUtils.ESC;
        bytes[i++] = (byte) '[';

        if (this.modeTable == questionMarkModes) {
            bytes[i++] = (byte) '?';
        }
        for (int argi = 0; argi < this.argc; argi++) {
            if (argi != 0) {
                bytes[i++] = (byte) ';';
            }
            for (final byte b : Integer.toString(this.argv[argi]).getBytes("UTF-8")) {
                bytes[i++] = b;
            }
        }
        bytes[i++] = this.finalChar;
        channel.pushBackBuffer(bytes, i);
        return true;
    }

    /**
     * Gets the count.
     *
     * @return the count
     */
    int getCount() {
        return this.argc;
    }

    /**
     * Gets the arg.
     *
     * @param index
     *            the index
     * @param def
     *            the def
     * @return the arg
     */
    final int getArg(final int index, final int def) {
        if (index >= this.argc) {
            return def;
        }
        return this.argv[index];
    }

    /**
     * Append to buffer.
     *
     * @param sb
     *            the sb
     */
    public final void appendToBuffer(final StringBuffer sb) {
        sb.append("ESC[");
        if (this.modeTable == questionMarkModes) {
            sb.append("?");
        }

        String sep = "";
        for (int i = 0; i < this.argc; i++) {
            sb.append(sep);
            sb.append(this.argv[i]);
            sep = ";";
        }
        sb.append((char) this.finalChar);

        if (this.unhandledChars != null) {
            sb.append(" Unhandled:");
            CharacterType last = CharacterType.NONE;
            for (final byte b : this.unhandledChars) {
                last = CharacterUtils.appendChar(sb, last, (char) b);
            }
        }
    }

    /**
     * Append actual bytes read.
     *
     * @param sb
     *            the sb
     * @param buffer
     *            the buffer
     */
    public final void appendActualBytesRead(final StringBuffer sb, final TtyChannel buffer) {
        if (this.lengthInBuf == -1) {
            sb.append("TermIOBuffer filled in reading");
        } else if (this.bufferVersion != buffer.serial) {
            sb.append("TermIOBuffer filled after reading");
        } else {
            buffer.appendBuf(sb, this.startInBuf, this.lengthInBuf);
        }
    }

    /**
     * Gets the final char.
     *
     * @return the final char
     */
    public byte getFinalChar() {
        return this.finalChar;
    }

    /**
     * Gets the mode table.
     *
     * @return the mode table
     */
    public Mode[] getModeTable() {
        return this.modeTable;
    }

}