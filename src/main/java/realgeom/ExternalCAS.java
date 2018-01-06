package realgeom;

import java.io.IOException;
import java.io.InputStream;

public class ExternalCAS {
    static String execute (String command) {
        StringBuffer output = new StringBuffer();
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
        }
        // trim trailing newline
        if (output.substring(output.length()-1,output.length()).equals("\n")) {
            return (output.substring(0,output.length()-1)).toString();
        }
        return output.toString();
    }
}
