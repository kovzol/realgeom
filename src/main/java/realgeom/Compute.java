package realgeom;

/**
 * It computes the real geometry problem.
 */

import java.sql.Timestamp;

public class Compute {

    static String code;
    static String ineqs;
    static String response;

    static String triangleInequality(String a, String b, String c, Cas cas) {
        return "(" + a + "+" + b + ">" + c + ")";
    }

    static void appendIneqs(String ineq, Cas cas, Tool tool) {
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
    }

    static String eq(String lhs, String rhs, Cas cas) {
        return "(" + lhs + "=" + rhs + ")";
    }

    static void appendResponse (String message) {
        if (!"".equals(response)) {
            response += "\n";
        }
        response += message;
        System.out.println(new Timestamp(System.currentTimeMillis()) + " " + message);
    }

    public static String triangleExplore(String lhs, String rhs, Cas cas, Tool tool, Subst subst, Log log, String timelimit) {
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
        appendIneqs("(" + m + ">0)", cas, tool);
        appendIneqs(triangleInequality(a, "b", "c", cas), cas, tool);
        appendIneqs(triangleInequality("b", "c", a, cas), cas, tool);
        appendIneqs(triangleInequality("c", a, "b", cas), cas, tool);
        appendIneqs(eq(lhs, m + "*(" + rhs + ")", cas), cas, tool);
        if (log == Log.VERBOSE) {
            appendResponse("LOG: ineqs=" + ineqs);
        }

        if (cas == Cas.MAPLE) {
            String initcode = "";
            String commandcode = "";
            String vars = "[";
            if (subst != Subst.AUTO) {
                vars += "a,";
            }
            vars += "b,c]";
            if (tool == Tool.REGULAR_CHAINS) {
                initcode = "with(RegularChains):with(SemiAlgebraicSetTools):";
                commandcode = "QuantifierElimination(&E(" + vars + ")," + ineqs + ")";
            }
            if (tool == Tool.SYNRAC) {
                initcode = "with(SyNRAC):";
                commandcode = "qe(Ex(" + vars + "," + ineqs + "))";
            }
            code = initcode + "timelimit(" + timelimit + ",lprint(" + commandcode + "));";
            if (log == Log.VERBOSE) {
                appendResponse("LOG: code=" + code);
            }
            String result = ExternalCAS.execute("echo \"" + code + "\" | maple -q");
            if (log == Log.VERBOSE) {
                appendResponse("LOG: result=" + result);
            }
            // hacky way to convert Maple formula to Mathematica formula
            String rewrite = result.replace("\\", "").replace("\n","").replace("`&or`(", "Or[").
                    replace("`&and`(", "And[").replace("Or(", "Or[").
                    replace("And(", "And[").replace("),", "],").replace("=", "==");
            // convert closing ) to ]
            int i;
            int l = rewrite.length();
            for (i = l - 1 ; rewrite.substring(i,i+1).equals(")"); i--);
            String b = "";
            for (int j = i; j < l -1 ; j++) {
                b += "]";
            }
            rewrite = rewrite.substring(0, i + 1) + b;
            String mathcode = "Print[Quiet[Reduce[" + rewrite + ",m,Reals] // InputForm]]";
            if (log == Log.VERBOSE) {
                appendResponse("LOG: mathcode=" + mathcode);
            }
            String real = ExternalCAS.execute("echo \"" + mathcode + "\" | math | grep 'In\\[1\\]:= '");
            real = real.substring("In[1]:= ".length());
            appendResponse(real);
        }
        return response;
    }
}
