package com.github.mideo.httpClient


case class EntityFoo(message: String, value: String)

class HttpResponseTest extends HttpIOTest {

  it should "Should be creatable" in {
    val response: HttpResponse[EntityFoo] = HttpResponse(
      200,
      Map("location" -> "http://foo.abr"),
      """{"message":"aMessage", "value": "aValue"}""".getBytes
    )
    response.StatusCode should equal(200)
    response.Headers("location") should equal("http://foo.abr")
    response.Entity should equal(EntityFoo("aMessage", "aValue"))
  }

  it should "handle non parsible entity" in {
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
