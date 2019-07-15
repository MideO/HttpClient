package com.github.mideo.httpClient

import com.fasterxml.jackson.databind.ObjectMapper

import scala.io.Source
import scala.util.Try

case class HttpResponse[T: Manifest](StatusCode: Int,
                                     Headers: Map[String, Any],
                                     private val raw: Seq[Byte]) (implicit Mapper:ObjectMapper){
  val Entity: T = Try {
    Mapper.readValue[T](raw.toArray, manifest.runtimeClass.asInstanceOf[Class[T]])
  }.toOption.getOrElse {
    // Last resort! dump as String
    Source.fromBytes(raw.toArray).mkString.asInstanceOf[T]
  }

}