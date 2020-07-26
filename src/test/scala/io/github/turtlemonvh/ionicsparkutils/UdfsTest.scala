package io.github.turtlemonvh.ionicsparkutils

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.scalatest.FunSuite
import org.apache.spark.sql.types._

/*
 * Test encryption and decryption operations with udf operations using cached key services objects.
 */
class UdfsTest extends FunSuite with DataFrameSuiteBase {
  import spark.implicits._

  test("encrypt") {
    // Create df and put into sql view
    val inputDF = Seq(
      ("bob", "tomato"),
      ("larry", "cucumber")).toDF("name", "plant_type")
    inputDF.createOrReplaceTempView("veggietales")

    // Pre-seed agent cache
    val a = new KeyServicesCache(new TestAgent())
    for (i <- 1 to 10) {
      a.createKey()
    }
    a.makeImmutable

    // Populate the agent cache
    Udfs.AddAgentWithId(a, "testAgent")

    // Populate the udf function registry
    spark.udf.register("ionicenc", Udfs.UDFEncrypt)
    spark.udf.register("ionicencw", Udfs.UDFEncryptWith)
    spark.udf.register("ionicdec", Udfs.UDFDecrypt)
    spark.udf.register("ionicdecw", Udfs.UDFDecryptWith)

    // Encrypt
    val encDF = spark.sql("SELECT name, ionicencw(plant_type, 'testAgent') as encrypted_plant_type FROM veggietales")
    encDF.show

    // Encrypt with default
    val encDefDF = spark.sql("SELECT name, ionicenc(plant_type) as encrypted_plant_type FROM veggietales")
    encDefDF.show

    // Save encrypted view for use in decrypt tests
    encDF.createOrReplaceTempView("enc_veggietales")

    // Decrypt
    val decDF = spark.sql("SELECT name, ionicdecw(encrypted_plant_type, 'testAgent') as encrypted_plant_type FROM enc_veggietales")
    decDF.show

    // Decrypt with default
    val decDefDF = spark.sql("SELECT name, ionicdec(encrypted_plant_type) as encrypted_plant_type FROM enc_veggietales")
    decDefDF.show

  }
}