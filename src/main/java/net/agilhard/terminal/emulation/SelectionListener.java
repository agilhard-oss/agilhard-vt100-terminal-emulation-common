package net.agilhard.terminal.emulation;

import java.awt.Point;

/**
 * The listener interface for receiving selection events. The class that is
 * interested in processing a selection event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addSelectionListener</code> method. When the selection
 * event occurs, that object's appropriate method is invoked.
 *
 * @author bei
 */
public interface SelectionListener {

    /**
     * Selection changed.
     *
     * @param selectionStart
     *            the selection start
     * @param selectionEnd
     *            the selection end
     */
    void selectionChanged(final Point selectionStart, final Point selectionEnd);

}
