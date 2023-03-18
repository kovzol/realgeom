package realgeom;

/*
 * Starts a benchmark.
 */

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;

import org.apache.commons.csv.*;

public class Benchmark {

    public static void start(String inputFile, String outputFile, String casToolList, int timelimit, String qepcadN,
                             String qepcadL) {

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
            File parentDir = file.getParentFile();
            parentDir.mkdirs();

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

        InetAddress ip;
        String hostname = "noname server";

        try {
            ip = InetAddress.getLocalHost();
            hostname = ip.getHostName();
        } catch (UnknownHostException e) {
            System.err.println("Cannot get hostname");
        }

        String head = "<!DOCTYPE html><html><head>\n" +
                "<title>realgeom benchmark</title>\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">" +
                "</head><body><h1>realgeom benchmark</h1>\n" +
                "<h2>on " + timestamp + " at " + hostname + "</h2>";
        StringBuilder tableHead = new StringBuilder("<table><tr><th>Name</th>\n");

        String[] casTools = casToolList.split(",");
        int[][] summary = new int[casTools.length][3];
        int total = 0;

        for (String casTool : casTools) {
            tableHead.append("<th>").append(casTool).append("</th>");
        }
        tableHead.append("</th>");
        StringBuilder[] table = new StringBuilder[3];
        table[0] = new StringBuilder();
        table[1] = new StringBuilder();
        table[2] = new StringBuilder();

        table[0].append("<h3>Automatic substitution</h2>");
        table[1].append("<h3>No substitution</h2>");
        table[2].append("<h3>QE exists</h2>");
        for (int i = 0; i < 3; ++i) {
            // adding head
            table[i].append(tableHead);
        }

        int cascounter = 0;
        for (CSVRecord record : records) {
            total++;
            String name = record.get("Name");
            String task = record.get("Task");
            String mode = record.get("Mode");
            String expected = record.get("Expected");

            if (task.equals("triangle") && mode.equals("explore")) {
                for (int i = 0; i < 2; ++i) {
                    // opening line
                    table[i].append("<tr><td class=\"ex\">").append(name).append("</td>");
                }
                String lhs = record.get("Parameter1");
                String rhs = record.get("Parameter2");
                cascounter = 0;
                for (String casTool : casTools) {
                    cascounter++;
                    Cas cas;
                    Tool tool = Tool.DEFAULT;
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
                            break;
                        case "qepcad":
                            cas = Cas.QEPCAD;
                            break;
                        case "tarski":
                            cas = Cas.TARSKI;
                            break;
                        case "redlog":
                            cas = Cas.REDLOG;
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
                        String response = Compute.triangleExplore(lhs, rhs, cas, tool, subst, Log.SILENT,
                                timelimit, qepcadN, qepcadL);
                        int elapsedTime = (int) (System.currentTimeMillis() - startTime);
                        System.out.print(name + ": triangleExplore(lhs=" + lhs + ",rhs=" + rhs +
                                ",cas=" + cas + ",tool=" + tool + ",subst=" + subst + ")");
                        String time = Double.toString((double) elapsedTime / 1000);
                        System.out.print("=" + response + " (" + time + " sec) ");
                        table[i].append("<td class=\"");
                        if (elapsedTime/1000 >= timelimit) {
                            System.out.println("FAIL TO");
                            table[i].append("timeout\">");
                            table[i].append("t/o");
                        } else if (response.equals(expected)) {
                            System.out.print("SUCCESS ");
                                summary[cascounter-1][i]++;
                                if (elapsedTime < 10000) {
                                    // solved in time
                                    System.out.println("ST");
                                    table[i].append("st\">");
                                } else
                                if (elapsedTime < 300000) {
                                    // solved in 300 sec
                                    System.out.println("S2");
                                    table[i].append("s2\">");
                                } else {
                                    // solved within timelimit
                                    System.out.println("S3");
                                    table[i].append("s3\">");
                                }
                            table[i].append(time);
                        } else {
                            System.out.println("FAIL ER");
                            table[i].append("error\">");
                            table[i].append(time);
                        }
                        table[i].append("</td>");
                    }
                }
            }
            if (task.equals("qe") && mode.equals("exists")) {
                // opening line
                table[2].append("<tr><td class=\"ex\">").append(name).append("</td>");
                String vars = record.get("Parameter1");
                String expr = record.get("Parameter2");
                cascounter = 0;
                for (String casTool : casTools) {
                    cascounter++;
                    Cas cas;
                    Tool tool = Tool.DEFAULT;
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
                            break;
                        case "qepcad":
                            cas = Cas.QEPCAD;
                            break;
                        case "tarski":
                            cas = Cas.TARSKI;
                            break;
                        case "redlog":
                            cas = Cas.REDLOG;
                            break;
                        default:
                            System.err.println("Cannot parse cas/tool " + casTool);
                            return;
                    }
                long startTime = System.currentTimeMillis();
                String response = Compute.qeExists(vars, expr, cas, tool, Log.SILENT,
                        timelimit, qepcadN, qepcadL);
                int elapsedTime = (int) (System.currentTimeMillis() - startTime);
                System.out.print(name + ": qeExists(vars=" + vars + ",expr=" + expr +
                        ",cas=" + cas + ",tool=" + tool + ")");
                String time = Double.toString((double) elapsedTime / 1000);
                System.out.print("=" + response + " (" + time + " sec) ");
                table[2].append("<td class=\"");
                if (elapsedTime/1000 >= timelimit) {
                    System.out.println("FAIL TO");
                    table[2].append("timeout\">");
                    table[2].append("t/o");
                } else if (response.equals(expected)) {
                    System.out.print("SUCCESS ");
                    summary[cascounter-1][2]++;
                    if (elapsedTime < 10000) {
                        // solved in time
                        System.out.println("ST");
                        table[2].append("st\">");
                    } else
                    if (elapsedTime < 300000) {
                        // solved in 300 sec
                        System.out.println("S2");
                        table[2].append("s2\">");
                    } else {
                        // solved within timelimit
                        System.out.println("S3");
                        table[2].append("s3\">");
                    }
                    table[2].append(time);
                } else {
                    System.out.println("FAIL ER");
                    table[2].append("error\">");
                    table[2].append(time);
                }
                table[2].append("</td>");
                }
            }
            // closing line
            table[2].append("</tr>");
        }
        for (int i = 0; i < 3; ++i) {
            // summary
            table[i].append("<tr><td class=\"summary\"><b>Summary (of ").append(total).append(")</b></td>");
            for (int j=0; j<cascounter; ++j) {
                table[i].append("<td class=\"summary\"><b>").append(summary[j][i]).append("</b></td>");
            }
            // closing summary line and table
            table[i].append("</tr></table>");
        }

        StringBuilder b = new StringBuilder();
        b.append(head).append(table[0]).append(table[1]).append(table[2]).append("</body></html>\n");

        try {
            out.write(b.toString());
            out.close();
        } catch (Exception e) {
            System.err.println("Error on writing file " + outputFile);
        }

    }
}
