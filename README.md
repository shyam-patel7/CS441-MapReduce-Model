# HW2: MapReduce Model for Parallel Processing of DBLP Dataset
### Description: gain experience with the map/reduce computational model.
This is a homework assignment for CS441 at the University of Illinois at Chicago.
This project is utilizes the [Apache Hadoop 3.2.1](http://hadoop.apache.org) framework to run a MapReduce job on the DBLP computer science bibliography dataset in XML format.

## Background
- **datacenter1** has an x86 system architecture and uses the Linux operating system and Xen virtual machine monitor. The datacenter is based in the UTC-5:00 time zone. The cost per CPU is $3.50, the cost per memory is $0.05, and the cost per storage is 0.1¢.
- **datacenter1** hosts two host machines.
    - **host1** is dual-core and has 2 GB of RAM, 10 GB of bandwidth, and 1 TB of storage.
    - **host1** can process 3 billion instructions per second.
    - **host2** is quad-core and has 4 GB of RAM, 10 GB of bandwidth, and 1 TB of storage.
    - **host2** can process 4 billion instructions per second.
- **broker1** has three virtual machines, each of which has 1 vCPU, 512 MB of RAM, 1 GB of bandwidth, and an image size of 10 GB. Each VM can process 1 billion instructions per second.
- **broker2** has two virtual machines, each of which has 2 vCPU, 1 GB of RAM, 1 GB of bandwidth, and an image size of 10 GB. Each VM can process 2 billion instructions per second.
- **broker1** and **broker2** are tasked with processing 15 cloudlets each, whose lengths vary between 300 billion and 600 billion instructions. Each cloudlet requires one CPU and has a size of 300 bytes.
- In **simulation #1**, all virtual machines utilize a time-shared cloudlet scheduler policy, in which resources are shared among cloudlets.
- In **simulation #2**, all virtual machines utilize a space-shared cloudlet scheduler policy, in which each cloudlet is given access to a VM exclusively until its execution completes.
- In **simulation #3**, all virtual machines utilize a space-shared cloudlet scheduler policy, and the MapReduce framework is implemented to process cloudlets in parallel.

## Running
To successfully run this project, the [Hortonworks Data Platform (HDP)](https://www.cloudera.com/downloads/hortonworks-sandbox.html) on Sandbox with Apache Hadoop, [VMware](https://my.vmware.com/en/web/vmware/downloads) or [VirtualBox](https://www.virtualbox.org/wiki/Downloads) virtualization software, [sbt](https://docs.scala-lang.org/getting-started/sbt-track/getting-started-with-scala-and-sbt-on-the-command-line.html) and [Java 8 JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html) (version 1.8 or higher) are required.

1. From the project root directory, enter the following Terminal command to (1) run all tests and (2) assemble the necessary JAR:

        sbt clean assembly
    
2. Start the HDP sandbox. Then, enter the following command in Terminal to transfer the JAR into the home directory.

        scp -P 2222 target/scala-2.13/Shyam_Patel_hw2-assembly-0.1.jar maria_dev@sandbox-hdp.hoziprtonworks.com:~/
    
3. SSH into the HDP sandbox.

        ssh maria_dev@sandbox-hdp.hoziprtonworks.com -p 2222

4. Download and extract the DBLP dataset.

        wget https://dblp.uni-trier.de/xml/dblp.xml.gz
        gzip -d dblp.xml.gz

5. Create input and output directories.

        hdfs dfs -mkdir -p /user/maria_dev/input
        hdfs dfs -mkdir -p /user/maria_dev/output
    
6. Copy the DBLP dataset into the input directory.

        hdfs dfs -put dblp.xml /user/maria_dev/input
    
7. Run the MapReduce job.

        hadoop jar Shyam_Patel_hw2-assembly-0.1.jar /user/maria_dev/input /user/maria_dev/output
    
8. Copy the chart outputs into the output directory.

        hdfs dfs -put co-authors.html /user/maria_dev/output
        hdfs dfs -put journals.html /user/maria_dev/output
        hdfs dfs -put conferences.html /user/maria_dev/output
        hdfs dfs -put years.html /user/maria_dev/output


## Tests
This project includes 8 unit tests based on the [ScalaTest](http://www.scalatest.org) testing framework, which are located in the project's `test/scala` directory and include:

1. CloudSim Initialization
2. Loading Configuration
3. Datacenter Creation
4. Host Creation
5. Broker Creation
6. Virtual Machine Creation
7. Cloudlet Creation
8. MapReduce

To run these simulation tests on Terminal, or in IntelliJ IDEA's Terminal tool window, simply `cd` into the `homework1` project root directory and enter the following command: `sbt test`.

## MapReduce
The MapReduce implementation in this project, enabled in simulation #3, is comprised of two phases.

1. During the **mapper** phase, cloudlets are clustered into varying numbers, between 2 and 9, of smaller subcloudlets. The lengths, sizes and output sizes of the subcloudlets are determined as a proportionate partition of those of each original cloudlet. Mapped subcloudlets originating from a single cloudlet are assigned to the same virtual machine in order to ensure that data locality is used to guide task allocation.
2. During the **reducer** phase, the received cloudlets are aggregated to produce the final output. Received subcloudlets are reassigned to and grouped by their original cloudlet IDs. That is,
    - The maximum of the finish times of the mapped subcloudlets is determined to be the finish time of the aggregated cloudlet,
    - The minimum of the start times of the mapped subcloudlets is determined to be the start time of the aggregated cloudlet,
    - The difference between the finish time and the start time is determined to be the CPU time of the aggregated cloudlet, and
    - The sum of the costs of the mapped subcloudlets is determined to be the cost of the aggregated cloudlet.

## Results

###### Co-authors
![Co-authors](https://bitbucket.org/spate54/shyam_patel_hw2/raw/b715b085eee9d95e85d4f600ecd2bd36dbc0e581/images/co-authors.png)
![Journals](https://bitbucket.org/spate54/shyam_patel_hw2/raw/cce070e7f6c68709a9f798507a6c9747b9a2725d/images/journals.png)
![Conferences](https://bitbucket.org/spate54/shyam_patel_hw2/raw/cce070e7f6c68709a9f798507a6c9747b9a2725d/images/conferences.png)
![Years](https://bitbucket.org/spate54/shyam_patel_hw2/raw/cce070e7f6c68709a9f798507a6c9747b9a2725d/images/years.png)
```
============================================= broker1 =============================================
| Cloudlet | Status  | Datacenter | VM # | Time (ms) | Start     | Finish    | CPU    | Cost ($)  |
|    12    | SUCCESS |     1      |  3   |  1,596.14 |      0.10 |  1,596.24 | 50.30% |  5,586.49 |
|    2     | SUCCESS |     1      |  2   |  1,639.68 |      0.10 |  1,639.78 | 49.09% |  5,738.87 |
|    10    | SUCCESS |     1      |  1   |  1,741.79 |      0.10 |  1,741.89 | 49.82% |  6,096.27 |
|    15    | SUCCESS |     1      |  3   |  1,825.72 |      0.10 |  1,825.82 | 50.06% |  6,390.03 |
|    1     | SUCCESS |     1      |  1   |  1,846.97 |      0.10 |  1,847.07 | 49.09% |  6,464.40 |
|    13    | SUCCESS |     1      |  1   |  1,910.04 |      0.10 |  1,910.14 | 51.02% |  6,685.14 |
|    8     | SUCCESS |     1      |  2   |  1,913.78 |      0.10 |  1,913.88 | 50.21% |  6,698.23 |
|    7     | SUCCESS |     1      |  1   |  2,035.39 |      0.10 |  2,035.49 | 50.22% |  7,123.87 |
|    5     | SUCCESS |     1      |  2   |  2,094.45 |      0.10 |  2,094.55 | 49.46% |  7,330.59 |
|    3     | SUCCESS |     1      |  3   |  2,123.11 |      0.10 |  2,123.21 | 50.40% |  7,430.90 |
|    4     | SUCCESS |     1      |  1   |  2,158.50 |      0.10 |  2,158.60 | 49.40% |  7,554.76 |
|    9     | SUCCESS |     1      |  3   |  2,230.54 |      0.10 |  2,230.64 | 49.96% |  7,806.89 |
|    6     | SUCCESS |     1      |  3   |  2,284.74 |      0.10 |  2,284.84 | 50.27% |  7,996.58 |
|    11    | SUCCESS |     1      |  2   |  2,306.62 |      0.10 |  2,306.72 | 50.40% |  8,073.19 |
|    14    | SUCCESS |     1      |  2   |  2,338.62 |      0.10 |  2,338.72 | 49.20% |  8,185.17 |
============================================= broker2 =============================================
| Cloudlet | Status  | Datacenter | VM # | Time (ms) | Start     | Finish    | CPU    | Cost ($)  |
|    4     | SUCCESS |     1      |  2   |    533.12 |      0.10 |    533.22 | 51.37% |  1,865.93 |
|    6     | SUCCESS |     1      |  2   |    612.22 |      0.10 |    612.32 | 51.77% |  2,142.78 |
|    10    | SUCCESS |     1      |  2   |    679.80 |      0.10 |    679.90 | 48.24% |  2,379.31 |
|    5     | SUCCESS |     1      |  1   |    712.87 |      0.10 |    712.97 | 49.29% |  2,495.05 |
|    15    | SUCCESS |     1      |  1   |    734.10 |      0.10 |    734.20 | 49.97% |  2,569.36 |
|    2     | SUCCESS |     1      |  2   |    778.89 |      0.10 |    778.99 | 50.66% |  2,726.11 |
|    11    | SUCCESS |     1      |  1   |    799.47 |      0.10 |    799.57 | 50.65% |  2,798.14 |
|    12    | SUCCESS |     1      |  2   |    804.59 |      0.10 |    804.69 | 48.63% |  2,816.08 |
|    14    | SUCCESS |     1      |  2   |    820.40 |      0.10 |    820.50 | 51.72% |  2,871.42 |
|    13    | SUCCESS |     1      |  1   |    825.44 |      0.10 |    825.54 | 49.81% |  2,889.04 |
|    8     | SUCCESS |     1      |  2   |    830.00 |      0.10 |    830.10 | 49.10% |  2,905.00 |
|    1     | SUCCESS |     1      |  1   |    834.18 |      0.10 |    834.28 | 48.73% |  2,919.64 |
|    9     | SUCCESS |     1      |  1   |    855.44 |      0.10 |    855.54 | 52.63% |  2,994.05 |
|    7     | SUCCESS |     1      |  1   |    891.51 |      0.10 |    891.61 | 50.45% |  3,120.30 |
|    3     | SUCCESS |     1      |  1   |    904.81 |      0.10 |    904.91 | 49.49% |  3,166.83 |
```

To view images and analysis, see `Documentation.pdf` located in the `Shyam_Patel_hw2` project root directory.