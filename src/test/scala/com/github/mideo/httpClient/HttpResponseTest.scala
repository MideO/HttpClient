package com.github.mideo.httpClient

import com.fasterxml.jackson.databind.ObjectMapper


case class EntityFoo(message: String, value: String)

class HttpResponseTest extends HttpClientTest {

  it should "be creatable from json" in {
    implicit val Mapper: ObjectMapper = Mappers.JsonMapper
    val response: HttpResponse[EntityFoo] = HttpResponse(
      200,
      Map("location" -> "http://foo.abr"),
      """{"message":"aMessage", "value": "aValue"}""".getBytes
    )
    response.StatusCode should equal(200)
    response.Headers("location") should equal("http://foo.abr")
    response.Entity should equal(EntityFoo("aMessage", "aValue"))
  }

  it should "be creatable from xml" in {
    implicit val Mapper: ObjectMapper = Mappers.XmlMapper
    val response: HttpResponse[EntityFoo] = HttpResponse(
      200,
      Map("location" -> "http://foo.abr"),
      "<EntityFoo><message>aMessage</message><value>aValue</value></EntityFoo>".getBytes
    )
    response.StatusCode should equal(200)
    response.Headers("location") should equal("http://foo.abr")
    response.Entity should equal(EntityFoo("aMessage", "aValue"))
  }

  it should "handle non parsible entity" in {
    implicit val Mapper: ObjectMapper = Mappers.JsonMapper
    val response: HttpResponse[String] = HttpResponse(
      200,
      Map("location" -> "http://foo.abr"),
      "A String that cannot be parsed".getBytes
    )
    response.StatusCode should equal(200)
    response.Headers("location") should equal("http://foo.abr")
    response.Entity should equal("A String that cannot be parsed")
  }
}
