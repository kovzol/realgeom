package realgeom;

/*
 * It evaluates CAS calls via GIAC.
 */

import javagiac.*;

class GiacCAS {
    static String execute(String command) {
        context C = new context();
        gen g = new gen(command, C);
        g = g.eval(1, C);
        String ret = g.print(C);
        if (Start.debug) {
            System.err.println("GIAC: " + command + " -> " + ret);
        }
        return ret;
    }
}
