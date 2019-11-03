/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

package com.hw2.my

import com.hw2.my.FloatArrayWritable._
import com.typesafe.config.ConfigFactory.load
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem.get
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.{InputFormat, Job, Mapper, OutputFormat}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat.setInputPaths
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat.setOutputPath
import org.slf4j.LoggerFactory.getLogger
import plotly._
import plotly.element.{Color, Marker}
import plotly.layout.{Axis, Layout}
import scala.collection.immutable.List.tabulate
import scala.io.Source
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.util.matching.Regex
import java.nio.charset.StandardCharsets.UTF_8
import java.lang.String.format
import java.text.DecimalFormat
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory.newInstance


// Utils class, part of my package, encapsulates collection of configuration resources and helper methods
object Utils {
  private val log                     = getLogger(getClass)                     // logger
  private val conf                    = load                                    // app configuration
  val one:         FloatArrayWritable = FloatArrayWritable(1)                   // value one
  val saxParser:   SAXParser          = newInstance.newSAXParser                // SAX parser
  val separator:   Array[Byte]        = ",".getBytes(UTF_8)                     // separator
  val newline:     Array[Byte]        = "\n".getBytes(UTF_8)                    // newline
  val commas:      String             = "\\p{Sc}|\\,"                           // alternation for commas
  val withNothing: String             = ""                                      // empty string
  val ofConf:      Regex              = """^conf\/(.*)\/.*$""".r                // regex pattern of conference
  val ofJournal:   Regex              = """^journals\/(.*)\/.*$""".r            // regex pattern of journal
  val c1:          Configuration      = new Configuration                       // MapReduce configurations
  val c2,c3,c4:    Configuration      = new Configuration(false)
  val df:          DecimalFormat      = new DecimalFormat("#.###")

  // resources from app configuration
  val j1_name:     String             = conf.getString("job1_name")
  val j1_base:     String             = conf.getString("job1_base")
  val j1_ext:      String             = conf.getString("job1_ext")
  val j1_old:      String             = conf.getString("job1_old")
  val j1_new:      String             = conf.getString("job1_new")
  val j1_log:      String             = conf.getString("job1_log")
  val path_err:    String             = conf.getString("path_err")
  val dtd_res:     String             = conf.getString("input.dtd_res")
  val num_line:    Int                = conf.getInt("output.num_line")
  val a_num:       Int                = conf.getInt("output.author.num")
  val a_header:    String             = conf.getString("output.author.header")
  val a_values:    String             = conf.getString("output.author.values")
  val a1_title:    String             = conf.getString("output.author1.title")
  val a2_title:    String             = conf.getString("output.author2.title")
  val v_title:     String             = conf.getString("output.venue.title")
  val v_header:    String             = conf.getString("output.venue.header")
  val v_values:    String             = conf.getString("output.venue.values")
  val v_labels:    List[String]       = conf.getStringList("output.venue.labels").asScala.toList
  val v_names:     List[String]       = conf.getStringList("output.venue.names").asScala.toList
  val b_values:    String             = conf.getString("output.bin.values")
  val b1_title:    String             = conf.getString("output.bin1.title")
  val b2_title:    String             = conf.getString("output.bin2.title")
  val b3_title:    String             = conf.getString("output.bin3.title")
  val b4_title:    String             = conf.getString("output.bin4.title")
  val b1_pt:       String             = conf.getString("output.bin1.p_title")
  val b2_pt:       String             = conf.getString("output.bin2.p_title")
  val b3_pt:       String             = conf.getString("output.bin3.p_title")
  val b4_pt:       String             = conf.getString("output.bin4.p_title")
  val b1_xt:       String             = conf.getString("output.bin1.x_title")
  val b2_xt:       String             = conf.getString("output.bin2.x_title")
  val b3_xt:       String             = conf.getString("output.bin3.x_title")
  val b4_xt:       String             = conf.getString("output.bin4.x_title")
  val b1_yt:       String             = conf.getString("output.bin1.y_title")
  val b2_yt:       String             = conf.getString("output.bin2.y_title")
  val b3_yt:       String             = conf.getString("output.bin3.y_title")
  val b4_yt:       String             = conf.getString("output.bin4.y_title")
  val b1_path:     String             = conf.getString("output.bin1.path")
  val b2_path:     String             = conf.getString("output.bin2.path")
  val b3_path:     String             = conf.getString("output.bin3.path")
  val b4_path:     String             = conf.getString("output.bin4.path")
  val b1_header:   String             = conf.getString("output.bin1.header")
  val b2_header:   String             = conf.getString("output.bin2.header")
  val b3_header:   String             = conf.getString("output.bin3.header")
  val b4_header:   String             = conf.getString("output.bin4.header")
  val b1_names:    List[String]       = conf.getStringList("output.bin1.names").asScala.toList
  val b2_names:    List[String]       = conf.getStringList("output.bin2.names").asScala.toList
  val b3_names:    List[String]       = conf.getStringList("output.bin3.names").asScala.toList
  val b4_names:    List[String]       = conf.getStringList("output.bin4.names").asScala.toList
  val b1_gt:       List[Int]          = conf.getIntList("output.bin1.gt").asScala.toList.map(_.toInt)
  val b2_gt:       List[Int]          = conf.getIntList("output.bin2.gt").asScala.toList.map(_.toInt)
  val b3_gt:       List[Int]          = conf.getIntList("output.bin3.gt").asScala.toList.map(_.toInt)
  val b4_gt:       List[Int]          = conf.getIntList("output.bin4.gt").asScala.toList.map(_.toInt)
  val b1_lt:       List[Int]          = conf.getIntList("output.bin1.lt").asScala.toList.map(_.toInt)
  val b2_lt:       List[Int]          = conf.getIntList("output.bin2.lt").asScala.toList.map(_.toInt)
  val b3_lt:       List[Int]          = conf.getIntList("output.bin3.lt").asScala.toList.map(_.toInt)
  val b4_lt:       List[Int]          = conf.getIntList("output.bin4.lt").asScala.toList.map(_.toInt)
  val sTags:       List[Array[Byte]]  = conf.getStringList("input.s_tags").asScala.toList.map(_.getBytes(UTF_8))
  val eTags:       List[Array[Byte]]  = conf.getStringList("input.e_tags").asScala.toList.map(_.getBytes(UTF_8))
  val endTo:       Map[Array[Byte], Array[Byte]] = sTags.zip(eTags).toMap

  // helper method to prepare output directory for MapReduce job
  private def prepareDirectory(path: Path): Unit = {
    get(c1).mkdirs(path)                                                        // make  output directory
    if (get(c1).delete(path, true))
      log.info(s"Cleared output directory: $path")                              // clear output directory
    else
      log.trace(s"Unable to clear output directory: $path")
  }//end def prepareDirectory

  // helper method to rename MapReduce job output path
  private def renameOutput(oldPath: Path, newPath: Path): Unit =
    if (get(c1).rename(oldPath, newPath))
      log.info(s"Results successfully stored at: $newPath")                     // rename output path
    else
      log.trace(s"Unable to access path: $oldPath")

  // helper method to set job attributes
  def setJob(job: Job, jar: Class[_], inputPath: Path, outputPath: Path, inputFormat: Class[_ <: InputFormat[_, _]],
             outputFormat: Class[_ <: OutputFormat[_, _]], outputKey: Class[_], outputValue: Class[_]): Unit = {
    job.setJarByClass(jar)                                                      // set jar
    job.setInputFormatClass(inputFormat)                                        // set input  format
    job.setOutputFormatClass(outputFormat)                                      // set output format
    job.setOutputKeyClass(outputKey)                                            // set output key
    job.setOutputValueClass(outputValue)                                        // set output value
    prepareDirectory(outputPath)                                                // prepare output directory
    setInputPaths(job, inputPath)                                               // set input  path
    setOutputPath(job, outputPath)                                              // set output path
  }//end def setJob

  // helper method to emit bin counts, top 100 and bottom 100 authors
  def emitValues(c: Mapper[Text, FloatArrayWritable, Text, Text]#Context): Unit = {
    counts.foreach(kv => c.write(new Text(kv._1), new Text(s"${kv._2}")))
    authors1.foreach(kv => c.write(new Text("8," + f"${kv._1}%1.3f"), new Text(kv._2)))
    authors2.foreach(kv => c.write(new Text("9," + f"${kv._1}%1.3f"), new Text(kv._2)))
  }//end def emitValues

  // helper method to retrieve data, create charts and display results
  def processResults(path: String): Unit = {
    val oldPath = new Path(path + j1_old)                                                 // old output path
    val newPath = new Path(path + j1_new)                                                 // new output path
    renameOutput(oldPath, newPath)                                                        // rename output

    // access and group data into corresponding collections
    val s1, s2  = Source.fromInputStream(get(c1).open(newPath))                           // open  input stream
    val data    = s1.getLines.drop(s2.getLines.size - num_line).map(_.split(',')).toList  // get   relevant lines
    val ve      = data.filter(_.head.equals("3"))                                         //  (3)  venues,
    val b1      = data.filter(_.head.equals("4"))                                         //  (4)  co-authors,
    val b2      = data.filter(_.head.equals("5"))                                         //  (5)  years,
    val b3      = data.filter(_.head.equals("6"))                                         //  (6)  conferences,
    val b4      = data.filter(_.head.equals("7"))                                         //  (7)  journals,
    val a1      = data.filter(_.head.equals("8"))                                         //  (8)  top    100 authors,
    val a2      = data.filter(_.head.equals("9"))                                         //  (9)  bottom 100 authors
    s1.close; s2.close                                                                    // close input stream

    // create charts
    log.info(s"Creating charts: $b1_path, $b4_path, $b3_path, $b2_path")
    createChart(b1, b1_pt, b1_xt, b1_yt, b1_path)                                         //  (b1) co-authors,
    createChart(b4, b4_pt, b4_xt, b4_yt, b4_path)                                         //  (b4) journals,
    createChart(b3, b3_pt, b3_xt, b3_yt, b3_path)                                         //  (b3) conferences,
    createChart(b2, b2_pt, b2_xt, b2_yt, b2_path)                                         //  (b2) years

    // print results
    log.info("Tabulating results...")
    print(tabulateBins(b1), b1_title, b1_header)                                          //  (b1) co-authors,
    print(tabulateBins(b4), b4_title, b4_header)                                          //  (b4) journals,
    print(tabulateBins(b3), b3_title, b3_header)                                          //  (b3) conferences,
    print(tabulateBins(b2), b2_title, b2_header)                                          //  (b2) years,
    print(tabulateAuthors(a1), a1_title)                                                  //  (a1) top    100 authors,
    print(tabulateAuthors(a2), a2_title)                                                  //  (a2) bottom 100 authors,
    print(tabulateVenues(ve))                                                             //  (ve) venues
    println
  }//end def printResults

  // helper method to create bar chart (e.g., histogram) for bins
  private def createChart(v: List[Array[String]], t: String, xt: String, yt: String, path: String): Unit = {
    val x      = tabulate(v.size)(n => v(n)(1))
    val y      = tabulate(v.size)(n => v(n)(2).toInt)
    val data   = List(Bar(x, y, marker = Marker(color = Color.RGBA(96, 150, 187, 0.6))))
    val layout = Layout(title = t, xaxis = Axis(xt), yaxis = Axis(yt))
    Plotly.plot(path, data, layout)
  }//end def createBarChart

  // helper methods to extract and store relevant information from data into lists of tuples
  private def tabulateAuthors(v: List[Array[String]]): List[(Float, String, Int, Int, Float, Float)] =
    tabulate(v.size)(n => (v(n)(1).toFloat, v(n)(2), v(n)(3).toInt, v(n)(4).toInt, v(n)(5).toFloat, v(n)(6).toFloat))
  private def tabulateVenues(v: List[Array[String]]):  List[(String, Int, Int, Float, Float)] =
    tabulate(v.size)(n => (v(n)(1), v(n)(2).toInt, v(n)(3).toInt, v(n)(4).toFloat, v(n)(5).toFloat))
  private def tabulateBins(v: List[Array[String]]):    List[(String, Int)] =
    tabulate(v.size)(n => (v(n)(1), v(n)(2).toInt))

  // helper methods to print tabulated results for authors, venues and bins to console
  private def print(values: List[(Float, String, Int, Int, Float, Float)], title: String): Unit = {
    println; println(title);   println(a_header)
    values.foreach(v => println(format(a_values, f"${v._1}%.3f", v._2, v._3, v._4, f"${v._5}%.1f", f"${v._6}%.1f")))
  }//end def print
  private def print(values: List[(String, Int, Int, Float, Float)]): Unit = {
    println; println(v_title); println(v_header)
    values.foreach(v => println(format(v_values, v._1, v._2, v._3, f"${v._4}%.1f", f"${v._5}%.1f")))
  }//end def print
  private def print(values: List[(String, Int)], title: String, header: String): Unit = {
    println; println(title);   println(header)
    values.foreach(v => println(format(b_values, v._1, v._2)))
  }//end def print

  // collection methods to easily convert values to FloatArrayWritable
  def FloatArrayWritable(v: Array[Float]):         FloatArrayWritable = new FloatArrayWritable(v)
  def FloatArrayWritable(v: Array[Int]):           FloatArrayWritable = FloatArrayWritable(v.map(_.toFloat))
  def FloatArrayWritable(v: Array[Int], s: Float): FloatArrayWritable = FloatArrayWritable(s +: v.map(_.toFloat))
  def FloatArrayWritable(v: Float, s: Float):      FloatArrayWritable = FloatArrayWritable(Array(s, v))
  def FloatArrayWritable(v: Float):                FloatArrayWritable = FloatArrayWritable(Array(v))
}//end object Utils
