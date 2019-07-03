package com.github.mideo.httpClient

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import language.higherKinds

object Implicits {
  final val EntityMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  implicit def implicitlyConvertOption[K](t: K): Option[K] = Some(t)

  implicit def implicitlyConvertHttpMethodToString(method: HttpMethod): String = method
    .getClass
    .getSimpleName
    .replace("$", "")
    .toUpperCase

}

