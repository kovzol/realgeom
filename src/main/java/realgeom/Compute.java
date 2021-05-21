package realgeom;

/*
 * It computes the real geometry problem.
 */

import static realgeom.Start.logfile;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.TreeSet;

public class Compute {

    private static String formulas;
    private static String response;
    private static Log maxLogLevel;

    private static String triangleInequality(String a, String b, String c, Cas cas) {
        return a + "+" + b + ">" + c;
    }

    private static void appendIneqs(String ineq, Cas cas, Tool tool) {
        if (ineq.equals("")) {
            return;
        }

        // Rewrites first:

        if (cas == Cas.QEPCAD || cas == Cas.TARSKI) {
            ineq = ineq.replaceAll("\\*", " ").replace("and", "/\\").replace("or", "\\/");
        }

        if (cas == Cas.REDLOG) {
            if (!"".equals(formulas)) {
                formulas += " and ";
            }
            formulas += ineq;
        }
        if (cas == Cas.QEPCAD || cas == Cas.TARSKI) {
            if (!"".equals(formulas)) {
                formulas += " /\\ ";
            }
            formulas += ineq;
        }
        if (cas == Cas.MAPLE) {
            if (tool == Tool.REGULAR_CHAINS) {
                if (!"".equals(formulas)) {
                    formulas += " &and ";
                }
                formulas += "(" + ineq + ")";
            }
            if (tool == Tool.SYNRAC) {
                if (!"".equals(formulas)) {
                    formulas = formulas.substring(0, formulas.length() - 1) + "," + ineq + ")";
                } else {
                    formulas = "And(" + ineq + ")";
                }
            }
        }
        if (cas == Cas.MATHEMATICA) {
            ineq = ineq.replace("and", "\\[And]").replace("or", "\\[Or]");
            if (!"".equals(formulas)) {
                formulas += " \\[And] ";
            }
            formulas += "(" + ineq + ")";
        }
    }

    private static String eq(String lhs, String rhs, Cas cas) {
        if (cas == Cas.MAPLE || cas == Cas.QEPCAD || cas == Cas.REDLOG || cas == Cas.TARSKI) {
            return lhs + "=" + rhs;
        }
        // if (cas == Cas.MATHEMATICA)
        return "" + lhs + " == " + rhs + "";
    }

    private static String product(String a, String b, Cas cas) {
        if (cas == Cas.QEPCAD || cas == Cas.TARSKI) {
            return "(" + a + ") (" + b + ")";
        }
        return "(" + a + ")*(" + b + ")";
    }

    private static void appendResponse(String message, Log logLevel) {
        String data = new Timestamp(System.currentTimeMillis()) + " " + message;
        if (!"".equals(logfile)) {
            try {
                FileWriter writer = new FileWriter(logfile, true);
                writer.write(data + "\n");
                writer.close();
            } catch (IOException e) {
                // We acknowledge silently that the logging is not possible for some reason.
            }
        }
        if (maxLogLevel == Log.VERBOSE || (maxLogLevel == Log.INFO && logLevel != Log.SILENT)) {
            System.out.println(data);
        }
        if (maxLogLevel != Log.VERBOSE && logLevel == Log.VERBOSE) {
            return;
        }
        if (!"".equals(response)) {
            response += "\n";
        }
        response += message;
    }

    private static String rewriteMathematica(String formula, int timeLimit) {
        String mathcode = "Reduce[" + formula + ",m,Reals]";
        appendResponse("LOG: mathcode=" + mathcode, Log.VERBOSE);
        return ExternalCAS.executeMathematica(mathcode, timeLimit);
    }

    private static String rewriteGiac(String formula) {
        // A typical example:
        // m^2 + m - 1 >= 0 /\ m^2 - m - 1 <= 0 /\ [ m^2 - m - 1 = 0 \/ m^2 + m - 1 = 0 ]

        // appendResponse("LOG: formula=" + formula, Log.VERBOSE);
        String[] conjunctions = formula.split(" && ");

        StringBuilder rewritten = new StringBuilder();
        for (String c : conjunctions) {
            if (c.startsWith("[ ") &&
                    c.endsWith(" ]")) {
                c = removeHeadTail(c, 2); // trim [ ... ]
            }
            if (c.startsWith("[") &&
                    c.endsWith("]")) {
                c = removeHeadTail(c, 1); // trim [...] (for Tarski)
            }
            // appendResponse("LOG: c=" + c, Log.VERBOSE);
            String[] disjunctions = c.split(" \\\\/ ");
            StringBuilder product = new StringBuilder();
            for (String d : disjunctions) {
                // appendResponse("LOG: d=" + d, Log.VERBOSE);
                if (d.endsWith(" = 0")) {
                    d = d.substring(0, d.length() - 4); // remove = 0
                }
                if (d.contains("/=")) { // !=
                    // Giac currently cannot handle inequalities.
                    // So we remove this part and hope for the best.
                    d = "";
                } else {
                    d = "(" + d + ")";
                }
                product.append(d).append("*");
                // appendResponse("LOG: product=" + product, Log.VERBOSE);
            }
            product = new StringBuilder(product.substring(0, product.length() - 1)); // remove last *
            // appendResponse("LOG: product=" + product, Log.VERBOSE);
            rewritten.append(product).append(",");
            // appendResponse("LOG: rewritten=" + rewritten, Log.VERBOSE);
        }
        rewritten = new StringBuilder(rewritten.substring(0, rewritten.length() - 1)); // remove last ,

        String mathcode = "solve([" + rewritten + "],m)";
        appendResponse("LOG: mathcode=" + mathcode, Log.VERBOSE);
        String giacOutput = GiacCAS.execute(mathcode);
        // keep only the middle of "list[...]":
        if (giacOutput.contains("list")) {
          giacOutput = giacOutput.replaceAll("list", "");
          giacOutput = removeHeadTail(giacOutput, 1);
          }
        if (giacOutput.contains("rootof")) {
            mathcode = "evalf(" + giacOutput + ")";
            giacOutput = GiacCAS.execute(mathcode) + "..."; // this is just an approximation
        }
        giacOutput = giacOutput.replaceAll("√", "sqrt");
        return giacOutput;
    }

    public static String triangleExplore(String lhs, String rhs, Cas cas, Tool tool, Subst subst, Log log,
                                         int timelimit, String qepcadN, String qepcadL) {
        String m = "m";
        String code;
        formulas = "";
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
        if (cas != Cas.QEPCAD && cas != Cas.TARSKI) {
            appendIneqs(m + ">0", cas, tool);
        }
        appendIneqs(triangleInequality(a, "b", "c", cas), cas, tool);
        appendIneqs(triangleInequality("b", "c", a, cas), cas, tool);
        appendIneqs(triangleInequality("c", a, "b", cas), cas, tool);

        String eq = lhs + " = m * (" + rhs + ")";
        // Convert the equation to a polynomial equation for Maple, QEPCAD and RedLog:
        if (cas == Cas.MAPLE || cas == Cas.QEPCAD || cas == Cas.REDLOG || cas == Cas.TARSKI) {
            eq = GiacCAS.execute("simplify(denom(lhs(" + eq + "))*denom(rhs(" + eq + "))*(" + eq + "))");
        }
        if (cas == Cas.QEPCAD || cas == Cas.TARSKI) {
            eq = eq.replace("*", " ");
        }
        if (cas == Cas.MATHEMATICA) {
            eq = eq.replace("=", "==");
        }
        appendIneqs(eq, cas, tool);
        appendResponse("LOG: ineqs=" + formulas, Log.VERBOSE);

        if (cas == Cas.REDLOG) {
            String exists = "ex({";
            if (subst != Subst.AUTO) {
                exists += "a,";
            }
            exists += "b,c}";

            code = "rlqe(" + exists + ", " + formulas + "));";
            appendResponse("LOG: code=" + code, Log.VERBOSE);
            String result = ExternalCAS.executeRedlog(code, timelimit);
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            // remove trailing $
            int l = result.length();
            if (l > 0) {
                result = result.substring(0, result.length() - 1);
            }
            // hacky way to convert RedLog formula to Mathematica formula FIXME
            String rewrite = result.replace(" and ", " && ").replace("=", "==").
                    replace(">==", ">=").replace("<==", "<=").replace("**", "^").
                    replace(" or ", " || ").replace("<>", "!=");
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

            code = "[]\n" + vars + "\n1\n" + exists + "[" + formulas + "].\n" +
                    "assume[" + m + ">0].\nfinish\n";
            appendResponse("LOG: code=" + code, Log.VERBOSE);
            String result = ExternalCAS.executeQepcad(code, timelimit, qepcadN, qepcadL);
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            // hacky way to convert QEPCAD formula to Mathematica formula FIXME
            String rewrite = result.replace("/\\", "&&").replace("=", "==").
                    replace(">==", ">=").replace("<==", "<=").
                    replace("TRUE", "True");
            // add missing condition to output
            rewrite += " && m>0";
            String real = rewriteMathematica(rewrite, timelimit);
            // String real = rewriteGiac(rewrite);
            appendResponse(real, Log.INFO);
        }

        if (cas == Cas.MATHEMATICA) {

            String vars = "{";
            if (subst != Subst.AUTO) {
                vars += "a,";
            }
            vars += "b,c}";

            code = "Reduce[Resolve[Exists[" + vars + "," + formulas + "],Reals],Reals]";
            appendResponse("LOG: code=" + code, Log.VERBOSE);
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
                commandCode = "QuantifierElimination(&E(" + vars + ")," + formulas + ")";
            }
            if (tool == Tool.SYNRAC) {
                initCode = "with(SyNRAC):";
                commandCode = "qe(Ex(" + vars + "," + formulas + "))";
            }
            code = initCode + "timelimit(" + timelimit + ",lprint(" + commandCode + "));";
            appendResponse("LOG: code=" + code, Log.VERBOSE);

            String result = ExternalCAS.executeMaple(code, timelimit);
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            // hacky way to convert Maple formula to Mathematica formula FIXME
            String rewrite = result.replace("\\", "").replace("\n", "").replace("`&or`(", "Or[").
                    replace("`&and`(", "And[").replace("Or(", "Or[").
                    replace("And(", "And[").replace("),", "],").replace("=", "==").
                    replace(">==", ">=").replace("<==", "<=").replace("<>", "!=");
            // convert closing ) to ]
            int i;
            int l = rewrite.length();
            i = l - 1;
            if (i > 0) {
                while (rewrite.charAt(i) == ')') {
                    i--;
                }
            }
            StringBuilder b = new StringBuilder();
            for (int j = i; j < l - 1; j++) {
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

    static String ggbGiac(String in) {
        String[] ins = in.split("\n");
        StringBuilder out = new StringBuilder();
        for (String s : ins) {
            s = s.replace("->", ":=")
                    .replace("}", " end ")
                    .replace("&&", " and ")
                    .replace("||", " or ")
                    .replaceAll("\\s+"," ");
            if (s.startsWith("while") || s.startsWith("{")) {
                s = s.replace("{", " begin ");
            } else if (s.startsWith("if")) {
                s = s.replace("{", " do ");
            }
            s = s.replace("{", " begin ");
            out.append(s);
        }
        return out.toString();
    }

    static String ggInit = "caseval(\"init geogebra\")";

    static String ilsDef() {
        return ggbGiac("isLinearSum\n" +
                " (poly)-> \n" +
                "{ local degs,vars,ii,ss; \n" +
                "  vars:=lvar(poly);  \n" +
                "  ii:=1;  \n" +
                "  ss:=size(poly);  \n" +
                "  while(ii<ss){ \n" +
                "      degs:=degree(poly[ii],vars);  \n" +
                "      if ((sum(degs))>1) {\n" +
                "          return(false); " +
                "        };\n" +
                "      ii:=ii+1;  \n" +
                "    };\n" +
                "  return(true);  \n" +
                "}");
    }

    static String dlDef(boolean keep) {
        return ggbGiac(
                "delinearize\n" +
                        " (polys,excludevars)-> \n" +
                        "{ local ii,degs,pos,vars,linvar,p,qvar,pos2,keep,cc,substval,substs; \n" +
                        "  keep:=[];\n" +
                        "  substs:=\"\";\n" +
                        "  vars:=lvar(polys);\n" +
                        "  print(\"Input: \"+size(polys)+\" eqs in \"+size(vars)+\" vars\");  \n" +
                        "  cc:=1;\n" +
                        "  while(cc<(size(lvar(polys)))){ \n" +
                        "      ii:=0;  \n" +
                        "      while(ii<(size(polys)-1)){ \n" +
                        "          degs:=degree(polys[ii],vars);  \n" +
                        "          if ((sum(degs)=cc) && (isLinear(polys[ii]))) { \n" +
                        "              pos:=find(1,degs);  \n" +
                        "              if (((size(pos))=cc)) { \n" +
                        "                  p:=0;  \n" +
                        "                  linvar:=vars[pos[p]];  \n" +
                        "                  while(((is_element(linvar,excludevars)) && (cc>1)) && (p<(size(pos)-1))){ \n" +
                        "                      p:=p+1;  \n" +
                        "                      linvar:=vars[pos[p]];  \n" +
                        "                    }; \n" +
                        "                  if ((not(is_element(linvar,excludevars))) || (cc<2)) { \n" +
                        // "                      if (is_element(linvar,excludevars) && (cc>1)) { \n" +
                        "                      if (is_element(linvar,excludevars)) { \n" +

                        (keep ?
                            "                      keep:=append(keep,polys[ii]);  \n" +
                            "                      print(\"Keeping \" + polys[ii]); \n"
                            :
                                "                  print(\"Keeping disabled\"); \n "
                        )
                        +
                        "                         };  \n" +
                        "                      substval:=(op((solve(polys[ii]=0,linvar))[0]))[1];  \n" +
                        "                      print(\"Removing \" + polys[ii] + \", substituting \" + linvar + \" by \" + substval); \n" +
                        "                      substs:=substs + linvar + \"=\" + substval + \",\"; \n" +
                        "                      polys:=remove(0,expand(expand(subs(polys,[linvar],[substval]))));  \n" +
                        "                      print(\"New set: \" + polys); \n" +
                        "                      vars:=lvar(polys);  \n" +
                        "                      ii:=-1;  \n" +
                        "                    };  \n" +
                        "                };  \n" +
                        "            }  \n" +
                        // Quadratic check (FIXME: do that only for rational roots):
                        "          else { \n" +
                        //"              print(\"ii=\"+ii + \" size=\" + size(polys));  \n" +
                        "              if ((sum(degs)=2) && (not(isLinear(polys[ii])))) { \n" +
                        "                  pos2:=find(2,degs);  \n" +
                        "                  if (size(pos2)>0) { \n" +
                        "                      qvar:=vars[pos2[0]];  \n" +
                        "                      if (is_element(qvar,excludevars)) { \n" +
                        "                          print(\"Considering positive roots of \"+(polys[ii]=0)+\" in variable \"+qvar);  \n" +
                        "                          print(solve(polys[ii]=0,qvar));  \n" +
                        "                          substval:=rhs((op(solve(polys[ii]=0,qvar)))[1]);  \n" +
                        "                          print(\"Positive root is \"+substval);  \n" +
                        "                          if (type(substval)==integer || type(substval)==rational) { \n" +
                        "                              polys:=remove(0,expand(subs(polys,[qvar],[substval])));  \n" +
                        "                              print(\"New set: \" + polys); \n" +
                        (keep ?
                                "                      keep:=append(keep,substval-qvar);  \n" +
                                "                      print(\"Keeping \" + (substval-qvar)); \n"
                                :
                                    "                  print(\"Keeping disabled\"); \n "
                        )
                        +
                        "                              substs:=substs + qvar + \"=\" + substval + \",\"; \n" +
                        "                              vars:=lvar(polys);  \n" +
                        "                              ii:=-1;  \n" +
                        "                            };  \n" +
                        "                        };  \n" +
                        "                    };  \n" +
                        //"                  print(ii);  \n" +
                        "                };  \n" +
                        "            };  \n" +
                        // End of quadratic check.
                        "          ii:=ii+1;  \n" +
                        "        }; \n" +
                        "      cc:=cc+1;  \n" +
                        //"      print(cc);  \n" +
                        "    };  \n" +
                        "  polys:=flatten(append(polys,keep));  \n" +
                        "  print(\"Set after delinearization: \" + polys); \n" +
                        "  vars:=lvar(polys);  \n" +
                        "  print(\"Delinearization output: \"+size(polys)+\" eqs in \"+size(vars)+\" vars\");  \n" +
                        "  return([polys,substs]);  \n" +
                        "}");
    }

    static String rdDef() {
        return ggbGiac("removeDivisions\n" +
                " (polys)->{ local ii; \n" +
                "             ii:=0; \n" +
                "             while(ii<(size(polys))) { \n" +
                //"                 polys[ii]:=expand(lcm(denom(coeff(polys[ii])))*(polys[ii])); \n" +
                "                 polys[ii]:=numer(simplify(polys[ii])); \n" +
                "                 ii:=ii+1;\n" +
                "                 };\n" +
                "             return(polys); \n" +
                "        }");
    }

    static String ilDef() {
        return ggbGiac("isLinear\n" +
                " (poly)->{if (((sommet(poly))=\"+\")) { \n" +
                "              return(isLinearSum(poly));\n" +
                "            };\n" +
                "          return(isLinearSum(poly+1234567));\n" + // FIXME, this is a dirty hack
                "        }");
    }

    static String rmwDef() {
        return ggbGiac("removeW12\n" +
                " (polys, m, w1, w2)->{ local ii, vars, w1e, w2e, neweq; \n" +
                "                 ii:=0; \n" +
                "                 neweq:=0; \n" +
                "                 while(ii<(size(polys))) { \n" +
                "                     vars:=lvar(polys[ii]); \n" +
                "                     if (vars intersect [w1] != set[] && vars intersect [m] == set[]) {\n" +
                "                         w1e:=rhs((solve(polys[ii]=0,w1))[0]);\n" +
                "                         print(\"Remove \" + polys[ii]); \n" +
                "                         polys:=suppress(polys,ii); \n" +
                "                         ii:=ii-1; \n" +
                "                         vars:=[]; \n" +
                "                       } \n" +
                "                     if (vars intersect [w2] != set[] && vars intersect [m] == set[]) {\n" +
                "                         w2e:=rhs((solve(polys[ii]=0,w2))[0]);\n" +
                "                         print(\"Remove \" + polys[ii]); \n" +
                "                         polys:=suppress(polys,ii); \n" +
                "                         ii:=ii-1; \n" +
                "                         vars:=[]; \n" +
                "                       } \n" +
                "                     ii:=ii+1;\n" +
                "                   } \n" +
                "                 ii:=0; \n" +
                "                 while(ii<(size(polys))) { \n" +
                "                     vars:=lvar(polys[ii]); \n" +
                "                     if (vars intersect [m] == set[m]) {\n" +
                "                         print(\"Remove \" + polys[ii]); \n" +
                "                         polys:=suppress(polys,ii); \n" +
                "                         neweq:=(w1e)-m*(w2e); \n" +
                "                         ii:=ii-1; \n" +
                "                         vars:=[]; \n" +
                "                       } \n" +
                "                     ii:=ii+1;\n" +
                "                   } \n" +
                "                 if (neweq != 0) {\n" +
                "                     print(\"Add \" + neweq); \n" +
                "                     polys:=flatten(append(polys,neweq)); \n" +
                "                   } \n" +
                "                 return(polys);\n" +
                "               }");
    }

    /**
     * Solve a problem with coordinates. Example call:
     * http://your.domain.or.ip.address:8765/euclideansolver?lhs=a%2bb-c&rhs=g&polys=(b1-c1)%5e2%2b(b2-c2)%5e2-a%5e2,(a1-c1)%5e2%2b(a2-c2)%5e2-b%5e2,(a1-b1)%5e2%2b(a2-b2)%5e2-c%5e2,(g1-c1)%5e2%2b(g2-c2)%5e2-g%5e2,(a1%2bb1)-2g1,(a2%2bb2)-2g2&vars=a1,a2,b1,b2,c1,c2,g1,g2,a,b,c,g&posvariables=a,b,c,g&triangles=a,b,c&log=verbose&mode=explore
     */

    public static String euclideanSolverExplore(String lhs, String rhs, String ineqs, String polys,
                                                String triangles, String vars, String posvariables,
                                                Cas cas, Tool tool, Subst subst, Log log,
                                                int timelimit, String qepcadN, String qepcadL) {
        String m = "m"; // TODO: Use a different dummy variable
        String code;
        formulas = "";
        response = "";
        maxLogLevel = log;

        if (cas != Cas.QEPCAD && cas != Cas.TARSKI) {
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

        String eq = "(" + lhs + ")-m*(" + rhs + ")";
        appendResponse("LOG: ineqs=" + formulas, Log.VERBOSE);

        String[] varsArray = vars.split(",");
        StringBuilder varsubst = new StringBuilder();
        for (int i = 0; i < Math.min(varsArray.length, 4); ++i) {
            int value = 0;
            if (i == 2)
                value = 1;
            // 0,0,1,0 according to (0,0) and (1,0)
            if (i > 0)
                varsubst.append(",");
            varsubst.append(varsArray[i]).append("=").append(value);
        } // in varsubst we have something like a1=0, a2=0, b1=1, b2=0

        String ineqs2 = "";
        if (!ineqs.equals("")) {
            ineqs2 = GiacCAS.execute("subst([" + ineqs + "],[" + varsubst + "])");
            ineqs2 = removeHeadTail(ineqs2, 1);
        }

        String ineqVars = "";
        String[] ineqs2Array = ineqs2.split(",");
        if (!ineqs2.equals("")) {
            for (String ie : ineqs2Array) {
                String[] disjunctionsArray = ie.split(" or ");
                for (String d : disjunctionsArray) {
                    String[] conjunctionsArray = d.split(" and ");
                    for (String c : conjunctionsArray) {
                        String ieRewriteEq = c.replace(">", "=").replace("<", "=")
                                .replace("==", "=").replace("(", ""). replace(")", "");
                        if (ieRewriteEq.contains("or") || ieRewriteEq.contains("and")) {
                            appendResponse("LOG: unimplemented: variables cannot be read off in a Boolean expression, doing nothing and hoping for the best",
                                    Log.VERBOSE);
                        } else {
                            String ieVarsCode = "lvar(lhs(" + ieRewriteEq + "),rhs(" + ieRewriteEq + "))";
                            String ieVars = GiacCAS.execute(ieVarsCode);
                            ieVars = removeHeadTail(ieVars, 1);
                            ineqVars += "," + ieVars;
                        }
                    }
                }
            }
        }

        appendResponse("LOG: before substitution, polys=" + polys + ", ineqs=" + ineqs2, Log.VERBOSE);
        String polys2 = GiacCAS.execute("subst([" + polys + "],[" + varsubst + "])");

        polys2 = removeHeadTail(polys2, 1); // removing { and } in Mathematica (or [ and ] in Giac)

        // Add main equation:
        polys2 += "," + eq;
        appendResponse("LOG: before delinearization, polys=" + polys2, Log.VERBOSE);
        String linCode = "[[" + ggInit + "],[" + ilsDef() + "],[" + ilDef() + "],[" + dlDef(true) + "],[" + rmwDef() +
                "],[" + rdDef() + "],";
        if (lhs.equals("w1") && rhs.equals("w2")) {
            linCode += "removeDivisions(removeW12(delinearize([" + polys2 + "],[" + posvariables + ineqVars + ",w1,w2])[0],m,w1,w2))][6]";
        } else {
            linCode += "removeDivisions(delinearize([" + polys2 + "],[" + posvariables + ineqVars + "," + lhs + "," + rhs + "])[0])][6]";
        }
        appendResponse("LOG: delinearization code=" + linCode, Log.VERBOSE);
        polys2 = GiacCAS.execute(linCode);
        if (polys2.equals("0")) {
            appendResponse("ERROR: Giac returned 0", Log.VERBOSE);
            appendResponse("GIAC ERROR", Log.INFO);
            return response;
        }
        appendResponse("LOG: after delinearization, polys=" + polys2, Log.VERBOSE);
        appendResponse("LOG: before removing unnecessary variables, vars=" + vars, Log.VERBOSE);
        polys2 = removeHeadTail(polys2, 1); // removing { and } in Mathematica (or [ and ] in Giac)
        String minimVarsCode = "lvar([" + polys2 + "])"; // remove unnecessary variables
        vars = GiacCAS.execute(minimVarsCode);
        appendResponse("LOG: after removing unnecessary variables, vars=" + vars, Log.VERBOSE);
        vars = removeHeadTail(vars, 1); // removing { and } in Mathematica (or [ and ] in Giac)
        // Remove m from vars (but keep it only if there is no other variable):
        vars = vars.replace(",m", "").replace("m,", "");
        appendResponse("LOG: after removing m, vars=" + vars, Log.VERBOSE);
        varsArray = vars.split(",");

        String[] posvariablesArray = posvariables.split(",");
        for (String item : posvariablesArray) {
            if (Arrays.asList(varsArray).contains(item)) appendIneqs(item + ">0", cas, tool);
        }

        String[] polys2Array = polys2.split(",");

        if (!ineqVars.equals("")) {
            vars += ineqVars;
        }

        // Remove duplicated vars.
        // Even this can be improved by rechecking all polys/ineqs/ineq:
        TreeSet<String> varsSet = new TreeSet<>();
        varsArray = vars.split(",");
        for (String v : varsArray) {
            varsSet.add(v);
        }
        vars = "";
        for (String v : varsSet) {
            vars += v + ",";
        }
        if (!vars.equals("")) {
            vars = vars.substring(0, vars.length() - 1); // remove last , if exists
        }

        // FINAL COMPUTATION.

        // Currently only Mathematica, QEPCAD and Tarski are implemented, TODO: create implementation for all other systems

        if (cas == Cas.MATHEMATICA) {
            // Remove m completely:
            vars = vars.replace("m", "");
            for (String s : polys2Array) appendIneqs(s + "==0", cas, tool);
            for (String s : ineqs2Array) appendIneqs(s, cas, tool);
            code = "ToRadicals[Reduce[Resolve[Exists[{" + vars + "}," + formulas + "],Reals],Reals],Cubics->False]";
            appendResponse("LOG: code=" + code, Log.VERBOSE);
            String result = ExternalCAS.executeMathematica(code, timelimit);
            appendResponse(result, Log.INFO);
        }

        if (cas == Cas.QEPCAD) {
            for (String s : polys2Array) appendIneqs(s+ "=0", cas, tool);
            for (String s : ineqs2Array) appendIneqs(s, cas, tool);
            StringBuilder exists = new StringBuilder();
            vars = "(m," + vars + ")"; // putting m back

            for (String item : varsArray) {
                exists.append("(E").append(item).append(")");
                }

            String result;
            if (Start.qepcadPipe) {
                String[] codePipe = {"[]", vars, "1", exists + "[" + formulas + "].",
                        "assume[m>0].", "go", "go", "go", "sol T"};
                int[] expectedResponseLines = {1, 1, 1, 5, 2, 2, 2, 2, 7};
                result = ExternalCAS.executeQepcadPipe(codePipe, expectedResponseLines, timelimit);
                String[] results = result.split(Start.nl);
                if (results.length >= 3) {
                    result = results[3];
                    if (result.contains("failure")) {
                        // E.g. "Reason for the failure: Too few cells reclaimed."
                        ExternalCAS.restartQepcadConnection();
                        result = "";
                    } else {
                        String[] cont = {"continue"};
                        int[] contLines = {1};
                        ExternalCAS.executeQepcadPipe(cont, contLines, timelimit);
                    }
                } else {
                    result = "";
                }
            } else {
                code = "[]\n" + vars + "\n1\n" + exists + "[" + formulas + "].\n" +
                        "assume[m>0].\nfinish\n";
                appendResponse("LOG: code=" + code, Log.VERBOSE);
                result = ExternalCAS.executeQepcad(code, timelimit, qepcadN, qepcadL);
            }
            if (result.equals("")) {
               appendResponse("ERROR: empty output", Log.VERBOSE);
               appendResponse("QEPCAD ERROR", Log.INFO);
               return response;
               }
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            if (result.equals("TRUE")) {
                // No usable answer is received (m is arbitrary)
                appendResponse("m>0", Log.INFO);
                return response;
            }
            // hacky way to convert QEPCAD formula to Mathematica formula FIXME
            String rewrite = result.replace("/\\", "&&").
                    replace(">==", ">=").replace("<==", "<=").
                    replace("TRUE", "1=1");
            // add missing condition to output
            rewrite += " && m>0";
            // String real = rewriteMathematica(rewrite, timelimit);
            String real = rewriteGiac(rewrite);
            appendResponse(real, Log.INFO);
        }

        if (cas == Cas.TARSKI) {
            // Remove m completely:
            vars = vars.replace("m", "");

            for (String s : polys2Array) appendIneqs(s + "=0", cas, tool);
            for (String s : ineqs2Array) appendIneqs(s, cas, tool);

            String result;
            int expectedLines;
            if (!vars.equals("")) {
                /*
                 * This code was contributed by Chris W. Brown.
                 * (process F)
                 * this is a script following a strategy that should be pretty
                 * good when F is of the form [ex x1, ..., xk [ G ] ] where G is
                 * a quantifier-free conjunction, and there is only one free
                 * variable and it is named m.  It assumes F is of that form.
                 */
                code = "(def process " +
                        "(lambda (F) " +
                        "(def L (getargs F)) " +
                        // V is the quantified variable set (all but variable m):
                        "(def V (get L 0 0 1)) " +
                        // B is a simplified form of "G" the quantifier free part (or UNSAT):
                        "(def B (bbwb (get L 1))) " +
                        "(if (equal? (get B 0) 'UNSAT) " +
                        // This is the case that bbwb determined G UNSAT without really doing any algebra:
                        "[false] " +
                        // This is the case when bbwb did not determine UNSAT:
                        "((lambda () " +
                        "(def G (qfr (t-ex V (get B 1)))) " +
                        "(if (equal? (t-type G) 6) " +
                        "(qepcad-qe G) " +
                        "(if (equal? (t-type G) 5) " +
                        "(qepcad-qe (bin-reduce t-or (map (lambda (H) (qepcad-qe (exclose H '(m)))) (getargs G)))) " +
                        // Simplifies result in case qfr eliminates all quantified variables:
                        "(qepcad-qe G)" + "" +
                        "))))))) " +
                        "(process [ ex " + vars + " [" + formulas + "]])";
                expectedLines = 2;
            } else {
                // Fallback in case m is not present:
                code = "(qepcad-qe (qfr [" + formulas + "]))";
                expectedLines = 1;
            }

            appendResponse("LOG: code=" + code, Log.VERBOSE);

            if (Start.tarskiPipe) {
                result = ExternalCAS.executeTarskiPipe(code, expectedLines, timelimit);
            } else {
                result = ExternalCAS.executeTarski(code, timelimit, qepcadN, qepcadL);
            }
            if (result.contains("\n")) {
                String [] resultlines = result.split("\n");
                result = resultlines[resultlines.length - 2];
            }
            if (result.equals("")) {
                appendResponse("ERROR: empty output", Log.VERBOSE);
                appendResponse("TARSKI ERROR", Log.INFO);
                return response;
            }
            appendResponse("LOG: result=" + result, Log.VERBOSE);
            if (result.contains("error") || result.contains("failure")) { // TODO: do it properly
                appendResponse("TARSKI ERROR", Log.INFO);
                return response;
            }
            if (result.equals("TRUE")) {
                // No usable answer is received (m is arbitrary)
                appendResponse("m>0", Log.INFO);
                return response;
            }
            // hacky way to convert QEPCAD formula to Mathematica formula FIXME
            String rewrite = result.replace("/\\", "&&").
                    replace(">==", ">=").replace("<==", "<=").
                    replace("TRUE", "1=1");

            // add missing condition to output
            rewrite += " && m>0";

            String real = rewriteGiac(rewrite);
            appendResponse(real, Log.INFO);
        }

        return response;
    }

    /* Prove an inequality with coordinates. */
    // Consider unifying this with euclideanSoverExplore.
    public static String euclideanSolverProve(int maxfixcoords, String ineq, String ineqs, String polys,
                                                String triangles, String vars, String posvariables,
                                                Cas cas, Tool tool, Subst subst, Log log,
                                                int timelimit, String qepcadN, String qepcadL) {

        // Currently only Tarski is implemented, TODO: create implementation for all other systems
        if (cas != Cas.TARSKI) return null;

        String code;
        formulas = "";
        response = "";
        maxLogLevel = log;

        if (!"".equals(triangles)) {
            String[] trianglesArray = triangles.split(";");
            for (String s : trianglesArray) {
                String[] variables = s.split(",");
                appendIneqs(triangleInequality(variables[0], variables[1], variables[2], cas), cas, tool);
                appendIneqs(triangleInequality(variables[1], variables[2], variables[0], cas), cas, tool);
                appendIneqs(triangleInequality(variables[2], variables[0], variables[1], cas), cas, tool);
            }
        }

        String[] varsArray = vars.split(",");
        StringBuilder varsubst = new StringBuilder();
        appendResponse("LOG: maxfixcoords=" + maxfixcoords, Log.VERBOSE);
        for (int i = 0; i < Math.min(varsArray.length, maxfixcoords); ++i) {
            int value = 0;
            if (i == 2)
                value = 1;
            // 0,0,1,0 according to (0,0) and (1,0)
            if (i > 0)
                varsubst.append(",");
            varsubst.append(varsArray[i]).append("=").append(value);
        } // in varsubst we have something like a1=0, a2=0, b1=1, b2=0
        appendResponse("LOG: before substitution, polys=" + polys + ", ineqs=" + ineqs + ", ineq=" + ineq,
                Log.VERBOSE);
        String polys2 = GiacCAS.execute("subst([" + polys + "],[" + varsubst + "])");
        polys2 = removeHeadTail(polys2, 1); // removing [ and ]

        String ineqs2 = "";
        if (!ineqs.equals("")) {
            ineqs2 = GiacCAS.execute("subst([" + ineqs + "],[" + varsubst + "])");
            ineqs2 = removeHeadTail(ineqs2, 1);
        }

        String ineq2 = GiacCAS.execute("subst([" + ineq + "],[" + varsubst + "])");
        ineq2 = removeHeadTail(ineq2, 1);

        appendResponse("LOG: after substitution, polys=" + polys2+ ", ineqs=" + ineqs2 + ", ineq=" + ineq2, Log.VERBOSE);
        boolean keep = false;
        if (cas == Cas.QEPCAD) {
            keep = true;
        }

        String linCode = "[[" + ggInit + "],[" + ilsDef() + "],[" + ilDef() + "],[" + dlDef(keep) + "],[" + rdDef() + "],";

        // Collect variables from inequalities.

        String ineqRewriteEq = ineq2.replace(">", "=").replace("<", "=")
                .replace("==", "=");
        String ineqVarsCode = "lvar(lhs(" + ineqRewriteEq + "),rhs(" + ineqRewriteEq + "))";
        String ineqVars = GiacCAS.execute(ineqVarsCode);
        ineqVars = removeHeadTail(ineqVars, 1);

        String[] ineqs2Array = ineqs2.split(",");
        if (!ineqs2.equals("")) {
            for (String ie : ineqs2Array) {
                String ieRewriteEq = ie.replace(">", "=").replace("<", "=")
                        .replace("==", "=");
                if (ieRewriteEq.contains("or") || ieRewriteEq.contains("and")) {
                    appendResponse("LOG: unimplemented: variables cannot be read off in a Boolean expression, doing nothing and hoping for the best",
                            Log.VERBOSE);
                } else {
                    String ieVarsCode = "lvar(lhs(" + ieRewriteEq + "),rhs(" + ieRewriteEq + "))";
                    String ieVars = GiacCAS.execute(ieVarsCode);
                    ieVars = removeHeadTail(ieVars, 1);
                    ineqVars += "," + ieVars;
                }
            }
        }

        // End collecting variables.

        // linCode += "removeDivisions(delinearize([" + polys2 + "],[" + posvariables + "]))][5]";
        // linCode += "removeDivisions(delinearize([" + polys2 + "],[" + posvariables + ineqVars + "])[0])][5]";
        linCode += "[dl:=delinearize([" + polys2 + "],[" + posvariables + ineqVars + "])],removeDivisions(dl[0]),dl[1]][6..7]";

        // linCode += "removeDivisions([" + polys2 + "],[" + posvariables + "])][5]";
        appendResponse("LOG: delinearization code=" + linCode, Log.VERBOSE);
        String polys_substs = GiacCAS.execute(linCode);
        if (polys_substs.equals("0")) {
            appendResponse("ERROR: Giac returned 0", Log.VERBOSE);
            appendResponse("GIAC ERROR", Log.INFO);
            return response;
        }
        appendResponse("LOG: after delinearization, {polys,substs}=" + polys_substs, Log.VERBOSE);
        appendResponse("LOG: before removing unnecessary poly variables, vars=" + vars, Log.VERBOSE);
        // {{-v11^2+2*v11*v9-v9^2+v15^2-1,v16^2-1,-v5+1,-v6+1,v7,v8-1,-v12+1,v10},"v5=1,v6=1,v7=0,v8=1,v12=1,v10=0,"}
        polys_substs = removeHeadTail(polys_substs, 1); // removing { and } in Mathematica (or [ and ] in Giac)
        // {-v11^2+2*v11*v9-v9^2+v15^2-1,v16^2-1,-v5+1,-v6+1,v7,v8-1,-v12+1,v10},"v5=1,v6=1,v7=0,v8=1,v12=1,v10=0,"
        int split = polys_substs.indexOf("}");
        polys2 = polys_substs.substring(1, split);
        appendResponse("LOG: polys after split=" + polys2, Log.VERBOSE);
        String substs = polys_substs.substring(split + 3, polys_substs.length() - 1);
        if (!substs.equals("")) {
            substs = substs.substring(0, substs.length() - 1); // remove last , if exists
        }
        appendResponse("LOG: substs after split=" + substs, Log.VERBOSE);
        String minimVarsCode = "lvar([" + polys2 + "])"; // remove unnecessary variables
        vars = GiacCAS.execute(minimVarsCode);
        appendResponse("LOG: after removing unnecessary poly variables, vars=" + vars, Log.VERBOSE);
        vars = removeHeadTail(vars, 1); // removing { and } in Mathematica (or [ and ] in Giac)
        varsArray = vars.split(",");

        String[] posvariablesArray = posvariables.split(",");
        for (String item : posvariablesArray) {
            // FIXME: Make a distinction between variables like sqrt2 and the other ones that can be eliminated.
            // if (Arrays.asList(varsArray).contains(item))
            appendIneqs(item + ">0", cas, tool);
            if (!Arrays.asList(varsArray).contains(item)) {
                vars += "," + item;
            }
        }
        vars += "," + ineqVars;

        String[] polys2Array = polys2.split(",");

        if (!substs.equals("")) {
            String[] substsArray = substs.split(",");
            for (String s : substsArray) {
                appendIneqs(s, cas, tool);
                String[] substitution = s.split("=");
                vars += "," + substitution[0];
            }
        }

        for (String s : polys2Array) appendIneqs(s + "=0", cas, tool);
        if (!ineqs2.equals("")) {
            for (String s : ineqs2Array) {
                if (!substs.equals("")) {
                    s = GiacCAS.execute("subst([" + s + "],[" + substs + "])");
                    s = removeHeadTail(s, 1);
                }
                appendIneqs(s, cas, tool);
            }
        }
        if (!substs.equals("")) {
            ineq = GiacCAS.execute("subst([" + ineq + "],[" + substs + "])");
            ineq = removeHeadTail(ineq, 1);
        }
        appendIneqs("~(" + ineq + ")", cas, tool);

        // Remove duplicated vars.
        // Even this can be improved by rechecking all polys/ineqs/ineq:
        TreeSet<String> varsSet = new TreeSet<>();
        varsArray = vars.split(",");
        for (String v : varsArray) {
            varsSet.add(v);
        }
        vars = "";
        for (String v : varsSet) {
            vars += v + ",";
        }
        if (!vars.equals("")) {
            vars = vars.substring(0, vars.length() - 1); // remove last , if exists
        }

        String result;

        code = "(def process " +
                "(lambda (F) " +
                "(def L (getargs F)) " +
                // V is the quantified variable set (all variables):
                "(def V (get L 0 0 1)) " +
                // B is a simplified form of "G" the quantifier free part (or UNSAT):
                "(def B (bbwb (get L 1))) " +
                "(if (equal? (get B 0) 'UNSAT) " +
                // This is the case that bbwb determined G UNSAT without really doing any algebra:
                "[false] " +
                // This is the case when bbwb did not determine UNSAT:
                "((lambda () " +
                "(def G (qfr (t-ex V (get B 1)))) " +
                "(if (equal? (t-type G) 6) " +
                "(qepcad-qe G) " +
                "(if (equal? (t-type G) 5) " +
                "(qepcad-qe (bin-reduce t-or (map (lambda (H) (qepcad-qe (exclose H '(m)))) (getargs G)))) " +
                // Simplifies result in case qfr eliminates all quantified variables:
                "(qepcad-qe G)" + "" +
                "))))))) " +
                "(process [ ex " + vars + " [" + formulas + "]])";

        // code = "(qepcad-qe (qfr [ex " + vars + " [" + formulas + "]]))";
        code = "(qepcad-qe [ex " + vars + " [" + formulas + "]])";
        // FIXME: Discuss which one of the above codes should be used.

        appendResponse("LOG: code=" + code, Log.VERBOSE);

        if (Start.tarskiPipe) {
            result = ExternalCAS.executeTarskiPipe(code, 1, timelimit);
        } else {
            result = ExternalCAS.executeTarski(code, timelimit, qepcadN, qepcadL);
        }
        if (result.contains("\n")) {
            String [] resultlines = result.split("\n");
            result = resultlines[resultlines.length - 2];
        }
        appendResponse("LOG: result=" + result, Log.VERBOSE);
        appendResponse(result, Log.INFO);

        return response;
    }

    static String removeHeadTail(String input, int length) {
        if (input.length() > 2 * length) {
            return input.substring(length, input.length() - length);
        }
        return input;
    }

}
