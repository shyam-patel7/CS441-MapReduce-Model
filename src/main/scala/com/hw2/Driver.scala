/* CS441 HW2: MapReduce Model for Parallel Processing of DBLP Dataset
 * Name:   Shyam Patel
 * NetID:  spate54
 * Date:   Nov 4, 2019
 */

package com.hw2

import com.hw2.my._
import com.hw2.my.Utils._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.Job.getInstance
import org.apache.hadoop.mapreduce.lib.chain.ChainMapper.{addMapper => setMapper}
import org.apache.hadoop.mapreduce.lib.chain.ChainReducer.{addMapper, setReducer}
import org.slf4j.LoggerFactory.getLogger


// Driver class that runs MapReduce model
object Driver {
  private val log = getLogger(getClass)                                             // logger

  // main driver method that takes input and output path arguments and runs MapReduce jobs
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {                                                          // paths not provided :
      log.error(path_err)                                                           //  exit with code 1
      System.exit(1)
    }//end if

    val inputPath  = new Path(args(0))                                              // input  path
    val outputPath = new Path(args(1))                                              // output path
    log.info(s"Input path: $inputPath")
    log.info(s"Output path: $outputPath")

    val j1 = getInstance(c1, j1_name)                                               // MapReduce job :
    setJob(j1, getClass, inputPath, outputPath, classOf[XmlInputFormat],            //  Mapper1, Reducer1, Mapper3
      classOf[CsvOutputFormat], classOf[Text], classOf[FloatArrayWritable])         //  [XmlInputFormat]
    setMapper(j1, classOf[Mapper1], classOf[LongWritable], classOf[Text],           //   -> [Long], [Text]
      classOf[Text], classOf[FloatArrayWritable], c2)                               //   -> [Text], [FloatArray]
    setReducer(j1, classOf[Reducer1], classOf[Text], classOf[FloatArrayWritable],   //   -> [Text], [Text]
      classOf[Text], classOf[FloatArrayWritable], c3)                               //   -> [CsvOutputFormat]
    addMapper(j1, classOf[Mapper2], classOf[Text], classOf[FloatArrayWritable],
      classOf[Text], classOf[Text], c4)

    log.info(j1_log)                                                                // run job
    if (j1.waitForCompletion(true)) {
      log.info("Job completed successfully!")
      processResults(args(1))                                                       // process results
      System.exit(0)
    } else {
      log.error("Job failed.")
      System.exit(1)
    }
  }//end def main
}//end object Driver
