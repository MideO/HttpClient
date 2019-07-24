package com.github.mideo.httpClient

import scala.annotation.tailrec
import scala.language.higherKinds
import scala.util.Try

object Implicits {

  implicit def implicitlyConvertOption[K](t: K): Option[K] = Some(t)

  implicit def implicitlyConvertHttpMethodToString(method: HttpMethod): String = method
    .getClass
    .getSimpleName
    .replace("$", "")
    .toUpperCase

}

