package com.github.mideo.httpClient

import com.github.mideo.httpClient.Implicits._
import com.github.tomakehurst.wiremock.client.WireMock._

import scala.language.higherKinds


class HttpRequestFunctionalTest extends HttpIOTest {

  override def beforeAll() = mockServer.start()


  override def afterAll() = mockServer.stop()


  it should "send post request successfully" in {


    val request = HttpRequest(
      Post,
      "http://localhost:8080",
      Map("Accept" -> "application/json"),
      Payload("abc"))

    val optionResponse: Option[Either[Throwable, HttpResponse[Response]]] = request.send
    optionResponse.get.isRight should be(true)
    optionResponse.get.right.get.StatusCode should equal(404)
    optionResponse.get.right.get.Entity should equal("No response could be served as there are no stub mappings in this WireMock instance.")

  }

  it should "send post request successfully and deserialize response" in {
    mockServer.addStubMapping {
      stubFor {
        put(urlEqualTo("/foo"))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""{"value":"response"}"""))
      }
    }

    val request = HttpRequest(
      Put,
      "http://localhost:8080/foo",
      Map("Accept" -> "application/json"),
      Payload("abc"))

    val optionResponse: Option[Either[Throwable, HttpResponse[Response]]] = request.send
    optionResponse.get.isRight should be(true)
    optionResponse.get.right.get.StatusCode should equal(200)
    optionResponse.get.right.get.Entity should equal(Response("response"))

  }


}
