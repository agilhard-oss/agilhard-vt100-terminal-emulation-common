package net.agilhard.terminal.emulation.shell;

import net.agilhard.terminal.emulation.Tty;

/**
 * A factory for creating LocalPty objects.
 */
public final class LocalPtyFactory {

    /** The Constant WINDOWS. */
    private static final String WINDOWS = "WINDOWS";

    /** The Constant LINUX. */
    private static final String LINUX = "Linux";

    /** The Constant OS_NAME. */
    private static final String OS_NAME = "os.name";

    /**
     * Private constructor for utility class.
     */
    private LocalPtyFactory() {
        // .
    }

    /**
     * Checks if is interactive supported.
     *
     * @return true, if is interactive supported
     */
    public static boolean isInteractiveSupported() {
        final String prop = System.getProperty(OS_NAME);
        return prop != null && (prop.equalsIgnoreCase(LINUX) || prop.toUpperCase().startsWith(WINDOWS));
    }

    /**
     * Checks if is command supported.
     *
     * @return true, if is command supported
     */
    public static boolean isCommandSupported() {
        final String prop = System.getProperty(OS_NAME);
        return prop != null && (prop.equalsIgnoreCase(LINUX) || prop.toUpperCase().startsWith(WINDOWS));
    }

    /**
     * .
     * 
     * @return a Tty
     */
    public static Tty createPty() {
        final String prop = System.getProperty(OS_NAME);
        if (prop != null) {
            if (prop.equalsIgnoreCase(LINUX)) {
                return new JNAPtyLinux();
            } else if (prop.toUpperCase().startsWith(WINDOWS)) {
                // interactive does not work yet
                return new PtyWindowsCmd();
            }
        }
        return null;
    }

    /**
     * Creates a new LocalPty object.
     *
     * @param cmd
     *            the cmd
     * @return the tty
     */
    public static Tty createPty(final String cmd) {
        final String prop = System.getProperty(OS_NAME);
        if (prop != null) {
            if (prop.equalsIgnoreCase(LINUX)) {
                return new JNAPtyLinux(cmd);
            } else if (prop.toUpperCase().startsWith(WINDOWS)) {
                return new PtyWindowsCmd(cmd);
            }
        }
        return null;
    }

    /**
     * Creates a new LocalPty object.
     *
     * @param cmd
     *            the cmd
     * @param directory
     *            the directory
     * @return the tty
     */
    public static Tty createPty(final String cmd, final String directory) {
        final String prop = System.getProperty(OS_NAME);
        if (prop != null) {
            if (prop.equalsIgnoreCase(LINUX)) {
                return new JNAPtyLinux(cmd, directory);
            } else if (prop.toUpperCase().startsWith(WINDOWS)) {
                return new PtyWindowsCmd(cmd, directory);
            }
        }
        return null;
    }
}
