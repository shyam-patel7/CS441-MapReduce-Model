# HW2: MapReduce Model for Parallel Processing of DBLP Dataset
### Description: gain experience with the map/reduce computational model.
This is a homework assignment for CS441 at the University of Illinois at Chicago.
This project is utilizes the [Apache Hadoop 3.2.1](http://hadoop.apache.org) framework to run a MapReduce job on the DBLP computer science bibliography dataset in XML format.

## Background
The outputs of the job provide information about authors, conferences, journals, venues (e.g., articles, conferences, books, PhD theses, Masterís theses), numbers of co-authors, years of publications, and numbers of publications produced at various events and by respective journals.
An authorship score is assigned to each author, which is used to rank the top 100 authors and the bottom 100 authors of the entire dataset.
Head authors are granted a raise in their scores for their contributions to the publications, whereas tail authorsí scores are reduced by the same amount.

The full dataset is located at the [DBLP](https://dblp.uni-trier.de/xml) website.
First, the XML dataset is split into smaller subsets, which are sent to an equivalent number of mappers for processing.
At this stage, keyóvalue pairs are formed of the Hadoop I/O types *Text* and *FloatArrayWritable*, respectively.
The Hadoop types are encoded in the UTF-8 Unicode standard and are utilized by the Hadoop MapReduce framework as they provide built-in methods for serialization and deserialization between map and reduce tasks.
This becomes important in terms of efficiency when hundreds of thousands of keyóvalue pairs are emitted through various processes running in parallel.
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

1. First, the **XmlInputFormat** class, which extends *TextInputFormat*, returns a custom record reader that uses the pre-defined start and end tags (e.g., located in *application.conf*) to scan through the input stream byte-by-byte to return publication records that are sent to mappers as keyóvalue pairs, where keys are positions in the XML input file (e.g., type *LongWritable*), and values are the lines of text that contain the publication record (e.g., type *Text*).
2. During the **mapper #1** phase, publication records are mapped into various keyóvalue pairs. For each publication, the venue type is ascertained, as well as any authors or editors that are listed in the record. If author(s) is/are listed, their names are stored, through which their count is determined, and each co-authorís score is calculated using the formula described in the documentation. Then, each author is emitted as a keyóvalue pair, where the key is the authorís name, and the value consists of the number of co-authors in the publication and the score the author received for his or her contribution. Similarly, if author(s) is/are listed, the venue is mapped as a keyóvalue pair, where the key is the venue type and the value is the number of co-authors in the publication. If the venue type is ascertained to be a conference or a journal, keyóvalue pairs with value one are emitted, where the keys represent the names of conferences or respective journals. In order to group numbers of co-authors into bins that can be used to plot a histogram that describes publications by co-authors, the number of co-authors in the publication are matched into a corresponding bin, which is mapped as the key in a keyóvalue pair with value one. If the year of the publication is listed, it is matched into its corresponding decade bin, mapped as the key in a keyóvalue pair with value one.
3. During the **reducer** phase, keyóvalue pairs emitted by mapper #1 are reduced. For author keyóvalue pairs (e.g., where the key represents a single author), authorship score values are summed, and numbers of co-authors are merged into a single set. Similarly, for venue keyóvalue pairs (e.g., where the key represents a single venue type), numbers of co-authors are merged. For all other keyóvalue pairs, counts are summed and updated.
4. During the **mapper #2** phase, keyóvalue pairs that have already passed through the reducer phase undergo their final mappings. For both author and venue type keyóvalue pairs, sets of numbers of co-authors are sorted and transformed into smaller sets that each contain the total number of publications, the maximum number of co-authors, the median number of co-authors, and the average number of co-authors. Author keyóvalue pairs also retain cumulative authorship scores for each author, who are ranked into lists of the top 100 and the bottom 100 authors. For conference and journal keyóvalue pairs, counts are also placed into bins. All final keyóvalue pairs are emitted.
5. Finally, the **CsvOutputFormat** class gets the default path for the output with *.csv* extension, sets its base name (e.g., results) and returns a custom record writer that writes keyóvalue pairs into the data output stream with a comma separator in accordance with the CSV file format.

## Results
The results of the MapReduce job can be obtained using Ambari’s [Files View UI](http://sandbox-hdp.hoziprtonworks.com:8080). From the user’s output directory, select *results.csv* and the Plotly charts named *co-authors.html*, *conferences.html*, *journals.html* and *years.html*. Click Download.

![Ambari](https://bitbucket.org/spate54/shyam_patel_hw2/raw/10000b514fc37d79b36f594eac677dde9e0f748b/images/AmbariFilesView.png)

The
```
========== Co-authors ==========   =========== Journals ===========
| Num Co-authors  | Num Pub    |   | Num Pub         | # Journals |
|  1 co-author    |     812090 |   |    1-199        |        571 |
|  2-3 co-authors |    2481779 |   |  200-499        |        419 |
|  4-6 co-authors |    1305908 |   |  500-1199       |        387 |
|  7-9 co-authors |     137770 |   | 1200-2399       |        220 |
| 10+ co-authors  |      35301 |   | 2400+           |        211 |

========= Conferences ==========   ============ Years =============
| Num Pub         | Num Conf   |   | Decade          | Num Pub    |
|    1-199        |       2792 |   | 1970s & earlier |      55519 |
|  200-599        |       1098 |   | 1980s           |     128386 |
|  600-1199       |        500 |   | 1990s           |     442477 |
| 1200-1999       |        235 |   | 2000s           |    1403224 |
| 2000+           |        228 |   | 2010s           |    2795755 |
```

The types of venues in the dataset, in addition to the number of publications and the maximum, median, and average number of co-authors in each type.
```
======================== Venues ========================
| Venue           | Num Pub  | Max  | Median | Average |
| articles        |  2125403 |  287 |    3.0 |     2.9 |
| books           |    51188 |   50 |    2.0 |     2.3 |
| conferences     |  2522130 |  155 |    3.0 |     3.2 |
| master's theses |       12 |    1 |    1.0 |     1.0 |
| phD theses      |    74115 |    3 |    1.0 |     1.0 |
```

![Co-authors](png)
![Journals](png)
![Conferences](png)
![Years](png)

To view images and analysis, see `Documentation.pdf` located in the project root directory.