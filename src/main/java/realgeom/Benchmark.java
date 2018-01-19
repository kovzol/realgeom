package realgeom;

/*
 * Starts a benchmark.
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.csv.*;

public class Benchmark {

    public static void start(String inputFile, String casToolList, String timelimit) {

        Reader in;
        try {
            in = new FileReader(inputFile);
        } catch (FileNotFoundException e) {
            System.err.println("Input file " + inputFile + " cannot be found");
            return;
        }
        Iterable<CSVRecord> records;
        try {
            records = CSVFormat.DEFAULT.withHeader().parse(in);
        } catch (IOException e) {
            System.err.println("Error parsing file " + inputFile);
            return;
        }

        String[] casTools = casToolList.split(",");
        for (CSVRecord record : records) {
            String name = record.get("Name");
            String task = record.get("Task");
            String mode = record.get("Mode");
            String expected = record.get("Expected");
            if (task.equals("triangle") && mode.equals("explore")) {
                String lhs = record.get("LHS");
                String rhs = record.get("RHS");
                for (String casTool : casTools) {
                    Cas cas;
                    Tool tool;
                    switch (casTool) {
                        case "maple/synrac":
                            cas = Cas.MAPLE;
                            tool = Tool.SYNRAC;
                            break;
                        case "maple/regularchains":
                        case "maple":
                            cas = Cas.MAPLE;
                            tool = Tool.REGULAR_CHAINS;
                            break;
                        case "mathematica":
                        case "mathematica/default":
                            cas = Cas.MATHEMATICA;
                            tool = Tool.DEFAULT;
                        default:
                            System.err.println("Cannot parse cas/tool " + casTool);
                            return;
                    }
                    for (int i = 0; i < 2; ++i) {
                        Subst subst = Subst.AUTO;
                        if (i == 1) {
                            subst = Subst.NO;
                        }

                        long startTime = System.currentTimeMillis();
                        String response = Compute.triangleExplore(lhs, rhs, cas, tool, subst, Log.SILENT, timelimit);
                        int elapsedTime = (int) (System.currentTimeMillis() - startTime);
                        System.out.print(name + ": triangleExplore(lhs=" + lhs + ",rhs=" + rhs +
                                ",cas=" + cas + ",tool=" + tool + ",subst=" + subst + ")");
                        System.out.print("=" + response + " (" + ((double) elapsedTime / 1000) + " sec) ");
                        if (response.equals(expected)) {
                            System.out.println("SUCCESS");
                        } else {
                            System.out.println("FAIL");
                        }
                    }
                }
            }
        }
    }
}
