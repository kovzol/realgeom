package realgeom;

/**
 * Listens on a TCP port, parses the GET parameters and forwards them to the computation subsystem.
 */

/* Taken from https://stackoverflow.com/a/3732328/1044586
 * and http://www.rgagnon.com/javadetails/java-get-url-parameters-using-jdk-http-server.html
 */

public class Compute {

    static String code;
    static String ineqs;
    static String response;

    static String triangleInequality(String a, String b, String c, Cas cas) {
        return "(" + a + "+" + b + ">" + c + ")";
    }

    static void appendIneqs(String ineq, Cas cas) {
        if (cas == Cas.MAPLE) {
            if (!"".equals(ineqs)) {
                ineqs += " &and ";
            }
            ineqs += ineq;
        }
    }

    static String eq(String lhs, String rhs, Cas cas) {
        return "(" + lhs + "=" + rhs + ")";
    }

    static void appendResponse (String message) {
        if (!"".equals(response)) {
            response += "\n";
        }
        response += message;
    }

    public static String triangleExplore(String lhs, String rhs, Cas cas, Subst subst, Log log, String timelimit) {
        String m = "m";
        code = "";
        ineqs = "";
        response = "";

        if (subst == Subst.AUTO) {
            lhs = GiacCAS.execute("subst(" + lhs + ",a=1)");
            rhs = GiacCAS.execute("subst(" + rhs + ",a=1)");
            if (log == Log.VERBOSE) {
                appendResponse("LOG: subst() => lhs=" + lhs + ",rhs=" + rhs);
            }
        }
        String a = "a";
        if (subst == Subst.AUTO) {
            a = "1";
        }
        appendIneqs("(" + m + ">0)", cas);
        appendIneqs(triangleInequality(a, "b", "c", cas), cas);
        appendIneqs(triangleInequality("b", "c", a, cas), cas);
        appendIneqs(triangleInequality("c", a, "b", cas), cas);
        appendIneqs(eq(lhs, m + "*(" + rhs + ")", cas), cas);
        if (log == Log.VERBOSE) {
            appendResponse("LOG: ineqs=" + ineqs);
        }

        if (cas == Cas.MAPLE) {
            code += "with(RegularChains):with(SemiAlgebraicSetTools):";
            code += "loct:=" + timelimit +":";
            code += "inputform:=" + "&E([";
            if (subst != Subst.AUTO) {
                code += "a,";
            }
            code += "b,c]), " + ineqs + ":";
            code += "timelimit(loct,lprint(QuantifierElimination(inputform)));";
            if (log == Log.VERBOSE) {
                appendResponse("LOG: code=" + code);
            }
            appendResponse(ExternalCAS.execute("echo \"" + code + "\" | maple -q"));
        }
        return response;
    }
}
