# realgeom, a tool to solve problems in real geometry #
This tool can solve some real geometry problems
by using several computer algebra systems (CAS)
by formalizing the problems, analyzing the
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

The program offers two ways to solve such problems:

* by working as a web service or
* reading the list of given problems from a CSV file.

An example database of problems is also available based on
the book above. 

## Installation ##
You need to have the following pieces of software installed:

* Linux (most distributions should work, for example, Debian 8) or macOS (Catalina 10.15 should work) or Windows 10
* [Java SE 7/8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

Optional:

* [Tarski](https://github.com/chriswestbrown/tarski) (recommended for educational uses on Linux)
* [Mathematica](https://www.wolfram.com/mathematica/) (recommended for heavy computations)
* [QEPCAD](https://www.usna.edu/CS/qepcadweb/B/QEPCAD.html) (recommended for educational uses on macOS and Windows)
* [Maple](https://www.maplesoft.com/) with the following libraries:
  * a recent version of [RegularChains](http://www.regularchains.org/) and/or
  * [SyNRAC](http://www.fujitsu.com/jp/group/labs/en/resources/tech/announced-tools/synrac/)
* [Reduce](http://www.reduce-algebra.com/) with
  * [RedLog](http://www.redlog.eu/)

## Usage ##
The command `./gradlew installDist` will download and compile
all additional tools you may
eventually need, or informs you about the further steps.
Finally the following command will start the program:

    $ ./gradlew run

This should perform a self-test that results in something like this:
```
Loading Giac Java interface...
Time limit is set to 3600 seconds
Testing Giac connection...
Testing shell connection...
Testing Mathematica connection via shell...
Testing Maple connection via shell...
Testing Maple/RegularChains...
Testing Maple/SyNRAC...
Testing QEPCAD connection via shell...
Testing Reduce connection via shell...
Testing RedLog connection via shell...
All required tests are passed
Supported backends: mathematica,maple/regularchains,maple/synrac,qepcad,redlog
```
By entering

    $ ./gradlew run --args="-h"
    
you will get some help on the command line options:
```
Loading Giac Java interface...
Unrecognized option: -h
usage: realgeom
 -b,--benchmark         run benchmark
 -c,--backends <arg>    backends
 -d,--dry-run           do not run heavy computations 
 -i,--input <arg>       benchmark input file path
 -L,--qepcadL <arg>     space for prime list (QEPCAD +L)
 -l,--logfile <arg>     filename for logging
 -N,--qepcadN <arg>     garbage collected space in cells (QEPCAD +N) 
 -o,--output <arg>      benchmark output file
 -p,--port <arg>        HTTP server port number
 -s,--server            run HTTP server
 -t,--timelimit <arg>   time limit
```
By default all supported backends will be used unless
you explicitly give a comma separated list of the
backends. Note that all arguments should be passed in the way given above.

## Running the benchmark ##
Use the option -b to run the benchmark.
 
The default input file is [src/test/resources/benchmark.csv](src/test/resources/benchmark.csv),
while the default output is the file *build/benchmark.html*.
You may want to see a [demo](demo/benchmark.html) of
the generated output ([rendered version](http://htmlpreview.github.io/?https://github.com/kovzol/realgeom/blob/master/demo/benchmark.html)).
Here the time limit is 1 hour which is the default. It can be changed by
setting the maximal amount of seconds with the -t option.

## Running realgeom as a web service ##

Use the option -s to start the program as a web service.

By default a web server will be listening on port 8765, this
can be changed by using the -p option. (Remember that you may
not use port numbers less than 1024 on your machine unless
you have root access. The given port may also be blocked
by various firewalls.)

Then you can invoke a concrete computation
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

(You can use both the -b and -s options. In this case first
the benchmark will be performed, and only after it will
be available the connection to the web service.)

## Use case

**realgeom** is an external service used by recent versions of [GeoGebra Discovery](https://github.com/kovzol/geogebra-discovery).

## Frequently asked questions ##
* My Java is too old, how to work this around? Download a newer Java JDK version and set the JAVA_HOME to the correct installation folder after unzipping it, before typing `./gradlew run`.
* How to enter the caret (^) symbol in the web service? Use %5e instead, e.g. a%5e2 instead of a^2.
* Which parameters are accepted by the web service? See the file [HTTPServer.java](src/main/java/realgeom/HTTPServer.java) for the current options.

## Credits ##
**realgeom** internally uses the Java port of the [Giac](https://www-fourier.ujf-grenoble.fr/~parisse/giac.html) CAS for some
computations, based on the [SWIG](http://www.swig.org/) C++ to Java translator. Automated loading
of the native JAR package of Giac was borrowed from [GeoGebra](http://www.geogebra.org/).

We are thankful to the [Research Institute for Symbolic Computations (RISC)](http://www.risc.jku.at/) at
the [Johannes Kepler University (JKU)](http://www.jku.at/content), Linz, Austria, for allowing access to their
computer algebra resources.

[Christopher W. Brown](https://github.com/chriswestbrown) kindly helped in speeding up operation of the backends QEPCAD and Tarski.
We acknowledge [Daniel Carvalho](https://community.wolfram.com/web/danielscarvalho)'s help in speeding up
Mathematica's computations.

## Note on Mathematica runtime ##
For technical convenience this source tree contains Mathematica's `JLink.jar` and the shared library
`libJLinkNativeLibrary.so`.
These are required to compile and start **realgeom** properly. To be able to use Mathematica, however,
you need a license and a full installation.

## Related projects ##
[GeoGebra Discovery](https://github.com/kovzol/geogebra-discovery) uses **realgeom**
via QEPCAD to outsource real geometry computations.

## Authors ##
* Róbert Vajda <vajdar@math.u-szeged.hu>
* Zoltán Kovács <zoltan@geogebra.org>
