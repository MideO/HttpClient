package com.github.mideo.httpClient

import java.net.HttpURLConnection

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mideo.httpClient.Implicits._
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelInitializer, EventLoopGroup}
import io.netty.handler.codec.http
import io.netty.handler.codec.http.{HttpClientCodec, HttpContentDecompressor, HttpUtil}
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

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

  def doNettySend[T, K: ClassTag](request: AbstractHttpRequest[T])(implicit objectMapper: ObjectMapper) = Try {


    val port: Int = request.URL.toURI.getPort match {
      case i if i == -1 && request.URL.toURI.getScheme.equalsIgnoreCase("https") => 443
      case i if i == -1 && request.URL.toURI.getScheme.equalsIgnoreCase("http") => 80
      case i => i
    }

    val group: EventLoopGroup = new NioEventLoopGroup()
    val bootstrap: Bootstrap = new Bootstrap()

    request.URL.toURI.getScheme match {
      case s if s.equalsIgnoreCase("https") =>

        bootstrap.group(group)
          .channel(classOf[NioSocketChannel])
          .handler(new ChannelInitializer[SocketChannel] {
            override def initChannel(ch: SocketChannel): Unit = {
              ch.pipeline
                .addLast(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build().newHandler(ch.alloc()))
                .addLast(new HttpClientCodec())
                .addLast(new HttpContentDecompressor())
                .addLast(new HttpClientHandler())


            }
          })
      case _ => _
    }

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

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{HttpContent, HttpObject, LastHttpContent}
import io.netty.util.CharsetUtil
import collection.JavaConverters._
class HttpClientHandler extends SimpleChannelInboundHandler[HttpObject] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {
    msg match {
      case response: http.HttpResponse =>
        if (!response.headers.isEmpty) {
          response.headers.names().asScala.foreach(
            name =>  response.headers.getAll(name).forEach(
              value => System.err.println("HEADER: " + name + " = " + value)
            )
          )
        }
        if (HttpUtil.isTransferEncodingChunked(response)) System.err.println("CHUNKED CONTENT {")
        else System.err.println("CONTENT {")
      case content: HttpContent =>
        System.err.print(content.content.toString(CharsetUtil.UTF_8))
        System.err.flush()
        if (content.isInstanceOf[LastHttpContent]) {
          System.err.println("} END OF CONTENT")
          ctx.close
        }
      case _ =>
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close
  }
}