package realgeom;

/*
  Link to external computer algebra systems via a shell call.
  Currently Maple and Mathematica use it.
 */

import java.io.IOException;
import java.io.InputStream;

public class ExternalCAS {
    static String execute (String command, String timeLimit) {
        StringBuilder output = new StringBuilder();
        String[] cmd = {
            "/usr/bin/timeout",
            timeLimit,
            "/bin/bash",
            "-c",
            command
        };
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
        // System.out.println("maple command = " + command);
        return execute("echo \"" + command + "\" | maple -q", timeLimit);
    }

    static String executeMathematica (String command, String timeLimit) {
        String output = ExternalCAS.execute("echo \"" + command + "\" | math | grep 'In\\[1\\]:= '", timeLimit);
        int ltrim = "In[1]:= ".length();
        if (output.length() < ltrim) {
            System.err.println("Error executing Mathematica command");
            return "";
        }
        return output.substring(ltrim);
    }

    static String executeQepcad (String command, String timeLimit, String qepcadN, String qepcadL) {
        // System.out.println("qepcad in = " + command);
        String output = ExternalCAS.execute("echo \"" + command + "\" | qepcad +N" + qepcadN + " +L" + qepcadL,
                timeLimit);
        String[] outputLines = output.split("\n");
        int i = 0;
        int l = outputLines.length;
        if (l==0) {
            return "";
        }
        while (!"An equivalent quantifier-free formula:".equals(outputLines[i]) && (i < (l - 1))) i++;
        if (i==l-1) {
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

    static String executeRedlog (String command, String timeLimit) {
        String preamble = "off echo$off nat$rlset r$linelength(100000)$";
        return executeReduce(preamble + command, timeLimit);
    }

    static String executeReduce (String command, String timeLimit) {
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
