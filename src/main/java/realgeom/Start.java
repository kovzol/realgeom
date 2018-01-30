package realgeom;

/*
 * The main process. It attempts to connect to various computer algebra subsystems.
 * If everything is successful, then a HTTP server will be started.
 */

import org.apache.commons.cli.*;

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

    private static String test(String timeLimit, String qepcadN, String qepcadL) {
        String supported;
        System.out.println("Testing Giac connection...");
        String input = "1+2";
        String test = GiacCAS.execute(input);
        if (!test.equals("3")) {
            return "";
        }

        System.out.println("Testing shell connection...");
        input = "expr 1 + 2";
        test = ExternalCAS.execute(input, timeLimit);
        if (!test.equals("3")) {
            return "";
        }

        input = "Print[Quiet[Reduce[0 < m-1,m,Reals] // InputForm]]";
        System.out.println("Testing Mathematica connection via shell...");
        test = ExternalCAS.executeMathematica(input, timeLimit);
        if (!test.equals("m > 1")) {
            return "";
        }
        supported = "mathematica";

        input = "lprint(1+2);";
        System.out.println("Testing Maple connection via shell...");
        test = ExternalCAS.executeMaple(input, timeLimit);
        if (!test.equals("3")) {
            System.out.println("Consider installing Maple (make sure you have the executable `maple' on your path)");
        } else {
            input = "with(RegularChains):with(SemiAlgebraicSetTools)"
                    + ":inputform:=&E([b,c]), (m>0) &and (1+b>c) &and (b+c>1) &and (c+1>b) &and (1+b=m*(c))"
                    + ":timelimit(300,lprint(QuantifierElimination(inputform)));";
            System.out.println("Testing Maple/RegularChains...");
            test = ExternalCAS.executeMaple(input, timeLimit);
            if (!test.equals("0 < m-1")) {
                System.out.println("Consider installing RegularChains from http://www.regularchains.org/downloads.html");
            } else {
                supported += ",maple/regularchains";
            }

            input = "with(SyNRAC):timelimit(300,lprint(qe(Ex([b,c],And((m>0),(1+b>c),(b+c>1),(c+1>b),(1+2*b=m*(c)))))));";
            System.out.println("Testing Maple/SyNRAC...");
            test = ExternalCAS.executeMaple(input, timeLimit);
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
        test = ExternalCAS.executeQepcad(input, timeLimit, qepcadN, qepcadL);
        if (!test.equals("m - 4 < 0 /\\ m - 3 >= 0")) {
            System.out.println("Consider installing QEPCAD (make sure you have a script `qepcad' on your path that correctly starts QEPCAD)");
        } else {
            supported += ",qepcad";
        }

        input = "1+2;";
        System.out.println("Testing Reduce connection via shell...");
        test = ExternalCAS.executeReduce(input, timeLimit);
        if (!test.equals("3")) {
            System.out.println("Consider installing Reduce (make sure you have the executable `reduce' on your path)."
                    + "\nSee also http://www.redlog.eu/get-redlog/ to have RedLog installed");
        } else {
            input = "rlqe(ex({b,c}, 1+b>c and 1+c>b and b+c>1 and a=m*(b+c)));";
            System.out.println("Testing RedLog connection via shell...");
            test = ExternalCAS.executeRedlog(input, timeLimit);
            if (!test.equals("m = 0 and a = 0 or m <> 0 and a*m - m**2 > 0$")) {
                System.out.println("Consider installing RedLog from http://www.redlog.eu/get-redlog/");
            } else {
                supported += ",redlog";
            }
        }

        System.out.println("All required tests are passed");
        System.out.println("Supported backends: " + supported);
        return supported;
    }

    public static void main(String argv[]) {

        Options options = new Options();

        Option serverOption = new Option("s", "server", false, "run HTTP server");
        serverOption.setRequired(false);
        options.addOption(serverOption);

        Option portOption = new Option("p", "port", true, "HTTP server port number");
        portOption.setRequired(false);
        options.addOption(portOption);

        Option benchmarkOption = new Option("b", "benchmark", false, "run benchmark");
        benchmarkOption.setRequired(false);
        options.addOption(benchmarkOption);

        Option backendOption = new Option("c", "backends", true, "backends");
        backendOption.setRequired(false);
        options.addOption(backendOption);

        Option inputOption = new Option("i", "input", true, "benchmark input file path");
        inputOption.setRequired(false);
        options.addOption(inputOption);

        Option outputOption = new Option("o", "output", true, "benchmark output file");
        outputOption.setRequired(false);
        options.addOption(outputOption);

        Option timeLimitOption = new Option("t", "timelimit", true, "time limit");
        timeLimitOption.setRequired(false);
        options.addOption(timeLimitOption);

        Option qepcadNOption = new Option("N", "qepcadN", true, "garbage collected space in cells (QEPCAD +N)");
        qepcadNOption.setRequired(false);
        options.addOption(qepcadNOption);

        Option qepcadLOption = new Option("L", "qepcadL", true, "space for prime list (QEPCAD +L)");
        qepcadLOption.setRequired(false);
        options.addOption(qepcadLOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, argv);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("realgeom", options);
            System.exit(1);
            return;
        }

        int timeLimit = 3600;
        if (cmd.hasOption("t")) {
            timeLimit = Integer.parseInt(cmd.getOptionValue("timelimit"));
        }
        System.out.println("Time limit is set to " + timeLimit + " seconds");

        String qepcadN = "500000000";
        if (cmd.hasOption("N")) {
            qepcadN = cmd.getOptionValue("qepcadN");
        }
        System.out.println("QEPCAD +N is set to " + qepcadN + " cells");

        String qepcadL = "200000";
        if (cmd.hasOption("L")) {
            qepcadL = cmd.getOptionValue("qepcadL");
        }
        System.out.println("QEPCAD +L is set to " + qepcadL + " primes");

        String supported = test(timeLimit + "", qepcadN, qepcadL);
        if (supported.equals("")) {
            System.err.println("Unexpected results on self-test, exiting");
            System.exit(1);
        }

        if (cmd.hasOption("b")) {
            StringBuilder backends = new StringBuilder(supported);
            if (cmd.hasOption("c")) {
                backends = new StringBuilder();
                String backendsRequested = cmd.getOptionValue("backends");
                System.out.println("Requested backends: " + backendsRequested);
                // The result of check should be put in a global array, TODO
                String[] backendsRequestedList = backendsRequested.split(",");
                for (String backendRequested : backendsRequestedList) {
                    if (!supported.contains(backendRequested)) {
                        System.err.println("Unsupported backend " + backendRequested);
                    } else {
                        backends.append(",").append(backendRequested);
                    }
                }
                if ("".equals(backends.toString())) {
                    System.err.println("No backends are available according to the request");
                    System.exit(2);
                }
                // trim leading ,
                backends = new StringBuilder(backends.substring(1));
            }

            String inputFilePath = "src/test/resources/benchmark.csv";
            if (cmd.hasOption("i")) {
                inputFilePath = cmd.getOptionValue("input");
            }
            System.out.println("Using " + inputFilePath + " as input file");

            String outputFilePath = "build/benchmark.html";
            if (cmd.hasOption("o")) {
                outputFilePath = cmd.getOptionValue("output");
            }
            System.out.println("Using " + outputFilePath + " as output file");

            System.out.println("Running benchmarks on backends " + backends + ", this may take a while...");
            Benchmark.start(inputFilePath,
                    outputFilePath,
                    backends.toString(), timeLimit, qepcadN, qepcadL);
            System.out.println("The benchmark has been successfully performed");
        }

        if (cmd.hasOption("s")) {
            int port = 8765;
            if (cmd.hasOption("p")) {
                port = Integer.parseInt(cmd.getOptionValue("port"));
            }
            System.out.println("Starting HTTP server on port " + port + ", press CTRL-C to terminate");
            try {
                HTTPServer.start(port);
            } catch (Exception e) {
                System.err.println("Cannot start HTTP server, exiting");
                System.exit(1);
            }
        }
    }
}