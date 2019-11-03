/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

package com.hw2.my

import com.hw2.my.Mapper1._
import com.hw2.my.Utils._
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.Mapper
import org.slf4j.LoggerFactory.getLogger
import scala.collection.immutable.List.tabulate
import scala.collection.Seq
import scala.xml.Node
import scala.xml.XML.withSAXParser


// Mapper1 class, part of my package, used by the Hadoop MapReduce framework to map text inputs from
//  the DBLP xml dataset into independent [Text, FloatArray] (k, v) pairs for further processing
class Mapper1 extends Mapper[LongWritable, Text, Text, FloatArrayWritable] {
  // map input (key, value) pairs to set of intermediate (key, value) pairs
  override def map(k: LongWritable, v: Text, c: Mapper[LongWritable, Text, Text, FloatArrayWritable]#Context): Unit = {
    val pubElem = withSAXParser(saxParser).loadString(s"""<?xml version="1.0" encoding="ISO-8859-1"?>
      |<!DOCTYPE dblp SYSTEM "${getClass.getClassLoader.getResource(dtd_res).toURI}"><dblp>$v</dblp>""".stripMargin)
                                                                                          // xml publication element
    val venue = pubElem.child.head.label                                                  // publication venue
    val names = (pubElem \\ authorOrEditor(venue)).map(_.text)                            // names of co-authors

    if (names.nonEmpty) {
      val num_ca = names.size                                                             // # of co-authors
      val scores = calculateScores(num_ca)                                                // authorship scores
                                                                                          // emit (k, v) pairs :
      (0 until num_ca).foreach(a => emitAuthorKeyValue(names(a), num_ca, scores(a), c))   //  (0) authors,
      matchVenueAndEmit(venue, num_ca, c)                                                 //  (3) venues,
      matchBinAndEmit(num_ca, b1_gt, b1_lt, b1_names, c)                                  //  (4) # of co-authors,
    }//end if

    if (isConference(venue))
      emitConferenceKey(pubElem.child.head.attribute("key"), c)                           //  (1) conferences,
    if ((pubElem \\ "journal").nonEmpty)
      emitJournalKey((pubElem \\ "journal").head.text, c)                                 //  (2) journals,
    if ((pubElem \\ "year").nonEmpty)
      matchBinAndEmit((pubElem \\ "year").head.text.toInt, b2_gt, b2_lt, b2_names, c)     //  (5) years
  }//end def map
}//end class Mapper1


// Mapper1 companion object
object Mapper1 {
  // logger
  private val log = getLogger(getClass)

  // helper method to determine whether to look for author or editor
  private def authorOrEditor(venue: String): String = venue match {
    case "book" | "proceedings" => "editor"
    case _                      => "author"
  }//end def authorOrEditor

  // helper method to determine whether venue is type conference
  private def isConference(venue: String): Boolean = venue match {
    case "inproceedings" | "proceedings" => true
    case _                               => false
  }//end def isConference

  // helper method to calculate authorship scores using # of co-authors in publication
  private def calculateScores(num_ca: Int): List[Float] = {
    val base = 1F / num_ca                    // base score
    val adj  = 1F / (4 * num_ca)              // adjustment
    tabulate(num_ca)(a => {
      if      (num_ca == 1)     base          //  one  author -> base score
      else if (a == 0)          base + adj    //  1st  author -> debit  adjustment
      else if (a == num_ca - 1) base - adj    //  last author -> credit adjustment
      else                      base          //  all others  -> base score
    })//end tabulate
  }//def calculateScores

  // helper method to match value with one of 5 pre-configured bins, the specifications of
  //  which are determined by application.conf, and emit corresponding (key, value) pair
  private def matchBinAndEmit(value: Int, min: List[Int], max: List[Int], names: List[String],
                              c: Mapper[LongWritable, Text, Text, FloatArrayWritable]#Context): Unit = (value match {
    case v if (v > min(0)) && (v < max(0)) => names(0)
    case v if (v > min(1)) && (v < max(1)) => names(1)
    case v if (v > min(2)) && (v < max(2)) => names(2)
    case v if (v > min(3)) && (v < max(3)) => names(3)
    case _                                 => names(4)
  }) match {    // emit (k, v) pair with key bin and value one
    case bin => emitKeyValue(bin, one, c)
  }//end def matchBinAndEmit

  // helper method to match venue with one of 7 venue types, the specifications of which
  //  are established by DBLP, and emit (key, value) pair where value is # of coauthors
  private def matchVenueAndEmit(venue: String, num_ca: Int,
                                c: Mapper[LongWritable, Text, Text, FloatArrayWritable]#Context): Unit = (venue match {
    case v if v == v_labels(0) => Some(v_names(0))
    case v if v == v_labels(1) => Some(v_names(1))
    case v if v == v_labels(2) => Some(v_names(2))
    case v if v == v_labels(3) => Some(v_names(3))
    case v if v == v_labels(4) => Some(v_names(4))
    case v if v == v_labels(5) => Some(v_names(5))
    case v if v == v_labels(6) => Some(v_names(6))
    case _                     => None
  }) match {
    case None        => // do nothing, otherwise emit (k, v) pair with key venue
    case Some(venue) => emitKeyValue(venue, FloatArrayWritable(num_ca), c)
  }//end def matchVenueAndEmit

  // helper method to emit (key, value) pair, where key represents author name
  //  and value contains: (1) authorship score, (2) # of co-authors
  private def emitAuthorKeyValue(name: String, num_ca: Int, score: Float,
                                 c: Mapper[LongWritable, Text, Text, FloatArrayWritable]#Context): Unit =
    emitKeyValue("0," + s"${name.replaceAll(commas, withNothing)}", FloatArrayWritable(num_ca, score), c)

  // helper method to emit (key, value) pair for conference with value one
  private def emitConferenceKey(k: Option[Seq[Node]],
                                c: Mapper[LongWritable, Text, Text, FloatArrayWritable]#Context): Unit = k match {
    case None    => // do nothing, otherwise check for conference or journal key
    case Some(v) => v.text match {
      case ofConf(k)    => emitKeyValue("1," + s"$k", one, c)
      case ofJournal(k) => emitJournalKey(k, c)
      case _            => // do nothing
    }//end match
  }//end def emitConferenceKey

  // helper method to emit (key, value) pair for journal with value one
  private def emitJournalKey(name: String, c: Mapper[LongWritable, Text, Text, FloatArrayWritable]#Context): Unit =
    emitKeyValue("2," + s"${name.replaceAll(commas, withNothing)}", one, c)

  // helper method to emit single (key, value) pair
  private def emitKeyValue(key: String, value: FloatArrayWritable,
                           c: Mapper[LongWritable, Text, Text, FloatArrayWritable]#Context): Unit = {
    log.info(s"Mapper emit: ($key -> $value)")
    c.write(new Text(key), value)
  }//end def emitKeyValue
}//end object Mapper1
