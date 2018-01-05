package realgeom;

/* Taken from https://stackoverflow.com/a/3732328/1044586
 * and http://www.rgagnon.com/javadetails/java-get-url-parameters-using-jdk-http-server.html
 */

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HTTPServer {

    enum Mode {EXPLORE, CHECK};
    enum Cas {MAPLE, GIAC, REDLOG, MATHEMATICA, QEPCAD};
    enum Subst {AUTO, NO};
    enum Log {VERBOSE, INFO, SILENT}

    // defaults
    static Mode mode = Mode.EXPLORE;
    static Cas cas = Cas.MAPLE;
    static Subst subst = Subst.AUTO;
    static Log log = Log.INFO;
    static String lhs = "0";
    static String rhs = "0";

    public static void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/triangle", new TriangleHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class TriangleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "";
            Map <String,String>parms = HTTPServer.queryToMap(t.getRequestURI().getQuery());
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
            if (log == Log.VERBOSE) {
                response += "LOG:log=" + log + ",mode=" + mode + ",cas=" + cas + ",subst=" + subst + ",lhs=" + lhs + ",rhs=" + rhs;
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        // Example: http://gonzales.risc.jku.at:8765/triangle?lhs=a+b&rhs=c
    }

  /**
   * returns the url parameters in a map
   * @param query
   * @return map
   */
  public static Map<String, String> queryToMap(String query){
    Map<String, String> result = new HashMap<String, String>();
    for (String param : query.split("&")) {
        String pair[] = param.split("=");
        if (pair.length>1) {
            result.put(pair[0], pair[1]);
        }else{
            result.put(pair[0], "");
        }
    }
    return result;
  }

}
