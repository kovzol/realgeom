package realgeom;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.MapleException;

class MapleCAS {

    static Engine t;
    static JCEngineCallBacks callbacks;

    static void start() {
        String a[];
        String output = "";
        a = new String[1];
        a[0] = "java";
        // EngineCallBacks callbacks = new EngineCallBacksDefault();
        callbacks = new JCEngineCallBacks();
        try {
            t = new Engine(a, callbacks, null, null);
        } catch (MapleException e) {
            System.err.println("Error starting OpenMaple");
        }
    }

    static String execute(String command) {
        Algebraic ret;
        String result = "";
        try {
            for (String line : command.split(";")) {
                ret = t.evaluate(line + ";");
                while (callbacks.numOfLines() > 0) {
                    String resultLine = callbacks.getLine();
                    result += resultLine + "\n";
                }
            }
        } catch (MapleException e) {
            System.out.println("Error on executing OpenMaple command " + command);
            return null;
        }
        return result;
    }
}
