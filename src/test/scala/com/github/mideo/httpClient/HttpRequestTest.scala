package com.github.mideo.httpClient

import java.net.URL

import com.github.mideo.httpClient.Implicits._

import scala.io.Source
import scala.language.higherKinds


case class BarEntity(value: String)

class HttpRequestTest extends HttpClientTest {
  it should "create http request" in {
    val request: HttpRequest[BarEntity] = HttpRequest(
      Post,
      "http://foo.bar/bar",
      Map.empty,
      None,
      TimeOutOption(2000, 50000)
    )

    request.method should equal(Post)
    request.Headers should equal(Map.empty)
    request.timeOutOptions should equal(TimeOutOption(2000, 50000))
    request.URL should equal(new URL("http://foo.bar/bar"))
    request.Entity should equal(None)
  }

  it should "create http request with default timeout" in {
    val request: HttpRequest[BarEntity] = HttpRequest(
      Get,
      "http://foo.bar/bar",
      Map.empty,
      None
    )

    request.method should equal(Get)
    request.Headers should equal(Map.empty)
    request.timeOutOptions should equal(TimeOutOption())
    request.URL should equal(new URL("http://foo.bar/bar"))
    request.Entity should equal(None)
  }


  it should "create http request with entity" in {
    val request: HttpRequest[BarEntity] = HttpRequest(
      Put,
      "http://foo.bar/bar",
      Map("Accept" -> "application/json"),
      Some(BarEntity("bar"))
    )

    request.method should equal(Put)
    request.Headers should equal(Map("Accept" -> "application/json"))
    request.timeOutOptions should equal(TimeOutOption())
    request.URL should equal(new URL("http://foo.bar/bar"))
    request.Entity.get should equal("""{"value":"bar"}""".getBytes)
  }

  it should "create http request from implicitly converted entity" in {

    val request: HttpRequest[BarEntity] = HttpRequest(
      Put,
      "http://foo.bar/bar",
      Map("Accept" -> "application/json"),
      BarEntity("bar")
    )

    request.method should equal(Put)
    request.Headers should equal(Map("Accept" -> "application/json"))
    request.timeOutOptions should equal(TimeOutOption())
    request.URL should equal(new URL("http://foo.bar/bar"))
    request.Entity.get should equal("""{"value":"bar"}""".getBytes)
  }

  it should "create json http request" in {
    val request: JsonHttpRequest[BarEntity] = JsonHttpRequest(
      Put,
      "http://foo.bar/bar",
      Map("Accept" -> "application/json"),
      BarEntity("bar")
    )

    request.method should equal(Put)
    request.Headers should equal(Map(
      "Accept" -> "application/json",
      "Content-Type" -> "application/json",
    ))
    request.timeOutOptions should equal(TimeOutOption())
    request.URL should equal(new URL("http://foo.bar/bar"))
    request.Entity.get should equal("""{"value":"bar"}""".getBytes)
  }

  it should "create xml request" in {
    val request: XmlHttpRequest[BarEntity] = XmlHttpRequest(
      Put,
      "http://foo.bar/bar",
      Map("Accept" -> "application/xml"),
      BarEntity("bar")
    )

    request.method should equal(Put)
    request.Headers should equal(Map(
      "Accept" -> "application/xml",
      "Content-Type" -> "application/xml",
    ))
    request.timeOutOptions should equal(TimeOutOption())
    request.URL should equal(new URL("http://foo.bar/bar"))
    Source.fromBytes(request.Entity.get.toArray).mkString should equal("<BarEntity><value>bar</value></BarEntity>")
  }

}
