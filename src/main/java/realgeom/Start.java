package realgeom;

/*
 * The main process. It attempts to connect to various computer algebra subsystems.
 * If everything is successful, then a HTTP server will be started.
 */

import java.io.File;

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

    private static String test() {
        String supported;
        System.out.println("Testing Giac connection...");
        String input = "1+2";
        String test = GiacCAS.execute(input);
        if (!test.equals("3")) {
            return "";
        }

        System.out.println("Testing shell connection...");
        input = "expr 1 + 2";
        test = ExternalCAS.execute(input);
        if (!test.equals("3")) {
            return "";
        }

        input = "Print[Quiet[Reduce[0 < m-1,m,Reals] // InputForm]]";
        System.out.println("Testing Mathematica connection via shell...");
        test = ExternalCAS.executeMathematica(input);
        if (!test.equals("m > 1")) {
            return "";
        }
        supported = "mathematica";

        input = "lprint(1+2);";
        System.out.println("Testing Maple connection via shell...");
        test = ExternalCAS.executeMaple(input);
        if (!test.equals("3")) {
            System.out.println("Consider installing Maple");
        } else {
            input = "with(RegularChains):with(SemiAlgebraicSetTools)"
                    + ":inputform:=&E([b,c]), (m>0) &and (1+b>c) &and (b+c>1) &and (c+1>b) &and (1+b=m*(c))"
                    + ":timelimit(300,lprint(QuantifierElimination(inputform)));";
            System.out.println("Testing Maple/RegularChains...");
            test = ExternalCAS.executeMaple(input);
            if (!test.equals("0 < m-1")) {
                System.out.println("Consider installing RegularChains from http://www.regularchains.org/downloads.html");
            } else {
                supported += ",maple/regularchains";
            }

            input = "with(SyNRAC):timelimit(300,lprint(qe(Ex([b,c],And((m>0),(1+b>c),(b+c>1),(c+1>b),(1+2*b=m*(c)))))));";
            System.out.println("Testing Maple/SyNRAC...");
            test = ExternalCAS.executeMaple(input);
            if (!test.equals("-m < -1")) {
                System.out.println("Consider installing SyNRAC from http://www.fujitsu.com/jp/group/labs/en/resources/tech/announced-tools/synrac/");
            } else {
                supported += ",maple/synrac";
            }
        }

        input = "[]\n" +
                "(m,b,c)\n" +
                "1\n" +
                "(Eb)(Ec)[1+b>c /\\ 1+c>b /\\ b+c>1 /\\  (1+b+c)^2=m (b+c+b c)].\n" +
                "assume[m>0].\n" +
                "finish";
        System.out.println("Testing QEPCAD connection via shell...");
        test = ExternalCAS.executeQepcad(input);
        if (!test.equals("m - 4 < 0 /\\ m - 3 >= 0")) {
            System.out.println("Consider installing QEPCAD (make sure you have a script `qepcad' on your path that correctly starts QEPCAD)");
        } else {
            supported += ",qepcad";
        }

        System.out.println("All required tests are passed");
        return supported;
    }

    public static void main(String argv[]) {
        String supported = test();
        if (supported.equals("")) {
            System.err.println("Unexpected results on self-test, exiting");
            System.exit(1);
        }
        System.out.println("Running benchmarks, this may take a while...");
        // this is hardcoded, FIXME
        new File("build").mkdirs();
        Benchmark.start("src/test/resources/benchmark.csv",
                "build/benchmark.html",
                supported, "300");
        System.out.println("Starting HTTP server on port 8765, press CTRL-C to terminate");
        try {
            // this is hardcoded, FIXME
            HTTPServer.start(8765);
        } catch (Exception e) {
            System.err.println("Cannot start HTTP server, exiting");
            System.exit(1);
        }
    }
}