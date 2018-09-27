package net.agilhard.terminal.emulation.shell;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.agilhard.jsch.UserInfo;
import net.agilhard.terminal.emulation.Questioner;
import net.agilhard.terminal.emulation.Tty;

/**
 * The Class PtyWindowsCmd.
 */
public class PtyWindowsCmd implements Tty {

    /** The Logger. */
    private final Logger log = LoggerFactory.getLogger(PtyWindowsCmd.class);

    /** The in. */
    private InputStream in;

    /** The out. */
    private OutputStream out;

    /** The proc. */
    private Process proc;

    /** The cmd. */
    private final String cmd;

    /** The directory. */
    private final String directory;

    /**
     * Instantiates a new pty windows cmd.
     */
    public PtyWindowsCmd() {
        this.cmd = null;
        this.directory = null;
    }

    /**
     * Instantiates a new pty windows cmd.
     *
     * @param csCmd
     *            the cs_cmd
     */
    public PtyWindowsCmd(final String csCmd) {
        this.cmd = csCmd;
        this.directory = null;
    }

    /**
     * Instantiates a new pty windows cmd.
     *
     * @param csCmd
     *            the cs_cmd
     * @param csDirectory
     *            the cs directory
     */
    public PtyWindowsCmd(final String csCmd, final String csDirectory) {
        this.cmd = csCmd;
        this.directory = csDirectory;
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#init(net.agilhard.jsch.UserInfo, net.agilhard.terminal.emulation.Questioner)
     */
    @SuppressWarnings("unused")
    @Override
    public boolean init(final UserInfo userInfo, final Questioner questioner) {
        boolean success = true;

        ProcessBuilder pb;
        if (this.cmd == null) {
            pb = new ProcessBuilder("cmd.exe", "/T:F0", "/E:ON", "/F:ON");
        } else {
            pb = new ProcessBuilder("cmd.exe", "/T:F0", "/E:ON", "/C", this.cmd);
        }

        if (this.directory != null) {
            pb.directory(new File(this.directory));
        }

        final Map<String, String> env = pb.environment();
        env.put("LANG", "en_US.utf8");

        pb.redirectErrorStream(true);

        try {
            this.proc = pb.start();
            this.in = this.proc.getInputStream();
            this.out = this.proc.getOutputStream();
            final PrintWriter pw = new PrintWriter(this.out);
            pw.write("@set TERM=vt100\r\n");
            pw.flush();
        }
        catch (final IOException ioe) {
            this.log.error("I/O Exception", ioe);
            success = false;
        }

        return success;
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#close()
     */
    @Override
    public void close() {
        if (this.out != null) {
            try {
                this.out.close();
            }
            catch (final IOException e) {
                this.log.error("I/O Exception in close:", e);
            }
        }
        if (this.in != null) {
            try {
                this.in.close();
            }
            catch (final IOException e) {
                this.log.error("I/O Exception in close:", e);
            }
        }
        if (this.proc != null) {
            this.proc.destroy();
        }

    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#resize(java.awt.Dimension, java.awt.Dimension)
     */
    @SuppressWarnings("unused")
    @Override
    public void resize(final Dimension termSize, final Dimension pixelSize) {
        // ..
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#getName()
     */
    @Override
    public String getName() {
        return "PtyWindowsCmd";
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] buf, final int offset, final int length) throws IOException {
        if (this.in != null) {
            return this.in.read(buf, offset, length);
        }
        return 0;
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#write(byte[])
     */
    @Override
    public void write(final byte[] bytes) throws IOException {
        if (this.out != null) {
            this.log.debug("PtyWindowsCmd write bytes.size=" + (bytes != null ? bytes.length : 0));
            this.out.write(bytes);
            this.out.flush();
        } else {
            this.log.warn("PtyWindowsCmd do not write bytes.size=" + (bytes != null ? bytes.length : 0));
        }
    }

    /**
     * Checks if is interactive.
     *
     * @return true, if is interactive
     */
    public boolean isInteractive() {
        return this.cmd == null;
    }

    /**
     * Gets the exit status.
     *
     * @return the exit status
     */
    @Override
    public int getExitStatus() {
        if (this.proc == null) {
            return 0;
        }
        int i = 0;
        try {
            i = this.proc.exitValue();
        }
        catch (final IllegalThreadStateException e) {
            i = 0;
        }
        return i;
    }

}
