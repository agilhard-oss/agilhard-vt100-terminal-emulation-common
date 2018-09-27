/* -*-mode:java; c-basic-offset:2; -*- */
/*
 * JCTerm
 * Copyright (C) 2002 ymnk, JCraft,Inc.
 * Written by: 2002 ymnk<ymnk@jcaft.com>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Library General Public License for more details.
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.agilhard.terminal.emulation;

import static net.agilhard.terminal.emulation.CharacterUtils.BEL;
import static net.agilhard.terminal.emulation.CharacterUtils.BS;
import static net.agilhard.terminal.emulation.CharacterUtils.CR;
import static net.agilhard.terminal.emulation.CharacterUtils.DEVICE_ATTRIBUTES_RESPONSE;
import static net.agilhard.terminal.emulation.CharacterUtils.ESC;
import static net.agilhard.terminal.emulation.CharacterUtils.FF;
import static net.agilhard.terminal.emulation.CharacterUtils.LF;
import static net.agilhard.terminal.emulation.CharacterUtils.TAB;
import static net.agilhard.terminal.emulation.CharacterUtils.VT;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.agilhard.terminal.emulation.CharacterUtils.CharacterType;

/**
 * The Class Emulator.
 */
public class Emulator {

    /** The controller. */
    private final TerminalEmulationController controller;

    /** The session running. */
    private final AtomicBoolean sessionRunning = new AtomicBoolean();

    /** The Logger. */
    private final Logger log = LoggerFactory.getLogger(Emulator.class);

    /** The tw. */
    private final TerminalWriter tw;

    /** The channel. */
    private final TtyChannel channel;

    /**
     * Instantiates a new emulator.
     *
     * @param tw
     *            the tw
     * @param channel
     *            the channel
     * @param controller
     *            the controller
     */
    public Emulator(final TerminalWriter tw, final TtyChannel channel, final TerminalEmulationController controller) {
        this.channel = channel;
        this.controller = controller;
        this.tw = tw;
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
        if (this.sessionRunning.get()) {
            this.channel.sendBytes(bytes);
        }
    }

    /**
     * Start.
     */
    public void start() {
        this.go();
    }

    /**
     * Gets the code.
     *
     * @param key
     *            the key
     * @return the code
     */
    public byte[] getCode(final int key) {
        return CharacterUtils.getCode(key);
    }

    /**
     * Go.
     */
    void go() {
        this.sessionRunning.set(true);
        boolean hasError = false;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                this.singleIteration();
            }
        }
        catch (final InterruptedIOException e) {
            this.log.info("Terminal exiting");
        }
        catch (final Exception e) {
            this.log.error("Caught exception in terminal thread", e);
            hasError = true;
        }
        this.sessionRunning.set(false);

        if (this.controller != null) {
            if (hasError && this.controller.isCloseOnError()) {
                this.controller.close();
            } else if (this.controller.isCloseOnExit()) {
                this.controller.close();
            }
        }

    }

    /**
     * Post resize.
     *
     * @param dimension
     *            the dimension
     * @param origin
     *            the origin
     */
    public void postResize(final Dimension dimension, final RequestOrigin origin) {
        Dimension pixelSize;
        synchronized (this.tw) {
            pixelSize = this.tw.resize(dimension, origin);
        }
        this.channel.postResize(dimension, pixelSize);
    }

    /**
     * Single iteration.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    void singleIteration() throws IOException {
        byte b = this.channel.getChar();

        switch (b) {
        case 0:
            break;
        case ESC: // ESC
            b = this.channel.getChar();
            this.handleESC(b);
            break;
        case BEL:
            this.tw.beep();
            break;
        case BS:
            this.tw.backspace();
            break;
        case TAB: // ht(^I) TAB
            this.tw.horizontalTab();
            break;
        case CR:
            this.tw.carriageReturn();
            break;
        case FF:
        case VT:
        case LF:
            // '\n'
            this.tw.newLine();
            break;
        default:
            if (b <= CharacterUtils.US) {
                if (this.log.isInfoEnabled()) {
                    final StringBuffer sb = new StringBuffer("Unhandled control character:");
                    CharacterUtils.appendChar(sb, CharacterType.NONE, (char) b);
                    this.log.info(sb.toString());
                }
            } else if (b > CharacterUtils.DEL) {
                // TODO: double byte character.. this is crap
                final byte[] bytesOfChar = new byte[2];
                bytesOfChar[0] = b;
                bytesOfChar[1] = this.channel.getChar();
                this.tw.writeDoubleByte(bytesOfChar);
            } else {
                this.channel.pushChar(b);
                final int availableChars = this.channel.advanceThroughASCII(this.tw.distanceToLineEnd());
                this.tw.writeASCII(this.channel.buf, this.channel.offset - availableChars, availableChars);
            }
            break;
        }
    }

    /**
     * Handle esc.
     *
     * @param initByte
     *            the init byte
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void handleESC(final byte initByte) throws IOException {
        byte b = initByte;
        if (b == '[') {
            this.doControlSequence();
        } else {
            final byte[] intermediate = new byte[10];
            int intCount = 0;
            while (b >= 0x20 && b <= 0x2F) {
                intCount++;
                intermediate[intCount - 1] = b;
                b = this.channel.getChar();
            }
            if (b >= 0x30 && b <= 0x7E) {
                synchronized (this.tw) {
                    switch (b) {
                    case 'M':
                        // Reverse index ESC M
                        this.tw.reverseIndex();
                        break;
                    case 'D':
                        // Index ESC D
                        this.tw.index();
                        break;
                    case 'E':
                        this.tw.nextLine();
                        break;
                    case '7':
                        this.saveCursor();
                        break;
                    case '8':
                        if (intCount > 0 && intermediate[0] == '#') {
                            this.tw.fillScreen('E');
                        } else {
                            this.restoreCursor();
                        }
                        break;
                    default:
                        if (this.log.isDebugEnabled()) {
                            this.log.debug("Unhandled escape sequence : "
                                + this.escapeSequenceToString(intermediate, intCount, b));
                        }
                    }
                }
            } else {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Malformed escape sequence, pushing back to buffer: "
                        + this.escapeSequenceToString(intermediate, intCount, b));
                }
                // Push backwards
                for (int i = intCount - 1; i >= 0; i--) {
                    final byte ib = intermediate[i];
                    this.channel.pushChar(ib);
                }
                this.channel.pushChar(b);
            }
        }
    }

    /** The stored cursor. */
    private StoredCursor storedCursor;

    /**
     * Save cursor.
     */
    private void saveCursor() {

        if (this.storedCursor == null) {
            this.storedCursor = new StoredCursor();
        }
        this.tw.storeCursor(this.storedCursor);
    }

    /**
     * Restore cursor.
     */
    private void restoreCursor() {
        this.tw.restoreCursor(this.storedCursor);
    }

    /**
     * Escape sequence to string.
     *
     * @param intermediate
     *            the intermediate
     * @param intCount
     *            the int count
     * @param b
     *            the b
     * @return the string
     */
    private String escapeSequenceToString(final byte[] intermediate, final int intCount, final byte b) {

        final StringBuffer sb = new StringBuffer("ESC ");

        for (int i = 0; i < intCount; i++) {
            final byte ib = intermediate[i];
            sb.append(' ');
            sb.append((char) ib);
        }
        sb.append(' ');
        sb.append((char) b);
        return sb.toString();
    }

    /**
     * Do control sequence.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void doControlSequence() throws IOException {
        final ControlSequence cs = new ControlSequence(this.channel);

        if (this.log.isDebugEnabled()) {
            final StringBuffer sb = new StringBuffer();
            sb.append("Control sequence\n");
            sb.append("parsed                        :");
            cs.appendToBuffer(sb);
            sb.append('\n');
            sb.append("bytes read                    :ESC[");
            cs.appendActualBytesRead(sb, this.channel);
            this.log.debug(sb.toString());
        }
        if (cs.pushBackReordered(this.channel)) {
            return;
        }

        synchronized (this.tw) {

            switch (cs.getFinalChar()) {
            case 'm':
                this.tw.setCharacterAttributes(cs);
                break;
            case 'r':
                this.tw.setScrollingRegion(cs);
                break;
            case 'A':
                this.tw.cursorUp(cs);
                break;
            case 'B':
                this.tw.cursorDown(cs);
                break;
            case 'C':
                this.tw.cursorForward(cs);
                break;
            case 'D':
                this.tw.cursorBackward(cs);
                break;
            case 'f':
            case 'H':
                this.tw.cursorPosition(cs);
                break;
            case 'K':
                this.tw.eraseInLine(cs);
                break;
            case 'J':
                this.tw.eraseInDisplay(cs);
                break;
            case 'h':
                this.setModes(cs, true);
                break;
            case 'l':
                this.setModes(cs, false);
                break;
            case 'c':
                // What are you
                // ESC [ c or ESC [ 0 c
                // Response is ESC [ ? 6 c
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Identifying to remote system as VT102");
                }
                this.channel.sendBytes(DEVICE_ATTRIBUTES_RESPONSE);
                break;
            default:
                if (this.log.isInfoEnabled()) {
                    final StringBuffer sb = new StringBuffer();
                    sb.append("Unhandled Control sequence\n");
                    sb.append("parsed                        :");
                    cs.appendToBuffer(sb);
                    sb.append('\n');
                    sb.append("bytes read                    :ESC[");
                    cs.appendActualBytesRead(sb, this.channel);
                    this.log.info(sb.toString());
                }
                break;
            }
        }
    }

    /**
     * Sets the modes.
     *
     * @param args
     *            the args
     * @param on
     *            the on
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void setModes(final ControlSequence args, final boolean on) throws IOException {
        final int argCount = args.getCount();
        final Mode[] modeTable = args.getModeTable();
        for (int i = 0; i < argCount; i++) {
            final int num = args.getArg(i, -1);
            Mode mode = null;
            if (num >= 0 && num < modeTable.length) {
                mode = modeTable[num];
            }

            if (mode == null) {
                if (this.log.isInfoEnabled()) {
                    this.log.info("Unknown mode " + num);
                }
            } else if (on) {
                if (this.log.isInfoEnabled()) {
                    this.log.info("Modes: adding " + mode);
                }
                this.tw.setMode(mode);

            } else {
                if (this.log.isInfoEnabled()) {
                    this.log.info("Modes: removing " + mode);
                }
                this.tw.unsetMode(mode);
            }
        }
    }

    /**
     * Gets the session running.
     *
     * @return the session running
     */
    public AtomicBoolean getSessionRunning() {
        return this.sessionRunning;
    }

    /**
     * Gets the controller.
     *
     * @return the controller
     */
    public TerminalEmulationController getController() {
        return this.controller;
    }

    /**
     * Gets the channel.
     *
     * @return the channel
     */
    public TtyChannel getChannel() {
        return this.channel;
    }

    /**
     * Gets the terminal writer.
     *
     * @return the terminal writer
     */
    public TerminalWriter getTerminalWriter() {
        return this.tw;
    }

}
