package realgeom;

/*
  Link to external computer algebra systems via a shell call.
  Currently Maple and Mathematica use it.
 */

import java.io.IOException;
import java.io.InputStream;

public class ExternalCAS {
    static String execute (String command) {
        StringBuilder output = new StringBuilder();
        String[] cmd = {
            "/bin/sh",
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
        return output.toString();
    }

    static String executeMaple (String command) {
        return execute("echo \"" + command + "\" | maple -q");
    }

    static String executeMathematica (String command) {
        String output = ExternalCAS.execute("echo \"" + command + "\" | math | grep 'In\\[1\\]:= '");
        int ltrim = "In[1]:= ".length();
        if (output.length() < ltrim) {
            System.err.println("Error executing Mathematica command");
            return "";
        }
        return output.substring(ltrim);
    }





}
