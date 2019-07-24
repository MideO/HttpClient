package com.github.mideo.httpClient

import java.net.HttpURLConnection

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mideo.httpClient.Implicits._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.{Success, Try}


object RetryableTask {
  @tailrec final def retry[A, B](times: Int, function: A => Try[B], args: A): Try[B] = {
    if (times == 0) return function.apply(args)
    function.apply(args) match {
      case Success(s) => Success(s)
      case _ => retry(times - 1, function, args)
    }
  }
}

object HttpRequestSender {
  private type E[B] = Either[Throwable, HttpResponse[B]]

  def send[F[_], T, B: ClassTag](request: AbstractHttpRequest[T])
                                (implicit unit: E[B] => F[E[B]]): F[Either[Throwable, HttpResponse[B]]] = {
    implicit val objectMapper: ObjectMapper = request.objectMapper
    unit(RetryableTask.retry(request.RetryOptions.times, doSend[T, B], request).toEither)
  }


  def doSend[T, K: ClassTag](request: AbstractHttpRequest[T])(implicit objectMapper: ObjectMapper): Try[HttpResponse[K]] = Try {
    implicit val connection: HttpURLConnection = request.URL.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection.setRequestMethod(request.Method)
    connection.setConnectTimeout(request.TimeOutOptions.ConnectTimeoutMillis)
    connection.setReadTimeout(request.TimeOutOptions.ReadTimeoutMillis)
    for {(key: String, value: Object) <- request.contentType(request.Headers)} yield connection.setRequestProperty(key, String.valueOf(value))

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