package usr.umf;

import usr.console.Command;

public interface ConsoleSupport {
    /**
     * Find a command in the ManagementConsole.
     * @param commandName The name of the command
     */
    public Command find(String commandName);

    /**
     * Find a handler in the ManagementConsole.
     * @param pattern The pattern for the handler
     */
    public Command findHandler(String pattern);


    /**
     * Start the ManagementConsole.
     */
    public boolean start();

    /**
     * Stop the ManagementConsole.
     */
    public boolean stop();

}
