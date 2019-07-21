package com.github.mideo.httpClient


case class TestEntity(value: String = "Test")

class ObjectMapperTest extends HttpClientTest {

  it should "serialise entity" in {
    Mappers.JsonMapper.writeValueAsString(TestEntity()) should equal("""{"value":"Test"}""")
  }

  it should "serialise xml entity" in {
    Mappers.XmlMapper.writeValueAsString(TestEntity()) should equal("<TestEntity><value>Test</value></TestEntity>")
  }


  it should "deserialise entity" in {
    Mappers.JsonMapper.readValue[TestEntity]("""{"value":"Test"}""".getBytes, classOf[TestEntity])
  }

  it should "deserialise xml  entity" in {
    Mappers.XmlMapper.readValue[TestEntity]("<TestEntity><value>Test</value></TestEntity>".getBytes, classOf[TestEntity])
  }



}
