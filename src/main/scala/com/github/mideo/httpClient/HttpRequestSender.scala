package com.github.mideo.httpClient

import java.net.{HttpURLConnection, URI}
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mideo.httpClient.Implicits._
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.{HttpContent, HttpObject, HttpResponseStatus, LastHttpContent, HttpMethod => NettyHttpMethod, HttpResponse => NettyHttpResponse, _}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.CharsetUtil
import org.slf4j.{Logger, LoggerFactory}

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
  private val log: Logger = LoggerFactory.getLogger(this.getClass.getCanonicalName)

  private type E[B] = Either[Throwable, HttpResponse[B]]

  def send[F[_], T, B: ClassTag](request: AbstractHttpRequest[T])
                                (implicit unit: E[B] => F[E[B]]): F[Either[Throwable, HttpResponse[B]]] = {
    implicit val objectMapper: ObjectMapper = request.objectMapper
    unit(RetryableTask.retry(request.RetryOptions.times, doNettySend[T, B], request).toEither)
  }

  def doNettySend[T, K: ClassTag](request: AbstractHttpRequest[T])(implicit objectMapper: ObjectMapper): Try[HttpResponse[K]] = Try {
    log.info(s"sending request $request")
    val eventLoopGroup: EventLoopGroup = new NioEventLoopGroup()

    val uri: URI = request.URL.toURI
    val isSSL = uri.getScheme.equalsIgnoreCase("https")

    val port: Int = uri.getPort match {
      case i if i == -1 && isSSL => 443
      case i if i == -1 && !isSSL => 80
      case i => i
    }

    val channel: Channel = new Bootstrap()
      .group(eventLoopGroup)
      .channel(classOf[NioSocketChannel])
      .handler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.config().setConnectTimeoutMillis(request.TimeOutOptions.ConnectTimeoutMillis)

          val pipeline: ChannelPipeline = ch.pipeline
          if (isSSL) {
            pipeline.addLast(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build().newHandler(ch.alloc()))
          }
          pipeline.addLast(new HttpClientCodec)
            .addLast(new ReadTimeoutHandler(request.TimeOutOptions.ReadTimeoutMillis, TimeUnit.MILLISECONDS))
            .addLast(new LoggingHandler(LogLevel.DEBUG))
            .addLast(new HttpContentDecompressor)
            .addLast(new HttpChannelHandler)
        }
      }).connect(uri.getHost, port).sync().channel()




    // Prepare the HTTP request.
    val nettyRequest = if (request.Entity.isDefined) {
      new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
        new NettyHttpMethod(request.Method.toUpperCase),
        uri.getRawPath, Unpooled.copiedBuffer(request.Entity.get.toArray))
    } else {
      new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
        new NettyHttpMethod(request.Method.toUpperCase),
        uri.getRawPath)
    }

    nettyRequest.headers.set(HttpHeaderNames.HOST, uri.getHost)

    request.contentType(request.Headers).foreach { entry => nettyRequest.headers().set(entry._1, entry._2) }

    // Send the HTTP request.
    channel.writeAndFlush(nettyRequest)
    channel.closeFuture().sync()

    val handler: HttpChannelHandler = channel.pipeline().last().asInstanceOf[HttpChannelHandler]

    // Wait for the server to close the connection.
    eventLoopGroup.shutdownGracefully()

    val response = HttpResponse[K](handler.status.code(), handler.responseHeaders, handler.body.getBytes())
    log.info(s"got response $response")
    response
  }
}

private class HttpChannelHandler extends SimpleChannelInboundHandler[HttpObject] {
  private val log: Logger = LoggerFactory.getLogger(this.getClass.getCanonicalName)
  var status: HttpResponseStatus = _
  var responseHeaders: Map[String, String] = _
  var body = ""


  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {
    msg match {
      case response: NettyHttpResponse =>
        status = response.status
        responseHeaders = response.headers.entries().asScala.map {
          it => it.getKey -> it.getValue
        }.toMap
      case content: HttpContent =>
        body += content.content().toString(CharsetUtil.UTF_8)
        if (content.isInstanceOf[LastHttpContent]) {
          ctx.close
        }
      case it: HttpObject => log.info(it.toString)
    }
  }

}