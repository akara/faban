                HTTP Sample README

The http sample provides a very simple example for creating an http driver
which sends http post/get requests specified in the faban config file to a
server and reads back the http response (which is discarded). This driver
uses the faban config file to generate the java driver class at runtime in the
current directory. It also makes use of the com.sun.javac.Main class to
compile the generated java file. Make sure that the
$JAVA_HOME/lib/tools.jar jar is in the classpath along with the current
directory.  

Notes:
- The sbin directory contains utilities to run a benchmark outside
  the Faban harness. This will not be packaged into the deployment
  jar files. Benchmark-specific scripts and binaries should be places
  into the bin directory.
