package com.ionic.sparkutil

import org.apache.spark.sql.functions.{ udf, col }
import org.apache.spark.sql.{ DataFrame, Column, Row }
import org.apache.spark.sql.types.{ StringType, StructField, StructType }
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.slf4j.LoggerFactory

import com.ionic.sdk.key.KeyServices
import com.ionic.sdk.agent.cipher.chunk.ChunkCipherV3

object Transformers {

  val logger = LoggerFactory.getLogger(this.getClass.getName)

  /*
   * Encrypt or decrypt a set of columns on a dataframe object.
   * New columns are added with prefixes "ionic_enc_" and "ionic_dec_" for encrypted and decrypted columns, respectively.
   *
   * Currently, only StringType columns are supported.
   * https://spark.apache.org/docs/2.3.0/api/scala/index.html#org.apache.spark.sql.types.DataType
   *
   *
   * TODO:
   * - add support for additional column types: binaryType, VarcharType, CharType
   */
  def Encrypt(encryptCols: List[String], decryptCols: List[String], agentFactory: () => KeyServices)(df: DataFrame): DataFrame = {
    // Grab information about the fields to be encrypted and decrypted
    val toEncrypt: Array[StructField] = df.schema.apply(encryptCols.toSet: Set[String]).fields
    val toDecrypt: Array[StructField] = df.schema.apply(decryptCols.toSet: Set[String]).fields

    // Validate types
    val validTypes = Seq(StringType)
    toEncrypt.foreach { field =>
      {
        if (!validTypes.contains(field.dataType)) {
          throw new Exception(s"Field marked for encryption '${field}' is of type '${field.dataType}' which is not in the list of valid types: ${validTypes}")
        }
      }
    }
    toDecrypt.foreach { field =>
      {
        if (!validTypes.contains(field.dataType)) {
          throw new Exception(s"Field marked for decryption '${field}' is of type '${field.dataType}' which is not in the list of valid types: ${validTypes}")
        }
      }
    }

    // Generate StructField entries for new columns post transformation
    val encryptedFields: Array[StructField] = toEncrypt.map(sf => {
      StructField(
        s"ionic_enc_${sf.name}",
        sf.dataType,
        sf.nullable,
        sf.metadata)
    })

    val decryptedFields: Array[StructField] = toDecrypt.map(sf => {
      StructField(
        s"ionic_dec_${sf.name}",
        sf.dataType,
        sf.nullable,
        sf.metadata)
    })

    // Adding on the encrypted and decrypted columns
    val newSchema = StructType(df.schema.fields ++ encryptedFields ++ decryptedFields)

    // The row transformer takes just a row and a chunk cipher
    // All transform details are captured via enclosing scope
    def transformRow(
      row: Row,
      cc: ChunkCipherV3): Row = {

      logger.debug(s"transformRow: Row schema: ${row.schema}")
      logger.debug(s"transformRow: Row seq: ${row.toSeq}")
      logger.debug(s"transformRow: Row string: ${row.toString}")

      // Take lists of string column names and turn into sets of column values

      val encryptedCols: Array[String] = encryptCols.zipWithIndex.map {
        case (colName, idx) => {
          // Get the field and type information
          val colField = encryptedFields(idx)
          val fieldVal = colField.dataType match {
            case s: StringType => row.getAs[String](colName)
          }
          // Takes bytes or string, returns string
          cc.encrypt(fieldVal)
        }
      }.toArray

      val decryptedCols: Array[String] = decryptCols.zipWithIndex.map {
        case (colName, idx) => {
          // Get the field and type information
          val colField = decryptedFields(idx)
          val fieldVal = colField.dataType match {
            case s: StringType => row.getAs[String](colName)
          }
          // Takes bytes or string, returns string
          cc.decrypt(fieldVal)
        }
      }.toArray

      Row.fromSeq(row.toSeq ++ encryptedCols ++ decryptedCols)
    }

    def transformRows(
      iter: Iterator[Row]): Iterator[Row] = {
      // Generate the agent object and the ChunkCipher in each partition
      val agent = agentFactory()
      val cc = new ChunkCipherV3(agent)
      for (row <- iter) yield transformRow(row, cc)
    }

    // Explicitly set the encoder for the new schema
    df.mapPartitions(transformRows)(RowEncoder(newSchema))

  }

}