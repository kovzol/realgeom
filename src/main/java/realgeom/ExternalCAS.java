package realgeom;

/*
  Link to external computer algebra systems via a shell call.
  Currently Maple and Mathematica use it.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.wolfram.jlink.*;

public class ExternalCAS {
    private static KernelLink ml;

    static String execute (String command, int timeLimit) {
        StringBuilder output = new StringBuilder();
        String[] cmd = null;
        if (timeLimit != 0) {
            if (Start.isMac) {
                // This is actually not always working.
                // TODO: Use a real timeout implementation instead.
                // Based on https://stackoverflow.com/a/35512328/1044586
                // which is, unfortunately, not correct.
                cmd = new String[5];
                cmd[0] = "perl";
                cmd[1] = "-e";
                cmd[2] = "alarm shift; exec @ARGV";
                cmd[3] = timeLimit + "";
                cmd[4] = command;
                }
            if (Start.isLinux) {
                cmd = new String[5];
                cmd[0] = "/usr/bin/timeout";
                cmd[1] = timeLimit + "";
                cmd[2] = "/bin/bash";
                cmd[3] = "-c";
                cmd[4] = command;
                }
            if (Start.isWindows) {
                // TODO: implement timeout
                cmd = new String[3];
                cmd[0] = "cmd";
                cmd[1] = "/c";
                cmd[2] = command;
                }
            } else {
            cmd = new String[3];
            if (Start.isWindows) {
                cmd[0] = "cmd";
                cmd[1] = "/c";
                } else {
                cmd[0] = "/bin/bash";
                cmd[1] = "-c";
                cmd[2] = command;
                }
            }
        try {
            Process child = Runtime.getRuntime().exec(cmd);
            InputStream in = child.getInputStream();
            int c;
            while ((c = in.read()) != -1) {
                output.append((char)c);
            }
            in.close();
        } catch (IOException e) {
            System.err.println("Error on executing external command");
            return "";
        }
        if (output.length()==0) {
            return "";
        }
        // trim trailing newline
        if (output.substring(output.length()-1,output.length()).equals("\n")) {
            return (output.substring(0,output.length()-1));
        }
        System.out.println(output);
        return output.toString();
    }

    static String executeMaple (String command, int timeLimit) {
        if (Start.dryRun)
            return "";
        // System.out.println("maple command = " + command);
        return execute("echo \"" + command + "\" | maple -q", timeLimit);
    }

    static String executeMathematica_obsolete (String command, int timeLimit) {
        String mathematicaCommand = "math";
        if (Start.isPiUnix) {
            mathematicaCommand = "wolfram";
            }
        String output = ExternalCAS.execute("echo \"" + command + "\" | " +
            mathematicaCommand + " | tail -n +4 | grep -v \"In\\[2\\]\"", timeLimit);
        int ltrim = "In[1]:= ".length();
        if (output.length() < ltrim) {
            System.err.println("Error executing Mathematica command");
            return "";
        }
        output = output.replace("\n>", "");
        output = output.replace("\n", "");
        // output = output.replace("In[1]:= ", "");
        return output.substring(ltrim);
     }

    static String executeMathematica (String command, int timeLimit) {
        if (Start.dryRun)
            return "";
        command = "TimeConstrained[" + command + "," + timeLimit + "]";
        String ret = ml.evaluateToInputForm(command, 0);
        // System.out.println("executeMathematica: " + command + " -> " + ret);
        return ret;
    }

    static boolean createMathLink () {
        String mathematicaCommand = "math";
        if (Start.isPiUnix) {
            mathematicaCommand = "wolfram";
            }
        try {
            ml = MathLinkFactory.createKernelLink("-linkmode launch -linkname '" + mathematicaCommand + " -mathlink'");
            ml.discardAnswer();
            } catch (MathLinkException e) {
            System.err.println("createMathLink: " + e.toString());
            return false;
            }
        return true;
    }

    static void stopMathLink () {
        System.out.println("Stopping MathLink connection...");
        ml.close();
    }

    static String executeQepcad (String command, int timeLimit, String qepcadN, String qepcadL) {
        // System.out.println("qepcad in = " + command);
        if (Start.dryRun)
            return "";
        String output;
        if (Start.isWindows) {
            File tempFile;
            try {
                tempFile = File.createTempFile("qepcad-input-", ".tmp");
                FileWriter writer = new FileWriter(tempFile);
                writer.write(command + "\n");
                writer.close();
            } catch (IOException e) {
                return "";
            }
            // System.out.println("Using temporary file " + tempFile.getAbsolutePath());
            output = ExternalCAS.execute("qepcad -noecho +N" + qepcadN
                   + " < " + tempFile.getAbsolutePath(), timeLimit);
        } else {
            output = ExternalCAS.execute("echo \"" + command + "\" | qepcad +N" + qepcadN + " +L" + qepcadL
                    + " -t " + timeLimit, timeLimit);
        }
        // System.out.println(output);
        String[] outputLines;
        outputLines = output.split(Start.nl);

        int i = 0;
        int l = outputLines.length;
        if (l==0) {
            // System.out.println("No output 1");
            return "";
        }
        while (!"An equivalent quantifier-free formula:".equals(outputLines[i]) && (i < (l - 1))) i++;
        if (i==l-1) {
            // System.out.println("No output 2");
            // for (int j=0; j<l; ++j) {
            //     System.out.println(j + " " + outputLines[j]);
            // }
            return "";
        }
        StringBuilder retval = new StringBuilder();
        i++;
        while (!"=====================  The End  =======================".equals(outputLines[i]) && i < l) {
            if (!"".equals(outputLines[i])) {
                retval.append(outputLines[i]);
            }
            i++;
        }
        // System.out.println("qepcad out = " + retval);
        return retval.toString();
    }

    // static StringBuilder qepcadOutput;
    static Process qepcadChild;
    static String qepcadNSaved, qepcadLSaved;

    static boolean startQepcadConnection(String qepcadN, String qepcadL) {
        qepcadNSaved = qepcadN;
        qepcadLSaved = qepcadL;
        String qepcadCmd = "qepcad -noecho +N" + qepcadN + " +L" + qepcadL;
        String[] cmd;
        cmd = new String[3];
        if (Start.isWindows) {
            cmd[0] = "cmd";
            cmd[1] = "/c";
        } else {
            cmd[0] = "/bin/bash";
            cmd[1] = "-c";
        }
        cmd[2] = qepcadCmd;
        try {
            System.out.println("Starting QEPCAD connection...");
            qepcadChild = Runtime.getRuntime().exec(cmd);
            System.out.println("Waiting 2s...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("Error on executing external command '" + qepcadCmd + "'");
            return false;
        }
        System.out.println("Waiting for initial QEPCAD output...");
        getQepcadOutputUntil("Enter an informal description  between '[' and ']':" + Start.nl);
        System.out.println("QEPCAD is properly started");
        return true;
    }

    static String getQepcadOutputUntil(String end) {
        StringBuilder output = new StringBuilder();
        InputStream qepcadOut = qepcadChild.getInputStream();
        int c = -2;
        boolean found = false;
        try {
            while (!found && (c != -1)) {
                c = qepcadOut.read();
                output.append((char) c);
                found = output.toString().endsWith(end);
            }
        } catch (IOException e) {
            System.err.println("Error on reading QEPCAD output");
            return "";
        }
        if (c == -1) {
            // startQepcadConnection(qepcadNSaved, qepcadLSaved); // restart because an EOF was detected
            System.err.println("EOF detected");
            return "";
        }
        return output.toString();
    }

    static String executeQepcadPipe (final String[] commands, final int[] responseLinesExpected, int timeLimit) {
        String result = "";
        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<String> task = new Callable<String>() {
            public String call() {
                StringBuilder output = new StringBuilder();
                try {
                    OutputStream qepcadIn = qepcadChild.getOutputStream();
                    for (int i = 0; i < responseLinesExpected.length; ++i) {
                        output = new StringBuilder(); // reset
                        System.out.println(commands[i]);
                        byte[] b = commands[i].getBytes(StandardCharsets.UTF_8);
                        qepcadIn.write(b);
                        qepcadIn.write('\n'); // press ENTER
                        qepcadIn.flush();
                        for (int j = 0; j < responseLinesExpected[i]; ++j) {
                            String line = getQepcadOutputUntil(Start.nl);
                            if (line.equals("")) {
                                return "";
                            }
                            output.append(line);
                        }
                        // System.out.println(output);
                    }
                } catch (IOException e) {
                    System.err.println("Error on reading QEPCAD output");
                    return "";
                }

                // trim trailing newline
                if (output.substring(output.length() - 1, output.length()).equals(Start.nl)) {
                    return (output.substring(0, output.length() - 1));
                }
                System.out.println(output);
                return output.toString();
            }
        };
        Future<String> future = executor.submit(task);
        boolean restartNeeded = false;
        try {
            result = future.get(timeLimit, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            System.err.println("Timeout");
            restartNeeded = true;
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        } catch (ExecutionException e) {
            System.err.println("Execution error");
        } finally {
            // System.err.println("Cancelling...");
            future.cancel(true);
            if (restartNeeded) {
                restartQepcadConnection();
            }
        }
        return result;
    }

    static void stopQepcadConnection() {
        System.out.println("Stopping QEPCAD connection...");
        qepcadChild.destroy();
        try {
            System.out.println("Waiting 1s...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    static void restartQepcadConnection() {
        stopQepcadConnection();
        startQepcadConnection(qepcadNSaved, qepcadLSaved);
    }

    static String executeRedlog (String command, int timeLimit) {
        if (Start.dryRun)
            return "";
        String preamble = "off echo$off nat$rlset r$linelength(100000)$";
        return executeReduce(preamble + command, timeLimit);
    }

    static String executeReduce (String command, int timeLimit) {
        if (Start.dryRun)
            return "";
        // System.out.println("reduce in = " + command);
        String output = ExternalCAS.execute("echo '" + command + "' | reduce", timeLimit);
        String[] outputLines = output.split("\n");
        int i = 0;
        int l = outputLines.length;
        if (l==0) {
            return "";
        }
        StringBuilder retval = new StringBuilder();
        i++;
        while (i < l) {
            String line = outputLines[i];
            if (!"".equals(line) && !"*** End-of-file read ".equals(line)) {
                int ll = line.length();
                if (ll<2 || !line.substring(ll-2).equals(": ")) {
                    retval.append(line);
                }
            }
            i++;
        }
        // System.out.println("reduce out = " + retval);
        return retval.toString();
    }
}
