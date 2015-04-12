/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.spark.streaming.{Seconds, StreamingContext}
import StreamingContext._
import org.apache.spark.SparkContext._
import org.apache.spark.streaming.twitter._
import org.apache.spark.SparkConf

/**
 * Calculates popular hashtags (topics) over sliding 10 and 60 second windows from a Twitter
 * stream. The stream is instantiated with credentials and optionally filters supplied by the
 * command line arguments.
 *
 * Run this on your local machine as
 *
 */
object TwitterPopularTags {
  def main(args: Array[String]) {
    /*if (args.length < 4) {
      System.err.println("Usage: TwitterPopularTags <consumer key> <consumer secret> " +
        "<access token> <access token secret> [<filters>]")
      System.exit(1)
    }*/
    val interval = 1800
    StreamingExamples.setStreamingLogLevels()

    //val Array(consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.take(4)
    val filters = args.takeRight(args.length)

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generat OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", "ie9GWvxEjTxuPxkI40OgDhgZH")
    System.setProperty("twitter4j.oauth.consumerSecret", "CKhLtEYLoysT6sFe3C8EszW4uIWGdZEJ2WP5vLjdYt88Qv43S6")
    System.setProperty("twitter4j.oauth.accessToken", "2810299994-SbwhMFa2RP9zcshrHoeoZhqamtxSlbH60VKiAGa")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "0QYwhfhRzPT7mM48If2vlFoB9g0kgbTbRfK5aUnYs55VE")

    val sparkConf = new SparkConf().setAppName("TwitterPopularTags")
    val ssc = new StreamingContext(sparkConf, Seconds(5))
    val stream = TwitterUtils.createStream(ssc, None, filters)

    val hashTags = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))

    val topCounts = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(interval))
                     .map{case (topic, count) => (count, topic)}
                     .transform(_.sortByKey(false))

    /*val topCounts10 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(10))
                     .map{case (topic, count) => (count, topic)}
                     .transform(_.sortByKey(false))*/


    // Print popular hashtags
    topCounts.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println(("\nPopular topics in last " + interval.toString + " seconds (%s total):").format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })

    /*topCounts10.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nPopular topics in last 10 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })*/

    ssc.start()
    ssc.awaitTermination()
  }
}
