# demo-protect-ushift-control-plane

Repo with experiments to crash and protect baseOS and MicroShift control-plane

## demo-k8s-buggy-app 

Based on Quarkus RESTEasy, it provides the following endpoint to generate CPU and Memory leaks. 
- `/cpu-leak`: TBD
- `/memory-leak`: Allocate 1MB from memory every 100 milliseconds.

### Quick start with quarkus CLI

~~~
$ quarkus create app org.okd:demo-k8s-buggy-app --extension='resteasy-reactive,kubernetes,jib'
-----------
selected extensions: 
- io.quarkus:quarkus-kubernetes
- io.quarkus:quarkus-container-image-jib
- io.quarkus:quarkus-resteasy-reactive


applying codestarts...
📚 java
🔨 maven
📦 quarkus
📝 config-properties
🔧 dockerfiles
🔧 maven-wrapper
🚀 resteasy-reactive-codestart

-----------
[SUCCESS] ✅  quarkus project has been successfully generated in:
--> /home/arolivei/utils/code/demo-protect-ushift-control-plane/demo-k8s-buggy-app
-----------
Navigate into this directory and get started: quarkus dev
~~~