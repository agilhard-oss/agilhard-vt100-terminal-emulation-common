/* -*-mode:java; c-basic-offset:2; -*- */
/*
 * JCTerm
 * Copyright (C) 2002,2007 ymnk, JCraft,Inc.
 * Written by: ymnk<ymnk@jcaft.com>
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

package net.agilhard.terminal.emulation.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.agilhard.jsch.ChannelSftp;
import net.agilhard.jsch.SftpATTRS;
import net.agilhard.jsch.SftpException;
import net.agilhard.jsch.SftpProgressMonitor;

/**
 * The Class Sftp.
 */
public class Sftp implements Runnable {

    /** The in. */
    private final InputStream in;

    /** The out. */
    private final OutputStream out;

    /** The c. */
    private final ChannelSftp c;

    /** The lf. */
    private final byte[] lf = { 0x0a, 0x0d };

    /** The del. */
    private final byte[] del = { 0x08, 0x20, 0x08 };

    /**
     * Instantiates a new sftp.
     *
     * @param c
     *            the c
     * @param in
     *            the in
     * @param out
     *            the out
     */
    public Sftp(final ChannelSftp c, final InputStream in, final OutputStream out) {
        this.c = c;
        this.in = in;
        this.out = out;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    // CHECKSTYLE:OFF
    @Override
    public void run() {
        try {
            final java.util.Vector<String> cmds = new java.util.Vector<>();
            final byte[] buf = new byte[1024];
            int i;
            String str;
            final String lhome = this.c.lpwd();

            final StringBuffer sb = new StringBuffer();
            while (true) {
                //out.print("sftp> ");
                this.out.write("sftp> ".getBytes());
                cmds.removeAllElements();

                sb.setLength(0);

                loop: while (true) {
                    i = this.in.read(buf, 0, 1024);
                    if (i <= 0) {
                        break;
                    }
                    if (i != 1) {
                        continue;
                    }
                    if (buf[0] == 0x08) {
                        if (sb.length() > 0) {
                            sb.setLength(sb.length() - 1);
                            this.out.write(this.del, 0, this.del.length);
                            this.out.flush();
                        }
                        continue;
                    }

                    if (buf[0] == 0x0d) {
                        this.out.write(this.lf, 0, this.lf.length);
                    } else if (buf[0] == 0x0a) {
                        this.out.write(this.lf, 0, this.lf.length);
                    } else if (buf[0] < 0x20 || (buf[0] & 0x80) != 0) {
                        continue;
                    } else {
                        this.out.write(buf, 0, i);
                    }
                    this.out.flush();

                    for (int j = 0; j < i; j++) {
                        sb.append((char) buf[j]);
                        if (buf[j] == 0x0d) {
                            System.arraycopy(sb.toString().getBytes(), 0, buf, 0, sb.length());
                            i = sb.length();
                            break loop;
                        }
                        if (buf[j] == 0x0a) {
                            System.arraycopy(sb.toString().getBytes(), 0, buf, 0, sb.length());
                            i = sb.length();
                            break loop;
                        }
                    }
                }
                if (i <= 0) {
                    break;
                }

                i--;
                if (i > 0 && buf[i - 1] == 0x0d) {
                    i--;
                }
                if (i > 0 && buf[i - 1] == 0x0a) {
                    i--;
                }
                //str=new String(buf, 0, i);
                int s = 0;
                for (int ii = 0; ii < i; ii++) {
                    if (buf[ii] == ' ') {
                        if (ii - s > 0) {
                            cmds.addElement(new String(buf, s, ii - s));
                        }
                        while (ii < i) {
                            if (buf[ii] != ' ') {
                                break;
                            }
                            ii++;
                        }
                        s = ii;
                    }
                }
                if (s < i) {
                    cmds.addElement(new String(buf, s, i - s));
                }
                if (cmds.size() == 0) {
                    continue;
                }

                final String cmd = cmds.elementAt(0);
                if (cmd.equals("quit")) {
                    this.c.quit();
                    break;
                }
                if (cmd.equals("exit")) {
                    this.c.exit();
                    break;
                }
                if (cmd.equals("cd") || cmd.equals("lcd")) {
                    String path = null;
                    if (cmds.size() < 2) {
                        if (cmd.equals("cd")) {
                            path = this.c.getHome();
                        } else {
                            path = lhome;
                        }
                    } else {
                        path = cmds.elementAt(1);
                    }
                    try {
                        if (cmd.equals("cd")) {
                            this.c.cd(path);
                        } else {
                            this.c.lcd(path);
                        }
                    }
                    catch (final SftpException e) {
                        this.out.write(e.getMessage().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }
                if (cmd.equals("rm") || cmd.equals("rmdir") || cmd.equals("mkdir")) {
                    if (cmds.size() < 2) {
                        continue;
                    }
                    final String path = cmds.elementAt(1);
                    try {
                        if (cmd.equals("rm")) {
                            this.c.rm(path);
                        } else if (cmd.equals("rmdir")) {
                            this.c.rmdir(path);
                        } else {
                            this.c.mkdir(path);
                        }
                    }
                    catch (final SftpException e) {
                        this.out.write(e.getMessage().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }
                if (cmd.equals("lmkdir")) {
                    if (cmds.size() < 2) {
                        continue;
                    }
                    final String path = cmds.elementAt(1);

                    final java.io.File d = new java.io.File(this.c.lpwd(), path);
                    if (!d.mkdir()) {

                        this.out.write("failed to make directory".getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }

                if (cmd.equals("chgrp") || cmd.equals("chown") || cmd.equals("chmod")) {
                    if (cmds.size() != 3) {
                        continue;
                    }
                    final String path = cmds.elementAt(2);
                    int foo = 0;
                    if (cmd.equals("chmod")) {
                        final byte[] bar = cmds.elementAt(1).getBytes();
                        int k;
                        for (int j = 0; j < bar.length; j++) {
                            k = bar[j];
                            if (k < '0' || k > '7') {
                                foo = -1;
                                break;
                            }
                            foo <<= 3;
                            foo |= k - '0';
                        }
                        if (foo == -1) {
                            continue;
                        }
                    } else {
                        try {
                            foo = Integer.parseInt(cmds.elementAt(1));
                        }
                        catch (final Exception e) {
                            continue;
                        }
                    }
                    try {
                        if (cmd.equals("chgrp")) {
                            this.c.chgrp(foo, path);
                        } else if (cmd.equals("chown")) {
                            this.c.chown(foo, path);
                        } else if (cmd.equals("chmod")) {
                            this.c.chmod(foo, path);
                        }
                    }
                    catch (final SftpException e) {
                        this.out.write(e.getMessage().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }
                if (cmd.equals("pwd") || cmd.equals("lpwd")) {
                    str = cmd.equals("pwd") ? "Remote" : "Local";
                    str += " working directory: ";
                    if (cmd.equals("pwd")) {
                        str += this.c.pwd();
                    } else {
                        str += this.c.lpwd();
                    }
                    //out.print(str+"\n");
                    this.out.write(str.getBytes());
                    this.out.write(this.lf);
                    this.out.flush();
                    continue;
                }
                if (cmd.equals("ls") || cmd.equals("dir")) {
                    String path = ".";
                    if (cmds.size() == 2) {
                        path = cmds.elementAt(1);
                    }
                    try {
                        @SuppressWarnings("rawtypes")
                        final java.util.Vector vv = this.c.ls(path);
                        if (vv != null) {
                            for (int ii = 0; ii < vv.size(); ii++) {
                                //out.print(vv.elementAt(ii)+"\n");
                                //out.write(((String)(vv.elementAt(ii))).getBytes());
                                this.out.write(vv.elementAt(ii).toString().getBytes());
                                this.out.write(this.lf);
                            }
                            this.out.flush();
                        }
                    }
                    catch (final SftpException e) {
                        this.out.write(e.getMessage().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }
                if (cmd.equals("lls")) {
                    String path = this.c.lpwd();
                    if (cmds.size() == 2) {
                        path = cmds.elementAt(1);
                    }
                    try {
                        final java.io.File d = new java.io.File(path);
                        final String[] list = d.list();
                        for (int ii = 0; ii < list.length; ii++) {
                            this.out.write(list[ii].getBytes());
                            this.out.write(this.lf);
                        }
                        this.out.flush();
                    }
                    catch (final IOException e) {
                        this.out.write(e.getMessage().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }
                if (cmd.equals("get") || cmd.equals("put")) {
                    if (cmds.size() != 2 && cmds.size() != 3) {
                        continue;
                    }
                    final String p1 = cmds.elementAt(1);
                    //      String p2=p1;
                    String p2 = ".";
                    if (cmds.size() == 3) {
                        p2 = cmds.elementAt(2);
                    }
                    try {
                        final SftpProgressMonitor monitor = new MyProgressMonitor(this.out);
                        if (cmd.equals("get")) {
                            this.c.get(p1, p2, monitor);
                        } else {
                            this.c.put(p1, p2, monitor);
                        }
                    }
                    catch (final SftpException e) {
                        this.out.write(e.getMessage().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }
                if (cmd.equals("ln") || cmd.equals("symlink") || cmd.equals("rename")) {
                    if (cmds.size() != 3) {
                        continue;
                    }
                    final String p1 = cmds.elementAt(1);
                    final String p2 = cmds.elementAt(2);
                    try {
                        if (cmd.equals("rename")) {
                            this.c.rename(p1, p2);
                        } else {
                            this.c.symlink(p1, p2);
                        }
                    }
                    catch (final SftpException e) {
                        this.out.write(e.getMessage().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }
                if (cmd.equals("stat") || cmd.equals("lstat")) {
                    if (cmds.size() != 2) {
                        continue;
                    }
                    final String p1 = cmds.elementAt(1);
                    SftpATTRS attrs = null;
                    try {
                        if (cmd.equals("stat")) {
                            attrs = this.c.stat(p1);
                        } else {
                            attrs = this.c.lstat(p1);
                        }
                    }
                    catch (final SftpException e) {
                        //System.out.println(e.getMessage());
                        this.out.write(e.getMessage().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    if (attrs != null) {
                        //out.println(attrs);
                        this.out.write(attrs.toString().getBytes());
                        this.out.write(this.lf);
                        this.out.flush();
                    }
                    continue;
                }
                if (cmd.equals("version")) {
                    //out.print("SFTP protocol version "+c.version()+"\n");
                    this.out.write(("SFTP protocol version " + this.c.version()).getBytes());
                    this.out.write(this.lf);
                    this.out.flush();
                    continue;
                }
                if (cmd.equals("help") || cmd.equals("help")) {
                    //      out.print(help+"\n");
                    for (int j = 0; j < help.length; j++) {
                        this.out.write(help[j].getBytes());
                        this.out.write(this.lf);
                    }
                    this.out.flush();
                    continue;
                }
                //out.print("unimplemented command: "+cmd+"\n");
                this.out.write(("unimplemented command: " + cmd).getBytes());
                this.out.write(this.lf);
                this.out.flush();
            }
            try {
                this.in.close();
            }
            catch (final Exception ee) {
                // .
            }
            try {
                this.out.close();
            }
            catch (final Exception ee) {
                // .
            }
        }
        catch (final Exception e) {
            System.out.println(e);
        }
    }
    // CHECKSTYLE:ON

    /** The thread. */
    private Thread thread;

    /**
     * Kick.
     *
     * @return the thread
     */
    public Thread kick() {
        if (this.thread == null) {
            this.thread = new Thread(this);
            this.thread.start();
        }
        return this.thread;
    }

    /**
     * The Class MyProgressMonitor.
     */
    public static class MyProgressMonitor implements SftpProgressMonitor {

        /** The out. */
        private final OutputStream out;

        //    ProgressMonitor monitor;
        /** The count. */
        private long count;

        /** The max. */
        private long max;

        /** The src. */
        private String src;

        /** The percent. */
        private int percent;

        /**
         * Instantiates a new my progress monitor.
         *
         * @param out
         *            the out
         */
        MyProgressMonitor(final OutputStream out) {
            this.out = out;
        }

        /* (non-Javadoc)
         * @see net.agilhard.jsch.SftpProgressMonitor#init(int, java.lang.String, java.lang.String, long)
         */
        @SuppressWarnings("unused")
        @Override
        public void init(final int op, final String csSrc, final String dest, final long csMax) {
            this.max = csMax;
            this.src = csSrc;
            this.count = 0;
            this.percent = 0;
            this.status();
            //      monitor=new ProgressMonitor(null,
            //                                  ((op==SftpProgressMonitor.PUT)?
            //                                   "put" : "get")+": "+src,
            //                                  "",  0, (int)max);
            //      monitor.setProgress((int)this.count);
            //      monitor.setMillisToDecideToPopup(1000);
        }

        /* (non-Javadoc)
         * @see net.agilhard.jsch.SftpProgressMonitor#count(long)
         */
        @Override
        public boolean count(final long csCount) {
            this.count += csCount;
            //      monitor.setProgress((int)this.count);
            //      monitor.setNote("Completed "+this.count+" out of "+max+".");
            //      return !(monitor.isCanceled());
            this.percent = (int) ((float) this.count / (float) this.max * 100.0);
            this.status();
            return true;
        }

        // CHECKSTYLE:OFF

        /* (non-Javadoc)
         * @see net.agilhard.jsch.SftpProgressMonitor#end()
         */
        @Override
        public void end() {
            //      monitor.close();
            this.percent = (int) ((float) this.count / (float) this.max * 100.0);
            this.status();

            try {
                this.out.write((byte) 0x0d);
                this.out.write((byte) 0x0a);
                this.out.flush();
            }
            catch (final Exception e) {
                //.
            }

        }

        /**
         * Status.
         */
        private void status() {
            try {
                this.out.write((byte) 0x0d);

                this.out.write((byte) 0x1b);
                this.out.write((byte) '[');
                this.out.write((byte) 'K');

                this.out.write((this.src + ": " + this.percent + "% " + this.count + "/" + this.max).getBytes());
                this.out.flush();
            }
            catch (final Exception e) {
                //.
            }
        }
    }

    // CHECKSTYLE:ON

    /** The help. */
    private static String[] help = { "      Available commands:", "      * means unimplemented command.",
        "cd [path]                     Change remote directory to 'path'",
        "lcd [path]                    Change local directory to 'path'",
        "chgrp grp path                Change group of file 'path' to 'grp'",
        "chmod mode path               Change permissions of file 'path' to 'mode'",
        "chown own path                Change owner of file 'path' to 'own'",
        "help                          Display this help text", "get remote-path [local-path]  Download file",
        "lls [path]                    Display local directory listing",
        "ln oldpath newpath            Symlink remote file", "lmkdir path                   Create local directory",
        "lpwd                          Print local working directory",
        "ls [path]                     Display remote directory listing",
        "*lumask umask                 Set local umask to 'umask'",
        "mkdir path                    Create remote directory", "put local-path [remote-path]  Upload file",
        "pwd                           Display remote working directory",
        "stat path                     Display info about path\n" + "exit                          Quit sftp",
        "quit                          Quit sftp", "rename oldpath newpath        Rename remote file",
        "rmdir path                    Remove remote directory", "rm path                       Delete remote file",
        "symlink oldpath newpath       Symlink remote file", "version                       Show SFTP version",
        "?                             Synonym for help" };
}
