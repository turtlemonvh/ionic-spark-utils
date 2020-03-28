package com.ionic.sparkutil

/**
 * A simple test of a spark transformer.
 */

import com.holdenkarau.spark.testing.{ DataFrameSuiteBase }
import com.holdenkarau.spark.testing.SharedSparkContext
import org.scalatest.FunSuite

import org.apache.spark.sql.types._

class TransformersTest extends FunSuite with DataFrameSuiteBase {

  import spark.implicits._

  /*
    Apply a transformer and check the output.

    https://spark.apache.org/docs/2.3.0/
    https://spark.apache.org/docs/2.3.0/api/scala/index.html#org.apache.spark.package
    https://spark.apache.org/docs/2.3.0/api/scala/index.html#org.apache.spark.sql.Dataset
    */
  test("processKeyModifyGetRequest") {
    val inputDF = Seq(
      ("bob", "tomato"),
      ("larry", "cucumber")).toDF("name", "plant_type")

    val outputDF = inputDF.transform(Transformers.Encrypt("plant_type"))
    val columnNames = outputDF.columns
    assert(outputDF.count == 2)

    // Transform to an array for easy checking
    val output = outputDF.head(2)
    assert(output(0)(0) == "bob")
    assert(output(0)(1) == "XXXXXXX")
  }

}

