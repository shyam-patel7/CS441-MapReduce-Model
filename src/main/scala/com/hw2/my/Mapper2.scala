/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

package com.hw2.my

import com.hw2.my.Mapper2._
import com.hw2.my.Utils.emitValues
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Mapper
import org.slf4j.LoggerFactory.getLogger


// Mapper2 class, part of my package, used by the Hadoop MapReduce framework in job #1 to map
//  Reducer1 outputs into [Text, FloatArray] (k, v) pairs with stats set for authors and venues
class Mapper2 extends Mapper[Text, FloatArrayWritable, Text, Text] {
  // map intermediate (key, value) pairs to set of (key, value) pairs
  override def map(k: Text, v: FloatArrayWritable,
                   c: Mapper[Text, FloatArrayWritable, Text, Text]#Context): Unit =
  (k.charAt(0) match {                                                    // map value :
    case '0'     => v.refresh(in = 0, k); v                               //  (0) authors,    (1) conferences,
    case '1'     => v.refresh(in = 1); v                                  //  (2) journals,   (3) venues,
    case '2'     => v.refresh(in = 2); v                                  //  (4) co-authors, (5) years
    case '3'     => v.refresh(in = 3); v
    case _       => v
  }) match {                                                              // emit (k, v) pair
    case v => emitKeyValue(k, new Text(s"$v"), c)
  }//end def map

  // emit remaining (k, v) pairs
  override def cleanup(c: Mapper[Text, FloatArrayWritable, Text, Text]#Context): Unit = emitValues(c)
}//end class Mapper2


// Mapper2 companion object
object Mapper2 {
  // logger
  private val log = getLogger(getClass)

  // helper method to emit single (key, value) pair
  private def emitKeyValue(key: Text, value: Text,
                           c: Mapper[Text, FloatArrayWritable, Text, Text]#Context): Unit = {
    log.info(s"Mapper emit: ($key -> $value)")
    c.write(key, value)
  }//end def emit
}//end object Mapper3
