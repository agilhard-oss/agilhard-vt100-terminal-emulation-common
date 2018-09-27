package net.agilhard.terminal.emulation;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_F1;
import static java.awt.event.KeyEvent.VK_F10;
import static java.awt.event.KeyEvent.VK_F2;
import static java.awt.event.KeyEvent.VK_F3;
import static java.awt.event.KeyEvent.VK_F4;
import static java.awt.event.KeyEvent.VK_F5;
import static java.awt.event.KeyEvent.VK_F6;
import static java.awt.event.KeyEvent.VK_F7;
import static java.awt.event.KeyEvent.VK_F8;
import static java.awt.event.KeyEvent.VK_F9;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class CharacterUtils.
 */
public final class CharacterUtils {

    /** The Constant NUL. */
    public static final int NUL = 0x00;

    /** The Constant SOH. */
    public static final int SOH = 0x01;

    /** The Constant STX. */
    public static final int STX = 0x02;

    /** The Constant ETX. */
    public static final int ETX = 0x03;

    /** The Constant EOT. */
    public static final int EOT = 0x04;

    /** The Constant ENQ. */
    public static final int ENQ = 0x05;

    /** The Constant ACK. */
    public static final int ACK = 0x06;

    /** The Constant BEL. */
    public static final int BEL = 0x07;

    /** The Constant BS. */
    public static final int BS = 0x08;

    /** The Constant TAB. */
    public static final int TAB = 0x09;

    /** The Constant LF. */
    public static final int LF = 0x0a;

    /** The Constant VT. */
    public static final int VT = 0x0b;

    /** The Constant FF. */
    public static final int FF = 0x0c;

    /** The Constant CR. */
    public static final int CR = 0x0d;

    /** The Constant SO. */
    public static final int SO = 0x0e;

    /** The Constant SI. */
    public static final int SI = 0x0f;

    /** The Constant DLE. */
    public static final int DLE = 0x10;

    /** The Constant DC1. */
    public static final int DC1 = 0x11;

    /** The Constant DC2. */
    public static final int DC2 = 0x12;

    /** The Constant DC3. */
    public static final int DC3 = 0x13;

    /** The Constant DC4. */
    public static final int DC4 = 0x14;

    /** The Constant NAK. */
    public static final int NAK = 0x15;

    /** The Constant SYN. */
    public static final int SYN = 0x16;

    /** The Constant ETB. */
    public static final int ETB = 0x17;

    /** The Constant CAN. */
    public static final int CAN = 0x18;

    /** The Constant EM. */
    public static final int EM = 0x19;

    /** The Constant SUB. */
    public static final int SUB = 0x1a;

    /** The Constant ESC. */
    public static final int ESC = 0x1b;

    /** The Constant FS. */
    public static final int FS = 0x1c;

    /** The Constant GS. */
    public static final int GS = 0x1d;

    /** The Constant RS. */
    public static final int RS = 0x1e;

    /** The Constant US. */
    public static final int US = 0x1f;

    /** The Constant DEL. */
    public static final int DEL = 0x7f;

    /**
     * Instantiates a new character utils.
     */
    private CharacterUtils() {
        //.
    }

    /** The Constant NONPRINTING_NAMES. */
    private static final String[] NONPRINTING_NAMES =
        { "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL", "BS", "TAB", "LF", "VT", "FF", "CR", "S0", "S1",
            "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB", "CAN", "EM", "SUB", "ESC", "FS", "GS", "RS", "US" };

    /**
     * The Enum CharacterType.
     */
    enum CharacterType {

        /** The nonprinting. */
        NONPRINTING,

        /** The printing. */
        PRINTING,

        /** The nonascii. */
        NONASCII,
        /** The none. */
        NONE
    }

    /**
     * Append char.
     *
     * @param sb
     *            the sb
     * @param last
     *            the last
     * @param c
     *            the c
     * @return the character type
     */
    public static CharacterType appendChar(final StringBuffer sb, final CharacterType last, final char c) {
        if (c <= 0x1F) {
            sb.append(' ');
            sb.append(CharacterUtils.NONPRINTING_NAMES[c]);
            return CharacterType.NONPRINTING;
        } else if (c == DEL) {
            sb.append(" DEL");
            return CharacterType.NONPRINTING;
        } else if (c > 0x1F && c <= 0x7E) {
            if (last != CharacterType.PRINTING) {
                sb.append(' ');
            }
            sb.append(c);
            return CharacterType.PRINTING;
        } else {
            sb.append(" 0x").append(Integer.toHexString(c));
            return CharacterType.NONASCII;
        }
    }

    /**
     * Append buf.
     *
     * @param sb
     *            the sb
     * @param bs
     *            the bs
     * @param begin
     *            the begin
     * @param length
     *            the length
     */
    public static void appendBuf(final StringBuffer sb, final byte[] bs, final int begin, final int length) {
        CharacterType last = CharacterType.NONPRINTING;
        final int end = begin + length;
        for (int i = begin; i < end; i++) {
            final char c = (char) bs[i];
            last = appendChar(sb, last, c);
        }
    }

    /** The device attributes response. */
    public static final byte[] DEVICE_ATTRIBUTES_RESPONSE = makeCode(ESC, '[', '?', '6', 'c');

    /** The Constant codes. */
    private static final Map<Integer, byte[]> CODES = new HashMap<>();

    /**
     * Put code.
     *
     * @param code
     *            the code
     * @param bytesAsInt
     *            the bytes as int
     */
    @SuppressWarnings("boxing")
    static void putCode(final int code, final int... bytesAsInt) {
        CODES.put(code, makeCode(bytesAsInt));
    }

    /**
     * Make code.
     *
     * @param bytesAsInt
     *            the bytes as int
     * @return the byte[]
     */
    private static byte[] makeCode(final int... bytesAsInt) {
        final byte[] bytes = new byte[bytesAsInt.length];
        int i = 0;
        for (final int byteAsInt : bytesAsInt) {
            bytes[i] = (byte) byteAsInt;
            i++;
        }
        return bytes;
    }

    static {
        putCode(VK_ENTER, CR);
        putCode(VK_UP, ESC, 'O', 'A');
        putCode(VK_DOWN, ESC, 'O', 'B');
        putCode(VK_RIGHT, ESC, 'O', 'C');
        putCode(VK_LEFT, ESC, 'O', 'D');
        putCode(VK_F1, ESC, 'O', 'P');
        putCode(VK_F2, ESC, 'O', 'Q');
        putCode(VK_F3, ESC, 'O', 'R');
        putCode(VK_F4, ESC, 'O', 'S');
        putCode(VK_F5, ESC, 'O', 't');
        putCode(VK_F6, ESC, 'O', 'u');
        putCode(VK_F7, ESC, 'O', 'v');
        putCode(VK_F8, ESC, 'O', 'I');
        putCode(VK_F9, ESC, 'O', 'w');
        putCode(VK_F10, ESC, 'O', 'x');
    }

    /**
     * Gets the code.
     *
     * @param key
     *            the key
     * @return the code
     */
    @SuppressWarnings("boxing")
    public static byte[] getCode(final int key) {
        return CODES.get(key);
    }

}
