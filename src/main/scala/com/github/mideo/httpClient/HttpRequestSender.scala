package com.github.mideo.httpClient

import java.net.HttpURLConnection

import com.fasterxml.jackson.databind.ObjectMapper

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.Try
import collection.JavaConverters._
import Implicits._

object HttpRequestSender {
  def send[T, K: ClassTag](request: HttpRequest[T])
                          (implicit Mapper: ObjectMapper): Try[HttpResponse[K]] = Try {

    implicit val connection: HttpURLConnection = request.URL.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection.setRequestMethod(request.Method)
    connection.setConnectTimeout(request.timeOutOptions.ConnectTimeoutMillis)
    connection.setReadTimeout(request.timeOutOptions.ReadTimeoutMillis)
    for {(key: String, value: Object) <- request.Headers} yield connection.setRequestProperty(key, String.valueOf(value))

    for {body: Seq[Byte] <- request.Entity} yield {
      connection.getOutputStream.write(body.toArray)
      connection.getOutputStream.close()

    }

    val result = HttpResponse[K](connection.getResponseCode, responseHeaders, responseBody.toArray)
    connection.disconnect()
    result
  }

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