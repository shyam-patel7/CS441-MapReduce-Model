/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

package com.hw2.my

import com.hw2.my.XmlInputFormat._
import com.hw2.my.Utils.{endTo, sTags}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.{DataOutputBuffer, LongWritable, Text}
import org.apache.hadoop.mapreduce.{InputSplit, RecordReader, TaskAttemptContext}
import org.apache.hadoop.mapreduce.lib.input.{FileSplit, TextInputFormat}
import scala.annotation.tailrec


// XmlInputFormat class, part of my package, used by the Hadoop MapReduce framework to split and
//  format the DBLP xml dataset into independent (k, v) pairs that can be sent as input to Mapper1
class XmlInputFormat extends TextInputFormat {
  // return custom record reader for given input split
  override def createRecordReader(is: InputSplit, tac: TaskAttemptContext): RecordReader[LongWritable, Text] =
    new XmlRecordReader(is.asInstanceOf[FileSplit], tac.getConfiguration)
}//end class XmlInputFormat


// XmlInputFormat companion object
object XmlInputFormat {
  // XmlRecordReader class to convert byte-oriented view of input split and process record boundaries
  class XmlRecordReader(fs: FileSplit, c: Configuration) extends RecordReader[LongWritable, Text] {
    private val start   = fs.getStart                                               // start position
    private val end     = start + fs.getLength                                      // end   position
    private val fsin    = fs.getPath.getFileSystem(c).open(fs.getPath)              // input stream
    private val buf     = new DataOutputBuffer                                      // buffer
    private val currKey = new LongWritable                                          // current key
    private val currVal = new Text                                                  // current value

    override def initialize(is: InputSplit, tac: TaskAttemptContext): Unit          // on initialize,
                                               = fsin.seek(start)                   //  seek input stream to start
    override def getCurrentKey:   LongWritable = currKey                            // return current key
    override def getCurrentValue: Text         = currVal                            // return current value
    override def getProgress:     Float        = (fsin.getPos - start) /            // return progress as
                                                 (end - start).toFloat              //  current position of total
    override def close():         Unit         = fsin.close()
    override def nextKeyValue:    Boolean      = {                                  // read next (k, v) pair :
      if (fsin.getPos < end) {                                                      //  scan input stream until
        readUntilMatch(sTags, withinBlock = false) match {                          //   start tag is found
          case None        => // do nothing
          case Some(sTag)  => try {
            buf.write(sTag)                                                         //  store start tag in buffer
            readUntilMatch(List(endTo(sTag)), withinBlock = true) match {          //  scan until end tag is found
              case None    => // do nothing
              case Some(_) => currKey.set(fsin.getPos)                              //  set and return (k, v) pair
                              currVal.set(buf.getData, 0, buf.getLength)
                              return true
            }//end match
          } finally buf.reset                                                       //  reset buffer
        }//end match
      }; false
    }//end def nextKeyValue

    // private helper method to scan input stream until matched tag is found
    private def readUntilMatch(tags: List[Array[Byte]], withinBlock: Boolean): Option[Array[Byte]] = {
      @tailrec def continually(execute: => Unit): Array[Byte] = {                   // tail recursive helper :
        execute                                                                     //  execute
        continually(execute)                                                        //  recursive call
      }//end def continually

      val c = tags.indices.map(_ => 0).toArray                                      // initialize counts to 0
      continually {                                                                 // call tail recursive helper
        val currByte = fsin.read()                                                  // read  byte
        if (currByte == -1) return None                                             // return if reached EOF
        if (withinBlock) buf.write(currByte)                                        // write byte to buffer

        tags.indices.foreach { i => val tag = tags(i)                               // for each tag :
          if (currByte == tag(c(i))) {                                              //  tag byte matches current byte :
            c(i) += 1                                                               //   increment count
            if (c(i) >= tag.length) return Some(tag)                                //   return matched end tag
          } else c(i) = 0                                                           //  reset count if tag not matched
        }; if (!withinBlock && c.forall(_ == 0) && fsin.getPos >= end) return None  // return if reached end of block
      }; None                                                                       // return if no match found
    }//end def readUntilMatch
  }//end class XmlRecordReader
}//end object XmlInputFormat
