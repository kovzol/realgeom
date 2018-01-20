package realgeom;

/*
 * Starts a benchmark.
 */

import java.io.*;

import org.apache.commons.csv.*;
import java.sql.Timestamp;

public class Benchmark {

    public static void start(String inputFile, String outputFile, String casToolList, String timelimit) {

        Reader in;
        try {
            in = new FileReader(inputFile);
        } catch (FileNotFoundException e) {
            System.err.println("Input file " + inputFile + " cannot be found");
            return;
        }
        BufferedWriter out;
        try {
            File file = new File(outputFile);
            out = new BufferedWriter(new FileWriter(file));
        } catch (Exception e) {
            System.err.println("Error on opening file " + outputFile);
            return;
        }

        Iterable<CSVRecord> records;
        try {
            records = CSVFormat.DEFAULT.withHeader().parse(in);
        } catch (IOException e) {
            System.err.println("Error on parsing file " + inputFile);
            return;
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String head = "<!DOCTYPE html><html><head>\n" +
                "<title>realgeom benchmark</title>\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">" +
                "</head><body><h1>realgeom benchmark</h1>\n" +
                "<h2>on " + timestamp + "</h2>";
        StringBuilder tableHead = new StringBuilder("<table><tr>Name</th>\n");

        String[] casTools = casToolList.split(",");

        for (String casTool : casTools) {
            tableHead.append("<th>").append(casTool).append("</th>");
        }
        tableHead.append("</th>");
        StringBuilder[] table = new StringBuilder[2];
        table[0] = new StringBuilder();
        table[1] = new StringBuilder();

        table[0].append("<h2>Automatic substitution (a=1)</h2>");
        table[1].append("<h2>No substitution</h2>");
        for (int i = 0; i < 2; ++i) {
            // adding head
            table[i].append(tableHead);
        }

        for (CSVRecord record : records) {
            String name = record.get("Name");
            for (int i = 0; i < 2; ++i) {
                // opening line
                table[i].append("<tr><td class=\"ex\"</td>");
            }
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
                            break;
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
                        String time = Double.toString((double) elapsedTime / 1000);
                        System.out.print("=" + response + " (" + time + " sec) ");
                        table[i].append("<td class=\"");
                        if (response.equals(expected)) {
                            System.out.println("SUCCESS");
                            table[i].append("o1\">");
                        } else {
                            System.out.println("FAIL");
                            table[i].append("error\">");
                        }
                        table[i].append(time).append("></td>");
                    }
                }
            }
            for (int i = 0; i < 2; ++i) {
                // closing line
                table[i].append("</tr>");
            }
        }

        StringBuilder tail = new StringBuilder();
        tail.append("</body></html>");

        StringBuilder b = new StringBuilder();
        b.append(head).append(table[0]).append(table[1]).append(tail);

        try {
            out.write(b.toString());
            out.close();
        } catch (Exception e) {
            System.err.println("Error on writing file " + outputFile);
        }


    }
}
