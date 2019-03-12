// CHECKSTYLE:OFF
package net.agilhard.terminal.emulation.shell;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import net.agilhard.jsch.UserInfo;
import net.agilhard.terminal.emulation.Questioner;
import net.agilhard.terminal.emulation.Tty;

/**
 * The Class JNAPtyLinux.
 */
public class JNAPtyLinux implements Tty {

    /** The Logger. */
    private final Logger log = LoggerFactory.getLogger(JNAPtyLinux.class);

    /* c_cc characters */
    /** The Constant VINTR. */
    static final int VINTR = 0;

    /** The Constant VQUIT. */
    static final int VQUIT = 1;

    /** The Constant VERASE. */
    static final int VERASE = 2;

    /** The Constant VKILL. */
    static final int VKILL = 3;

    /** The Constant VEOF. */
    static final int VEOF = 4;

    /** The Constant VTIME. */
    static final int VTIME = 5;

    /** The Constant VMIN. */
    static final int VMIN = 6;

    /** The Constant VSWTC. */
    static final int VSWTC = 7;

    /** The Constant VSTART. */
    static final int VSTART = 8;

    /** The Constant VSTOP. */
    static final int VSTOP = 9;

    /** The Constant VSUSP. */
    static final int VSUSP = 10;

    /** The Constant VEOL. */
    static final int VEOL = 11;

    /** The Constant VREPRINT. */
    static final int VREPRINT = 12;

    /** The Constant VDISCARD. */
    static final int VDISCARD = 13;

    /** The Constant VWERASE. */
    static final int VWERASE = 14;

    /** The Constant VLNEXT. */
    static final int VLNEXT = 15;

    /** The Constant VEOL2. */
    static final int VEOL2 = 16;

    /* c_iflag bits */
    /** The Constant IGNBRK. */
    static final int IGNBRK = 0000001;

    /** The Constant BRKINT. */
    static final int BRKINT = 0000002;

    /** The Constant IGNPAR. */
    static final int IGNPAR = 0000004;

    /** The Constant PARMRK. */
    static final int PARMRK = 0000010;

    /** The Constant INPCK. */
    static final int INPCK = 0000020;

    /** The Constant ISTRIP. */
    static final int ISTRIP = 0000040;

    /** The Constant INLCR. */
    static final int INLCR = 0000100;

    /** The Constant IGNCR. */
    static final int IGNCR = 0000200;

    /** The Constant ICRNL. */
    static final int ICRNL = 0000400;

    /** The Constant IUCLC. */
    static final int IUCLC = 0001000;

    /** The Constant IXON. */
    static final int IXON = 0002000;

    /** The Constant IXANY. */
    static final int IXANY = 0004000;

    /** The Constant IXOFF. */
    static final int IXOFF = 0010000;

    /** The Constant IMAXBEL. */
    static final int IMAXBEL = 0020000;

    /** The Constant IUTF8. */
    static final int IUTF8 = 0040000;

    /* c_oflag bits */
    /** The Constant OPOST. */
    static final int OPOST = 0000001;

    /** The Constant OLCUC. */
    static final int OLCUC = 0000002;

    /** The Constant ONLCR. */
    static final int ONLCR = 0000004;

    /** The Constant OCRNL. */
    static final int OCRNL = 0000010;

    /** The Constant ONOCR. */
    static final int ONOCR = 0000020;

    /** The Constant ONLRET. */
    static final int ONLRET = 0000040;

    /** The Constant OFILL. */
    static final int OFILL = 0000100;

    /** The Constant OFDEL. */
    static final int OFDEL = 0000200;

    /** The Constant CS8. */
    static final int CS8 = 0000060;

    /** The Constant CREAD. */
    static final int CREAD = 0000200;

    /** The Constant CLOCAL. */
    static final int CLOCAL = 0004000;

    /** The Constant B9600. */
    static final int B9600 = 0000015;

    /** The Constant ECHO. */
    static final int ECHO = 0000010;

    /** The Constant ECHOE. */
    static final int ECHOE = 0000020;

    /** The Constant ECHOK. */
    static final int ECHOK = 0000040;

    /** The Constant ECHONL. */
    static final int ECHONL = 0000100;

    /** The Constant ECHOCTL. */
    static final int ECHOCTL = 0001000;

    /** The Constant ECHOPRT. */
    static final int ECHOPRT = 0002000;

    /** The Constant ECHOKE. */
    static final int ECHOKE = 0004000;

    /** The Constant ISIG. */
    static final int ISIG = 0000001;

    /** The Constant ICANON. */
    static final int ICANON = 0000002;

    /** The Constant TIOCGWINSZ. */
    public static final int TIOCGWINSZ =
        System.getProperty("os.name").equalsIgnoreCase("Linux") ? (int) 0x5413 : (int) 1074295912;

    public static final int TIOCSWINSZ =
        System.getProperty("os.name").equalsIgnoreCase("Linux") ? (int) 0x5414 : (int) -2146929561;

    /** The fd_buf. */
    private final IntByReference fdBuf = new IntByReference();

    /** The cmd. */
    private final String cmd;

    /** The args. */
    private final String[] args;

    /** The exit status. */
    private int exitStatus;

    /** The pid. */
    private int pid;

    /** The directory. */
    private final String directory;

    /**
     * The Class termios.
     */
    public static class termios extends Structure {

        /** The c_iflag. */
        public int c_iflag; /* input mode flags */

        /** The c_oflag. */
        public int c_oflag; /* output mode flags */

        /** The c_cflag. */
        public int c_cflag; /* control mode flags */

        /** The c_lflag. */
        public int c_lflag; /* local mode flags */

        /** The c_line. */
        public byte c_line; /* line discipline */

        /** The c_cc. */
        public byte[] c_cc = new byte[32]; /* control characters */

        /** The c_ispeed. */
        public int c_ispeed; /* input speed */

        /** The c_ospeed. */
        public int c_ospeed; /* output speed */

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                new String[] { "c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "c_cc", "c_ispeed", "c_ospeed" });
        }

    }

    /**
     * The Class winsize.
     */
    public static class winsize extends Structure {

        /** The ws_row. */
        public short ws_row;

        /** The ws_col. */
        public short ws_col;

        /** The ws_xpixel. */
        public short ws_xpixel;

        /** The ws_ypixel. */
        public short ws_ypixel;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ws_row", "ws_col", "ws_xpixel", "ws_ypixel" });
        }
    }

    /**
     * The Interface CLibrary.
     */
    public interface CLibrary extends Library {

        /** The instance. */
        static CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);

        /**
         * Read.
         *
         * @param fd
         *            the fd
         * @param buf
         *            the buf
         * @param count
         *            the count
         * @return the int
         */
        int read(int fd, byte[] buf, int count);

        /**
         * Write.
         *
         * @param fd
         *            the fd
         * @param buf
         *            the buf
         * @param count
         *            the count
         * @return the int
         */
        int write(int fd, byte[] buf, int count);

        /**
         * Close.
         *
         * @param fd
         *            the fd
         * @return the int
         */
        int close(int fd);

        /**
         * Execv.
         *
         * @param path
         *            the path
         * @param args
         *            the args
         * @return the int
         */
        int execv(String path, String[] args);

        /**
         * Setenv.
         *
         * @param name
         *            the name
         * @param value
         *            the value
         * @param overwrite
         *            the overwrite
         * @return the int
         */
        int setenv(String name, String value, int overwrite);

        /**
         * Gets the env.
         *
         * @param name
         *            the name
         * @return the env
         */
        String getenv(String name);

        /**
         * Ioctl.
         *
         * @param d
         *            the d
         * @param request
         *            the request
         * @param args
         *            the args
         * @return the int
         */
        int ioctl(int d, int request, Object... args);

    }

    /*
    public interface UtilLibrary extends Library {
        static UtilLibrary INSTANCE = (UtilLibrary) Native.loadLibrary("util", UtilLibrary.class);

        int forkpty(IntByReference amaster, String name, termios termp, winsize winp);
    }
    */

    /**
     * The Interface PtyLibrary.
     */
    public interface PtyLibrary extends Library {

        /** The instance. */
        static PtyLibrary INSTANCE = (PtyLibrary) Native.loadLibrary("agilhard-lib-pty-1.0-SNAPSHOT.so", PtyLibrary.class);

        /**
         * Startpty.
         *
         * @param amaster
         *            the amaster
         * @param termp
         *            the termp
         * @param winp
         *            the winp
         * @param cmd
         *            the cmd
         * @param directory
         *            the directory
         * @param argv
         *            the argv
         * @return the int
         */
        int startpty(IntByReference amaster, termios termp, winsize winp, String cmd, String directory, String argv[]);

        /**
         * Wait for pid.
         *
         * @param pid
         *            the pid
         * @return the int
         */
        int waitForPid(int pid);
    }

    /**
     * Instantiates a new jNA pty linux.
     */
    public JNAPtyLinux() {
        this.cmd = "/bin/bash";
        this.args = new String[2];
        this.args[0] = "bash";
        this.args[1] = "-i";
        this.directory = null;
    }

    /**
     * Instantiates a new jNA pty linux.
     *
     * @param cmd
     *            the cmd
     */
    public JNAPtyLinux(final String cmd) {
        this.cmd = "/bin/bash";
        this.args = new String[cmd == null ? 2 : 4];
        this.args[0] = "bash";
        this.args[1] = "-i";
        if (cmd != null) {
            this.args[2] = "-c";
            this.args[3] = cmd;
        }
        this.directory = null;
    }

    /**
     * Instantiates a new jNA pty linux.
     *
     * @param cmd
     *            the cmd
     * @param directory
     *            the directory
     */
    public JNAPtyLinux(final String cmd, final String directory) {
        this.cmd = "/bin/bash";
        this.args = new String[cmd == null ? 2 : 4];
        this.args[0] = "bash";
        this.args[1] = "-i";
        if (cmd != null) {
            this.args[2] = "-c";
            this.args[3] = cmd;
        }
        this.directory = directory;
    }

    /**
     * Start.
     *
     * @param cs_cmd
     *            the cs_cmd
     * @param cs_directory
     *            the cs_directory
     * @param cs_args
     *            the cs_args
     * @return the int
     */
    public int start(final String cs_cmd, final String cs_directory, final String... cs_args) {
        final termios t = new termios();

        t.c_iflag = IXON | IXOFF | ICRNL; /* ICRNL? */
        t.c_oflag = OPOST | ONLCR;
        t.c_cflag = CS8 | CREAD | CLOCAL | B9600; /* HUPCL? CRTSCTS? */
        t.c_lflag = ICANON | ISIG | ECHO | ECHOE | ECHOK | ECHOKE | ECHOCTL;
        t.c_cc[VSTART] = 'Q' & 0x1F;
        t.c_cc[VSTOP] = 'S' & 0x1F;
        t.c_cc[VERASE] = 0x7F;
        t.c_cc[VKILL] = 'U' & 0x1F;
        t.c_cc[VINTR] = 'C' & 0x1F;
        t.c_cc[VQUIT] = '\\' & 0x1F;
        t.c_cc[VEOF] = 'D' & 0x1F;
        t.c_cc[VSUSP] = 'Z' & 0x1F;
        t.c_cc[VWERASE] = 'W' & 0x1F;
        t.c_cc[VREPRINT] = 'R' & 0x1F;

        final winsize w = new winsize();
        w.ws_row = 24;
        w.ws_col = 80;

        this.fdBuf.setValue(0);

        this.pid = PtyLibrary.INSTANCE.startpty(this.fdBuf, t, w, cs_cmd, cs_directory, cs_args);
        return 0;
    }

    /**
     * Read full.
     *
     * @param buf
     *            the buf
     * @param s
     *            the s
     * @param len
     *            the len
     * @return the int
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public int readFull(final byte[] buf, final int s, final int len) throws IOException {
        byte[] _buf = buf;
        int _len = len;
        int _s = s;

        while (_len > 0) {
            if (_s != 0) {
                _buf = new byte[_len];
            }
            final int i = CLibrary.INSTANCE.read(this.fdBuf.getValue(), _buf, _len);
            if (i <= 0) {
                return -1;
                // throw new IOException("failed to read");
            }
            if (_s != 0) {
                System.arraycopy(_buf, 0, buf, _s, i);
            }
            _s += i;
            _len -= i;
        }
        return len;
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] buf, final int s, final int len) throws IOException {
        byte[] _buf = buf;
        int _len = len;
        int _s = s;

        if (_s != 0) {
            _buf = new byte[_len];
        }
        final int i = CLibrary.INSTANCE.read(this.fdBuf.getValue(), _buf, _len);
        if (i <= 0) {
            return -1;
            // throw new IOException("failed to read");
        }
        if (_s != 0) {
            System.arraycopy(_buf, 0, buf, _s, i);
        }
        _s += i;
        _len -= i;

        return i;
    }

    /**
     * Write.
     *
     * @param buf
     *            the buf
     * @param s
     *            the s
     * @param len
     *            the len
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void write(final byte[] buf, final int s, final int len) throws IOException {
        byte[] _buf = buf;
        if (s != 0) {
            _buf = new byte[len];
            System.arraycopy(buf, s, _buf, 0, len);
        }
        CLibrary.INSTANCE.write(this.fdBuf.getValue(), _buf, len);
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#close()
     */
    @Override
    public void close() {
        CLibrary.INSTANCE.close(this.fdBuf.getValue());
        this.exitStatus = PtyLibrary.INSTANCE.waitForPid(this.pid);
        this.log.info("exitStatus=" + this.exitStatus);
    }

    /**
     * Gets the exit status.
     *
     * @return the exit status
     */
    @Override
    public int getExitStatus() {
        return this.exitStatus;
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#init(net.agilhard.jsch.UserInfo, net.agilhard.terminal.emulation.Questioner)
     */
    @SuppressWarnings("unused")
    @Override
    public boolean init(final UserInfo userInfo, final Questioner questioner) {
        final int r = this.start(this.cmd, this.directory, this.args);
        return r == 0;
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#resize(java.awt.Dimension, java.awt.Dimension)
     */
    @Override
    public void resize(final Dimension termSize, final Dimension pixelSize) {
        final winsize ws = new winsize();
        CLibrary.INSTANCE.ioctl(this.fdBuf.getValue(), TIOCGWINSZ, ws);

        ws.ws_col = (short) termSize.width;
        ws.ws_row = (short) termSize.height;
        ws.ws_xpixel = (short) pixelSize.width;
        ws.ws_ypixel = (short) pixelSize.height;

        CLibrary.INSTANCE.ioctl(this.fdBuf.getValue(), TIOCSWINSZ, ws);

    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#getName()
     */
    @Override
    public String getName() {
        return "JNAPty";
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#write(byte[])
     */
    @Override
    public void write(final byte[] bytes) throws IOException {
        this.write(bytes, 0, bytes.length);
    }

}
//CHECKSTYLE:ON
