package net.agilhard.terminal.emulation;

import java.awt.Dimension;
import java.io.IOException;

import net.agilhard.jsch.UserInfo;

/**
 * The Interface Tty.
 */
public interface Tty {

    /**
     * Inits the.
     *
     * @param userInfo
     *            the user info
     * @param questioner
     *            the questioner
     * @return true, if successful
     */
    boolean init(UserInfo userInfo, Questioner questioner);

    /**
     * Close.
     */
    void close();

    /**
     * Resize.
     *
     * @param termSize
     *            the term size
     * @param pixelSize
     *            the pixel size
     */
    void resize(Dimension termSize, Dimension pixelSize);

    /**
     * Gets the name.
     *
     * @return the name
     */
    String getName();

    /**
     * Read.
     *
     * @param buf
     *            the buf
     * @param offset
     *            the offset
     * @param length
     *            the length
     * @return the int
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    int read(byte[] buf, int offset, int length) throws IOException;

    /**
     * Write.
     *
     * @param bytes
     *            the bytes
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    void write(byte[] bytes) throws IOException;

    /**
     * Gets the exit status.
     *
     * @return the exit status
     */
    int getExitStatus();

}
