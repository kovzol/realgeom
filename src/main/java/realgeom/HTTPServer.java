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

    public static void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/test", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Map <String,String>parms = HTTPServer.queryToMap(t.getRequestURI().getQuery());
            String response = "Welcome!\n";
            if (parms.containsKey("data")) {
                response += "data=" + parms.get("data");
            } else {
                response += "No data given";
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        // Example: http://gonzales.risc.jku.at:8765/test/data=1234
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
