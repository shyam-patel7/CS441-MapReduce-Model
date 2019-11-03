/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

import com.hw2.my._
import com.typesafe.config.ConfigFactory.load
import org.apache.hadoop.io.{FloatWritable, Writable}
import org.scalatest.{FlatSpec, Matchers}
import scala.jdk.CollectionConverters.IterableHasAsScala


// MapReduceTests class consisting of 8 MapReduce unit tests based on the ScalaTest testing framework
class MapReduceTests extends FlatSpec with Matchers {
  // TESTS 1-10: read from app configuration
  "Configuration" should "have DBLP Dataset Processing job" in {
    load.getString("job1_name") should be ("DBLP Dataset Processing")
  }
  it should "have correct extension for output" in {
    load.getString("job1_ext") should be (".csv")
  }
  it should "have path error message" in {
    load.getString("path_err") should be ("Input and output paths not provided. Exiting...")
  }
  it should "have correct start tags" in {
    load.getStringList("input.s_tags").asScala.toList should be (List
    ("<article ","<inproceedings ","<proceedings ","<book ","<incollection ","<phdthesis ","<mastersthesis "))
  }
  it should "have correct end tags" in {
    load.getStringList("input.e_tags").asScala.toList should be (List
    ("</article>","</inproceedings>","</proceedings>","</book>","</incollection>","</phdthesis>","</mastersthesis>"))
  }
  it should "have correct number of relevant lines" in {
    load.getInt("output.num_line") should be (225)
  }
  it should "have bin names for number of co-authors" in {
    load.getStringList("output.bin1.names").asScala.toList should be (List
    ("4, 1 co-author","4, 2-3 co-authors","4, 4-6 co-authors","4, 7-9 co-authors","4,10+ co-authors"))
  }
  it should "have bin minimum values for number of conferences" in {
    load.getIntList("output.bin3.gt").asScala.toList.map(_.toInt) should be (List (0,199,599,1199))
  }
  it should "have bin maximum values for years" in {
    load.getIntList("output.bin2.lt").asScala.toList.map(_.toInt) should be (List (1980,1990,2000,2010))
  }
  it should "have labels for stratification of venues" in {
    load.getStringList("output.venue.labels").asScala.toList should be (List
    ("article","inproceedings","proceedings","book","incollection","phdthesis","mastersthesis"))
  }

  // TESTS 11-14: FloatArrayWritable creation
  "FloatArrayWritable" should "contain an array of floats" in {
    new FloatArrayWritable(Array(15,60,102)).toArray should equal (Array(15F,60F,102F))
  }
  it should "return string" in {
    new FloatArrayWritable(Array(0.52F,1,6)).toString should equal ("0.52,1,6")
  }
  it should "return count" in {
    new FloatArrayWritable(Array(305)).toArray.head should equal (305)
  }
  it should "allow setting new values" in {
    val a = new FloatArrayWritable(Array(777F))
    a.set(Array(4F,4,1).map(n => new FloatWritable(n)).asInstanceOf[Array[Writable]])
    a.toArray should equal (Array(4F,4,1))
  }
}//end class MapReduceTests
