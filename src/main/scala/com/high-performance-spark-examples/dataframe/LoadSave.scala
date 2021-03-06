/**
 * Load and save data to/from DataFrames
 */
package com.highperformancespark.examples.dataframe

import java.util.Properties

import org.apache.spark.rdd._
import org.apache.spark.sql._
import org.apache.spark.sql.types._

case class LoadSave(sqlContext: SQLContext) {
  import sqlContext.implicits._
  //tag::createFromRDD[]
  def createFromCaseClassRDD(input: RDD[PandaPlace]) = {
    // Create DataFrame explicitly using sqlContext and schema inference
    val df1 = sqlContext.createDataFrame(input)

    // Create DataFrame using sqlContext implicits and schema inference
    val df2 = input.toDF()

    // Create a Row RDD from our RDD of case classes
    val rowRDD = input.map(pm => Row(pm.name,
      pm.pandas.map(pi => Row(pi.id, pi.zip, pi.happy, pi.attributes))))

    // Create DataFrame explicitly with specified schema
    val schema = StructType(List(StructField("name", StringType, true),
      StructField("pandas", ArrayType(StructType(List(
        StructField("id", LongType, true),
        StructField("zip", StringType, true),
        StructField("happy", BooleanType, true),
        StructField("attributes", ArrayType(FloatType), true)))))))

    val df3 = sqlContext.createDataFrame(rowRDD, schema)
  }
  //end::createFromRDD[]

  //tag::createFromLocal[]
  def createFromLocal(input: Seq[PandaPlace]) = {
    sqlContext.createDataFrame(input)
  }
  //end::createFromLocal[]

  //tag::collectResults[]
  def collectDF(df: DataFrame) = {
    val result: Array[Row] = df.collect()
    result
  }
  //end::collectResults[]

  //tag::toRDD[]
  def toRDD(input: DataFrame): RDD[RawPanda] = {
    val rdd: RDD[Row] = input.rdd
    rdd.map(row => RawPanda(row.getAs[Long](0), row.getAs[String](1),
      row.getAs[String](2), row.getAs[Boolean](3), row.getAs[Array[Double]](4)))
  }
  //end::toRDD[]

  //tag::partitionedOutput[]
  def writeOutByZip(input: DataFrame): Unit = {
    input.write.partitionBy("zipcode").format("json").save("output/")
  }
  //end::partitionedOutput[]

  //tag::saveAppend[]
  def writeAppend(input: DataFrame): Unit = {
    input.write.mode(SaveMode.Append).save("output/")
  }
  //end::saveAppend[]

  def createJDBC() = {
    //tag::createJDBC[]
    sqlContext.read.jdbc("jdbc:dialect:serverName;user=user;password=pass",
      "table", new Properties)
    sqlContext.read.format("jdbc")
      .option("url", "jdbc:dialect:serverName")
      .option("dbtable", "table").load()
    //end::createJDBC[]
  }

  def writeJDBC(df: DataFrame) = {
    //tag::writeJDBC[]
    df.write.jdbc("jdbc:dialect:serverName;user=user;password=pass",
      "table", new Properties)
    df.write.format("jdbc")
      .option("url", "jdbc:dialect:serverName")
      .option("user", "user")
      .option("password", "pass")
      .option("dbtable", "table").save()
    //end::writeJDBC[]
  }

  //tag::loadParquet[]
  def loadParquet(path: String): DataFrame = {
    // Configure Spark to read binary data as string, note: must be configured on SQLContext
    sqlContext.setConf("spark.sql.parquet.binaryAsString", "true")
    // Load parquet data using merge schema (configured through option)
    sqlContext.read
      .option("mergeSchema", "true")
      .format("parquet")
      .load(path)
  }
  //end::loadParquet[]

  //tag::writeParquet[]
  def writeParquet(df: DataFrame, path: String) = {
    df.write.format("parquet").save(path)
  }
  //end::writeParquet[]

  //tag::loadHiveTable[]
  def loadHiveTable(): DataFrame = {
    sqlContext.read.table("pandas")
  }
  //end::loadHiveTable[]

  //tag::saveManagedTable[]
  def saveManagedTable(df: DataFrame): Unit = {
    df.write.saveAsTable("pandas")
  }
  //end::saveManagedTable[]
}
