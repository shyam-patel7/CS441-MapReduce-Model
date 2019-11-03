/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

package com.hw2.my

import com.hw2.my.FloatArrayWritable._
import com.hw2.my.Utils._
import org.apache.hadoop.io.{ArrayWritable, FloatWritable, Text, Writable}
import scala.collection.immutable.TreeMap
import scala.math.Ordering.Float.TotalOrdering.reverse
import scala.math.Ordering.Int.{reverse => rev}
import scala.util.Sorting.quickSort


// FloatArrayWritable class, part of my package, encapsulates an array of float
//  values and is used by the Hadoop MapReduce framework as a custom data type
class FloatArrayWritable(v: Array[Float]) extends ArrayWritable(classOf[FloatWritable]) {
  // auxiliary constructor
  def this() = this(null)

  // set values array
  if (v != null) set(v)

  // helper method to set values array
  def set(v: Array[Float]): Unit =
    set(v.map(n => new FloatWritable(n)).asInstanceOf[Array[Writable]])

  // helper methods to return values stored in array
  override def toArray:  Array[Float] = get.map(_.asInstanceOf[FloatWritable].get)
  override def toString: String       = toArray.map(s => df.format(s)).mkString(",")
  def count:   Int                    = get.head.asInstanceOf[FloatWritable].get.toInt
  def authors: Array[Int]             = toArray.map(_.toInt)
  def values:  (Float, Array[Int])    = {val a = toArray; (a.head, a.drop(1).map(_.toInt))}

  // helper method to refresh values stored in array, useful when stratifying cumulative results for
  //  authors or venues venues (e.g., score, # of publications, max, median and average # of co-authors)
  def refresh(in: Int, k: Text = null): Unit = in match {
    case 0 => val (s, a) = values; val num = a.length; quickSort[Int](a)(rev)               // (0) authors,
              val stats  = Array(s, num, a.head, median(num, a), a.sum.toFloat / num)
              addAuthor(s, s"$k".drop(2) + "," + stats.drop(1).map(s => df.format(s)).mkString(","))
              set(stats)
    case 1 => addToBin(count, b3_gt, b3_lt, b3_names)                                       // (1) conferences,
    case 2 => addToBin(count, b4_gt, b4_lt, b4_names)                                       // (2) journals,
    case 3 => val a = authors;     val num = a.length; quickSort[Int](a)(rev)               // (3) venues
              set(Array(num, a.head, median(num, a), a.sum.toFloat / num))
    case _ => // do nothing
  }//end def refresh
}//end class FloatArrayWritable


// FloatArrayWritable companion object
object FloatArrayWritable {
  private var authors = new TreeMap[Float, String]()(reverse)                               // collection for authors
  private var bins    = new TreeMap[String, Int]                                            // collection for bins

  // helper method to add author to collection
  private def addAuthor(k: Float, v: String): Unit = authors += (k -> v)                    // add (k, v) to map

  // helper method to increment bin count in collection
  private def addToBin(c: Int, min: List[Int], max: List[Int], names: List[String]): Unit = (c match {
    case n if (n > min(0)) && (n < max(0)) => names(0)                                      // match count with
    case n if (n > min(1)) && (n < max(1)) => names(1)                                      //  specified bin ranges
    case n if (n > min(2)) && (n < max(2)) => names(2)
    case n if (n > min(3)) && (n < max(3)) => names(3)
    case _                                 => names(4)
  }) match {
    case bin => bins += (bin -> (bins.getOrElse(bin, 0) + 1))                               // increment count in map
  }//end def addToBin

  // return lists for collections
  def counts:   List[(String, Int)]   = bins.toList                                         // counts for bins
  def authors1: List[(Float, String)] = authors.take(a_num).toList                          // top    100 authors
  def authors2: List[(Float, String)] = authors.takeRight(a_num).toList.reverse             // bottom 100 authors

  // helper method to calculate median
  private def median(num: Int, a: Array[Int]): Float = num % 2 match {
    case 0 => val (up, down) = a.splitAt(num / 2)                                           // even : get average of 2
              (up.last + down.head).toFloat / 2                                             //         middle values
    case _ => a(num / 2)                                                                    // odd  : get middle value
  }//end def med
}//end object FloatArrayWritable
