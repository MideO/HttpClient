package com.github.mideo.httpClient

import java.net.{HttpURLConnection, URL}

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mideo.httpClient.Implicits._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.higherKinds
import scala.reflect._
import scala.util.Try

case class TimeOutOption(ConnectTimeoutMillis: Int = 1000, ReadTimeoutMillis: Int = 5000)

case class RetryOptions(times: Int = 0)

case class JsonHttpRequest[T: ClassTag](method: HttpMethod,
                                        Url: String,
                                        headers: Map[String, Object],
                                        private val entity: Option[T] = None,
                                        private[httpClient] val timeOutOptions: TimeOutOption = TimeOutOption(),
                                        private[httpClient] val retryOptions: RetryOptions = RetryOptions()
                                       )(implicit Mapper: ObjectMapper = Mappers.JsonMapper)
  extends AbstractHttpRequest[T](method, Url, headers, entity, timeOutOptions, retryOptions) {
  override def contentType(Headers: Map[String, Object]): Map[String, Object] = headers ++ Map("Content-Type" -> "application/json", "Accept" -> "application/json")

}


case class XmlHttpRequest[T: ClassTag](method: HttpMethod,
                                       Url: String,
                                       headers: Map[String, Object],
                                       private val entity: Option[T] = None,
                                       private[httpClient] val timeOutOptions: TimeOutOption = TimeOutOption(),
                                       private[httpClient] val retryOptions: RetryOptions = RetryOptions()
                                      )(implicit Mapper: ObjectMapper = Mappers.XmlMapper)
  extends AbstractHttpRequest[T](method, Url, headers, entity, timeOutOptions, retryOptions) {

  override def contentType(Headers: Map[String, Object]): Map[String, Object] = headers ++ Map("Content-Type" -> "application/xml", "Accept" -> "application/xml")

}

case class HttpRequest[T: ClassTag](method: HttpMethod,
                                    Url: String,
                                    headers: Map[String, Object],
                                    private val entity: Option[T] = None,
                                    private[httpClient] val timeOutOptions: TimeOutOption = TimeOutOption(),
                                    private[httpClient] val retryOptions: RetryOptions = RetryOptions()
                                   )(implicit Mapper: ObjectMapper = Mappers.JsonMapper)
  extends AbstractHttpRequest[T](method, Url, headers, entity, timeOutOptions, retryOptions) {

  override def contentType(Headers: Map[String, Object]): Map[String, Object] = headers

}


private[httpClient] abstract class AbstractHttpRequest[T: ClassTag](method: HttpMethod,
                                                                    Url: String,
                                                                    headers: Map[String, Object],
                                                                    entity: Option[T],
                                                                    timeOutOption: TimeOutOption,
                                                                    retryOptions: RetryOptions)
                                                                   (implicit Mapper: ObjectMapper) {


  val Entity: Option[Seq[Byte]] = entity.map {
    case e: T => Mapper.writeValueAsBytes(e)
    case _ => Seq.empty[Byte]
  }
  val URL: URL = new URL(Url)
  val Headers: Map[String, Object] = contentType(headers)
  val Method: HttpMethod = method
  val TimeOutOptions: TimeOutOption = timeOutOption
  val RetryOptions: RetryOptions = retryOptions
  val objectMapper: ObjectMapper = Mapper

  def contentType(Headers: Map[String, Object]): Map[String, Object]

}
