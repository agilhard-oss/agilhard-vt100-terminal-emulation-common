package net.agilhard.terminal.emulation;

/**
 * Interface implemented by classes controlling the Window containing the {@link TermPanel}.
 * 
 * @author bei
 */
public interface TerminalEmulationController {

    /**
     * Checks if is close_on_error.
     *
     * @return true, if is close_on_error
     */
    boolean isCloseOnError();

    /**
     * Checks if is close_on_exit.
     *
     * @return true, if is close_on_exit
     */
    boolean isCloseOnExit();

    /**
     * Close.
     */
    void close();

}
