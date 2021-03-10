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

import com.wolfram.jlink.*;

public class ExternalCAS {
    private static KernelLink ml;

    static String execute (String command, String timeLimit) {
        StringBuilder output = new StringBuilder();
        String[] cmd = null;
        if (timeLimit != null) {
            if (Start.isMac) {
                // This is actually not always working.
                // TODO: Use a real timeout implementation instead.
                // Based on https://stackoverflow.com/a/35512328/1044586
                // which is, unfortunately, not correct.
                cmd = new String[5];
                cmd[0] = "perl";
                cmd[1] = "-e";
                cmd[2] = "alarm shift; exec @ARGV";
                cmd[3] = timeLimit;
                cmd[4] = command;
                }
            if (Start.isLinux) {
                cmd = new String[5];
                cmd[0] = "/usr/bin/timeout";
                cmd[1] = timeLimit;
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

    static String executeMaple (String command, String timeLimit) {
        if (Start.dryRun)
            return "";
        // System.out.println("maple command = " + command);
        return execute("echo \"" + command + "\" | maple -q", timeLimit);
    }

    static String executeMathematica_obsolete (String command, String timeLimit) {
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

    static String executeMathematica (String command, String timeLimit) {
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

    static String executeQepcad (String command, String timeLimit, String qepcadN, String qepcadL) {
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
        if (Start.isWindows)
            outputLines = output.split("\r\n");
        else
            outputLines = output.split("\n");

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

    static boolean startQepcadConnection(String qepcadN, String qepcadL) {
        String qepcadCmd = "qepcad -noecho +N" + qepcadN + " +L" + qepcadL;
        String[] cmd = null;
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
            qepcadChild = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            System.err.println("Error on executing external command '" + qepcadCmd + "'");
            return false;
        }
        getQepcadOutputUntil("Enter an informal description  between '[' and ']':\n");
        return true;
    }

    static String getQepcadOutputUntil(String end) {
        StringBuilder output = new StringBuilder();
        InputStream qepcadOut = qepcadChild.getInputStream();
        int c;
        boolean found = false;
        try {
            while (!found) {
                c = qepcadOut.read();
                output.append((char) c);
                found = output.toString().endsWith(end);
            }
        } catch (IOException e) {
            System.err.println("Error on reading QEPCAD output");
            return null;
        }
        return output.toString();
    }

    static String executeQepcadPipe (String[] commands, int[] responseLinesExpected, String timeLimit) {
        String output = null;
        try {
            OutputStream qepcadIn = qepcadChild.getOutputStream();
            for (int i = 0; i < responseLinesExpected.length; ++i) {
                if (i == responseLinesExpected.length - 1) {
                    output = ""; // reset
                }
                byte[] b = commands[i].getBytes(StandardCharsets.UTF_8);
                qepcadIn.write(b);
                qepcadIn.write('\n'); // press ENTER
                qepcadIn.flush();
                for (int j = 0; j < responseLinesExpected[i]; ++j) {
                    output += getQepcadOutputUntil("\n");
                }
            }
        } catch (IOException e) {
                System.err.println("Error on reading QEPCAD output");
        }

        // trim trailing newline
        if (output.substring(output.length() - 1, output.length()).equals("\n")) {
            return (output.substring(0, output.length() - 1));
        }
        System.out.println(output);
        return output;
    }

    static void stopQepcadConnection() {
        qepcadChild.destroy();
    }

    static String executeRedlog (String command, String timeLimit) {
        if (Start.dryRun)
            return "";
        String preamble = "off echo$off nat$rlset r$linelength(100000)$";
        return executeReduce(preamble + command, timeLimit);
    }

    static String executeReduce (String command, String timeLimit) {
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
