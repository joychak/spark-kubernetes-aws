# Local Development - Kubernetes cluster running Spark accessing AWS

The purpose of this repo is to build a local (laptop based local node filesystem) 
multi-node (4 node) Kubernetes cluster running spark jobs accessing files from 
AWS-S3. Kubernetes cluster and AWS services are running in local node filesystem 
(macOS laptop).

## Software prerequisites
    1. VirtualBox (6.1)
    2. Vagrant (2.2.14)
    3. Docker (20.10.0)
    4. Java/JDK (1.8)
    5. Scala (2.12.10)
    6. SBT (1.4.5) - Optional (can use Maven)
    7. Spark (spark-3.0.1-bin-hadoop3.2)
    8. kubectl 

## Environment Setup Steps

### Step-1. To build 4-node local kubernetes cluster
This step builds a multi-node kubernetes cluster managed by vagrant and hosted within 
VirtualBox VMs. Run (make sure that docker is running): -

    cd [spark-kubernetes-aws folder]/vagrant-kubeadm
    vagrant up

#### Notes
1. This might take a long time to build the 4 nodes (master, node-1, node-2, node-3) 
   kubernetes cluster. Take a break and come back. You can modify the number of 
   nodes in the kubernetes cluster by modifying the NUM_NODE value in the 
   `Vagrantfile` located at `vagrant-kubeadm` folder.
   
2. Kubernetes master is running at https://192.168.26.10:6443

3. KubeDNS is running at https://192.168.26.10:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

4. The `kube.config` file is created and located at `vagrant-kubeadm` folder. Please 
   export the `KUBECONFIG` env variable and set the value to the `kube.config` path -
   
        export KUBECONFIG=[spark-kubernetes-aws folder]/vagrant-kubeadm/kube.config

5. Test the cluster by running following `vagrant` and `kubectl` commands -
   
        cd [spark-kubernetes-aws folder]/vagrant-kubeadm
        vagrant status
        kubectl cluster-info
        kubectl get nodes
        kubectl get all --all-namespaces 

6. To shut-down (& store the changes) the cluster, please run:

        cd [spark-kubernetes-aws folder]/vagrant-kubeadm
        vagrant pause
   
7. To restart the cluster, please run:

        cd [spark-kubernetes-aws folder]/vagrant-kubeadm
        vagrant up

### Step-2: To set up a docker registry in local machine
This step will set up a docker registry to host docker images accessible by the 
Kubernetes cluster nodes while creating kubernetes container (pod) using docker. Run:

    docker run -d --add-host="localhost:10.0.2.2" -p 5000:5000  --restart=always --name docker-registry registry:2

#### Notes
1. To check the docker container running docker registry, please run:

        docker ps -a

2. The docker registry is running with a local ip = `10.0.2.2`
   
3. To stop the docker container for the registry and clean up resources, please run:

        docker container stop docker-registry && docker container rm -v docker-registry

### Step-3: To change the docker setting within kubernetes nodes to access local docker registry
This step allows the kubernetes nodes hosted within VirtualBox VMs to access local
docker registry with ip = `10.0.2.2`. Execute the following steps for each Kubernetes 
nodes (master, node-1, node-2, node-3) -

1. Log in to master node via ssh. Run:
   
        cd [spark-kubernetes-aws folder]/vagrant-kubeadm
        vagrant ssh master

2. Set the `DOCKER_OPTS=--config-file=/etc/docker/daemon.json` within 
   `/etc/default/docker` file. Create using `sudo vi /etc/default/docker` command 
   if file doesn't exist.
   
3. Create the `daemon.json` file using `sudo vi /etc/docker/daemon.json` command and 
   add following content to this file -
   
        {
            "insecure-registries" : ["10.0.2.2:5000"]
        }

4. Restart docker service. Run:

        sudo service docker restart

5. Run 1-4 for node-1, node-2 & node-3.

### Step-4: To build and publish Spark docker image
This step builds a Spark docker images for Kubernetes and publish it to the local 
docker registry. Run:

    cd [Spark-installation-folder]
    docker build -t spark:latest -f kubernetes/dockerfiles/spark/Dockerfile .
    docker tag spark localhost:5000/spark
    docker push localhost:5000/spark

#### Notes:
1. You can test to pull the Spark docker image from any Kubernetes node. Run:
   
        cd [spark-kubernetes-aws folder]/vagrant-kubeadm
        vagrant ssh master
        sudo docker pull 10.0.2.2:5000/spark
        sudo docker images



## To be continued ... Not done yet ....
