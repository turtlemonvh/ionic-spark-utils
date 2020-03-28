package com.ionic.sparkutil

import org.apache.spark.sql.functions.{ udf, col }
import org.apache.spark.sql.{ DataFrame, Column }

object Transformers {

  // Just replace with a bogus string for now until we setting on how we want to handle encoding and decoding of objects
  def EncryptObject(in: Any): String = if (in != null) { "XXXXXXX" } else { null }
  val encryptObjectUdf = udf(EncryptObject _)

  def Encrypt(colName: String)(df: DataFrame): DataFrame = {
    val newCol = s"new${colName}"
    val newdf = df.withColumn(newCol, encryptObjectUdf(col(colName)))
      .drop(colName)
      .withColumnRenamed(newCol, colName)

    newdf
  }

}