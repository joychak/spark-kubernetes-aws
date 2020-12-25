# Local Development - Kubernetes cluster running Spark accessing AWS S3

The purpose of this repo is to build a local (laptop based local node filesystem) 
multi-node (4 nodes) Kubernetes cluster running spark jobs accessing files from 
AWS-S3. Kubernetes cluster and AWS services are running in single-host local node 
filesystem (macOS laptop). 

The motivation for this project is to build a local Spark/Kubernetes/AWS-S3 development 
setup to test `Distributed and Cloud Computing` technologies without using any cluster 
and cloud resources. 

## Software prerequisites
    1. VirtualBox (6.1)
    2. Vagrant (2.2.14)
    3. Docker (20.10.0)
    4. Java/JDK (1.8)
    5. Scala (2.12.10)
    6. SBT (1.4.5) - Optional (can use Maven)
    7. Spark (spark-3.0.1-bin-hadoop3.2)
    8. kubectl 
    9. awscli (2.1.13)

## Environment Setup and Testing Steps

### Step-1. Clone this repository to you local machine
Run:
   
      git clone https://github.com/joychak/spark-kubernetes-aws.git
      cd spark-kubernetes-aws 
      #change directory to the root of this repo

### Step-2. To build 4-node local kubernetes cluster
This step builds a multi-node kubernetes cluster managed by vagrant and hosted within 
VirtualBox VMs. Run (make sure that docker is running): -

    cd [spark-kubernetes-aws folder]/vagrant-kubeadm
    vagrant up

    # Please modify the Vagrantfile (accordingly) if your laptop is running within corporate 
    # domain network behind firewall and using proxy server to access internet.

#### Notes
1. This might take a long time to build the 4 nodes (master, node-1, node-2, node-3) 
   kubernetes cluster. Take a break and come back. You can modify the number of 
   nodes in the kubernetes cluster by modifying the NUM_NODE value in the 
   `Vagrantfile` located at `vagrant-kubeadm` folder.
   
2. Kubernetes master is running at https://192.168.26.10:6443. You can discover the 
   apiserver URL by executing -
   
        kubectl cluster-info

3. KubeDNS is running at https://192.168.26.10:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

4. The `kube.config` file is created and located at `vagrant-kubeadm` folder. Please 
   export the `KUBECONFIG` env variable and set the value to the `kube.config` path -
   
        export KUBECONFIG=[spark-kubernetes-aws folder]/vagrant-kubeadm/kube.config

5. Test the cluster by running following `vagrant` and `kubectl` commands -
   
        cd [spark-kubernetes-aws folder]/vagrant-kubeadm
        vagrant status
        kubectl get nodes
        kubectl get all --all-namespaces 

6. To shut-down (& store the changes) the cluster, please run:

        cd [spark-kubernetes-aws folder]/vagrant-kubeadm
        vagrant pause
   
7. To restart the cluster, please run:

        cd [spark-kubernetes-aws folder]/vagrant-kubeadm
        vagrant up

8. Each Kubernetes nodes has 1 CPU and 2 GB memory assigned. You can change the memory by 
   modifying line#98, `vb.memory = "2048"` within `Vagrantfile` located at `vagrant-kubeadm` folder.

### Step-3: To set up a docker registry in local machine
This step will set up a docker registry to host docker images accessible by the 
Kubernetes cluster nodes while creating kubernetes container (pod) using docker. Run:

    docker run -d --add-host="localhost:10.0.2.2" -p 5000:5000  --restart=always --name docker-registry registry:2

#### Notes
1. To check the docker container running docker registry, please run:

        docker ps -a

2. The docker registry is running with a local ip = `10.0.2.2`
   
3. To stop the docker container for the registry and clean up resources, please run:

        docker container stop docker-registry && docker container rm -v docker-registry

### Step-4: To change the docker setting within kubernetes nodes to access local docker registry
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

5. Repeat previous 4 steps (1-4) for node-1, node-2 & node-3.

### Step-5: To build and publish Spark docker image
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

### Step-6: To create kubernetes service account and role for Spark jobs
The default Kubernetes service account do not have the role that allows driver pods 
to create pods and services under the default Kubernetes RBAC policies. A custom service
account is required with right role granted.

1. To create a custom service account, run:
   
        kubectl create serviceaccount spark

2. To grant the `spark` service account a ClusterRole and ClusterRoleBinding, run:

        kubectl create clusterrolebinding spark-role --clusterrole=edit --serviceaccount=default:spark --namespace=default

### Step-7 (optional): To test a Spark job running in Kubernetes cluster
This step is to test (optionally) the local Kubernetes cluster running a sample Spark 
job. You can use the `SparkPi` program within `spark-examples_X.Y.Z.jar` that gets
installed during Spark deployment. Please note: Spark is deployed at `/opt/spark/` path
within the `localhost:5000/spark` docker image (build in step-5) and 
`spark-examples_X.Y.Z.jar` is located at `/opt/spark/examples/jars`. Run:

    spark-submit \
    --master k8s://https://192.168.26.10:6443 \
    --deploy-mode cluster \
    --name spark-pi \
    --class org.apache.spark.examples.SparkPi \
    --conf spark.driver.cores=1 \
    --conf spark.driver.memory=512m \
    --conf spark.executor.cores=1 \
    --conf spark.executor.instances=1 \
    --conf spark.executor.memory=512m \
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
    --conf spark.kubernetes.container.image=10.0.2.2:5000/spark \
    local:///opt/spark/examples/jars/spark-examples_2.12-3.0.1.jar

#### Notes:
1. The above executed `spark-submit` command will create 1 driver pod and 1 executor 
   pod and 1 driver service to run the `SparkPi` job.
   
2. You can find the driver pod name by executing the following command. The driver 
   pod's naming pattern is `pod/spark-pi-*-driver`.
   
        kubectl get pods --all-namespaces

3. The output of `SparkPi` job is written to the driver pod's log, which can be 
   followed by executing the following command. The driver pod's log prints the 
   value of `Pi`.
   
        kubectl logs [driver-pod-name] --follow

4. The Spark job will remove the executor pod(s) at the end of the job but not the 
   driver pod and service. You can remove these resources by executing the following 
   commands -
   
        kubectl delete [driver-pod-name] [driver-service-name]

### Step-8: To deploy local AWS S3 service
This step deploys LocalStack (available at https://github.com/localstack/localstack), 
a local mock deployment of fully functional local AWS cloud stack. This project only 
requires S3 service, but you can enable other AWS services (if required). 

#### Why we need S3?
Launching a Spark job (`spark-submit` command) requires an application jar which shall
be accessible from the kubernetes node (or pod) running the driver program. The spark
job is submitted from local host machine (laptop), but the Kubernetes pod doesn't have 
access to the local host resources. At same time, you can't deploy the application jar 
to the Kubernetes pod running driver program because it is ephemeral. So, S3 storage can 
be used as a repository to deploy application jar which is hosted outside kubernetes 
cluster but accessible by the cluster nodes. You can use other repository solution such 
as Artifactory.

Additionally, the Spark job can read input data from a file and write output to file stored 
in S3 bucket. This is one of the popular cloud storage solution.

#### To Start a docker container running LocalStack mocking AWS-S3

1.  Setup AWS credentials. Execute the following command and set `aws_access_key_id=123`, 
    `aws_secret_access_key=xyz` and `region = us-east-1` -
    
        aws configure 
        # Config & credential file will be created under ~/.aws folder

2.  Create and start a docker container running LocalStack. Run:

        cd [spark-kubernetes-aws folder]/aws-localStack
        docker-compose -f ./docker-compose-localstack.yml up -d

3.  Create a test S3 bucket by executing the following command - 
    
         aws --endpoint-url=http://localhost:4566 s3 mb s3://test-bucket

#### Notes:

1.  Open browser to test the S3 bucket UI using URL = http://localhost:8055/. The UI 
    dashboard should be empty initially because no bucket has been created yet.
    
2. You can refresh the UI after creating the `test-bucket` bucket to list the bucket, 
   and it's content by executing the following command -
   
        aws --endpoint-url=http://localhost:4566 s3 ls s3://test-bucket

3. You can optionally delete the LocalStack docker container. Run:

        docker-compose -f ./docker-compose-localstack.yml down --volumes --remove-orphans

### Step-9: To build a Spark application reading data from AWS S3
This step builds a Spark application that reads a file from stored in S3 bucket and 
calculates the word count. 

1. The Scala project source code for a Spark application that reads a file stored 
   in S3 bucket and performs word count is available at 
   `[spark-kubernetes-aws folder]/spark-example`.
   
2. The Spark project is configured (using `build.sbt`) to use SBT to compile and build 
   the application JAR. However, other build tool such as Maven can be used to compile
   and build application JAR. Please execute following `sbt` commands to build 
   application JAR. PLease note: it builds a FAT JAR that includes all dependencies 
   except Spark dependencies. Spark dependencies are provided by the Spark docker 
   image used to run Spark application within Kubernetes cluster.
   
         cd [spark-kubernetes-aws folder]/spark-example
         sbt assembly
   
         # The output JAR spark-example-assembly-0.1.jar is created at [spark-kubernetes-aws folder]/spark-example/target/scala-2.12/

### Step-10: To deploy the WordCount Spark application
This step deploys the Spark application and dependency jars to S3 in order to be 
accessible from Kubernetes cluster.

1. Deploy the application JAR

         cd [spark-kubernetes-aws folder]/spark-example
         aws --endpoint-url=http://localhost:4566 s3 cp spark-example/target/scala-2.12/spark-example-assembly-0.1.jar s3://test-bucket

2. Deploy the AWS dependency JARs. This application reads data from AWS S3 bucket. 
   Therefore, it needs `aws-java-sdk-bundle` (https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-bundle/1.11.874) 
   and `hadoop-aws` (https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-aws/3.2.0) 
   dependencies because these are not part of Spark distribution. Please download these 
   jars from Maven repository to your local machine. To download and deploy, run:
   
         cd [spark-kubernetes-aws folder]
         wget https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/1.11.874/aws-java-sdk-bundle-1.11.874.jar
         wget https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/3.2.0/hadoop-aws-3.2.0.jar
         aws --endpoint-url=http://localhost:4566 s3 cp aws-java-sdk-bundle-1.11.874.jar s3://test-bucket
         aws --endpoint-url=http://localhost:4566 s3 cp hadoop-aws-3.2.0.jar s3://test-bucket
   
#### Notes:

1. Please note that the application jar and AWS dependency jars are deployed to the 
   `test-bucket` S3 bucket created in step-8. You can list the content of `test-bucket` 
   to check the presence of application jar and AWS dependency jars by executing -
   
         aws --endpoint-url=http://localhost:4566 s3 ls s3://test-bucket

### Step-11: To run the WordCount Spark application
This step runs the Spark application within Kubernetes cluster that reads the input data
from a file stored in AWS S3 bucket.

1. Deploy any text file to the `test-bucket` S3 bucket. The Scala source code is designed
   to read a file at `s3a://test-bucket/Vagrantfile`. You can change it by editing the 
   `[spark-kubernetes-aws folder]/spark-example/src/main/scala/com/datalogs/WordCount.scala` 
   Scala source code file. To copy the Vagrantfile to S3 bucket, please run:

         cd [spark-kubernetes-aws folder]
         aws --endpoint-url=http://localhost:4566 s3 cp vagrant-kubeadm/Vagrantfile s3://test-bucket

2. Execute the following `spark-submit` command to run the WordCount Spark application within 
   Kubernetes cluster -
   
         spark-submit \
         --master k8s://https://192.168.26.10:6443 \
         --deploy-mode cluster \
         --name word-count \
         --class com.datalogs.WordCount \
         --jars=http://192.168.86.231:4566/test-bucket/aws-java-sdk-bundle-1.11.874.jar,http://192.168.86.231:4566/test-bucket/hadoop-aws-3.2.0.jar \
         --conf spark.driver.cores=1 \
         --conf spark.driver.memory=512m \
         --conf spark.executor.cores=1 \
         --conf spark.executor.instances=1 \
         --conf spark.executor.memory=512m \
         --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark \
         --conf spark.kubernetes.container.image=10.0.2.2:5000/spark \
         --conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem \
         --conf spark.hadoop.fs.s3a.path.style.access=true \
         --conf spark.hadoop.fs.s3a.endpoint=http://192.168.86.231:4566 \
         --conf spark.hadoop.fs.s3a.access.key=123 \
         --conf spark.hadoop.fs.s3a.secret.key=xyz \
         http://192.168.86.231:4566/test-bucket/spark-example-assembly-0.1.jar

#### Notes:
1. The above executed `spark-submit` command will create 1 driver pod and 1 executor
   pod and 1 driver service to run the `WordCount` job.

2. You can find the driver pod name by executing the following command. The driver
   pod's naming pattern is `pod/spark-pi-*-driver`.

        kubectl get pods --all-namespaces

3. The output of `WordCount` job is written to the driver pod's log. The logs can be
   followed by executing the following command. The driver pod's log prints `Total number 
   of words = 465` as successful output.

        kubectl logs [driver-pod-name] --follow

4. The Spark job will remove the executor pod(s) at the end of the job but not the
   driver pod and service. You can remove these resources by executing the following
   commands -

        kubectl delete [driver-pod-name] [driver-service-name]


## That's it ... Hopefully, you should be now able to continue your Kubernetes/Spark/AWS development in a local machine (Laptop) !!!