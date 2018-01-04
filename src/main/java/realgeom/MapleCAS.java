package realgeom;

import java.io.IOException;
import java.io.InputStream;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.MapleException;

class MapleCAS {
    static String execute (String command)
    {
        String a[];
        Engine t;
        int i;
        a = new String[1];
        a[0] = "java";
        try
        {
            t = new Engine( a, new EngineCallBacksDefault(), null, null );
            t.evaluate( command );
        }
        catch ( MapleException e )
        {
            return null;
        }
        return "done";
    }
}
