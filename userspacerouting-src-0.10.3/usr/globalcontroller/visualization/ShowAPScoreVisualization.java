package usr.globalcontroller.visualization;

import usr.globalcontroller.GlobalController;
import java.io.PrintStream;

/**
 * A view of the current network topology, showing where aggregation points are.
 */
public class ShowAPScoreVisualization implements Visualization {
    GlobalController gc;

    public ShowAPScoreVisualization() {
    }

    /**
     * Set the GlobalController this Visualization gets data from.
     */
    public void setGlobalController(GlobalController gc) {
        this.gc = gc;
    }

    /**
     * Visualize the current topology of the network.
     */
    public void visualize(PrintStream s) {
        s.println("Graph G {");

        for (int r : gc.getRouterList()) {
            int ap = gc.getAPController().getAP(r);

            if (ap == r) {
                s.print(r+" [shape=box");
            } else {
                s.print(r+" [shape=circle");
            }

            long time = gc.getEventTime(); // the current event time
            s.print(",label=\""+ap+" (" + gc.getAPController().getScore(time, r, gc) + ")\"");

            s.println("];");
        }

        for (int i : gc.getRouterList()) {
            for (int j : gc.getOutLinks(i)) {
                if (i < j) {
                    s.println(i+ " -- "+j+";");
                }
            }
        }
        s.println("}");


    }

}