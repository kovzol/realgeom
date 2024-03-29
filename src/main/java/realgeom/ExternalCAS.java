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
import java.util.ArrayList;
import java.util.Map;
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
        if (Start.wolframscript) {
            mathematicaCommand = "wolframscript";
        }

        String output;
        int ltrim = 0;
        command = "TimeConstrained[" + command + "," + timeLimit + "]";
        if (Start.wolframscript) {
            output = ExternalCAS.execute(mathematicaCommand + " -code \""
                    + command + "\"", timeLimit).replace("\n", "").replace("\r", "");
            return output;
        }
        output = ExternalCAS.execute("echo \"" + command + "\" | " +
                mathematicaCommand + " | tail -n +4 | grep -v \"In\\[2\\]\"", timeLimit);
        ltrim = "In[1]:= ".length();
        if (output.length() < ltrim) {
            System.err.println("Error executing Mathematica command");
            return "";
        }
        output = output.replace("\n>", "");
        output = output.replace("\n", "");
        return output.substring(ltrim);
     }

    static String executeMathematica (String command, int timeLimit) {
        if (Start.dryRun)
            return "";
        if (Start.wolframscript) {
            return executeMathematica_obsolete (command, timeLimit);
        }
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

    static Process qepcadChild;
    static String qepcadNSaved, qepcadLSaved;

    static boolean startQepcadConnection(String qepcadN, String qepcadL) {
        Start.state = State.REINITIALIZATION_IN_PROGRESS;
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
        getOutputUntil(qepcadChild, "Enter an informal description  between '[' and ']':" + Start.nl);
        System.out.println("QEPCAD is properly started");
        Start.state = State.READY;
        return true;
    }

    static Process tarskiChild;
    static String tarskiNSaved;

    static boolean startTarskiConnection(String tarskiN) {
        Start.state = State.REINITIALIZATION_IN_PROGRESS;
        String qe = System.getenv("qe");
        tarskiNSaved = tarskiN;
        String tarskiCmd = "tarski +N" + tarskiN;
        String[] cmd;
        cmd = new String[3];
        if (Start.isWindows) {
            cmd[0] = "cmd";
            cmd[1] = "/c";
        } else {
            cmd[0] = "/bin/bash";
            cmd[1] = "-c";
        }
        cmd[2] = tarskiCmd;
        try {
            System.out.println("Starting Tarski connection...");
            // Inherit all environment variables (PATH is mandatory):
            Map<String, String> env = System.getenv();
            ArrayList<String> envpal = new ArrayList<>();
            for (String envName : env.keySet()) {
                envpal.add(envName + "=" + env.get(envName));
            }
            int s = envpal.size();;
            String[] envp = new String[s];
            for (int i = 0; i < s; ++i) {
                envp[i] = envpal.get(i);
            }

            tarskiChild = Runtime.getRuntime().exec(cmd, envp);
            System.out.println("Waiting 2s...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("Error on executing external command '" + tarskiCmd + "'");
            return false;
        }
        System.out.println("Tarski is properly started");
        Start.state = State.READY;
        return true;
    }

    static String getOutputUntil(Process child, String end) {
        StringBuilder output = new StringBuilder();
        InputStream out = child.getInputStream();
        int c = -2;
        boolean found = false;
        try {
            while (!found && (c != -1)) {
                c = out.read();
                output.append((char) c);
                // System.err.print("<" + (char) c + ">");
                found = output.toString().endsWith(end);
            }
        } catch (IOException e) {
            System.err.println("Error on reading output: " + e);
            return "";
        }
        if (c == -1) {
            // startQepcadConnection(qepcadNSaved, qepcadLSaved); // restart because an EOF was detected
            System.err.println("EOF detected");
            Start.state = State.INITIALIZATION_REQUIRED;
            return "";
        }
        return output.toString();
    }

    static String executeQepcadPipe (final String[] commands, final int[] responseLinesExpected, int timeLimit) {
        while (Start.state != State.READY) {
            if (Start.state == State.INITIALIZATION_REQUIRED) {
                restartQepcadConnection();
            }
            try {
                System.out.println("Waiting for READY");
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
                return "";
            }
        }
        String result = "";
        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<String> task = new Callable<String>() {
            public String call() {
                StringBuilder output = new StringBuilder();
                try {
                    OutputStream qepcadIn = qepcadChild.getOutputStream();
                    for (int i = 0; i < responseLinesExpected.length; ++i) {
                        output = new StringBuilder(); // reset
                        if (Start.debug) {
                            System.out.println(commands[i]);
                        }
                        byte[] b = commands[i].getBytes(StandardCharsets.UTF_8);
                        qepcadIn.write(b);
                        qepcadIn.write('\n'); // press ENTER
                        qepcadIn.flush();
                        for (int j = 0; j < responseLinesExpected[i]; ++j) {
                            String line = getOutputUntil(qepcadChild, Start.nl);
                            if (line.equals("")) {
                                return "";
                            }
                            output.append(line);
                        }
                        // System.out.println(output);
                    }
                } catch (IOException e) {
                    System.err.println("Error on reading QEPCAD output: " + e);
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
        try {
            result = future.get(timeLimit, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            System.err.println("Timeout");
            Start.state = State.INITIALIZATION_REQUIRED;
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        } catch (ExecutionException e) {
            System.err.println("Execution error");
        } finally {
            // System.err.println("Cancelling...");
            future.cancel(true);
        }
        if (Start.state == State.INITIALIZATION_REQUIRED) {
            restartQepcadConnection();
        }
        return result;
    }

    static String executeTarskiPipe (final String command, final int expectedLines, int timeLimit) {
        while (Start.state != State.READY) {
            try {
                if (Start.state == State.INITIALIZATION_REQUIRED) {
                    restartTarskiConnection();
                }
                System.out.println("Waiting for READY");
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
                return "";
            }
        }
        String result = "";
        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<String> task = new Callable<String>() {
            public String call() {
                StringBuilder output = new StringBuilder();
                try {
                    OutputStream tarskiIn = tarskiChild.getOutputStream();
                    output = new StringBuilder(); // reset
                    // System.out.println(command);
                    byte[] b = command.getBytes(StandardCharsets.UTF_8);
                    tarskiIn.write(b);
                    tarskiIn.write('\n'); // press ENTER
                    tarskiIn.flush();
                    String line = "";
                    // Reading echoed input:
                    if (!Start.isMac) {
                        getOutputUntil(tarskiChild, Start.nl);
                        }
                    for (int i = 0; i <  expectedLines ; ++i) {
                        // Reading actual output:
                        line = getOutputUntil(tarskiChild, Start.nl);
                    }
                    if (line.equals("")) {
                        return "";
                        }
                    output.append(line);
                } catch (IOException e) {
                    System.err.println("Error on reading Tarski output:" + e);
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
        try {
            result = future.get(timeLimit, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            System.err.println("Timeout");
            Start.state = State.INITIALIZATION_REQUIRED;
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        } catch (ExecutionException e) {
            System.err.println("Execution error");
        } finally {
            // System.err.println("Cancelling...");
            future.cancel(true);
        }
        if (Start.state == State.INITIALIZATION_REQUIRED) {
            restartTarskiConnection();
        }
        if (Start.debug) {
            System.out.println(result);
        }
        return getTarskiOutput(result);
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

    static void stopTarskiConnection() {
        System.out.println("Stopping Tarski connection...");
        tarskiChild.destroy();
        try {
            System.out.println("Waiting 1s...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    static void restartTarskiConnection() {
        stopTarskiConnection();
        startTarskiConnection(tarskiNSaved);
    }

    static String executeTarski (String command, int timeLimit, String qepcadN, String qepcadL) {
        if (Start.dryRun)
            return "";
        String output = ExternalCAS.execute("echo \"" + command + "\" | tarski -t " + timeLimit
                + " +N" + qepcadN + " +L" + qepcadL, timeLimit);
        String[] outputs = output.split("\n");
        String ret = "";
        for (String line : outputs) {
            if (!line.startsWith(">")) {
                if (ret.length() > 0) {
                    ret += "\n"; // if multiple lines are returned (hopefully not)
                }
                ret += getTarskiOutput(line);
            }
        }
        // System.out.println("executeTarski: " + command + " -> " + ret);
        return ret;
    }

    static String getTarskiOutput(String line) {
        if (line.endsWith(":err")) {
            return "";
        }
        int semicolon = line.indexOf(":");
        if (semicolon > -1) {
            String content = line.substring(0, semicolon);
            if (content.startsWith("[")) {
                content = content.substring(1, content.length() - 1); // trim [ and ]
            }
            return content;
        }
        return "";
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
        if (Start.debug) {
            System.out.println("reduce in = " + command);
        }
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
            if (!"".equals(line) && !line.startsWith("Reduce") && !line.startsWith("Quitting")
                    && !line.startsWith("Redlog Revision")
                    && !line.startsWith("(c) ")
                    && !"type ?; for help".equals(line)
                    && !"*** End-of-file read ".equals(line)
                    ) {
                int ll = line.length();
                if (ll<2 || !line.substring(ll-2).equals(": ")) {
                    retval.append(line);
                }
            }
            i++;
        }
        if (Start.debug) {
            System.out.println("reduce out = " + retval);
        }
        return retval.toString();
    }
}
