kubeconfig_path = "~/.kube/config"
kube_context    = "docker-desktop"
namespace       = "workhub"
db_password     = "workhub_pass"
stable_image    = "workhub:1.0.0"
canary_image    = "workhub:2.0.0"
total_replicas  = 5
canary_ratio    = 0.20
