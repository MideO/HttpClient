package com.github.mideo.httpClient

import java.io.ByteArrayInputStream
import java.net.{HttpURLConnection, URL, URLStreamHandler}

import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.Mockito._

import scala.util.Try

case class ARequestEntity(value: String)

case class AResponseEntity(bazz: String)


class HttpRequestSenderTest extends HttpClientTest {
  implicit val objectMapper: ObjectMapper = new ObjectMapper()
  it should "send http request" in {
    val request: HttpRequest[ARequestEntity] = mock[HttpRequest[ARequestEntity]]
    val connection: HttpURLConnection = mock[HttpURLConnection]
    val uRLStreamHandler: URLStreamHandler = (_: URL) => connection
    val url: URL = new URL(null, "http://aurl.com", uRLStreamHandler)

    when(request.URL).thenReturn(url)
    when(request.Entity).thenReturn(None)
    when(request.Method).thenReturn(Get)
    when(request.TimeOutOptions).thenReturn(TimeOutOption())
    when(request.RetryOptions).thenReturn(RetryOptions())
    when(request.Headers).thenReturn(Map.empty[String, Object])
    when(request.contentType(request.Headers)).thenReturn(Map.empty[String, Object])
    when(connection.getResponseCode).thenReturn(200)
    when(connection.getInputStream).thenReturn(new ByteArrayInputStream("""{"value": "fish"}""".getBytes))


    val response: Try[HttpResponse[AResponseEntity]] = HttpRequestSender.doSend[ARequestEntity, AResponseEntity](request)

    response.isSuccess should be(true)
    response.get.StatusCode should equal(200)
    response.get.Headers should equal(Map.empty[String, Object])
  }
}