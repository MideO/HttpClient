package com.github.mideo.httpClient

import java.net.ConnectException

import com.github.mideo.httpClient.Implicits._

import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.higherKinds

case class Payload(value: String)

case class Response(value: String)

class HttpRequestErrorFunctionalTest extends HttpIOTest {

  it should "send request post Future monad" in {
    implicit def bind[T]: T => Future[T] = t => Future(t)

    val request = HttpRequest(
      Post,
      "http://localhost:8090/foo",
      Map("Accept" -> "application/json"),
      Payload("abc"))
    val future: Future[Either[Throwable, HttpResponse[Response]]] = request.send
    val eitherResponse: Either[Throwable, HttpResponse[Response]] = result(future, 5 seconds)


    eitherResponse.isLeft should be(true)
    the[ConnectException] thrownBy {
      throw eitherResponse.left.get
    } should have message "Connection refused"

  }

  it should "send post request with Option Monad" in {
    val request = HttpRequest(
      Post,
      "http://localhost:8090/foo",
      Map("Accept" -> "application/json"),
      Payload("abc"))

    val optionResponse: Option[Either[Throwable, HttpResponse[Response]]] = request.send
    the[ConnectException] thrownBy {
      throw optionResponse.get.left.get
    } should have message "Connection refused"

  }

}
