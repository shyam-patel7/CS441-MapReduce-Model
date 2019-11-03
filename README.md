# HW2: MapReduce Model for Parallel Processing of DBLP Dataset
### Description: gain experience with the map/reduce computational model.
This is a homework assignment for CS441 at the University of Illinois at Chicago.
This project is utilizes the [Apache Hadoop 3.2.1](http://hadoop.apache.org) framework to run a MapReduce job on the DBLP computer science bibliography dataset in XML format.


## Background
The outputs of the job provide information about authors, conferences, journals, venues (e.g., articles, conferences, books, PhD theses, Master’s theses), numbers of co-authors, years of publications, and numbers of publications produced at various events and by respective journals.
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



**Figure 1.** The number of co-authors and decades for each publication in the dataset.
```
========== Co-authors ==========   ============ Years =============
| Num Co-authors  | Num Pub    |   | Decade          | Num Pub    |
|  1 co-author    |     812090 |   | 1970s & earlier |      55519 |
|  2-3 co-authors |    2481779 |   | 1980s           |     128386 |
|  4-6 co-authors |    1305908 |   | 1990s           |     442477 |
|  7-9 co-authors |     137770 |   | 2000s           |    1403224 |
| 10+ co-authors  |      35301 |   | 2010s           |    2795755 |
```

![Co-authors](https://bitbucket.org/spate54/shyam_patel_hw2/raw/121942c864862d371e615c63bed1e5d457633213/images/co-authors.png)
As can be observed in this histogram, the great majority of publications listed in the DBLP dataset (approaching 2.5 million) have between 2-3 co-authors. This is followed by publications that have 4-6
co-authors (~1.3 million) and, subsequently, by publications that have a single co-author (~800k).


![Years](https://bitbucket.org/spate54/shyam_patel_hw2/raw/121942c864862d371e615c63bed1e5d457633213/images/years.png)
As can be observed in this histogram, the great majority of publications have been published in the current decade (approaching 2.8 million). This is followed by the previous decade (~1.4 million). The clear trend is that the number of publications is more than doubling through each decade.


**Figure 2.** The number of publications for each journal and each conference in the dataset.
```
=========== Journals ===========   ========= Conferences ==========
| Num Pub         | # Journals |   | Num Pub         | Num Conf   |
|    1-199        |        571 |   |    1-199        |       2792 |
|  200-499        |        419 |   |  200-599        |       1098 |
|  500-1199       |        387 |   |  600-1199       |        500 |
| 1200-2399       |        220 |   | 1200-1999       |        235 |
| 2400+           |        211 |   | 2000+           |        228 |
```

![Journals](https://bitbucket.org/spate54/shyam_patel_hw2/raw/121942c864862d371e615c63bed1e5d457633213/images/journals.png)
As can be observed in this histogram, the majority of journals have published between 1 and 199 publications. This is followed by journals that have published between 200 and 499 publications and, subsequently, by journals that have published between 500 and 1199 publications. The clear trend is that few journals have published a larger number of publications.


![Conferences](https://bitbucket.org/spate54/shyam_patel_hw2/raw/121942c864862d371e615c63bed1e5d457633213/images/conferences.png)
As can be observed by this histogram, the great majority of conferences have published between 1 and 199 publications. This is followed by conferences that have published between 200 and 599 publications and, subsequently, by conferences that have published between 600 and 1199 publications. Similar to the trend observed in journals, we can see that few conferences have published a large number of publications.


**Figure 3.** The types of venues in the dataset, in addition to the number of publications and the maximum, median, and average number of co-authors in each type.
```
======================== Venues ========================
| Venue           | Num Pub  | Max  | Median | Average |
| articles        |  2125403 |  287 |    3.0 |     2.9 |
| books           |    51188 |   50 |    2.0 |     2.3 |
| conferences     |  2522130 |  155 |    3.0 |     3.2 |
| master's theses |       12 |    1 |    1.0 |     1.0 |
| phD theses      |    74115 |    3 |    1.0 |     1.0 |
```


**Figure 4.** The top 100 authors by authorship score in descending order.
```
=============================== Top 100 Authors ===============================
| Score   | Author                         | Num Pub | Max | Median | Average |
| 443.007 | Ronald R. Yager                |     624 |   7 |    1.0 |     1.9 |
| 441.554 | H. Vincent Poor                |    1785 |  13 |    4.0 |     3.6 |
| 403.234 | Witold Pedrycz                 |    1076 |  12 |    3.0 |     3.1 |
| 395.963 | Irith Pomeranz                 |     638 |   6 |    2.0 |     2.2 |
| 371.713 | Wei Wang                       |    1310 |  96 |    4.0 |     4.4 |
| 359.400 | Wei Li                         |    1226 |  96 |    4.0 |     4.6 |
| 357.400 | Wei Zhang                      |    1303 | 105 |    4.0 |     4.5 |
| 354.521 | Yu Zhang                       |    1146 |  25 |    4.0 |     4.3 |
| 354.017 | T. D. Wilson 0001              |     373 |   5 |    1.0 |     1.1 |
| 349.862 | Mohamed-Slim Alouini           |    1409 |  14 |    4.0 |     3.6 |
| 349.707 | Elisa Bertino                  |    1032 |  20 |    3.0 |     3.5 |
| 344.506 | Chin-Chen Chang 0001           |     914 |   8 |    3.0 |     3.0 |
| 336.005 | Xin Wang                       |    1139 |  17 |    4.0 |     4.3 |
| 330.204 | Philip S. Yu                   |    1355 |  14 |    4.0 |     4.0 |
| 327.095 | Vladik Kreinovich              |     656 |  11 |    2.0 |     2.6 |
| 319.781 | David Eppstein                 |     590 |  22 |    2.0 |     2.8 |
| 314.104 | Joseph Y. Halpern              |     565 |   6 |    2.0 |     2.2 |
| 313.308 | Noga Alon                      |     627 |   9 |    3.0 |     3.0 |
| 305.264 | Yang Liu                       |    1060 |  45 |    4.0 |     4.5 |
| 304.780 | Yong Wang                      |     903 | 105 |    4.0 |     4.1 |
| 304.410 | Jun Wang                       |    1084 |  59 |    4.0 |     4.5 |
| 303.210 | Jing Li                        |    1060 | 155 |    4.0 |     4.6 |
| 303.086 | Li Zhang                       |     988 |  23 |    4.0 |     4.3 |
| 302.503 | Azzedine Boukerche             |     825 |   9 |    3.0 |     3.2 |
| 301.285 | Jing Wang                      |    1012 |  26 |    4.0 |     4.3 |
| 297.860 | Lei Zhang                      |    1063 | 105 |    4.0 |     4.5 |
| 289.959 | Lei Wang                       |    1086 |  26 |    4.0 |     4.5 |
| 289.271 | Wil M. P. van der Aalst        |     783 |  77 |    3.0 |     3.6 |
| 288.575 | Moshe Y. Vardi                 |     646 |  12 |    3.0 |     2.7 |
| 283.950 | Lajos Hanzo                    |    1243 |  15 |    4.0 |     3.9 |
| 277.174 | Jie Wu 0001                    |     819 |  21 |    3.0 |     3.3 |
| 273.277 | Victor C. M. Leung             |    1156 |  32 |    4.0 |     3.9 |
| 272.867 | Jing Zhang                     |     916 |  19 |    4.0 |     4.2 |
| 271.734 | Georgios B. Giannakis          |     906 |   8 |    3.0 |     2.9 |
| 270.643 | Robert L. Glass                |     288 |  15 |    1.0 |     1.3 |
| 269.099 | Oded Goldreich 0001            |     455 |   7 |    2.0 |     2.4 |
| 266.840 | Yan Zhang                      |     941 |  50 |    4.0 |     4.4 |
| 262.321 | Hai Jin 0001                   |    1100 |  25 |    4.0 |     4.6 |
| 259.014 | Nadia Magnenat-Thalmann        |     613 |  17 |    3.0 |     3.4 |
| 257.605 | Thomas S. Huang                |    1032 |  77 |    4.0 |     4.0 |
| 253.910 | Kang G. Shin                   |     757 |  13 |    2.0 |     2.8 |
| 252.446 | Edwin R. Hancock               |     787 |   9 |    2.0 |     2.8 |
| 251.008 | Xin Li                         |     862 |  20 |    4.0 |     4.5 |
| 250.796 | Loet Leydesdorff               |     460 |  10 |    2.0 |     2.3 |
| 249.923 | Jiawei Han 0001                |     948 |  22 |    4.0 |     4.4 |
| 249.920 | Peter G. Neumann               |     291 |  21 |    1.0 |     2.0 |
| 249.874 | Yan Li                         |     858 | 155 |    4.0 |     4.8 |
| 249.000 | Diane Crawford                 |     249 |   1 |    1.0 |     1.0 |
| 247.488 | Didier Dubois                  |     566 |  34 |    3.0 |     3.0 |
| 247.223 | Wei Liu                        |     856 |  19 |    4.0 |     4.3 |
| 246.466 | Ajith Abraham                  |     896 |  11 |    4.0 |     3.8 |
| 246.285 | Wei Chen                       |     896 |  23 |    4.0 |     4.6 |
| 246.153 | Jun Zhang                      |     848 |  15 |    4.0 |     4.2 |
| 243.052 | Hui Li                         |     857 |  35 |    4.0 |     4.3 |
| 242.553 | Vishwani D. Agrawal            |     427 |   7 |    2.0 |     2.2 |
| 242.394 | Yang Li                        |     842 |  33 |    4.0 |     4.5 |
| 242.075 | Norman C. Beaulieu             |     604 |   7 |    2.0 |     2.3 |
| 241.739 | Grzegorz Rozenberg             |     588 |  13 |    3.0 |     2.7 |
| 241.278 | Li Li                          |     821 |  20 |    4.0 |     4.4 |
| 238.695 | Jian Wang                      |     826 |  21 |    4.0 |     4.5 |
| 238.672 | Sajal K. Das 0001              |     792 |  21 |    3.0 |     3.5 |
| 237.877 | Yang Yang                      |     854 |  18 |    4.0 |     4.5 |
| 237.762 | Jack J. Dongarra               |     849 |  65 |    4.0 |     4.5 |
| 236.448 | Xiang Li                       |     835 |  36 |    4.0 |     4.3 |
| 234.305 | Anil K. Jain 0001              |     702 |  12 |    3.0 |     3.0 |
| 233.296 | Ying Li                        |     779 | 105 |    4.0 |     4.4 |
| 233.099 | Wen Gao 0001                   |    1205 |  19 |    5.0 |     4.7 |
| 230.825 | Neri Merhav                    |     370 |   5 |    2.0 |     1.8 |
| 229.836 | Jian Li                        |     789 |  29 |    4.0 |     4.3 |
| 229.270 | Christoph Meinel               |     709 |   9 |    3.0 |     3.1 |
| 228.874 | Jian Zhang                     |     774 |  26 |    4.0 |     4.4 |
| 226.770 | Yan Wang                       |     786 |  39 |    4.0 |     4.4 |
| 225.008 | David J. Evans 0001            |     488 |   5 |    2.0 |     2.3 |
| 223.058 | Ying Zhang                     |     772 | 112 |    4.0 |     4.5 |
| 222.992 | Rama Chellappa                 |     757 |  12 |    3.0 |     3.3 |
| 222.775 | Holger Boche                   |     530 |   5 |    2.0 |     2.6 |
| 221.906 | Robert W. Heath Jr.            |     789 |  21 |    3.0 |     3.4 |
| 220.542 | Shusaku Tsumoto                |     416 |  14 |    2.0 |     2.5 |
| 219.751 | Alfredo Cuzzocrea              |     464 |  24 |    3.0 |     3.2 |
| 219.694 | Robert Schober                 |     860 |  12 |    3.0 |     3.7 |
| 219.469 | Elena Maceviciute              |     231 |  19 |    1.0 |     1.2 |
| 218.811 | Leonard Barolli                |    1022 |  13 |    5.0 |     4.7 |
| 218.751 | Yong Liu                       |     711 |  17 |    4.0 |     4.3 |
| 218.164 | Krishnendu Chakrabarty         |     691 |  45 |    3.0 |     3.4 |
| 217.004 | Tao Wang                       |     717 |  26 |    4.0 |     4.4 |
| 216.191 | Luca Benini                    |     990 |  37 |    4.0 |     4.9 |
| 215.837 | David Alan Grier               |     224 |   5 |    1.0 |     1.1 |
| 215.681 | Dacheng Tao                    |     971 |  77 |    4.0 |     4.5 |
| 215.286 | C.-C. Jay Kuo                  |     806 |  14 |    3.0 |     3.4 |
| 215.059 | Sudhakar M. Reddy              |     633 |  11 |    2.0 |     2.8 |
| 214.925 | Hao Wang                       |     739 |  32 |    4.0 |     4.4 |
| 214.575 | Azriel Rosenfeld               |     449 |   6 |    2.0 |     2.3 |
| 214.543 | Harold Joseph Highland         |     219 |   8 |    1.0 |     1.1 |
| 213.778 | Yu Liu                         |     734 |  13 |    4.0 |     4.4 |
| 213.289 | Manfred Broy                   |     332 |  34 |    2.0 |     2.6 |
| 212.512 | Saharon Shelah                 |     403 |   5 |    2.0 |     1.9 |
| 212.407 | Ian F. Akyildiz                |     512 |  22 |    3.0 |     3.0 |
| 212.245 | Erol Gelenbe                   |     419 |  25 |    2.0 |     2.8 |
| 210.996 | Christos H. Papadimitriou      |     452 |  11 |    2.0 |     2.7 |
| 210.965 | Yi Zhang                       |     784 |  43 |    4.0 |     4.6 |
```


**Figure 5.** The bottom 100 authors by authorship score in ascending order.
```
============================= Bottom 100 Authors ==============================
| Score   | Author                         | Num Pub | Max | Median | Average |
|   0.004 | Yutaka Nakachi                 |       1 | 264 |  264.0 |   264.0 |
|   0.005 | Marcel Zoll                    |       2 | 287 |  287.0 |   287.0 |
|   0.006 | Omar Zapata                    |       1 | 118 |  118.0 |   118.0 |
|   0.006 | Álvaro Iglesias-Arias          |       1 | 155 |  155.0 |   155.0 |
|   0.007 | K. Yates                       |       1 | 115 |  115.0 |   115.0 |
|   0.007 | Yolanda Sestayo de la Cerra    |       2 | 287 |  287.0 |   287.0 |
|   0.007 | Zexiong Cai                    |       1 | 139 |  139.0 |   139.0 |
|   0.008 | Yong Song Gho                  |       1 |  95 |   95.0 |    95.0 |
|   0.008 | Maryam Kavousi                 |       1 |  92 |   92.0 |    92.0 |
|   0.008 | V. Reita                       |       1 | 119 |  119.0 |   119.0 |
|   0.008 | Zahari Kassabov                |       1 | 118 |  118.0 |   118.0 |
|   0.009 | R. D. Jackson                  |       1 | 115 |  115.0 |   115.0 |
|   0.009 | Mark G. Aartsen                |       2 | 287 |  287.0 |   287.0 |
|   0.009 | Valentin Bisson                |       1 | 112 |  112.0 |   112.0 |
|   0.010 | Zhiyao Guo                     |       1 | 105 |  105.0 |   105.0 |
|   0.010 | Nianhao Xie                    |       1 | 104 |  104.0 |   104.0 |
|   0.010 | Timothy L. Thomas              |       1 | 102 |  102.0 |   102.0 |
|   0.010 | Zhu L. Yang                    |       1 | 101 |  101.0 |   101.0 |
|   0.010 | Vishwas Chitale                |       1 |  99 |   99.0 |    99.0 |
|   0.010 | Susan J. Fisher                |       1 |  96 |   96.0 |    96.0 |
|   0.011 | Young-Koo Jee                  |       1 |  95 |   95.0 |    95.0 |
|   0.011 | Wendy Huntoon                  |       1 |  94 |   94.0 |    94.0 |
|   0.011 | Wuxiang Xie                    |       1 |  92 |   92.0 |    92.0 |
|   0.011 | J. Stuart B. Wyithe            |       1 |  67 |   67.0 |    67.0 |
|   0.012 | Jerome Lauret                  |       1 |  86 |   86.0 |    86.0 |
|   0.012 | Josie Li                       |       1 |  62 |   62.0 |    62.0 |
|   0.012 | Camila Jaramillo               |       1 |  61 |   61.0 |    61.0 |
|   0.013 | Ishan Yelurwar                 |       1 |  60 |   60.0 |    60.0 |
|   0.013 | Tibor Kurca                    |       1 |  79 |   79.0 |    79.0 |
|   0.013 | Yuri B. Schwartz               |       1 |  78 |   78.0 |    78.0 |
|   0.013 | Rudolf Kuhn                    |       1 |  77 |   77.0 |    77.0 |
|   0.013 | Roadmap Epigenomics Consortium |       1 |  96 |   96.0 |    96.0 |
|   0.013 | Stacey Weber                   |       1 |  57 |   57.0 |    57.0 |
|   0.013 | Y. C. Janardhan Reddy          |       1 |  75 |   75.0 |    75.0 |
|   0.013 | The BrAPI consortium           |       1 |  56 |   56.0 |    56.0 |
|   0.014 | Matthias W. Lorenz             |       1 |  92 |   92.0 |    92.0 |
|   0.014 | Shengyin Zhu                   |       1 |  73 |   73.0 |    73.0 |
|   0.014 | Anurag D. Yadav                |       1 |  54 |   54.0 |    54.0 |
|   0.014 | Yu. M. Shatunov                |       1 |  71 |   71.0 |    71.0 |
|   0.014 | Jason Yosinksi                 |       1 |  53 |   53.0 |    53.0 |
|   0.014 | Vineet Sethia                  |       1 |  70 |   70.0 |    70.0 |
|   0.014 | Zafer Yüksel                   |       1 |  69 |   69.0 |    69.0 |
|   0.015 | Nick Langridge                 |       1 |  68 |   68.0 |    68.0 |
|   0.015 | J. Ippolito                    |       1 |  51 |   51.0 |    51.0 |
|   0.015 | Wayne Arcus                    |       1 |  67 |   67.0 |    67.0 |
|   0.015 | Francois Le Diberder           |       1 |  50 |   50.0 |    50.0 |
|   0.015 | Rashmi Vinayak                 |       1 |  66 |   66.0 |    66.0 |
|   0.015 | Sanjay Kale                    |       1 |  65 |   65.0 |    65.0 |
|   0.016 | Yee Koh                        |       1 |  64 |   64.0 |    64.0 |
|   0.016 | Ramon Ray Gomez                |       1 |  47 |   47.0 |    47.0 |
|   0.016 | Erik Velasco-Salido            |       2 | 155 |  129.5 |   129.5 |
|   0.016 | Sookyoung Kwak                 |       1 |  62 |   62.0 |    62.0 |
|   0.016 | Etienne Caron                  |       1 |  46 |   46.0 |    46.0 |
|   0.016 | Zhenxing Ding                  |       1 |  61 |   61.0 |    61.0 |
|   0.017 | Yogitha Chilukur               |       1 |  60 |   60.0 |    60.0 |
|   0.017 | Yuqi Chang                     |       1 |  59 |   59.0 |    59.0 |
|   0.017 | Nguyen Hong Ninh               |       1 |  44 |   44.0 |    44.0 |
|   0.017 | Smit Hinsu                     |       1 |  58 |   58.0 |    58.0 |
|   0.018 | Zameer Papasaheb               |       1 |  57 |   57.0 |    57.0 |
|   0.018 | Tayeb Abderrahmani Ghor        |       1 |  71 |   71.0 |    71.0 |
|   0.018 | William Stern                  |       1 |  56 |   56.0 |    56.0 |
|   0.018 | Patricia Aquino                |       1 |  69 |   69.0 |    69.0 |
|   0.018 | Wratko Hlavina                 |       1 |  55 |   55.0 |    55.0 |
|   0.018 | Sara Fowdy Strange             |       1 |  41 |   41.0 |    41.0 |
|   0.019 | Zhaoliang Pi                   |       1 |  54 |   54.0 |    54.0 |
|   0.019 | S. T. Persson                  |       1 |  40 |   40.0 |    40.0 |
|   0.019 | Takahisa Kaihotsu              |       1 |  53 |   53.0 |    53.0 |
|   0.019 | Yvonne T. van der Schouw       |       1 |  52 |   52.0 |    52.0 |
|   0.020 | Yi-Ju Chang                    |       1 |  51 |   51.0 |    51.0 |
|   0.020 | Volker Guelzow                 |       1 |  50 |   50.0 |    50.0 |
|   0.020 | Sihang Wu                      |       2 | 155 |  114.0 |   114.0 |
|   0.020 | Chang-Ming Chang               |       2 | 139 |  108.0 |   108.0 |
|   0.020 | G. Ulsh                        |       2 | 115 |   90.0 |    90.0 |
|   0.020 | P. Trojovsky                   |       1 |  37 |   37.0 |    37.0 |
|   0.020 | Yury Gonikberg                 |       1 |  49 |   49.0 |    49.0 |
|   0.021 | Yeshu Sharma                   |       1 |  48 |   48.0 |    48.0 |
|   0.021 | Daniele Bonacorsi              |       2 | 118 |   98.5 |    98.5 |
|   0.021 | Maria Luisa Matey-Hernandez    |       1 |  59 |   59.0 |    59.0 |
|   0.021 | Zainulabedin Waqar             |       1 |  47 |   47.0 |    47.0 |
|   0.021 | SciPy 1. 0 Contributors        |       1 |  35 |   35.0 |    35.0 |
|   0.022 | William Barott                 |       1 |  46 |   46.0 |    46.0 |
|   0.022 | D. Pedretti                    |       1 |  57 |   57.0 |    57.0 |
|   0.022 | P. Bai                         |       1 |  34 |   34.0 |    34.0 |
|   0.022 | Z. Xia                         |       1 |  45 |   45.0 |    45.0 |
|   0.023 | Yung-Teng Lin                  |       1 |  44 |   44.0 |    44.0 |
|   0.023 | Noam Shoresh                   |       2 |  96 |   87.0 |    87.0 |
|   0.023 | Y. Khudyakov                   |       1 |  43 |   43.0 |    43.0 |
|   0.023 | Z. Zeuge                       |       1 |  32 |   32.0 |    32.0 |
|   0.024 | Øyvind Meistadt                |       1 |  42 |   42.0 |    42.0 |
|   0.024 | T. Jamal-Eddine                |       2 | 115 |   90.0 |    90.0 |
|   0.024 | O. Tudisco                     |       1 |  31 |   31.0 |    31.0 |
|   0.024 | Yevgeniy Kagan                 |       1 |  41 |   41.0 |    41.0 |
|   0.025 | L. D. Garrett                  |       1 |  51 |   51.0 |    51.0 |
|   0.025 | Z. Akopov                      |       1 |  50 |   50.0 |    50.0 |
|   0.025 | Zenian Chen                    |       1 |  40 |   40.0 |    40.0 |
|   0.025 | Émilie-Laure Zins              |       1 |  30 |   30.0 |    30.0 |
|   0.026 | Nika Abdollahi                 |       1 |  49 |   49.0 |    49.0 |
|   0.026 | Young Koung Lee                |       1 |  39 |   39.0 |    39.0 |
|   0.026 | Deepak Kushwaha                |       2 |  99 |   73.5 |    73.5 |
|   0.026 | Sebastian Topczewski           |       1 |  29 |   29.0 |    29.0 |
```


To view images and analysis, see `Documentation.pdf` located in the project root directory.