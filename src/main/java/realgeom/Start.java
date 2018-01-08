package realgeom;

/**
 * The main process. It attempts to connect to various computer algebra subsystems.
 * If everything is successfull, then a HTTP server will be started.
 */

import javagiac.*;

public class Start {
    static {
        try {
            System.out.println("Loading Giac Java interface...");
            MyClassPathLoader loader = new MyClassPathLoader();
            loader.loadLibrary("javagiac64");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            System.err.println("Native code library failed to load. See the chapter on Dynamic Linking Problems in the SWIG Java documentation for help.\n" + e);
            System.exit(1);
        }
    }

    public static boolean test() {
        System.out.println("Testing Giac connection...");
        String giacTest = GiacCAS.execute("1+2");
        if (!giacTest.equals("3")) {
            return false;
        }

        System.out.println("Testing shell connection...");
        String shellTest = ExternalCAS.execute("expr 1 + 2");
        if (!shellTest.equals("3")) {
            return false;
        }

        String mapleInput = "with(RegularChains):with(SemiAlgebraicSetTools):loct:=300"
                + ":inputform:=&E([b,c]), (m>0) &and (1+b>c) &and (b+c>1) &and (c+1>b) &and (1+b=m*(c))"
                + ":timelimit(loct,lprint(QuantifierElimination(inputform)));";
        System.out.println("Testing Maple connection via shell...");
        String mapleTest = ExternalCAS.execute("echo \"" + mapleInput + "\" | maple -q");
        if (!mapleTest.equals("0 < m-1")) {
            return false;
        }

        System.out.println("All tests passed");
        return true;
    }

    public static void main(String argv[])
    {
        if (!test()) {
            System.err.println("Unexpected results on self-test, exiting");
            System.exit(1);
        }
        try {
            System.out.println("Starting HTTP server on port 8765, press CTRL-C to terminate");
            // this is hardcoded, FIXME
            HTTPServer.start(8765);
        } catch (Exception e) {
            System.err.println("Cannot start HTTP server, exiting");
            System.exit(1);
        }
    }
}