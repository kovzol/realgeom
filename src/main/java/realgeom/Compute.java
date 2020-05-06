package realgeom;

/*
 * It computes the real geometry problem.
 */

import java.sql.Timestamp;
import java.util.Arrays;

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
        }
        if (cas == Cas.QEPCAD) {
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
        String mathcode = "Reduce[" + formula + ",m,Reals]";
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

            code = "[]\n" + vars + "\n1\n" + exists + "[" + ineqs + "].\n" +
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
            appendResponse(real, Log.INFO);
        }

        if (cas == Cas.MATHEMATICA) {

            String vars = "{";
            if (subst != Subst.AUTO) {
                vars += "a,";
            }
            vars += "b,c}";

            code = "Reduce[Resolve[Exists[" + vars + "," + ineqs + "],Reals],Reals]";
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
                commandCode = "QuantifierElimination(&E(" + vars + ")," + ineqs + ")";
            }
            if (tool == Tool.SYNRAC) {
                initCode = "with(SyNRAC):";
                commandCode = "qe(Ex(" + vars + "," + ineqs + "))";
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
                while (rewrite.substring(i, i + 1).equals(")")) {
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

        String eq = "(" + lhs + ")-m*(" + rhs + ")";
        appendResponse("LOG: ineqs=" + ineqs, Log.VERBOSE);

        // Currently only Mathematica is implemented, TODO: create implementation for all other systems
        if (cas == Cas.MATHEMATICA) {
            String[] varsArray = vars.split(",");
            StringBuilder varsubst = new StringBuilder();
            for (int i = 0; i < Math.min(varsArray.length, 4); ++i) {
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
            appendResponse("LOG: before substitution, polys=" + polys, Log.VERBOSE);
            String polys2 = GiacCAS.execute("subst([" + polys + "],[" + varsubst + "])");
            String ggInit = "caseval(\"init geogebra\")";
            String ilsDef = ggbGiac("isLinearSum\n" +
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
            String jpDef = ggbGiac(
                    "delinearize\n" +
                            " (polys,excludevars)-> \n" +
                            "{ local ii,degs,pos,vars,linvar,p,qvar,pos2,keep,cc,substval; \n" +
                            "  keep:=[];\n" +
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
                            "                      if (is_element(linvar,excludevars)) { \n" +
                            "                           keep:=append(keep,polys[ii]); \n" +
                            "                           print(\"Keeping \" + polys[ii]); \n" +
                            "                         };  \n" +
                            "                      substval:=(op((solve(polys[ii]=0,linvar))[0]))[1];  \n" +
                            "                      print(\"Removing \" + polys[ii] + \", substituting \" + linvar + \" by \" + substval); \n" +
                            "                      polys:=remove(0,expand(subs(polys,[linvar],[substval])));  \n" +
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
                            "                              keep:=append(keep,substval-qvar);  \n" +
                            "                              print(\"Keeping \" + (substval-qvar)); \n" +
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
                            "  return(polys);  \n" +
                            "}");

            String ilDef = ggbGiac("isLinear\n" +
                    " (poly)->{if (((sommet(poly))=\"+\")) { \n" +
                    "              return(isLinearSum(poly));\n" +
                    "            };\n" +
                    "          return(isLinearSum(poly+1234567));\n" +
                    "        }");

            String rmwDef = ggbGiac("removeW12\n" +
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

            polys2 = polys2.substring(1, polys2.length() - 1); // removing { and } in Mathematica (or [ and ] in Giac)

            // Add main equation:
            polys2 += "," + eq;
            appendResponse("LOG: before delinearization, polys=" + polys2, Log.VERBOSE);
            String linCode = "[[" + ggInit + "],[" + ilsDef + "],[" + ilDef + "],[" + jpDef + "],[" + rmwDef +
                    "],";
            if (lhs.equals("w1") && rhs.equals("w2")) {
                linCode += "removeW12(delinearize([" + polys2 + "],[" + posvariables + ",w1,w2]),m,w1,w2)][5]";
            } else {
                linCode += "delinearize([" + polys2 + "],[" + posvariables + "," + lhs + "," + rhs + "])][5]";
            }
            appendResponse("LOG: delinearization code=" + linCode, Log.VERBOSE);
            polys2 = GiacCAS.execute(linCode);
            if (polys2.equals("0")) {
                appendResponse("ERROR: Giac returned 0", Log.VERBOSE);
                appendResponse("GIAC ERROR", Log.INFO);
                return response;
            }
            appendResponse("LOG: after delinearization, polys=" + polys2, Log.VERBOSE);
            // String polys2 = ExternalCAS.executeMathematica(polysSubst, timelimit); // Mathematica call
            appendResponse("LOG: before removing unnecessary variables, vars=" + vars, Log.VERBOSE);
            polys2 = polys2.substring(1, polys2.length() - 1); // removing { and } in Mathematica (or [ and ] in Giac)
            String minimVarsCode = "lvar([" + polys2 + "])"; // remove unnecessary variables
            vars = GiacCAS.execute(minimVarsCode);
            appendResponse("LOG: after removing unnecessary variables, vars=" + vars, Log.VERBOSE);
            vars = vars.substring(1, vars.length() - 1); // removing { and } in Mathematica (or [ and ] in Giac)
            // Remove m from vars:
            vars = vars.replace(",m", "").replace("m,", "");
            appendResponse("LOG: after removing m, vars=" + vars, Log.VERBOSE);
            varsArray = vars.split(",");

            String[] posvariablesArray = posvariables.split(",");
            for (String item : posvariablesArray) {
                if (Arrays.asList(varsArray).contains(item)) appendIneqs(item + ">0", cas, tool);
            }

            String[] polys2Array = polys2.split(",");
            for (String s : polys2Array) appendIneqs(s + "==0", cas, tool);
            // appendResponse("LOG: polys2=" + polys2, Log.VERBOSE);

            code = "Reduce[Resolve[Exists[{" + vars + "}," + ineqs + "],Reals],Reals]";
            appendResponse("LOG: code=" + code, Log.VERBOSE);
            String result = ExternalCAS.executeMathematica(code, timelimit);
            appendResponse(result, Log.INFO);
        }

        return response;
    }

}
