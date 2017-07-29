# realgeom, a tool to solve problems in real geometry #

This tool intends to be a web service which forwards real geometry problems
to certain computer algebra systems (CAS) by rewriting them, analyzing the
result, and return the answer to the user in a readable format.

This project is a work-in-progress. We will continuously add new items
when there are visible steps forward. There is nothing very useful
available yet, so please come back later.

## Credits ##

**realgeom** internally uses the Java port of the Giac CAS for some
computations, by using the SWIG C++ to Java translator. Automated loading
of the native JAR package of Giac was borrowed from GeoGebra.

## Authors ##

* Róbert Vajda <vajdar@math.u-szeged.hu>
* Zoltán Kovács <zoltan@geogebra.org>
