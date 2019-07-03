package com.github.mideo.httpClient

import java.net.URL

import com.github.mideo.httpClient.Implicits._

import scala.language.higherKinds


case class BarEntity(value: String)

class HttpRequestTest extends HttpIOTest {
  it should "create http request" in {
    val request: HttpRequest[BarEntity] = HttpRequest(
      Post,
      "http://foo.bar/bar",
      Map.empty,
      None,
      TimeOutOption(2000, 50000)
    )

    request.Method should equal(Post)
    request.Headers should equal(Map.empty)
    request.Options should equal(TimeOutOption(2000, 50000))
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

    request.Method should equal(Get)
    request.Headers should equal(Map.empty)
    request.Options should equal(TimeOutOption())
    request.URL should equal(new URL("http://foo.bar/bar"))
    request.Entity should equal(None)
  }


  it should "create http request with entity" in {
    val request: HttpRequest[BarEntity] = HttpRequest(
      Put,
      "http://foo.bar/bar",
      Map("Accecpt" -> "application/json"),
      Some(BarEntity("bar"))
    )

    request.Method should equal(Put)
    request.Headers should equal(Map("Accecpt" -> "application/json"))
    request.Options should equal(TimeOutOption())
    request.URL should equal(new URL("http://foo.bar/bar"))
    request.Entity.get should equal("""{"value":"bar"}""".getBytes)
  }

  it should "create http request from implicitly converted entity" in {

    val request: HttpRequest[BarEntity] = HttpRequest(
      Put,
      "http://foo.bar/bar",
      Map("Accecpt" -> "application/json"),
      BarEntity("bar")
    )

    request.Method should equal(Put)
    request.Headers should equal(Map("Accecpt" -> "application/json"))
    request.Options should equal(TimeOutOption())
    request.URL should equal(new URL("http://foo.bar/bar"))
    request.Entity.get should equal("""{"value":"bar"}""".getBytes)
  }

}
