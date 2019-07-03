package com.github.mideo.httpClient
import com.github.mideo.httpClient.Implicits._

case class TestEntity(value: String = "Test")

class ObjectMapperTest extends HttpIOTest {

  it should "serialise entity" in {
    EntityMapper.writeValueAsString(TestEntity()) should equal("""{"value":"Test"}""")
  }

  it should "deserialise entity" in {
    EntityMapper.readValue[TestEntity]("""{"value":"Test"}""".getBytes, classOf[TestEntity])
  }

}
