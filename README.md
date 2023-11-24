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

#### Changed files 

~~~
$ tree demo-k8s-buggy-app/src/
demo-k8s-buggy-app/src/
├── main
(..) Truncated output
│   ├── java
│   │   └── org
│   │       └── okd
│   │           ├── (..) Truncated output
│   │           ├── MemoryLeakResource.java
│   │           └── MemoryLeakService.java
(..) Truncated output
└── test
(..) Truncated output
    └── k8s
        ├── deployment.yaml
        ├── namespace.yml
        ├── service-exposed.yaml
        └── service.yaml
~~~


### Deploy the APP POD (public registry)

~~~
$ oc apply -f demo-k8s-buggy-app/src/test/k8s/namespace.yml
$ oc apply -f demo-k8s-buggy-app/src/test/k8s/deployment.yml
$ oc apply -f demo-k8s-buggy-app/src/test/k8s/service.yaml
~~~

#### Trigger memory leak

~~~
$ curl -v -I http://demo-svc-k8s-buggy-app-demo-k8s.apps.example.com/memory-leak
~~~

##### Observe the system crash 

Open multiple SSH terminals to the RHDE system to observe the system crashing. 

~~~
$ watch -d "ps auxwwwf|egrep -i 'COMMAND|quarkus'"
~~~

in another terminal:
~~~
$ journalctl -f
$ vmstat -t 1 
$ iostat -dtx 1 
~~~