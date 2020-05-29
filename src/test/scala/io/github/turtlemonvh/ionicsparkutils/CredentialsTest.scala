package io.github.turtlemonvh.ionicsparkutils

/**
 * A simple test for for working with credentials.
 */

import org.scalatest.FunSuite

class CredentialsTest extends FunSuite {
  test("load credentials from string to create agent") {

    val profileJson = """
      {
          "activeDeviceId":"ABCD.G.f8c46c80-25e4-4504-718d-1c877806d756",
          "profiles":
          [
              {
                  "aesCdEiKey":"64eccd9219de5bf6cb8b222010b419f38d2d7087ec6b1c903df1907ba69e809c",
                  "aesCdIdcKey":"b374f6f900a7c64893075fca83a9bc4351b0f03dd345030d0457244fdb2996a5",
                  "creationTimestamp":1581544446,
                  "deviceId":"ABCD.G.f8c46c80-25e4-4504-718d-1c877806d756",
                  "name":"",
                  "server":"https://api.ionic.com"
                }
            ]
        }
      """
    val agent = Credentials.getAgent(profileJson)

    assert(agent.getAllProfiles.size == 1)
  }
}
