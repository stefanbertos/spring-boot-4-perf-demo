comiadd debug flag into perf tester controller to switch to debug logs

docker compose - missing acls for kubernetes export
2026-02-14T14:46:04.094Z  WARN 1 --- [perf-tester] [mcat-handler-87] c.e.p.kubernetes.KubernetesService       : Failed to export nodes.json: Operation: [list]  for kind: [Node]  with name: [null]  in namespace: [null]  failed.
2026-02-14T14:46:23.258029415Z 2026-02-14T14:46:23.257Z  WARN 1 --- [perf-tester] [mcat-handler-87] c.e.p.kubernetes.KubernetesService       : Failed to export namespaces.json: Operation: [list]  for kind: [Namespace]  with name: [null]  in namespace: [null]  failed.
2026-02-14T14:46:42.414453069Z 2026-02-14T14:46:42.414Z  WARN 1 --- [perf-tester] [mcat-handler-87] c.e.p.kubernetes.KubernetesService       : Failed to export persistent-volumes.json: Operation: [list]  for kind: [PersistentVolume]  with name: [null]  in namespace: [null]  failed.
2026-02-14T14:47:01.594718645Z 2026-02-14T14:47:01.594Z  WARN 1 --- [perf-tester] [mcat-handler-87] c.e.p.kubernetes.KubernetesService       : Failed to export pods.json: Operation: [list]  for kind: [Pod]  with name: [null]  in namespace: [perf-demo]  failed.
2026-02-14T14:47:20.742470881Z 2026-02-14T14:47:20.741Z  WARN 1 --- [perf-tester] [mcat-handler-87] c.e.p.kubernetes.KubernetesService       : Failed to export deployments.json: Operation: [list]  for kind: [Deployment]  with name: [null]  in namespace: [perf-demo]  failed.
2026-02-14T14:47:39.918542979Z 2026-02-14T14:47:39.918Z  WARN 1 --- [perf-tester] [mcat-handler-87] c.e.p.kubernetes.KubernetesService       : Failed to export statefulsets.json: Operation: [list]  for kind: [StatefulSet]  with name: [null]  in namespace: [perf-demo]  failed.
2026-02-14T14:47:59.063730871Z 2026-02-14T14:47:59.063Z  WARN 1 --- [perf-tester] [mcat-handler-87] c.e.p.kubernetes.KubernetesService       : Failed to export daemonsets.json: Operation: [list]  for kind: [DaemonSet]  with name: [null]  in namespace: [perf-demo]  failed.

docker componse - http://localhost:8888/ doesnt work

missing perf-ui url in docker compose http://localhost:5173/ 

docker compose build fails on 
=== Building Gradle projects ===
> Task :config-server:processComponentTestResources FAILED
> Task :kafka-consumer:processComponentTestResources FAILED
> Task :ibm-mq-consumer:processComponentTestResources FAILED



argocd is not starting
[2/5] Installing ArgoCD (stable) into namespace argocd...
customresourcedefinition.apiextensions.k8s.io/applications.argoproj.io created
customresourcedefinition.apiextensions.k8s.io/appprojects.argoproj.io created
serviceaccount/argocd-application-controller created
serviceaccount/argocd-applicationset-controller created
serviceaccount/argocd-dex-server created
serviceaccount/argocd-notifications-controller created
serviceaccount/argocd-redis created
serviceaccount/argocd-repo-server created
serviceaccount/argocd-server created
role.rbac.authorization.k8s.io/argocd-application-controller created
role.rbac.authorization.k8s.io/argocd-applicationset-controller created
role.rbac.authorization.k8s.io/argocd-dex-server created
role.rbac.authorization.k8s.io/argocd-notifications-controller created
role.rbac.authorization.k8s.io/argocd-redis created
role.rbac.authorization.k8s.io/argocd-server created
clusterrole.rbac.authorization.k8s.io/argocd-application-controller created
clusterrole.rbac.authorization.k8s.io/argocd-applicationset-controller created
clusterrole.rbac.authorization.k8s.io/argocd-server created
rolebinding.rbac.authorization.k8s.io/argocd-application-controller created
rolebinding.rbac.authorization.k8s.io/argocd-applicationset-controller created
rolebinding.rbac.authorization.k8s.io/argocd-dex-server created
rolebinding.rbac.authorization.k8s.io/argocd-notifications-controller created
rolebinding.rbac.authorization.k8s.io/argocd-redis created
rolebinding.rbac.authorization.k8s.io/argocd-server created
clusterrolebinding.rbac.authorization.k8s.io/argocd-application-controller created
clusterrolebinding.rbac.authorization.k8s.io/argocd-applicationset-controller created
clusterrolebinding.rbac.authorization.k8s.io/argocd-server created
configmap/argocd-cm created
configmap/argocd-cmd-params-cm created
configmap/argocd-gpg-keys-cm created
configmap/argocd-notifications-cm created
configmap/argocd-rbac-cm created
configmap/argocd-ssh-known-hosts-cm created
configmap/argocd-tls-certs-cm created
secret/argocd-notifications-secret created
secret/argocd-secret created
service/argocd-applicationset-controller created
service/argocd-dex-server created
service/argocd-metrics created
service/argocd-notifications-controller-metrics created
service/argocd-redis created
service/argocd-repo-server created
service/argocd-server created
service/argocd-server-metrics created
deployment.apps/argocd-applicationset-controller created
deployment.apps/argocd-dex-server created
deployment.apps/argocd-notifications-controller created
deployment.apps/argocd-redis created
deployment.apps/argocd-repo-server created
deployment.apps/argocd-server created
statefulset.apps/argocd-application-controller created
networkpolicy.networking.k8s.io/argocd-application-controller-network-policy created
networkpolicy.networking.k8s.io/argocd-applicationset-controller-network-policy created
networkpolicy.networking.k8s.io/argocd-dex-server-network-policy created
networkpolicy.networking.k8s.io/argocd-notifications-controller-network-policy created
networkpolicy.networking.k8s.io/argocd-redis-network-policy created
networkpolicy.networking.k8s.io/argocd-repo-server-network-policy created
networkpolicy.networking.k8s.io/argocd-server-network-policy created
The CustomResourceDefinition "applicationsets.argoproj.io" is invalid: metadata.annotations: Too long: may not be more than 262144 bytes
ERROR: Failed to install ArgoCD

docker compose - grafana dashboards doest display data, most porably not the same as in helm lets fix first the helm and then export it

implement functionality in perf-tester ui - and remove rest swagger ui
make better ui design, with most moderns principles, make sure to use ui library  or create internal ui component library for button, link, menu, table, dialog based on some material design or so, in future if i want to build a second ui i would use the libraries

put grafana, prometheus dashboards as iframes into perf tester

add possibility to upload test templates and maintanance

add integration into jira to create a automatic ticket

add some tool to guarrail quality in perf-ui similar like checkstyle, pmd, sonar etc so the rules are enforced and add standard react ts rules as default

add external api or enhance the current one so as well some other automation tool can call the perf tester like robo framework etc and perform the test, get results etc

