package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._
import java.time.{LocalDate, LocalDateTime, LocalTime}

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

class ArrayOfLocalDateTimesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[LocalDateTime] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map { i =>
      val n = Math.abs(i * 1498724053)
      LocalDateTime.of(LocalDate.ofEpochDay(i),
        LocalTime.ofNanoOfDay(((n % 86000) | 0x1) * 1000000000L + (i % 4 match {
          case 0 => 0
          case 1 => ((n % 1000) | 0x1) * 1000000
          case 2 => ((n % 1000000) | 0x1) * 1000
          case 3 => (n | 0x1) % 1000000000
        })))
    }.toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[LocalDateTime] = JsonStringInput.read[Array[LocalDateTime]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Array[LocalDateTime] = decode[Array[LocalDateTime]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readJacksonScala(): Array[LocalDateTime] = jacksonMapper.readValue[Array[LocalDateTime]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[LocalDateTime] = readFromArray[Array[LocalDateTime]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[LocalDateTime] = Json.parse(jsonBytes).as[Array[LocalDateTime]]

  @Benchmark
  def readUPickle(): Array[LocalDateTime] = read[Array[LocalDateTime]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}