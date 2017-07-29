package realgeom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adapted from
 * http://www.jotschi.de/Uncategorized/2011/09/26/jogl2-jogamp-classpathloader
 * -for-native-libraries.html
 *
 */
public class MyClassPathLoader {

    /**
     * Loads the given library with the libname from the classpath root
     *
     * @param libname
     *            eg javagiac or javagiac64
     * @return success
     */
    public boolean loadLibrary(String libname) {

        String extension, prefix;
        // assume Linux
        prefix = "lib";
        extension = ".so";

        String filename = prefix + libname + extension;
        InputStream ins = ClassLoader.getSystemResourceAsStream(filename);

        if (ins == null) {
            System.err.println(filename + " not found");
            return false;
        }

        String fname = prefix + libname + Math.random() + extension;

        try {

            // Math.random() to avoid problems with 2 instances
            File tmpFile = writeTmpFile(ins, fname);
            System.load(tmpFile.getAbsolutePath());
            tmpFile.delete();
            ins.close();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                ins.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return false;
        }

        return true;
    }

    /**
     * Write the content of the inputstream into a tempfile with the given
     * filename
     *
     * @param ins
     * @param filename
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static File writeTmpFile(InputStream ins, String filename)
    throws IOException {

        File tmpFile = new File(System.getProperty("java.io.tmpdir"), filename);
        tmpFile.delete();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tmpFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = ins.read(buffer)) != -1) {

                fos.write(buffer, 0, len);
            }
        } finally {
            if (ins != null) {

                // need try/catch to be sure fos gets closed
                try {
                    ins.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                fos.close();
            }
        }
        return tmpFile;
    }

}