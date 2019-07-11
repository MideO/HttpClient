package com.github.mideo.httpClient

import java.net.{HttpURLConnection, URL}

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mideo.httpClient.Implicits._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.higherKinds
import scala.util.Try

case class TimeOutOption(ConnectTimeoutMillis: Int = 1000, ReadTimeoutMillis: Int = 5000)

case class JsonHttpRequest[T: Manifest](Method: HttpMethod,
                                        private val Url: String,
                                        headers: Map[String, Object],
                                        private val entity: Option[T],
                                        Options: TimeOutOption = TimeOutOption())(implicit Mapper:ObjectMapper = Mappers.JsonMapper) extends AbstractHttpRequest[T](Method, Url, headers, entity, Options) {
  override def contentType(Headers: Map[String, Object]) = headers ++ Map("Content-Type" -> "application/json", "Accept" -> "application/json")

}


case class XmlHttpRequest[T: Manifest](Method: HttpMethod,
                                       private val Url: String,
                                       headers: Map[String, Object],
                                       private val entity: Option[T],
                                       Options: TimeOutOption = TimeOutOption())(implicit Mapper:ObjectMapper = Mappers.XmlMapper) extends AbstractHttpRequest[T](Method, Url, headers, entity, Options) {

  override def contentType(Headers: Map[String, Object]) = headers ++ Map("Content-Type" -> "application/xml", "Accept" -> "application/xml")

}

case class HttpRequest[T: Manifest](Method: HttpMethod,
                                    private val Url: String,
                                    headers: Map[String, Object],
                                    private val entity: Option[T],
                                    Options: TimeOutOption = TimeOutOption())(implicit Mapper:ObjectMapper = Mappers.JsonMapper) extends AbstractHttpRequest[T](Method, Url, headers, entity, Options) {

  override def contentType(Headers: Map[String, Object]) = headers

}


sealed abstract class AbstractHttpRequest[T: Manifest](Method: HttpMethod,
                                                       private val Url: String,
                                                       private val headers: Map[String, Object],
                                                       private val entity: Option[T],
                                                       Options: TimeOutOption = TimeOutOption())
                                                      (implicit Mapper: ObjectMapper) {


  val Entity: Option[Seq[Byte]] = entity.map {
    case e: T => Mapper.writeValueAsBytes(e)
    case _ => Seq.empty[Byte]
  }
  val URL: URL = new URL(Url)

  private type EitherResponse[B] = Either[Throwable, HttpResponse[B]]
  val Headers: Map[String, Object] = contentType(headers)

  def send[F[_], B: Manifest](implicit bind: EitherResponse[B] => F[EitherResponse[B]]): F[Either[Throwable, HttpResponse[B]]] = {
    implicit val connection: HttpURLConnection = URL.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection.setRequestMethod(Method)

    for {(key: String, value: Object) <- Headers} yield connection.setRequestProperty(key, String.valueOf(value))
    bind.apply {
      Try {
        for {body: Seq[Byte] <- Entity} yield {
          connection.getOutputStream.write(body.toArray)
          connection.getOutputStream.close()
        }
        val result = HttpResponse[B](connection.getResponseCode, responseHeaders, responseBody.toArray)
        connection.disconnect()
        result
      }.toEither
    }

  }

  protected def contentType(Headers: Map[String, Object]): Map[String, Object]

  private def responseHeaders(implicit connection: HttpURLConnection): Map[String, String] = connection
    .getHeaderFields
    .keySet().asScala
    .map {
      it: String => it -> connection.getHeaderField(it)
    }.toMap

  private def responseBody(implicit connection: HttpURLConnection): Seq[Byte] = {
    connection match {
      case it if it.getResponseCode < 300 && it.getInputStream != null => Source.fromInputStream(it.getInputStream).mkString.getBytes
      case it if it.getErrorStream != null => Source.fromInputStream(connection.getErrorStream).mkString.getBytes
      case _ => connection.getResponseMessage.getBytes
    }
  }
}
