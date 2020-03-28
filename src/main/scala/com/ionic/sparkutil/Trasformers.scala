package com.ionic.sparkutil

/*

  def ColAnyToStr(colName: String)(df: DataFrame): DataFrame = {
      def ArrayToStr(in: Any): String = if (in != null) { in.toString.stripPrefix("WrappedArray") } else { null }
    val newCol = s"new${colName}"
    val udfF = udf(ArrayToStr _)
    val newdf = df.withColumn(newCol, udfF(col(colName)))
      .drop(colName)
      .withColumnRenamed(newCol, colName)

    newdf
  }

*/

import org.apache.spark.sql.functions.{ udf, col }
import org.apache.spark.sql.{ DataFrame, Column }

object Transformers {

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