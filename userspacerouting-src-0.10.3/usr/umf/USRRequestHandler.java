package usr.umf;

import umf.common.rest.BasicRequestHandler;
import cc.clayman.console.ManagementConsole;


/**
 * An extention to BasicRequestHandler to support ManagementConsole.
 */

public class USRRequestHandler extends BasicRequestHandler {
    ManagementConsole mgmt = null;

    /**
     * Get the ManagementConsole
     */
    ManagementConsole getManagementConsole() {
        return mgmt;
    }

    /**
     * Set the ManagementConsole
     */
    public void setManagementConsole(ManagementConsole m) {
        mgmt = m;
    }

}