package com.github.mideo.httpClient

import com.github.mideo.httpClient.Implicits._

import scala.io.Source
import scala.util.Try

case class HttpResponse[T: Manifest](StatusCode: Int,
                                     Headers: Map[String, Any],
                                     private val raw: Seq[Byte]) {
  val Entity: T = Try {
    EntityMapper.readValue[T](raw.toArray, manifest.runtimeClass.asInstanceOf[Class[T]])
  }.toOption.getOrElse{
    Source.fromBytes(raw.toArray).mkString.asInstanceOf[T]
  }

}