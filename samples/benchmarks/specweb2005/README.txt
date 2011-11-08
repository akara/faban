                         SPECweb2005 README

The SPECweb2005 sample gives an idea how to integrate with an existing
benchmark by implementing the Benchmark interface directly. This is
relevant in case the benchmark was not written for the Faban driver
framework. For the SPECweb2005 sample to work, you need to obtain the
SPECweb2005 kit from SPEC (http://www.spec.org/), install it according
to SPEC's documentation and point the benchmark to the SPECweb2005
client installation. This is done through the submission form. The
Faban harness is only acting as the benchmark process controller here.

Please check the Faban documentation on Faban installation and setup.
To quickly deploy the SPECweb2005 sample, copy the pre-build benchmark
jar from faban/samples/specweb2005/build/specweb2005.jar to the
faban/benchmarks directory and click on "Schedule Run" in the web
interface.

For developers, deployment through the and deploy task and IDE is
preferred. Please see the Faban Harness Developers Guide for more
information.

NOTE: SPEC and SPECweb2005 are trademarks of th Standard Performance 
Evaluation Corporation.