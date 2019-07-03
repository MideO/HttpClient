package com.github.mideo.httpClient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

trait HttpIOTest
  extends FlatSpec
    with BeforeAndAfterAll
    with MockitoSugar
    with Matchers {

  protected val mockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8080))
}
