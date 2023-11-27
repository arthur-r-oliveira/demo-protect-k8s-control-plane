# demo-protect-k8s-control-plane

Repo with experiments to crash and protect baseOS and K8s control-plane

- [demo-protect-k8s-control-plane](#demo-protect-k8s-control-plane)
  - [demo-k8s-buggy-app](#demo-k8s-buggy-app)
    - [Quick start with quarkus CLI](#quick-start-with-quarkus-cli)
      - [Changed files](#changed-files)
    - [Build the App](#build-the-app)
      - [Build native](#build-native)
      - [Build image](#build-image)
      - [Push image to some registry](#push-image-to-some-registry)
    - [Deploy the APP POD (public registry)](#deploy-the-app-pod-public-registry)
      - [Trigger memory leak](#trigger-memory-leak)
        - [Observe the system crash](#observe-the-system-crash)
        - [Memory Allocation at crash:](#memory-allocation-at-crash)
      - [Test again, with Memory limit ranges](#test-again-with-memory-limit-ranges)


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

### Build the App 

#### Build native 

~~~
$ ./mvnw package -Dnative
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------------------< org.okd:demo-k8s-buggy-app >---------------------
[INFO] Building demo-k8s-buggy-app 1.0.0-SNAPSHOT
[INFO]   from pom.xml
(..) Truncated output
inished generating 'demo-k8s-buggy-app-1.0.0-SNAPSHOT-runner' in 1m 13s.
[INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildRunner] podman run --env LANG=C --rm --user 4205789:4205789 --userns=keep-id -v /home/arolivei/utils/code/demo-protect-ushift-control-plane/demo-k8s-buggy-app/target/demo-k8s-buggy-app-1.0.0-SNAPSHOT-native-image-source-jar:/project:z --entrypoint /bin/bash quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 -c objcopy --strip-debug demo-k8s-buggy-app-1.0.0-SNAPSHOT-runner
[INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 81122ms
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:29 min
[INFO] Finished at: 2023-11-24T18:49:20Z
[INFO] ------------------------------------------------------------------------
~~~

#### Build image

~~~
$ podman build -f src/main/docker/Dockerfile.native -t quarkus/demo-k8s-buggy-app .
STEP 1/7: FROM registry.access.redhat.com/ubi8/ubi-minimal:8.8
STEP 2/7: WORKDIR /work/
--> Using cache 4282b977e87c8961ce3a1acde451d92e728c08c05ba356650f4cf3c93f93acec
--> 4282b977e87c
STEP 3/7: RUN chown 1001 /work     && chmod "g+rwX" /work     && chown 1001:root /work
--> Using cache d91ee3385e69ba674a9e090294a5517b27dd05edeace0b9a1e7248d6c3125e8a
--> d91ee3385e69
STEP 4/7: COPY --chown=1001:root target/*-runner /work/application
--> 4edf13e57342
STEP 5/7: EXPOSE 8080
--> 52ae8237009e
STEP 6/7: USER 1001
--> a05a71c60623
STEP 7/7: ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
COMMIT quarkus/demo-k8s-buggy-app
--> fa33a35fa808
Successfully tagged localhost/quarkus/demo-k8s-buggy-app:latest
fa33a35fa808e995f16477e17e87caf658ce6e3e6e5beed6cc97dc2f25680f74
~~~

#### Push image to some registry

~~~
$ podman tag localhost/quarkus/demo-k8s-buggy-app quay.io/rhn_support_arolivei/demo-k8s-buggy-app:1.0.0
$ podman push quay.io/rhn_support_arolivei/demo-k8s-buggy-app:1.0.0
Getting image source signatures
Copying blob 329025f1789e skipped: already exists  
Copying blob 01858fc5b538 skipped: already exists  
Copying blob 238df5da329f skipped: already exists  
Copying config 40268de475 done   | 
Writing manifest to image destination
~~~


### Deploy the APP POD (public registry)

**Note: Ensure DNS resolution of MicroShift node IP to demo-k8s-buggy-demo-k8s.apps.example.com host**

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

As per documented with upstream k8s [if you do not specify a memory limit](https://kubernetes.io/docs/tasks/configure-pod-container/assign-memory-resource/#if-you-do-not-specify-a-memory-limit), the Container could use all of the memory available on the Node where it is running which in turn could invoke the OOM Killer. The problem with it is that a some point, due high memory allocation, the scheduled node could become so slower that turns into [`NotReady`](https://kubernetes.io/docs/reference/node/node-status/#node-status-fields) state.

This could cause unavalability of services with Single Node or shared control plane K8s deployments, as routers and another ingress services maybe affected during the peak of memory consumption, and before OOM Killer start working.

Open multiple SSH terminals to the RHDE system to observe the system crashing.

~~~
$ journalctl -f
$ vmstat -t 1 
$ iostat -dtx 1
$ ps auxwwwf|egrep -i 'COMMAND|quarkus'
$ crictl ps
~~~

Initial Memory allocation: 
- 971.64 MiB of VSZ, virtual memory size of the process in KiB (1024-byte units).
- 44.15 MiB of RSS, resident set size, the non-swapped physical memory that a task has used (in kilobytes).

##### Memory Allocation at crash: 
- 
- 2145.71 MiB of RSS

~~~
[root@microshift01 ~]# ps auxwwwf|egrep -i 'COMMAND|quarkus'
USER         PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
root       46397  0.0  0.0   3332  1764 pts/0    S+   18:58   0:00  |                   \_ grep -E --color=auto -i COMMAND|quarkus
1000130+   46263  0.1  1.2 994964 45212 ?        Ssl  18:58   0:00  \_ ./application -Dquarkus.http.host=0.0.0.0

[root@microshift01 ~]# ps auxwwwf|egrep -i 'COMMAND|quarkus'
USER         PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
root       47806  0.6  0.0   3332   128 pts/0    S+   19:07   0:00  |                   \_ grep -E --color=auto -i COMMAND|quarkus
1000130+   46263  0.5 58.8 3173844 2197216 ?     Ssl  18:58   0:03  \_ ./application -Dquarkus.http.host=0.0.0.0


$ watch -d "ps auxwwwf|egrep -i 'COMMAND|quarkus'"
~~~

~~~
$ oc logs demo-k8s-buggy-645c8db985-j9mzn -f
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2023-11-24 18:52:45,301 INFO  [io.quarkus] (main) demo-k8s-buggy-app 1.0.0-SNAPSHOT native (powered by Quarkus 3.5.3) started in 0.023s. Listening on: http://0.0.0.0:8080
2023-11-24 18:52:45,301 INFO  [io.quarkus] (main) Profile prod activated. 
2023-11-24 18:52:45,301 INFO  [io.quarkus] (main) Installed features: [cdi, resteasy-reactive, smallrye-context-propagation, vertx]
(..) Truncated output
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:04.265
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:04.367
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:04.468
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:04.569
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:04.67
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:04.771
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:04.872
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:04.973
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:05.074
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:05.175
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:05.276
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:05.377
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:05.478
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:05.579
Allocationg 1MB from memory at Timestamp: 2023-11-24 18:56:05.68
(..) Truncated output
~~~

After a few minutes, etcd and ovn start complaining and everything is really slow... 

~~~
$ journalctl -f
(..) Truncated output
Nov 24 19:06:42 microshift01.ocp.corp microshift[2586]: {"level":"warn","ts":"2023-11-24T19:06:42.88814Z","caller":"etcdserver/util.go:170","msg":"apply request took too long","took":"112.966862ms","expected-duration":"100ms","prefix":"read-only range ","request":"key:\"/kubernetes.io/apiregistration.k8s.io/apiservices/\" range_end:\"/kubernetes.io/apiregistration.k8s.io/apiservices0\" count_only:true ","response":"range_response_count:0 size:8"}
Nov 24 19:06:43 microshift01.ocp.corp microshift[2586]: {"level":"info","ts":"2023-11-24T19:06:42.93276Z","caller":"traceutil/trace.go:171","msg":"trace[887997540] range","detail":"{range_begin:/kubernetes.io/apiregistration.k8s.io/apiservices/; range_end:/kubernetes.io/apiregistration.k8s.io/apiservices0; response_count:0; response_revision:1385000; }","duration":"180.29697ms","start":"2023-11-24T19:06:42.745156Z","end":"2023-11-24T19:06:42.925453Z","steps":["trace[887997540] 'agreement among raft nodes before linearized reading'  (duration: 100.751246ms)"],"step_count":1}
Nov 24 19:06:44 microshift01.ocp.corp microshift[2586]: {"level":"warn","ts":"2023-11-24T19:06:44.851887Z","caller":"etcdserver/util.go:170","msg":"apply request took too long","took":"888.707043ms","expected-duration":"100ms","prefix":"","request":"header:<ID:16984635510226309758 > lease_revoke:<id:6bb58c0212865251>","response":"size:30"}
(..) Truncated output
Nov 24 19:06:48 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00001|timeval|WARN|Unreasonably long 2237ms poll interval (6ms user, 62ms system)
Nov 24 19:06:49 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00002|timeval|WARN|faults: 162 minor, 1304 major
Nov 24 19:06:49 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00003|timeval|WARN|disk: 260360 reads, 0 writes
Nov 24 19:06:50 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00004|timeval|WARN|context switches: 1297 voluntary, 98 involuntary
Nov 24 19:06:52 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00005|coverage|INFO|Event coverage, avg rate over last: 5 seconds, last minute, last hour,  hash=9dc1d32e:
Nov 24 19:06:52 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00006|coverage|INFO|util_xalloc                0.0/sec     0.000/sec        0.0000/sec   total: 6136
Nov 24 19:06:53 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00007|coverage|INFO|stream_open                0.0/sec     0.000/sec        0.0000/sec   total: 1
Nov 24 19:06:53 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00008|coverage|INFO|seq_change                 0.0/sec     0.000/sec        0.0000/sec   total: 3
Nov 24 19:06:53 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00009|coverage|INFO|poll_create_node           0.0/sec     0.000/sec        0.0000/sec   total: 8
Nov 24 19:06:53 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00010|coverage|INFO|hmap_expand                0.0/sec     0.000/sec        0.0000/sec   total: 430
Nov 24 19:06:53 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00011|coverage|INFO|hmap_pathological          0.0/sec     0.000/sec        0.0000/sec   total: 5
Nov 24 19:06:53 microshift01.ocp.corp ovs-vsctl[47800]: ovs|00012|coverage|INFO|112 events never hit
Nov 24 19:07:10 microshift01.ocp.corp microshift[2586]: {"level":"warn","ts":"2023-11-24T19:07:05.711193Z","caller":"etcdserver/server.go:1168","msg":"failed to revoke lease","lease-id":"6bb58c021286526e","error":"etcdserver: request timed out"}
(..) Truncated output
Nov 24 19:14:13 microshift01.ocp.corp microshift[2586]: {"level":"warn","ts":"2023-11-24T19:13:47.904491Z","caller":"embed/config_logging.go:169","msg":"rejected connection","remote-addr":"127.0.0.1:57186","server-name":"localhost","error":"EOF"}

time crictl ps
CONTAINER           IMAGE                                                                                                                    CREATED             STATE               NAME                    ATTEMPT             POD ID              POD
728007d80dc8a       69a6382cfdb53d447a58cfdb4baf002e86e52ada25f65eb6a9947478d370b900                                                         2 hours ago         Running             topolvm-node            6                   dff46ff9d3fb9       topolvm-node-tbkxx
0e37c1e1040c1       4c8707300593a5af55e126faca55870194e0be7bff0d1b9828b6569e828611f5                                                         2 hours ago         Running             service-ca-controller   1                   db9305567c014       service-ca-568ccf6fcb-jglbd
c14532b5e56f5       5b95dd17115a1cfd1ea157ebbb0c626d058ba3ce1c8130920220157dd31f88d4                                                         2 hours ago         Running             kube-rbac-proxy         0                   4d84a957dfc0c       dns-default-rps24
79a4bf6f520db       441bb0228a34a943c605c24fa23195ee4bed8caa4493d04deee9e34e6ca6d06e                                                         2 hours ago         Running             dns                     0                   4d84a957dfc0c       dns-default-rps24
5ab4c2954b6f7       7f38eb9bdf079a73c17b577c440922460b8a4d0a80928afa382fa26d68365153                                                         2 hours ago         Running             dns-node-resolver       0                   ba08b88d33218       node-resolver-rtlpr
08e0eb052d7c6       quay.io/openshift-release-dev/ocp-v4.0-art-dev@sha256:6228bf99e3b83a5442d40e4e888fa69ea6ed6e99d1fd59a6b9fd8e12243dfb4f   3 hours ago         Running             csi-snapshotter         0                   9d2c0b7776c0f       topolvm-controller-5d6b48f55f-qmdw8
b13c15baa4dfe       quay.io/openshift-release-dev/ocp-v4.0-art-dev@sha256:907629a8a52b152986c59424f49d332bef956282471d5f5704b0341792e7d0a5   3 hours ago         Running             router                  0                   1de9e6824ca28       router-default-77cfbf7866-tw6fz
5aa56d62c3d0a       f8c25e879caffb7055d833c24da9f798b08d6cfaf89b929baeab6ece071d2cdb                                                         3 hours ago         Running             liveness-probe          0                   9d2c0b7776c0f       topolvm-controller-5d6b48f55f-qmdw8
2e2978525c1ae       ca346d82f1f36c10d9fcd0da5ab2d51c1cf061e52713bb8c52df50c6016065a9                                                         3 hours ago         Running             csi-resizer             0                   9d2c0b7776c0f       topolvm-controller-5d6b48f55f-qmdw8
447742b58ea62       quay.io/openshift-release-dev/ocp-v4.0-art-dev@sha256:a47bcd1ba4587d1a032cf8f4aed1441cc427d9c935c34634c43a12e4e5aa1e8a   3 hours ago         Running             snapshot-controller     0                   82ebff129c258       csi-snapshot-controller-7cfff76db4-fqw8w
4183ed2a44f4c       f8c25e879caffb7055d833c24da9f798b08d6cfaf89b929baeab6ece071d2cdb                                                         3 hours ago         Running             liveness-probe          2                   dff46ff9d3fb9       topolvm-node-tbkxx
3221749f8f716       856105b75321beb922fbbe8aa3bac30ab815b490767fea0ca102986a6c8a68e2                                                         3 hours ago         Running             csi-registrar           2                   dff46ff9d3fb9       topolvm-node-tbkxx
2fac192ed430a       quay.io/openshift-release-dev/ocp-v4.0-art-dev@sha256:87e7f594576488fd00bd9eacbf5a4cc12a30954c0a7bb43d4b3259cb40476ef5   3 hours ago         Running             webhook                 0                   a174707d1018e       csi-snapshot-webhook-79fbd89b4-sdd5v
ea72ab9d6c285       69a6382cfdb53d447a58cfdb4baf002e86e52ada25f65eb6a9947478d370b900                                                         3 hours ago         Running             lvmd                    2                   dff46ff9d3fb9       topolvm-node-tbkxx
a5ea2a308cd3f       24ced9d27a33dc6ed08c03e2e99696796ad0ab1eace88ff8b7f540e54f525e9f                                                         3 hours ago         Running             ovnkube-master          0                   60e2f9cf3c3f7       ovnkube-master-fd76p
d1149f1e8b287       24ced9d27a33dc6ed08c03e2e99696796ad0ab1eace88ff8b7f540e54f525e9f                                                         3 hours ago         Running             sbdb                    0                   60e2f9cf3c3f7       ovnkube-master-fd76p
2e714664e860c       24ced9d27a33dc6ed08c03e2e99696796ad0ab1eace88ff8b7f540e54f525e9f                                                         3 hours ago         Running             nbdb                    0                   60e2f9cf3c3f7       ovnkube-master-fd76p
e013513ebeabd       24ced9d27a33dc6ed08c03e2e99696796ad0ab1eace88ff8b7f540e54f525e9f                                                         3 hours ago         Running             northd                  0                   60e2f9cf3c3f7       ovnkube-master-fd76p
06328630b53a5       quay.io/openshift-release-dev/ocp-v4.0-art-dev@sha256:78bb06bdf4a72f308ccb544536a4454d15d64aa18b0ddcaef723721e601585fd   3 hours ago         Running             ovn-controller          0                   22067f5c96bd0       ovnkube-node-h229m

real	0m17.222s
user	0m0.053s
sys	0m5.177s

~~~

Service Down: 

~~~
$ time curl -v -I demo-k8s-buggy-demo-k8s.apps.example.com/hello
* processing: demo-k8s-buggy-demo-k8s.apps.example.com/hello
*   Trying 10.32.111.162:80...
* connect to 10.32.111.162 port 80 failed: No route to host
* Failed to connect to demo-k8s-buggy-demo-k8s.apps.example.com port 80 after 3119 ms: Couldn't connect to server
* Closing connection
curl: (7) Failed to connect to demo-k8s-buggy-demo-k8s.apps.example.com port 80 after 3119 ms: Couldn't connect to server

real	0m3.124s
user	0m0.004s
sys	0m0.000s
~~~


But the control react to this issue at some point, restart the POD OOMKilled and start back the operations.
~~~
$ oc get pods demo-k8s-buggy-745cf66f44-zj6dv -o yaml|grep -A 10 lastState 
    lastState:
      terminated:
        containerID: cri-o://b4c5ed733b2609695ebe06419afee12b38cf057ed49e6f171145eec442fda874
        exitCode: 137
        finishedAt: "2023-11-24T19:15:40Z"
        reason: OOMKilled
        startedAt: "2023-11-24T18:58:18Z"
    name: demo-k8s-buggy
    ready: true
    restartCount: 1
    started: true

$ time curl -v -I demo-k8s-buggy-demo-k8s.apps.example.com/hello
* processing: demo-k8s-buggy-demo-k8s.apps.example.com/hello
*   Trying 10.32.111.162:80...
* Connected to demo-k8s-buggy-demo-k8s.apps.example.com (10.32.111.162) port 80
> HEAD /hello HTTP/1.1
> Host: demo-k8s-buggy-demo-k8s.apps.example.com
> User-Agent: curl/8.2.1
> Accept: */*
> 
< HTTP/1.1 200 OK
HTTP/1.1 200 OK
< content-type: text/plain;charset=UTF-8
content-type: text/plain;charset=UTF-8
< set-cookie: cfe230d36e793c8decf63fec0c82a880=b4cd5e2ff844feffd005108d788bc5f2; path=/; HttpOnly
set-cookie: cfe230d36e793c8decf63fec0c82a880=b4cd5e2ff844feffd005108d788bc5f2; path=/; HttpOnly

< 
* Connection #0 to host demo-k8s-buggy-demo-k8s.apps.example.com left intact

real	0m0.138s
user	0m0.001s
sys	0m0.003s
~~~


#### Test again, with Memory limit ranges

[_The Container is running in a namespace that has a default memory limit, and the Container is automatically assigned the default limit. Cluster administrators can use a LimitRange to specify a default value for the memory limit._](https://kubernetes.io/docs/tasks/configure-pod-container/assign-memory-resource/#if-you-do-not-specify-a-memory-limit).

See more about with k8s upstream doc [_Configure Default Memory Requests and Limits for a Namespace_](https://kubernetes.io/docs/tasks/administer-cluster/manage-resources/memory-default-namespace/#before-you-begin).


~~~
$ cat limits.yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: mem-limit-range
spec:
  limits:
  - default:
      memory: 512Mi
    defaultRequest:
      memory: 256Mi
    type: Container

$ oc apply -f limits.yaml 
limitrange/mem-limit-range created

$ oc get pods -o yaml|grep -A 5 resources
      resources:
        limits:
          memory: 512Mi
        requests:
          memory: 256Mi
      securityContext:

[root@microshift01 ~]# time ps auxwwwf|egrep -i 'COMMAND|quarkus'
USER         PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
root       50810  0.0  0.0   3332  1756 pts/0    S+   19:27   0:00  |                   \_ grep -E --color=auto -i COMMAND|quarkus
1000130+   50608  0.0  1.2 994964 44916 ?        Ssl  19:26   0:00  \_ ./application -Dquarkus.http.host=0.0.0.0

real	0m0.027s
user	0m0.006s
sys	0m0.024s
~~~

Now the control-plane reacted much faster to the buggy POD, not affecting another services in the node:
~~~
$ oc get pods 
NAME                              READY   STATUS      RESTARTS      AGE
demo-k8s-buggy-745cf66f44-m9wss   0/1     OOMKilled   1 (96s ago)   4m47s
~~~

