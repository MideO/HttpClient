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


  implicit class RetryableTask[K](f: () => Try[K]) {
    @tailrec final def retry(times: Int): Try[K] = {
      if (times == 0) return f()
      f() match {
        case result if result.isSuccess => result
        case _ => retry(times - 1)
      }

    }
  }

}

