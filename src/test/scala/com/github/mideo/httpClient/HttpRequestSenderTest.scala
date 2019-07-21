package com.github.mideo.httpClient

import java.io.ByteArrayInputStream
import java.net.{HttpURLConnection, URL, URLStreamHandler}

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mideo.httpClient.Implicits._
import org.mockito.Mockito._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.reflect.{ClassTag, classTag}
import scala.util.Try

case class ARequestEntity(value: String)

case class AResponseEntity(bazz: String)


class HttpRequestSenderTest extends HttpClientTest {
  implicit val objectMapper: ObjectMapper = new ObjectMapper()
  it should "send http request" in {
    val expected: AResponseEntity = AResponseEntity("fish")
    val request: HttpRequest[ARequestEntity] = mock[HttpRequest[ARequestEntity]]
    val connection: HttpURLConnection = mock[HttpURLConnection]
    val uRLStreamHandler: URLStreamHandler = (_: URL) => connection
    val url: URL = new URL(null, "http://aurl.com", uRLStreamHandler)

    when(request.URL).thenReturn(url)
    when(request.Entity).thenReturn(None)
    when(request.Method).thenReturn(Get)
    when(request.timeOutOptions).thenReturn(TimeOutOption())
    when(request.retryOptions).thenReturn(RetryOptions())
    when(request.Headers).thenReturn(Map.empty[String, Object])
    when(connection.getResponseCode).thenReturn(200)
    when(connection.getInputStream).thenReturn(new ByteArrayInputStream("""{"value": "fish"}""".getBytes))


    val response: Try[HttpResponse[AResponseEntity]] = HttpRequestSender.send[ARequestEntity, AResponseEntity](request)

    response.isSuccess should be(true)
    response.get.StatusCode should equal(200)
    response.get.Headers should equal(Map.empty[String, Object])
  }
}