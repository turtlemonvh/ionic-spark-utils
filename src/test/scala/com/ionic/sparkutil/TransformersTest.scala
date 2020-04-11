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

  test("encrypt") {
    val inputDF = Seq(
      ("bob", "tomato"),
      ("larry", "cucumber")).toDF("name", "plant_type")

    // Note that passing in a shared keystore for the testagent doesn't work bc the testagent is not serializable
    // FIXME: We need to do this better so we can encrypt AND decrypt
    val outputDF = inputDF.transform(Transformers.Encrypt(
      encryptCols = List("plant_type"),
      decryptCols = List[String](),
      agentFactory = () => { new TestAgent() }))

    // For now we just check that the data is the correct shape
    val columnNames = outputDF.columns
    outputDF.show
    assert(outputDF.count == 2)
    assert(outputDF.columns.size == 3)
  }

}

