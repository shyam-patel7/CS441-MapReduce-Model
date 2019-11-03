/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

package com.hw2.my

import com.hw2.my.CsvOutputFormat._
import com.hw2.my.Utils.{j1_base, j1_ext, newline, separator}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.{RecordWriter, TaskAttemptContext}
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat, FileOutputFormat._
import java.io.DataOutputStream


// CsvOutputFormat class, part of my package, used by the Hadoop MapReduce framework to format
//  outputs from Mapper2 into readable CSV format
class CsvOutputFormat extends FileOutputFormat[Text, Text] {
  // return record writer to write (key, value) pairs to output path with csv extension
  override def getRecordWriter(tac: TaskAttemptContext): RecordWriter[Text, Text] = {
    // set base output name
    setOutputName(tac, j1_base)

    // get default path for output format
    val path = getDefaultWorkFile(tac, j1_ext)

    // return csv record reader
    new CsvRecordWriter(path.getFileSystem(tac.getConfiguration).create(path, true))
  }//end def getRecordWriter
}//end class CsvOutputFormat


// CsvOutputFormat companion object
object CsvOutputFormat {
  // write job output (key, value) pairs to data output stream in csv format
  class CsvRecordWriter(dos: DataOutputStream) extends RecordWriter[Text, Text] {
    // data output stream
    private val out = dos

    // write (key, value) pair to output stream
    override def write(k: Text, v: Text): Unit = {
      out.write(k.getBytes, 0, k.getLength)
      out.write(separator)
      out.write(v.getBytes, 0, v.getLength)
      out.write(newline)
    }//end def write

    // close output stream
    override def close(context: TaskAttemptContext): Unit = out.close()
  }//end class CsvOutputFormat
}//end object CsvOutputFormat
