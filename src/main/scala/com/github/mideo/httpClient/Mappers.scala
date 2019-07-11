package com.github.mideo.httpClient

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Mappers {

  final val JsonMapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  final val XmlMapper = new XmlMapper().registerModule(DefaultScalaModule)
}
