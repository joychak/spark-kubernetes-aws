package com.datalogs

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession

object WordCount {

  def main(args: Array[String]): Unit = {
    val sc: SparkContext = SparkSession
      .builder
      .getOrCreate
      .sparkContext

    //val textFile = sc.textFile("/Users/joychak/joy/workspace/repo/vagrant-kubeadm/Vagrantfile")
    val textFile = sc.textFile("s3a://test-bucket/Vagrantfile")

    val counts = textFile.flatMap(line => line.split(" "))
      .map(word => (word, 1))
      .reduceByKey(_ + _)

    println(s"Total number of words = ${counts.count}")
  }
}
