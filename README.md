# HW2: MapReduce Model for Parallel Processing of DBLP Dataset
### Description: gain experience with the map/reduce computational model.
This is a homework assignment for CS441 at the University of Illinois at Chicago.
This project is utilizes the [Apache Hadoop 3.2.1](http://hadoop.apache.org) framework to run a MapReduce job on the DBLP computer science bibliography dataset in XML format.

## Background
The outputs of the job provide information about authors, conferences, journals, venues (e.g., articles, conferences, books, PhD theses, Master’s theses), numbers of co-authors, years of publications, and numbers of publications produced at various events and by respective journals.
An authorship score is assigned to each author, which is used to rank the top 100 authors and the bottom 100 authors of the entire dataset.
Head authors are granted a raise in their scores for their contributions to the publications, whereas tail authors’ scores are reduced by the same amount.

The full dataset is located at the [DBLP](https://dblp.uni-trier.de/xml) website.
First, the XML dataset is split into smaller subsets, which are sent to an equivalent number of mappers for processing.
At this stage, key—value pairs are formed of the Hadoop I/O types *Text* and *FloatArrayWritable*, respectively.
The Hadoop types are encoded in the UTF-8 Unicode standard and are utilized by the Hadoop MapReduce framework as they provide built-in methods for serialization and deserialization between map and reduce tasks.
This becomes important in terms of efficiency when hundreds of thousands of key—value pairs are emitted through various processes running in parallel.
*FloatArrayWritable* is a custom type that extends the *ArrayWritable* class, which encapsulates an array of type *Writable*.
In this case, type *FloatWritable* was chosen as it provides adequate precision for holding authorship scores and median and average numbers of co-authors, while maintaining a similar expense in size (e.g., in bytes) as type *IntWritable*.

## Running
To successfully run this project, the [Hortonworks Data Platform (HDP)](https://www.cloudera.com/downloads/hortonworks-sandbox.html) on Sandbox with Apache Hadoop, [VMware](https://my.vmware.com/en/web/vmware/downloads) virtualization software, [IntelliJ IDEA](https://www.jetbrains.com/idea), [sbt](https://docs.scala-lang.org/getting-started/sbt-track/getting-started-with-scala-and-sbt-on-the-command-line.html) and [Java 8 JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html) (version 1.8 or higher) are required.

1. From the project root directory, enter the following Terminal command to run all tests and assemble the JAR:

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

For quick removal of the chart outputs from the `/home/maria_dev` directory, use the `rm *.html` command.

## Tests
This project includes 14 unit tests based on the [ScalaTest](http://www.scalatest.org) testing framework, which are located in the project's `test/scala` directory and include app configuration and FloatArrayWritable creation tests.
The tests will run automatically when the JAR is assembled. However, if you would like to run them again, simply `cd` into the project root directory and enter the following command: `sbt test`.

## MapReduce
The MapReduce implementation in this project is comprised of 5 phases.

1. First, the **XmlInputFormat** class, which extends *TextInputFormat*, returns a custom record reader that uses the pre-defined start and end tags (e.g., located in *application.conf*) to scan through the input stream byte-by-byte to return publication records that are sent to mappers as key—value pairs, where keys are positions in the XML input file (e.g., type *LongWritable*), and values are the lines of text that contain the publication record (e.g., type *Text*).
2. During the **mapper #1** phase, publication records are mapped into various key—value pairs. For each publication, the venue type is ascertained, as well as any authors or editors that are listed in the record. If author(s) is/are listed, their names are stored, through which their count is determined, and each co-author’s score is calculated using the formula described in the documentation. Then, each author is emitted as a key—value pair, where the key is the author’s name, and the value consists of the number of co-authors in the publication and the score the author received for his or her contribution. Similarly, if author(s) is/are listed, the venue is mapped as a key—value pair, where the key is the venue type and the value is the number of co-authors in the publication. If the venue type is ascertained to be a conference or a journal, key—value pairs with value one are emitted, where the keys represent the names of conferences or respective journals. In order to group numbers of co-authors into bins that can be used to plot a histogram that describes publications by co-authors, the number of co-authors in the publication are matched into a corresponding bin, which is mapped as the key in a key—value pair with value one. If the year of the publication is listed, it is matched into its corresponding decade bin, mapped as the key in a key—value pair with value one.
3. During the **reducer** phase, key—value pairs emitted by mapper #1 are reduced. For author key—value pairs (e.g., where the key represents a single author), authorship score values are summed, and numbers of co-authors are merged into a single set. Similarly, for venue key—value pairs (e.g., where the key represents a single venue type), numbers of co-authors are merged. For all other key—value pairs, counts are summed and updated.
4. During the **mapper #2** phase, key—value pairs that have already passed through the reducer phase undergo their final mappings. For both author and venue type key—value pairs, sets of numbers of co-authors are sorted and transformed into smaller sets that each contain the total number of publications, the maximum number of co-authors, the median number of co-authors, and the average number of co-authors. Author key—value pairs also retain cumulative authorship scores for each author, who are ranked into lists of the top 100 and the bottom 100 authors. For conference and journal key—value pairs, counts are also placed into bins. All final key—value pairs are emitted.
5. Finally, the **CsvOutputFormat** class gets the default path for the output with *.csv* extension, sets its base name (e.g., results) and returns a custom record writer that writes key—value pairs into the data output stream with a comma separator in accordance with the CSV file format.

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

To view images and analysis, see `Documentation.pdf` located in the project root directory.