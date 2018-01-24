# realgeom, a tool to solve problems in real geometry #
This tool is a web service which forwards real geometry problems
to certain computer algebra systems (CAS) by rewriting them, analyzing the
result, and returning the answer to the user in readable format.

For example, such a problem is comparing (a²+b²+c²) and (a·b+b·c+c·a) in
a triangle where a, b and c are the lengths of the sides. It can be
proven that

a·b+b·c+c·a ≦ a²+b²+c² < 2·(a·b+b·c+c·a)

**realgeom** is capable of solving this kind of problem (among many others) by using recent computer
algebra algorithms from various recent tools. Similar problems have been
posed by the first chapter of the book

* Bottema, Djordjević, Janić, Mitronović, Vasić:
  _Geometric inequalities_, Wolters-Noordhoff Publishing, Groningen, The Nederlands (1969)

## Installation ##
You need to have the following pieces of software installed:

* Linux
* Java 7/8 JDK
* Mathematica

Optional:

* Maple
* recent versions of the [RegularChains](http://www.regularchains.org/) and/or the [SyNRAC](http://www.fujitsu.com/jp/group/labs/en/resources/tech/announced-tools/synrac/) library (in Maple)
* [QEPCAD](https://github.com/PetterS/qepcad)

## Usage ##
The command `./gradlew run` will download all additional tools you may
eventually need. After a self-test and running a benchmark it starts
a web server listening on port 8765. Then you can invoke the software
by issuing the following HTTP request (from your browser):
```
http://your.domain.or.ip.address:8765/triangle?lhs=a*a+b*b+c*c&rhs=a*b+b*c+c*a&log=verbose
```
that should return something like
```
LOG: log=VERBOSE,mode=EXPLORE,cas=MAPLE,tool=REGULAR_CHAINS,subst=AUTO,lhs=a*a+b*b+c*c,rhs=a*b+b*c+c*a,timelimit=300
LOG: subst() => lhs=1+b*b+c*c,rhs=b+b*c+c
LOG: ineqs=(m>0) &and (1+b>c) &and (b+c>1) &and (c+1>b) &and (1+b*b+c*c=m*(b+b*c+c))
LOG: code=with(RegularChains):with(SemiAlgebraicSetTools):timelimit(300,lprint(QuantifierElimination(&E([b,c]),(m>0) &and (1+b>c) &and (b+c>1) &and (c+1>b) &and (1+b*b+c*c=m*(b+b*c+c)))));
LOG: result=`&or`(m-1 = 0,5*m-6 = 0,`&and`(0 < m-1,5*m < 6),`&and`(0 < 5*m-6,m^2+2*m < 4),
`&and`(m^2+2*m-4 = 0,0 < m),`&and`(m < 2,0 < m^2+2*m-4,0 < m))
LOG: mathcode=Print[Quiet[Reduce[Or[m-1 == 0,5*m-6 == 0,And[0 < m-1,5*m < 6],And[0 < 5*m-6,m^2+2*m < 4],And[m^2+2*m-4 == 0,0 < m],And[m < 2,0 < m^2+2*m-4,0 < m]],m,Reals] // InputForm]]
Inequality[1, LessEqual, m, Less, 2]
LOG: time=1.926
```
in your browser. The interpretation of this result is that the equation
(a²+b²+c²)=m·(a·b+b·c+c·a)
has solutions for 1≦m<2.

The benchmark puts HTML output in the *build* folder.
You may want to see a [demo](demo/benchmark.html) of
the generated output ([rendered version](http://htmlpreview.github.io/?https://github.com/kovzol/realgeom/blob/master/demo/benchmark.html)). 

## Documentation ##
It is a work in progress.

## Frequently asked questions ##
* My Java is too old, how to work this around? Download a newer Java JDK version and set the JAVA_HOME to the correct installation folder after unzipping it, before typing `./gradlew run`.
* How to enter the caret (^) symbol? Use %5e instead, e.g. a%5e2 instead of a^2.
* How to start the program on a different port than 8765? Currently you need to change this manually in the file [Start.java](src/main/java/realgeom/Start.java).
* Which parameters are accepted? See the file [HTTPServer.java](src/main/java/realgeom/HTTPServer.java) for the current options.
* Which systems are planned to be supported in the future? Only RedLog for the moment. See [Cas.java](src/main/java/realgeom/Cas.java) and [Tool.java](src/main/java/realgeom/Tool.java) for more details.

## Credits ##
**realgeom** internally uses the Java port of the Giac CAS for some
computations, based on the SWIG C++ to Java translator. Automated loading
of the native JAR package of Giac was borrowed from GeoGebra.

We are thankful to the Research Institute for Symbolic Computations (RISC) at
the Johannes Kepler University (JKU), Linz, for allowing access to their
computer algebra resources.

## Authors ##
* Róbert Vajda <vajdar@math.u-szeged.hu>
* Zoltán Kovács <zoltan@geogebra.org>
