/**
 * .
 */
package net.agilhard.terminal.emulation.jsch;

import static net.agilhard.terminal.emulation.TerminalEmulationConstants.SFTP;
import static net.agilhard.terminal.emulation.TerminalEmulationConstants.SHELL;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.agilhard.jsch.Channel;
import net.agilhard.jsch.ChannelExec;
import net.agilhard.jsch.ChannelShell;
import net.agilhard.jsch.JSch;
import net.agilhard.jsch.JSchException;
import net.agilhard.jsch.Session;
import net.agilhard.jsch.UserInfo;
import net.agilhard.jschutil.JSchUtil;
import net.agilhard.terminal.emulation.Questioner;
import net.agilhard.terminal.emulation.Tty;

/**
 * The Class JSchTty.
 */
public class JSchTty implements Tty {

    /** The default port. */
    private static final int DEFAULT_PORT = 22;

    /** The Logger. */
    private final Logger log = LoggerFactory.getLogger(JSchTty.class);

    /** The in. */
    private InputStream in;

    /** The out. */
    private OutputStream out;

    /** The session. */
    private Session session;

    /** The channel. */
    private Channel channel;

    /** The port. */
    private int port = DEFAULT_PORT;

    /** The user. */
    private String user;

    /** The host. */
    private String host;

    /** The password. */
    private String password;

    /** The mode. */
    private int mode = SHELL;

    /** The pending term size. */
    private Dimension pendingTermSize;

    /** The pending pixel size. */
    private Dimension pendingPixelSize;

    /**
     * Instantiates a new j sch tty.
     */
    public JSchTty() {
        super();
        //final JSchConfigurationRepository rep = JSchUtil.getCR();
        //this.jSchConfig = rep.load("default");
    }

    /**
     * Instantiates a new j sch tty.
     *
     * @param host
     *            the host
     * @param user
     *            the user
     * @param password
     *            the password
     * @param mode
     *            the mode
     */
    public JSchTty(final String host, final String user, final String password, final int mode) {
        this();
        this.setHost(host);
        this.user = user;
        this.password = password;
        this.mode = mode;

    }

    /**
     * Instantiates a new j sch tty.
     *
     * @param host
     *            the host
     * @param user
     *            the user
     * @param password
     *            the password
     * @param port
     *            the port
     * @param mode
     *            the mode
     */
    public JSchTty(final String host, final String user, final String password, final int port, final int mode) {
        this();
        this.setHost(host);
        this.user = user;
        this.password = password;
        this.port = port;
        this.mode = mode;
        //log.info("XXXXXXXXXXXXXXXXXXX config=" + this.jSchConfig);
        //log.info("XXXXXXXXXXXXXXXXXXX host=" + host);
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#resize(java.awt.Dimension, java.awt.Dimension)
     */
    @Override
    public void resize(final Dimension termSize, final Dimension pixelSize) {
        this.pendingTermSize = termSize;
        this.pendingPixelSize = pixelSize;
        if (this.channel != null && this.channel instanceof ChannelShell) {
            this.resizeImmediately();
        }
    }

    /**
     * Resize immediately.
     */
    private void resizeImmediately() {
        if (this.pendingTermSize != null && this.pendingPixelSize != null) {
            ((ChannelShell) this.channel).setPtySize(this.pendingTermSize.width, this.pendingTermSize.height,
                this.pendingPixelSize.width, this.pendingPixelSize.height);
            this.pendingTermSize = null;
            this.pendingPixelSize = null;
        }
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#close()
     */
    @Override
    public void close() {
        if (this.session != null) {
            this.session.disconnect();
            this.session = null;
            this.channel = null;
            this.in = null;
            this.out = null;
        }
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#init(net.agilhard.jsch.UserInfo, net.agilhard.terminal.emulation.Questioner)
     */
    @Override
    public boolean init(final UserInfo userInfo, final Questioner questioner) {

        this.getAuthDetails(userInfo, questioner);
        if (this.user == null) {
            return false;
        }
        if (this.host == null) {
            this.setHost("localhost");
        }
        try {
            this.session = this.connectSession(userInfo);
            this.channel = this.session.openChannel(this.mode == SFTP ? "sftp" : "shell");
            this.in = this.channel.getInputStream();
            this.out = this.channel.getOutputStream();

            if (this.channel instanceof ChannelShell) {
                ((ChannelShell) this.channel).setAgentForwarding(true);
            } else if (this.channel instanceof ChannelExec) {
                ((ChannelExec) this.channel).setAgentForwarding(true);
            }

            this.channel.connect();
            if (this.channel instanceof ChannelShell) {
                this.resizeImmediately();
            }
            return true;
        }
        catch (final IOException e) {
            questioner.showMessage("\n" + e.getMessage());
            this.log.error("Error opening channel", e);
            return false;
        }
        catch (final JSchException e) {
            questioner.showMessage("\n" + e.getMessage());
            this.log.error("Error opening session or channel", e);
            return false;
        }
    }

    /**
     * Gets the channel.
     *
     * @return the channel
     */
    public Channel getChannel() {
        return this.channel;
    }

    /**
     * Connect session.
     *
     * @param userinfo
     *            the userinfo
     * @return the session
     * @throws JSchException
     *             the j sch exception
     */
    private Session connectSession(final UserInfo userinfo) throws JSchException {

        final JSch jsch = JSchUtil.getJSch();
        Session aSession = null;
        aSession = jsch.getSession(this.user, this.host, this.port);

        if (this.password != null) {
            aSession.setPassword(this.password);
            // userinfo.setPassword(password);
        }
        aSession.setUserInfo(userinfo);

        final java.util.Properties myConfig = new java.util.Properties();
        myConfig.put("compression.s2c", "zlib,none");
        myConfig.put("compression.c2s", "zlib,none");
        myConfig.put("StrictHostKeyChecking", "none");
        //config.put("HashKnownHosts", "yes");
        this.configureSession(aSession, myConfig);
        aSession.setTimeout(25000);
        //session.setTimeout(5000);
        aSession.connect();
        //session.setTimeout(0);

        return aSession;
    }

    /**
     * Gets the session.
     *
     * @return the session
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * Configure session.
     *
     * @param csSession
     *            the cs_session
     * @param configProps
     *            the configProps
     */
    protected void configureSession(final Session csSession, final java.util.Properties configProps) {
        csSession.setConfig(configProps);
    }

    /**
     * Gets the auth details.
     *
     * @param userInfo
     *            the user info
     * @param q
     *            the q
     */
    @SuppressWarnings("unused")
    private void getAuthDetails(final UserInfo a, final Questioner q) {

        int retries = 0;
        while (retries++ < 10) {
            if (this.host == null) {
                this.setHost(q.question("host:", "localhost"));
            }
            if (this.host == null || this.host.length() == 0) {
                continue;
            }
            if (this.host.indexOf(':') != -1) {
                final String portString = this.host.substring(this.host.indexOf(':') + 1);
                try {
                    this.port = Integer.parseInt(portString);
                }
                catch (final NumberFormatException eee) {
                    q.showError("Could not parse port : " + portString);
                    continue;
                }
                this.setHost(this.host.substring(0, this.host.indexOf(':')));
            }

            if (this.user == null) {
                this.user = q.question("user:", System.getProperty("user.name").toLowerCase());
            }
            if (this.host == null || this.host.length() == 0) {
                continue;
            }
            break;
        }
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#getName()
     */
    @Override
    public String getName() {
        return "ConnectRunnable";
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] buf, final int offset, final int length) throws IOException {
        if (this.in == null) {
            throw new InterruptedIOException("JSchTty closed");
        }
        return this.in.read(buf, offset, length);
    }

    /* (non-Javadoc)
     * @see net.agilhard.terminal.emulation.Tty#write(byte[])
     */
    @Override
    public void write(final byte[] bytes) throws IOException {
        if (this.out == null) {
            throw new InterruptedIOException("JSchTty closed");
        }
        this.out.write(bytes);
        this.out.flush();
    }

    /**
     * Gets the port.
     *
     * @return the port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Sets the port.
     *
     * @param port
     *            the new port
     */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * Gets the user.
     *
     * @return the user
     */
    public String getUser() {
        return this.user;
    }

    /**
     * Sets the user.
     *
     * @param user
     *            the new user
     */
    public void setUser(final String user) {
        this.user = user;
    }

    /**
     * Gets the host.
     *
     * @return the host
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Sets the host.
     *
     * @param host
     *            the new host
     */
    public void setHost(final String host) {
        /*
        if (this.jSchConfig != null && this.jSchConfig.useFixedHostMapping) {
            final String ip = SeastepFixedHostMap.getFixedIpOf(host);
            if (ip != null) {
                this.host = ip;
                return;
            }
        }
        */
        this.host = host;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the password.
     *
     * @param password
     *            the new password
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Gets the mode.
     *
     * @return the mode
     */
    public int getMode() {
        return this.mode;
    }

    /**
     * Sets the mode.
     *
     * @param mode
     *            the new mode
     */
    public void setMode(final int mode) {
        this.mode = mode;
    }

    /**
     * Gets the exit status.
     *
     * @return the exit status
     */
    @Override
    public int getExitStatus() {
        if (this.channel == null) {
            return 0;
        }
        return this.channel.getExitStatus();
    }

}
