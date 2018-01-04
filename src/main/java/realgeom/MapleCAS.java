package realgeom;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.MapleException;

class MapleCAS {
    static String execute (String command)
    {
        String a[];
        Engine t;
        Algebraic ret;
        String output = "";
        a = new String[1];
        a[0] = "java";
        JCEngineCallBacks callbacks = new JCEngineCallBacks();
        try
        {
            t = new Engine( a, callbacks, null, null );
            ret = t.evaluate( command );
        }
        catch ( MapleException e )
        {
            return null;
        }

        return ret.toString();
    }
}
