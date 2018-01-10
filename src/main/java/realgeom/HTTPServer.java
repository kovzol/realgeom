package realgeom;

/**
 * Listens on a TCP port, parses the GET parameters and forwards them to the computation subsystem.
 */

/* Taken from https://stackoverflow.com/a/3732328/1044586
 * and http://www.rgagnon.com/javadetails/java-get-url-parameters-using-jdk-http-server.html
 */

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HTTPServer {

    public static void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/triangle", new TriangleHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class TriangleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            long startTime = System.currentTimeMillis();
            response = "";

            // defaults
            Mode mode = Mode.EXPLORE;
            Cas cas = Cas.MAPLE;
            Tool tool = Tool.DEFAULT;
            Subst subst = Subst.AUTO;
            Log log = Log.INFO;
            String lhs = "0";
            String rhs = "0";
            String timelimit = "300";

            Map<String, String> parms = HTTPServer.queryToMap(t.getRequestURI().getQuery());
            // reading parameters
            if (parms.containsKey("mode")) {
                if (parms.get("mode").equals("check")) {
                    mode = Mode.CHECK;
                }
                if (parms.get("mode").equals("explore")) {
                    mode = Mode.EXPLORE;
                }
            }
            if (parms.containsKey("cas")) {
                if (parms.get("cas").equals("giac")) {
                    cas = Cas.GIAC;
                }
                if (parms.get("cas").equals("maple")) {
                    cas = Cas.MAPLE;
                }
                if (parms.get("cas").equals("mathematica")) {
                    cas = Cas.MATHEMATICA;
                }
                if (parms.get("cas").equals("redlog")) {
                    cas = Cas.REDLOG;
                }
                if (parms.get("cas").equals("qepcad")) {
                    cas = Cas.QEPCAD;
                }
            }
            if (cas == Cas.MAPLE) {
                tool = Tool.REGULAR_CHAINS; // default
                if (parms.containsKey("tool")) {
                    if (parms.get("tool").equals("synrac")) {
                        tool = Tool.SYNRAC;
                    }
                    if (parms.get("tool").equals("regularchains")) {
                        tool = Tool.REGULAR_CHAINS;
                    }
                }
            }
            if (parms.containsKey("subst")) {
                if (parms.get("subst").equals("auto")) {
                    subst = Subst.AUTO;
                }
                if (parms.get("subst").equals("no")) {
                    subst = Subst.NO;
                }
            }
            if (parms.containsKey("log")) {
                if (parms.get("log").equals("verbose")) {
                    log = Log.VERBOSE;
                }
                if (parms.get("log").equals("info")) {
                    log = Log.INFO;
                }
                if (parms.get("log").equals("silent")) {
                    log = Log.SILENT;
                }
            }
            if (parms.containsKey("lhs")) {
                lhs = parms.get("lhs");
            }
            if (parms.containsKey("rhs")) {
                rhs = parms.get("rhs");
            }
            if (parms.containsKey("timelimit")) {
                timelimit = parms.get("timelimit");
            }

            if (log == Log.VERBOSE) {
                appendResponse("LOG: log=" + log + ",mode=" + mode + ",cas=" + cas + ",tool="+tool+",subst=" + subst + ",lhs=" + lhs
                        + ",rhs=" + rhs + ",timelimit=" + timelimit, true);
            }
            if (mode == Mode.EXPLORE) {
                appendResponse(Compute.triangleExplore(lhs, rhs, cas, tool, subst, log, timelimit), false);
            }
            if (mode == Mode.CHECK) {
                String min = "";
                String max = "";
                String inf = "";
                String sup = "";
                if (parms.containsKey("min")) {
                    min = parms.get("min");
                    if (parms.containsKey("inf")) {
                        appendResponse("ERROR: min and inf cannot be defined at the same time", true);
                        message(t, 400);
                        return;
                    }
                }
                if (parms.containsKey("max")) {
                    max = parms.get("max");
                    if (parms.containsKey("sup")) {
                        appendResponse("ERROR: max and sup cannot be defined at the same time", true);
                        message(t, 400);
                        return;
                    }
                }
                if (parms.containsKey("inf")) {
                    inf = parms.get("inf");
                }
                if (parms.containsKey("sup")) {
                    min = parms.get("sup");
                }
                appendResponse("LOG: min=" + min + ",max=" + max + ",inf=" + inf + ",sup=" + sup, true);
            }
            int elapsedTime = (int) ((long) System.currentTimeMillis() - startTime);
            if (log == Log.VERBOSE) {
                appendResponse("LOG: time=" + ((double) elapsedTime/1000), true);
            }
            message(t, 200);


        }

        static void message(HttpExchange t, int retval) throws IOException {
            t.sendResponseHeaders(retval, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        static String response;
        public void appendResponse (String message, boolean echo) {
            if (!"".equals(response)) {
                response += "\n";
            }
            response += message;
            if (echo) {
                System.out.println(new Timestamp(System.currentTimeMillis()) + " " + message);
                }
        }



        // Example: http://gonzales.risc.jku.at:8765/triangle?lhs=a+b&rhs=c
    }

    /**
     * returns the url parameters in a map
     * @param query
     * @return map
     */
    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }

}
