package net.agilhard.terminal.emulation;

/**
 * The Interface Questioner.
 */
public interface Questioner {

    /**
     * Question.
     *
     * @param question
     *            the question
     * @param defValue
     *            the def value
     * @return the string
     */
    String question(String question, String defValue);

    /**
     * Show message.
     *
     * @param message
     *            the message
     */
    void showMessage(String message);

    /**
     * Show error.
     *
     * @param message
     *            the message
     */
    void showError(String message);

}
