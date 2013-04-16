Graph Export
============

Small application which can convert the GraphSON exported from Faunus and convert it into GraphML which is readable by applications such as Gephi.

Running this program
--------------------

1. Aquire the source code. There are no pre-built jars available.
2. Build a jar using `mvn package` and navigating to target/ after build completes
3. Move the jar to the location of your GraphSON file.
4. Execute the jar with two parameters: `java jar <NAME OF JAR> <INPUT FILE NAME> <OUTPUT FILE NAME>`
5. Conversion will output an intermediary GraphSON file while is usable by the [Graph Visualizer](https://github.com/WhySearchTwice/Graph-Visualizer) and a GraphML file for use with programs such as Gephi.

The intermediary file name is unfortunately not configurable.
