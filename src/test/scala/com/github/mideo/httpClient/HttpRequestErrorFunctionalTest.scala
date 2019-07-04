package com.github.mideo.httpClient

import java.net.ConnectException

import com.github.mideo.httpClient.Implicits._

import scala.language.higherKinds

case class Payload(value: String)

case class Response(value: String)

class HttpRequestErrorFunctionalTest extends HttpIOTest {

  it should "send post request with Option Monad" in {
    val request = HttpRequest(
      Post,
      "http://localhost:8090/foo",
      Map("Accept" -> "application/json"),
      Payload("abc"))

    val optionResponse: Option[Either[Throwable, HttpResponse[Response]]] = request.send

    optionResponse.get.left.get.isInstanceOf[ConnectException] should be(true)
  }

}
