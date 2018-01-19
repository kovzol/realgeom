package realgeom;

/*
 * It computes the real geometry problem.
 */

import java.sql.Timestamp;

public class Compute {

    private static String code;
    private static String ineqs;
    private static String response;
    private static Log maxLogLevel;

    private static String triangleInequality(String a, String b, String c, Cas cas) {
        return "(" + a + "+" + b + ">" + c + ")";
    }

    private static void appendIneqs(String ineq, Cas cas, Tool tool) {
        if (cas == Cas.MAPLE) {
            if (tool == Tool.REGULAR_CHAINS) {
                if (!"".equals(ineqs)) {
                    ineqs += " &and ";
                }
                ineqs += ineq;
            }
            if (tool == Tool.SYNRAC) {
                if (!"".equals(ineqs)) {
                    ineqs = ineqs.substring(0, ineqs.length() - 1) + "," + ineq + ")";
                } else {
                    ineqs = "And(" + ineq + ")";
                }
            }
        }
        if (cas == Cas.MATHEMATICA) {
            if (!"".equals(ineqs)) {
                ineqs += " \\[And] ";
            }
            ineqs += ineq;
        }
    }

    private static String eq(String lhs, String rhs, Cas cas) {
        if (cas == Cas.MAPLE) {
            return "(" + lhs + "=" + rhs + ")";
        }
        // if (cas == Cas.MATHEMATICA)
        return "" + lhs + " == " + rhs + "";
    }

    private static void appendResponse(String message, Log logLevel) {
        if (maxLogLevel == Log.VERBOSE || (maxLogLevel == Log.INFO && logLevel != Log.SILENT)) {
            System.out.println(new Timestamp(System.currentTimeMillis()) + " " + message);
        }
        if (maxLogLevel != Log.VERBOSE && logLevel == Log.VERBOSE) {
            return;
        }
        if (!"".equals(response)) {
            response += "\n";
        }
        response += message;
    }

    public static String triangleExplore(String lhs, String rhs, Cas cas, Tool tool, Subst subst, Log log, String timelimit) {
        String m = "m";
        code = "";
        ineqs = "";
        response = "";
        maxLogLevel = log;

        if (subst == Subst.AUTO) {
            lhs = GiacCAS.execute("subst(" + lhs + ",a=1)");
            rhs = GiacCAS.execute("subst(" + rhs + ",a=1)");
            appendResponse("LOG: subst() => lhs=" + lhs + ",rhs=" + rhs, Log.VERBOSE);
        }
        String a = "a";
        if (subst == Subst.AUTO) {
            a = "1";
        }
        appendIneqs("(" + m + ">0)", cas, tool);
        appendIneqs(triangleInequality(a, "b", "c", cas), cas, tool);
        appendIneqs(triangleInequality("b", "c", a, cas), cas, tool);
        appendIneqs(triangleInequality("c", a, "b", cas), cas, tool);
        appendIneqs(eq(lhs, m + "*(" + rhs + ")", cas), cas, tool);
        appendResponse("LOG: ineqs=" + ineqs, Log.VERBOSE);

        if (cas == Cas.MATHEMATICA) {
            String code;
            String vars = "{";
            if (subst != Subst.AUTO) {
                vars += "a,";
            }
            vars += "b,c}";

            // String mathcode = "Print[Quiet[Reduce[" + rewrite + ",m,Reals] // InputForm]]";

            code = "Print[Quiet[Reduce[Resolve[Exists[" + vars + "," + ineqs + "],Reals],Reals] // InputForm]]";
            appendResponse("LOG: code=" + code,Log.VERBOSE);
            String result = ExternalCAS.executeMathematica(code);
            appendResponse(result, Log.INFO);
        }

        if (cas == Cas.MAPLE) {
            String initCode = "";
            String commandCode = "";
            String vars = "[";
            if (subst != Subst.AUTO) {
                vars += "a,";
            }
            vars += "b,c]";
            if (tool == Tool.REGULAR_CHAINS) {
                initCode = "with(RegularChains):with(SemiAlgebraicSetTools):";
                commandCode = "QuantifierElimination(&E(" + vars + ")," + ineqs + ")";
            }
            if (tool == Tool.SYNRAC) {
                initCode = "with(SyNRAC):";
                commandCode = "qe(Ex(" + vars + "," + ineqs + "))";
            }
            code = initCode + "timelimit(" + timelimit + ",lprint(" + commandCode + "));";
            appendResponse("LOG: code=" + code,Log.VERBOSE);

            String result = ExternalCAS.executeMaple(code);
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            // hacky way to convert Maple formula to Mathematica formula FIXME
            String rewrite = result.replace("\\", "").replace("\n","").replace("`&or`(", "Or[").
                    replace("`&and`(", "And[").replace("Or(", "Or[").
                    replace("And(", "And[").replace("),", "],").replace("=", "==").
                    replace(">==", ">=").replace("<==", "<=");
            // convert closing ) to ]
            int i;
            int l = rewrite.length();
            i = l - 1 ;
            while (rewrite.substring(i,i+1).equals(")")) {
                i--;
            }
            StringBuilder b = new StringBuilder();
            for (int j = i; j < l -1 ; j++) {
                b.append("]");
            }
            rewrite = rewrite.substring(0, i + 1) + b;
            String mathcode = "Print[Quiet[Reduce[" + rewrite + ",m,Reals] // InputForm]]";
            appendResponse("LOG: mathcode=" + mathcode, Log.VERBOSE);
            String real = ExternalCAS.executeMathematica(mathcode);
            appendResponse(real, Log.INFO);
        }
        return response;
    }
}
