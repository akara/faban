                Sample configuration files for fhb

This sample provides a very simple example for creating an http driver
which sends http post/get requests specified in the fhb config file to a
server and reads back the http response (which is discarded). This driver
uses the fhb config file to generate the java driver class at runtime in the
current directory. It also makes use of the com.sun.javac.Main class to
compile the generated java file.
