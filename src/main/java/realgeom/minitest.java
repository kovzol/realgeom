package realgeom;

import javagiac.*;

public class minitest {
    static {
        try {
            System.out.println("Loading giac java interface...");
            MyClassPathLoader loader = new MyClassPathLoader();
            loader.loadLibrary("javagiac64");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            System.err.println("Native code library failed to load. See the chapter on Dynamic Linking Problems in the SWIG Java documentation for help.\n" + e);
            System.exit(1);
        }
    }

    public static void main(String argv[])
    {
        context C=new context();
        String s = "1+2";
        gen g=new gen(s,C);
        g=g.eval(1,C);
        System.out.println(g.print(C));
        System.out.println(ExternalCAS.execute("ls | sort"));
        System.out.println(MapleCAS.execute("int(x,x);"));
        try {
            HTTPServer.start(8765);
        } catch (Exception e) {
            System.err.println("Cannot start HTTP server");
            }
    }
}
