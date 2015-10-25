package better.files

import java.io.BufferedReader

/**
 * Base interface to test
 */
abstract class AbstractScanner(protected[this] val reader: BufferedReader) {
  def hasNext: Boolean
  def next(): String
  def nextInt() = next().toInt
  def nextLine() = reader.readLine()
  def close() = reader.close()
}

/**
 * Based on java.util.Scanner
 */
class JavaScanner(reader: BufferedReader) extends AbstractScanner(reader) {
  private[this] val scanner = new java.util.Scanner(reader)
  override def hasNext = scanner.hasNext
  override def next() = scanner.next()
  override def nextInt() = scanner.nextInt()
  override def nextLine() = {
    scanner.nextLine()
    scanner.nextLine()
  }

  override def close() = scanner.close()
}

/**
 * Based on StringTokenizer + resetting the iterator
 */
class IterableScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterable[String] {
  import scala.collection.JavaConversions._
  val lines: Iterator[String] = reader.lines().iterator()
  override def iterator = for {
    line <- lines
    tokenizer = new java.util.StringTokenizer(line)
    _ <- Iterator.continually(tokenizer).takeWhile(_.hasMoreTokens)
  } yield tokenizer.nextToken()

  private[this] var current = iterator

  override def hasNext = current.hasNext
  override def next() = current.next()
  override def nextLine() = {
    current = iterator
    super.nextLine()
  }
}

/**
 * Based on a mutating var StringTokenizer
 */
class IteratorScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterator[String] {
  import java.util.StringTokenizer
  private[this] var current: Option[StringTokenizer] = None

  @inline private[this] def tokenizer(): Option[StringTokenizer] = current.find(_.hasMoreTokens) orElse {
    Option(reader.readLine()) flatMap {line =>
      current = Some(new StringTokenizer(line))
      tokenizer()
    }
  }
  override def hasNext = tokenizer().exists(_.hasMoreTokens)
  override def next() = tokenizer().get.nextToken()
  override def nextLine() = {
    current = None
    super.nextLine()
  }
}

/**
 * Based on java.io.StreamTokenizer
 */
class StreamingScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterator[String] {
  import java.io.StreamTokenizer
  private[this] val in = new StreamTokenizer(reader)

  override def hasNext = in.ttype != StreamTokenizer.TT_EOF
  override def next() = {
    in.nextToken()
    in.sval
  }
  override def nextInt() = nextDouble().toInt
  def nextDouble() = {
    in.nextToken()
    in.nval
  }
}

/**
 * Based on a reusable StringBuilder
 */
class StringBuilderScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterator[String] {
  val chars = Iterator.continually(reader.read()).takeWhile(_ != -1).map(_.toChar)
  private[this] val buffer = new StringBuilder()

  override def next() = {
    buffer.clear()
    while (buffer.isEmpty && hasNext) {
      chars.takeWhile(c => !c.isWhitespace).foreach(buffer += _)
    }
    buffer.toString()
  }
  override def hasNext = chars.hasNext
}

/**
 * Scala version of the ArrayBufferScanner
 */
class CharBufferScanner(reader: BufferedReader) extends AbstractScanner(reader) with Iterator[String] {
  val chars = Iterator.continually(reader.read()).takeWhile(_ != -1).map(_.toChar)
  private[this] var buffer = Array.ofDim[Char](1<<4)

  override def next() = {
    var pos = 0
    while (pos == 0 && hasNext) {
      for {
        c <- chars.takeWhile(c => c != ' ' && c != '\n')
      } {
        if (pos == buffer.length) buffer = java.util.Arrays.copyOf(buffer, 2 * pos)
        buffer(pos) = c
        pos += 1
      }
    }
    String.copyValueOf(buffer, 0, pos)
  }
  override def hasNext = chars.hasNext
}
