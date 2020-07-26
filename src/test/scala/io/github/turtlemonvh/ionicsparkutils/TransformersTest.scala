package io.github.turtlemonvh.ionicsparkutils

/**
 * A simple test of a spark transformer.
 */

import com.holdenkarau.spark.testing.DataFrameSuiteBase
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
    val a = new KeyServicesCache(new TestAgent())
    for (i <- 1 to 10) {
      // Create some keys
      a.createKey()
    }
    a.makeImmutable

    // Ensure that before serializing, fetch works fine (no exceptions)
    a.createKey()
    val cc = new ChunkCipherV3(a)
    cc.encrypt("hello")

    inputDF.printSchema
    val encryptedDF = inputDF.transform(Transformers.Encrypt(
      encryptCols = List("plant_type"),
      decryptCols = List(),
      agentFactory = () => { a }))

    // For now we just check that the data is the correct shape
    encryptedDF.persist
    encryptedDF.show
    encryptedDF.printSchema
    assert(encryptedDF.count == 2)
    assert(encryptedDF.columns.size == 3)

    // Decrypt
    // FIXME: Seems to fail when not calling persist on the first dataframe
    val decryptedDF = encryptedDF
      .drop("plant_type")
      .withColumnRenamed("ionic_enc_plant_type", "plant_type")
      .transform(Transformers.Encrypt(
        encryptCols = List(),
        decryptCols = List("plant_type"),
        agentFactory = () => { a }))

    // Check shape
    decryptedDF.persist
    decryptedDF.show
    decryptedDF.printSchema
    println(decryptedDF.take(1)(0).schema)
    assert(decryptedDF.count == 2)
    assert(decryptedDF.columns.size == 3)

  }

}

