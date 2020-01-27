package realgeom;

/*
 * It computes the real geometry problem.
 */

import java.sql.Timestamp;

public class Compute {

    private static String ineqs;
    private static String response;
    private static Log maxLogLevel;

    private static String triangleInequality(String a, String b, String c, Cas cas) {
        return a + "+" + b + ">" + c;
    }

    private static void appendIneqs(String ineq, Cas cas, Tool tool) {
        if (cas == Cas.REDLOG) {
            if (!"".equals(ineqs)) {
                ineqs += " and ";
            }
            ineqs += ineq;
        }        if (cas == Cas.QEPCAD) {
            if (!"".equals(ineqs)) {
                ineqs += " /\\ ";
            }
            ineqs += ineq;
        }
        if (cas == Cas.MAPLE) {
            if (tool == Tool.REGULAR_CHAINS) {
                if (!"".equals(ineqs)) {
                    ineqs += " &and ";
                }
                ineqs += "(" + ineq + ")";
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
            ineqs += "(" + ineq + ")";
        }
    }

    private static String eq(String lhs, String rhs, Cas cas) {
        if (cas == Cas.MAPLE || cas == Cas.QEPCAD || cas == Cas.REDLOG) {
            return lhs + "=" + rhs;
        }
        // if (cas == Cas.MATHEMATICA)
        return "" + lhs + " == " + rhs + "";
    }

    private static String product(String a, String b, Cas cas) {
        if (cas == Cas.QEPCAD) {
            return "(" + a + ") (" + b + ")";
        }
        return "(" + a + ")*(" + b + ")";
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

    private static String rewriteMathematica(String formula, String timeLimit) {
        String mathcode = "Print[Quiet[Reduce[" + formula + ",m,Reals] // InputForm]]";
        appendResponse("LOG: mathcode=" + mathcode, Log.VERBOSE);
        return ExternalCAS.executeMathematica(mathcode, timeLimit);
    }

    public static String triangleExplore(String lhs, String rhs, Cas cas, Tool tool, Subst subst, Log log,
                                         String timelimit, String qepcadN, String qepcadL) {
        String m = "m";
        String code;
        ineqs = "";
        response = "";
        maxLogLevel = log;

        // substitutions (see p. 9 in Bottema's book)
        lhs = GiacCAS.execute("subst(" + lhs + ",s=(a+b+c)/2)");
        rhs = GiacCAS.execute("subst(" + rhs + ",s=(a+b+c)/2)");

        if (subst == Subst.AUTO) {
            lhs = GiacCAS.execute("subst(" + lhs + ",a=1)");
            rhs = GiacCAS.execute("subst(" + rhs + ",a=1)");
            appendResponse("LOG: subst() => lhs=" + lhs + ",rhs=" + rhs, Log.VERBOSE);
        }
        String a = "a";
        if (subst == Subst.AUTO) {
            a = "1";
        }
        if (cas != Cas.QEPCAD) {
            appendIneqs(m + ">0", cas, tool);
        }
        appendIneqs(triangleInequality(a, "b", "c", cas), cas, tool);
        appendIneqs(triangleInequality("b", "c", a, cas), cas, tool);
        appendIneqs(triangleInequality("c", a, "b", cas), cas, tool);

        String eq = lhs + " = m * (" + rhs + ")";
        // Convert the equation to a polynomial equation for Maple, QEPCAD and RedLog:
        if (cas == Cas.MAPLE || cas == Cas.QEPCAD || cas == Cas.REDLOG) {
            eq = GiacCAS.execute("simplify(denom(lhs(" + eq + "))*denom(rhs(" + eq + "))*(" + eq + "))");
        }
        if (cas == Cas.QEPCAD) {
            eq = eq.replace("*", " ");
        }
        if (cas == Cas.MATHEMATICA) {
            eq = eq.replace("=", "==");
        }
        appendIneqs(eq, cas, tool);
        appendResponse("LOG: ineqs=" + ineqs, Log.VERBOSE);

        if (cas == Cas.REDLOG) {
            String exists = "ex({";
            if (subst != Subst.AUTO) {
                exists += "a,";
            }
            exists += "b,c}";

            code = "rlqe(" + exists + ", " + ineqs + "));";
            appendResponse("LOG: code=" + code,Log.VERBOSE);
            String result = ExternalCAS.executeRedlog(code, timelimit);
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            // remove trailing $
            int l = result.length();
            if (l>0) {
                result = result.substring(0, result.length() - 1);
            }
            // hacky way to convert RedLog formula to Mathematica formula FIXME
            String rewrite = result.replace(" and ", " && ").replace("=", "==").
                    replace(">==", ">=").replace("<==", "<=").replace("**", "^").
                    replace(" or ", " || ").replace("<>","!=");
            // appendResponse("LOG: rewrite=" + rewrite, Log.INFO);
            String real = rewriteMathematica(rewrite, timelimit);
            appendResponse(real, Log.INFO);
        }

        if (cas == Cas.QEPCAD) {
            String exists = "";
            String vars = "(" + m + ",";
            if (subst != Subst.AUTO) {
                vars += "a,";
                exists += "(Ea)";
            }
            exists += "(Eb)(Ec)";
            vars += "b,c)";

            code = "[]\n" + vars +"\n1\n" + exists + "[" + ineqs + "].\n" +
                    "assume[" + m + ">0].\nfinish\n";
            appendResponse("LOG: code=" + code,Log.VERBOSE);
            String result = ExternalCAS.executeQepcad(code, timelimit, qepcadN, qepcadL);
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            // hacky way to convert QEPCAD formula to Mathematica formula FIXME
            String rewrite = result.replace("/\\", "&&").replace("=", "==").
                    replace(">==", ">=").replace("<==", "<=").
                    replace("TRUE", "True");
            // add missing condition to output
            rewrite += " && m>0";
            String real = rewriteMathematica(rewrite, timelimit);
            appendResponse(real, Log.INFO);
        }

        if (cas == Cas.MATHEMATICA) {

            String vars = "{";
            if (subst != Subst.AUTO) {
                vars += "a,";
            }
            vars += "b,c}";

            code = "Print[Quiet[Reduce[Resolve[Exists[" + vars + "," + ineqs + "],Reals],Reals] // InputForm]]";
            appendResponse("LOG: code=" + code,Log.VERBOSE);
            String result = ExternalCAS.executeMathematica(code, timelimit);
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

            String result = ExternalCAS.executeMaple(code, timelimit);
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            // hacky way to convert Maple formula to Mathematica formula FIXME
            String rewrite = result.replace("\\", "").replace("\n","").replace("`&or`(", "Or[").
                    replace("`&and`(", "And[").replace("Or(", "Or[").
                    replace("And(", "And[").replace("),", "],").replace("=", "==").
                    replace(">==", ">=").replace("<==", "<=").replace("<>", "!=");
            // convert closing ) to ]
            int i;
            int l = rewrite.length();
            i = l - 1 ;
            if (i>0) {
                while (rewrite.substring(i, i + 1).equals(")")) {
                    i--;
                }
            }
            StringBuilder b = new StringBuilder();
            for (int j = i; j < l -1 ; j++) {
                b.append("]");
            }
            // This is tested only in Bottema 1.24 (specific fix)
            if (tool == Tool.SYNRAC) {
                rewrite = rewrite.replace(")", "]");
            }
            rewrite = rewrite.substring(0, i + 1) + b;
            String real = rewriteMathematica(rewrite, timelimit);
            appendResponse(real, Log.INFO);
        }
        return response;
    }

    /**
     * Solve a problem with coordinates. Example call:
     * http://your.domain.or.ip.address:8765/euclideansolver?lhs=a%2bb-c&rhs=g&polys=(b1-c1)%5e2%2b(b2-c2)%5e2-a%5e2,(a1-c1)%5e2%2b(a2-c2)%5e2-b%5e2,(a1-b1)%5e2%2b(a2-b2)%5e2-c%5e2,(g1-c1)%5e2%2b(g2-c2)%5e2-g%5e2,(a1%2bb1)-2g1,(a2%2bb2)-2g2&vars=a1,a2,b1,b2,c1,c2,g1,g2,a,b,c,g&posvariables=a,b,c,g&triangles=a,b,c&log=verbose&mode=explore
     */

    public static String euclideanSolverExplore(String lhs, String rhs, String polys,
                                                String triangles, String vars, String posvariables,
                                                Cas cas, Tool tool, Subst subst, Log log,
                                                String timelimit, String qepcadN, String qepcadL) {
        String m = "m"; // TODO: Use a different dummy variable
        String code;
        ineqs = "";
        response = "";
        maxLogLevel = log;

        if (cas != Cas.QEPCAD) {
            appendIneqs(m + ">0", cas, tool);
        }

        if (!"".equals(triangles)) {
            String[] trianglesArray = triangles.split(";");
            for (String s : trianglesArray) {
                String[] variables = s.split(",");
                appendIneqs(triangleInequality(variables[0], variables[1], variables[2], cas), cas, tool);
                appendIneqs(triangleInequality(variables[1], variables[2], variables[0], cas), cas, tool);
                appendIneqs(triangleInequality(variables[2], variables[0], variables[1], cas), cas, tool);
            }
        }

        String eq = lhs + " = m * (" + rhs + ")";
        // Convert the equation to a polynomial equation for Maple, QEPCAD and RedLog:
        if (cas == Cas.MAPLE || cas == Cas.QEPCAD || cas == Cas.REDLOG) {
            eq = GiacCAS.execute("simplify(denom(lhs(" + eq + "))*denom(rhs(" + eq + "))*(" + eq + "))");
        }
        if (cas == Cas.QEPCAD) {
            eq = eq.replace("*", " ");
        }
        if (cas == Cas.MATHEMATICA) {
            eq = eq.replace("=", "==");
        }
        appendIneqs(eq, cas, tool);
        appendResponse("LOG: ineqs=" + ineqs, Log.VERBOSE);

        // Currently only Mathematica is implemented, TODO: create implementation for all other systems
        if (cas == Cas.MATHEMATICA) {
            String[] posvariablesArray = posvariables.split(",");
            for (String item : posvariablesArray) appendIneqs(item + ">0", cas, tool);
            String[] varsArray = vars.split(",");
            StringBuilder varsubst = new StringBuilder();
            for (int i = 0; i < Math.min(varsArray.length,4); ++i) {
                int value = 0;
                if (i == 2)
                    value = 1;
                // 0,0,1,0 according to (0,0) and (1,0)
                if (i > 0)
                    varsubst.append(",");
                // varsubst.append(varsArray[i]).append("->").append(value); // Mathematica syntax
                varsubst.append(varsArray[i]).append("=").append(value);
                } // in varsubst we have something like a1->0, a2->0, b1->1, b2->0
            // String polysSubst = "Print[Quiet[{" + polys + "}/.{" + varsubst + "} // InputForm]]"; // Mathematica syntax
            // appendResponse("LOG: polysSubst=" + polysSubst, Log.VERBOSE);
            appendResponse("LOG: before substitution, polys=" + polys,Log.VERBOSE);
            String polys2 = GiacCAS.execute("subst([" + polys + "],[" + varsubst + "])");
            String ggInit = "caseval(\"init geogebra\")";
            // String jpDef = "jacobiPrepare(polys,excludevars):=begin local ii, degrees, pos, vars, linvar; vars:=lvar(polys); ii:=0; while (ii<size(polys)-1) do degrees:=degree(polys[ii],vars); if (sum(degrees)=1) begin pos:=find(1,degrees); linvar:=vars[pos[0]]; if (!is_element(linvar,excludevars)) begin substval:=op(solve(polys[ii]=0,linvar)[0])[1]; polys:=remove(0,expand(subs(polys,[linvar],[substval]))); print(polys); ii:=-1; end; end; ii:=ii+1; od; return polys; end";
            String jpDef = "jacobiPrepare(polys,excludevars):=begin local ii, degrees, pos, vars, linvar; vars:=lvar(polys); print(\"input: \"+size(polys)+\" eqs in \"+size(vars)+\" vars\"); c:=1; while (c<size(lvar(polys))) do ii:=0; while (ii<size(polys)-1) do degrees:=degree(polys[ii],vars); if ((sum(degrees)=c) and (isLinear(polys[ii]))) begin pos:=find(1,degrees);  if (size(pos)=c) begin linvar:=vars[pos[0]]; if (!is_element(linvar,excludevars)) begin substval:=op(solve(polys[ii]=0,linvar)[0])[1]; polys:=remove(0,expand(subs(polys,[linvar],[substval]))); od; vars:=lvar(polys); ii:=-1; end; end; ii:=ii+1; od; c:=c+1; od; vars:=lvar(polys); print(\"output: \"+size(polys)+\" eqs in \"+size(vars)+\" vars\");  return polys; end";
            String ilsDef = "isLinearSum(poly):=begin local degrees, vars, ii, ss; vars:=lvar(poly); ii:=1; ss:=size(poly); while (ii<ss) do degrees:=degree(poly[ii], vars); if (sum(degrees)>1) begin return false; end; ii:=ii+1; od; return true; end";
            String ilDef = "isLinear(poly):=begin if (sommet(poly)==\"+\") begin return isLinearSum(poly); end; return isLinearSum(poly+1234567); end";
            polys2 = polys2.substring(1,polys2.length() - 1); // removing { and } in Mathematica (or [ and ] in Giac)
            appendResponse("LOG: before delinearization, polys=" + polys2,Log.VERBOSE);
            String linCode = "[[" + ggInit + "],[" + ilsDef + "],[" + ilDef + "],[" + jpDef + "],jacobiPrepare([" + polys2 + "],[" + "])][4]";
            appendResponse("LOG: delinearization code=" + linCode,Log.VERBOSE);
            polys2 = GiacCAS.execute(linCode);
            appendResponse("LOG: after delinearization, polys=" + polys2,Log.VERBOSE);
            // String polys2 = ExternalCAS.executeMathematica(polysSubst, timelimit); // Mathematica call
            polys2 = polys2.substring(1,polys2.length() - 1); // removing { and } in Mathematica (or [ and ] in Giac)

            String[] polys2Array = polys2.split(",");
            for (String s : polys2Array) appendIneqs(s + "==0", cas, tool);
            // appendResponse("LOG: polys2=" + polys2, Log.VERBOSE);

            code = "Print[Quiet[Reduce[Resolve[Exists[{" + vars + "}," + ineqs + "],Reals],Reals] // InputForm]]";
            appendResponse("LOG: code=" + code,Log.VERBOSE);
            String result = ExternalCAS.executeMathematica(code, timelimit);
            appendResponse(result, Log.INFO);
        }

        return response;
    }

}
