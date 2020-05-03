package com.ionic.sparkutil

/**
 * A simple test of a spark transformer.
 */

import com.holdenkarau.spark.testing.{ DataFrameSuiteBase }
import com.holdenkarau.spark.testing.SharedSparkContext
import org.scalatest.FunSuite

import com.ionic.sdk.agent.cipher.chunk.ChunkCipherV3

import org.apache.spark.sql.types._

class TransformersTest extends FunSuite with DataFrameSuiteBase {

  import spark.implicits._

  test("encrypt") {
    val inputDF = Seq(
      ("bob", "tomato"),
      ("larry", "cucumber")).toDF("name", "plant_type")

    // Note that we have to take some special steps to make this agent serializable
    // In general we will likely simply create the agent inside each partition instead of passing around
    val a = new CreateToFetchTranslation(new TestAgent())
    for (i <- 1 to 10) {
      // Create some keys
      a.createKey()
    }
    a.makeImmutable

    // Ensure that before serializing, fetch works fine (no exceptions)
    a.createKey()
    val cc = new ChunkCipherV3(a)
    cc.encrypt("hello")

    val outputDF = inputDF.transform(Transformers.Encrypt(
      encryptCols = List("plant_type"),
      decryptCols = List[String](),
      agentFactory = () => { a }))

    // For now we just check that the data is the correct shape
    val columnNames = outputDF.columns
    outputDF.show
    assert(outputDF.count == 2)
    assert(outputDF.columns.size == 3)
  }

}

