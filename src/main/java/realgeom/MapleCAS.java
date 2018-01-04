package realgeom;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.MapleException;

class MapleCAS {
    static String execute (String command)
    {
        String a[];
        Engine t;
        Algebraic ret;
        JCEngineCallBacks callbacks;
        callbacks = new JCEngineCallBacks();
        String output = "";
        a = new String[1];
        a[0] = "java";
        try
        {
            t = new Engine( a, callbacks, null, null );
            ret = t.evaluate( command );
        }
        catch ( MapleException e )
        {
            return null;
        }

        while ( callbacks.numOfLines() > 0 ) {
            String line = callbacks.getLine();
            if (line.length() > 0) {
                output += line.replaceAll("^\\s+","") + "\n";
                }
            }
        return output.substring(0,output.length()-1);
    }
}
