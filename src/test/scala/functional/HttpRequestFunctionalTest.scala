package functional

import com.github.mideo.httpClient.Implicits._
import com.github.mideo.httpClient.{HttpIOTest, HttpRequest, HttpResponse, JsonHttpRequest, Payload, Post, Put, Response, XmlHttpRequest}
import com.github.tomakehurst.wiremock.client.WireMock._

import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.higherKinds


class HttpRequestFunctionalTest extends HttpIOTest {


  override def beforeAll(): Unit = mockServer.start()


  override def afterAll(): Unit = mockServer.stop()


  it should "send request post Future" in {
    implicit def bind[T]: T => Future[T] = t => Future(t)

    val request = HttpRequest(
      Post,
      "http://localhost:8080/lalal",
      Map("Accept" -> "application/xml"),
      Payload("abc"))
    val future: Future[Either[Throwable, HttpResponse[Response]]] = request.send
    val eitherResponse: Either[Throwable, HttpResponse[Response]] = result(future, 5 seconds)


    eitherResponse.isRight should be(true)
    eitherResponse.right.get.StatusCode should equal(404)
    eitherResponse.right.get.Entity should equal("No response could be served as there are no stub mappings in this WireMock instance.")

  }

  it should "send post request successfully" in {
    val request = JsonHttpRequest(
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

  it should "send post request successfully and deserialize xml response" in {
    mockServer.addStubMapping {
      stubFor {
        put(urlEqualTo("/foo"))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/xml")
            .withBody("<xml><value>response</value></xml>"))
      }
    }

    val request = XmlHttpRequest(
      Put,
      "http://localhost:8080/foo",
      Map("Accept" -> "application/xml"),
      Payload("abc"))

    val optionResponse: Option[Either[Throwable, HttpResponse[Response]]] = request.send
    optionResponse.get.isRight should be(true)
    optionResponse.get.right.get.StatusCode should equal(200)
    optionResponse.get.right.get.Entity should equal(Response("response"))

  }


}
