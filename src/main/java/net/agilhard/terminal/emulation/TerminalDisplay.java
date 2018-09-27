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

import java.awt.Dimension;

/**
 * The Interface TerminalDisplay.
 */
public interface TerminalDisplay {

    // Size information
    /**
     * Gets the row count.
     *
     * @return the row count
     */
    int getRowCount();

    /**
     * Gets the column count.
     *
     * @return the column count
     */
    int getColumnCount();

    /**
     * Sets the cursor.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     */
    void setCursor(int x, int y);

    /**
     * Beep.
     */
    void beep();

    /**
     * Do resize.
     *
     * @param pendingResize
     *            the pending resize
     * @param origin
     *            the origin
     * @return the dimension
     */
    Dimension doResize(Dimension pendingResize, RequestOrigin origin);

    /**
     * Scroll area.
     *
     * @param y
     *            the y
     * @param h
     *            the h
     * @param dy
     *            the dy
     */
    void scrollArea(final int y, final int h, int dy);

}
