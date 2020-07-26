package io.github.turtlemonvh.ionicsparkutils

import org.apache.spark.sql.functions.udf
import org.apache.spark.internal.Logging

import com.ionic.sdk.key.KeyServices
import com.ionic.sdk.agent.cipher.chunk.ChunkCipherV3
import scala.collection.mutable.{ Map, SynchronizedMap, HashMap }

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import collection.JavaConverters._

/*
 * Encrypt and decrypt using cached key services objects.
 *
 */
object Udfs extends Logging with Serializable {

  // Object for saving the chunk cipher along with the agent
  case class CachedAgent(agent: KeyServices, cc: ChunkCipherV3)

  // Synchronized agent pool cache
  var Agents = new ConcurrentHashMap[String, CachedAgent]

  def AddAgentWithId(agent: KeyServices, agentId: String) = {
    Agents.put(agentId, CachedAgent(agent, new ChunkCipherV3(agent)))
  }

  def getDefaultAgentId(): String = {
    Agents.keys.nextElement
  }

  def EncryptWith(in: String, agentId: String): String = {
    val agent = Agents.get(agentId)
    if (agent == null) {
      logError(s"No agent found for id: ${agentId}. Known agent ids: ${Agents.keys.asScala}")
    }
    agent.cc.encrypt(in)
  }
  def Encrypt(in: String): String = {
    EncryptWith(in, getDefaultAgentId)
  }
  val UDFEncryptWith = udf(EncryptWith _)
  val UDFEncrypt = udf(Encrypt _)

  def DecryptWith(in: String, agentId: String): String = {
    val agent = Agents.get(agentId)
    if (agent == null) {
      logError(s"No agent found for id: ${agentId}. Known agent ids: ${Agents.keys.asScala}")
    }
    agent.cc.decrypt(in)
  }
  def Decrypt(in: String): String = {
    DecryptWith(in, getDefaultAgentId)
  }
  val UDFDecryptWith = udf(DecryptWith _)
  val UDFDecrypt = udf(Decrypt _)

}