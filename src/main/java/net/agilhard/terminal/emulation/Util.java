package net.agilhard.terminal.emulation;

import java.lang.reflect.Array;
import java.util.BitSet;

// In Java 5, the java.util.Arrays class has no copyOf() members...
/**
 * The Class Util.
 */
public final class Util {

    /**
     * Private constructor for utility class.
     */
    private Util() {
        // .
    }

    /**
     * Copy of.
     *
     * @param <T>
     *            the generic type
     * @param original
     *            the original
     * @param newLength
     *            the new length
     * @return the t[]
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] copyOf(final T[] original, final int newLength) {
        final Class<T> type = (Class<T>) original.getClass().getComponentType();
        final T[] newArr = (T[]) Array.newInstance(type, newLength);

        System.arraycopy(original, 0, newArr, 0, Math.min(original.length, newLength));

        return newArr;
    }

    /**
     * Copy of.
     *
     * @param original
     *            the original
     * @param newLength
     *            the new length
     * @return the int[]
     */
    public static int[] copyOf(final int[] original, final int newLength) {
        final int[] newArr = new int[newLength];

        System.arraycopy(original, 0, newArr, 0, Math.min(original.length, newLength));

        return newArr;
    }

    /**
     * Copy of.
     *
     * @param original
     *            the original
     * @param newLength
     *            the new length
     * @return the char[]
     */
    public static char[] copyOf(final char[] original, final int newLength) {
        final char[] newArr = new char[newLength];

        System.arraycopy(original, 0, newArr, 0, Math.min(original.length, newLength));

        return newArr;
    }

    /**
     * Bitset copy.
     *
     * @param src
     *            the src
     * @param srcOffset
     *            the src offset
     * @param dest
     *            the dest
     * @param destOffset
     *            the dest offset
     * @param length
     *            the length
     */
    public static void bitsetCopy(final BitSet src, final int srcOffset, final BitSet dest, final int destOffset,
        final int length) {
        for (int i = 0; i < length; i++) {
            dest.set(destOffset + i, src.get(srcOffset + i));
        }
    }

}
