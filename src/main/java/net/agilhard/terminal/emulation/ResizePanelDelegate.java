package net.agilhard.terminal.emulation;

import java.awt.Dimension;

/**
 * The Interface ResizePanelDelegate.
 */
public interface ResizePanelDelegate {

    /**
     * Resized panel.
     *
     * @param pixelDimension
     *            the pixel dimension
     * @param origin
     *            the origin
     */
    void resizedPanel(Dimension pixelDimension, RequestOrigin origin);

}
