/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

package com.hw2.my

import com.hw2.my.Reducer1._
import com.hw2.my.Utils._
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Reducer
import org.slf4j.LoggerFactory.getLogger
import scala.jdk.CollectionConverters.IterableHasAsScala
import java.lang.Iterable


// Reducer1 class, part of my package, used by the Hadoop MapReduce framework to reduce Mapper1
//  map-outputs into intermediate [Text, IntArray] (k, v) pairs to stratify results for authors
//  and venues, and to sum counts for conferences, journals, # of co-authors and years
class Reducer1 extends Reducer[Text, FloatArrayWritable, Text, FloatArrayWritable] {
  // reduce set of intermediate values which share key to smaller set of values
  override def reduce(k: Text, v: Iterable[FloatArrayWritable],
                      c: Reducer[Text, FloatArrayWritable, Text, FloatArrayWritable]#Context): Unit =
    (k.charAt(0) match {                                                  // reduce values :
      case '0' => val (s, a)  = v.asScala.map(_.values).unzip             //  (0) authors,    (1) conferences,
                  val authors = a.flatten.toArray                         //  (2) journals,   (3) venues,
                  FloatArrayWritable(authors, s.sum)                      //  (4) co-authors, (5) years
      case '3' => val authors = v.asScala.flatMap(_.authors).toArray
                  FloatArrayWritable(authors)
      case _   => FloatArrayWritable(v.asScala.map(_.count).sum)
    }) match {
      case v   => emitKeyValue(k, v, c)                                   // emit (k, v) pair
    }//end def reduce
}//end class Reducer1


// Reducer1 companion object
object Reducer1 {
  // logger
  private val log = getLogger(getClass)

  // helper method to emit single (key, value) pair
  private def emitKeyValue(key: Text, value: FloatArrayWritable,
                           c: Reducer[Text, FloatArrayWritable, Text, FloatArrayWritable]#Context): Unit = {
    log.info(s"Reducer emit: ($key -> $value)")
    c.write(key, value)
  }//end def emitKeyValue
}//end object Reducer1
