package marshallers

import models.{Bird, Place}
import org.typelevel.jawn.{Facade, FContext, Parser}
import scala.collection.mutable.ListBuffer

/** Schema-specific decoding: Jawn events construct Bird/Place without a generic JSON AST.
  * An instance is reused sequentially, not concurrently. Jawn does not provide an encoder.
  */
class JawnFacadeParser extends BirdParser {
  private val facade = new BirdFacade

  def parse(s: String): Bird =
    try {
      Parser.parseUnsafe[AnyRef](s)(facade) match {
        case bird: Bird => bird
        case _ => throw new IllegalArgumentException("Expected a Bird object")
      }
    } finally facade.reset()
}

private final class BirdFacade extends Facade.NoIndexFacade[AnyRef] {
  private case class NumberToken(text: String)
  private case class ArrayValue(kind: String, values: List[AnyRef])
  private var stack = List.empty[Context]
  def reset(): Unit = stack = Nil
  private def fail(message: String): Nothing = throw new IllegalArgumentException(message)
  private def push[A <: Context](context: A): A = { stack = context :: stack; context }
  private def pop(context: Context): Unit = {
    require(stack.head eq context, "Unbalanced facade context")
    stack = stack.tail
  }
  private def string(value: AnyRef): String = value match {
    case s: String => s
    case _ => fail("Expected a string")
  }
  private def integer(value: AnyRef): Int = value match {
    case NumberToken(s) =>
      if (s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0) {
        val n = BigDecimal(s)
        if (!n.isValidInt) fail("Expected a 32-bit integer")
        n.toInt
      } else java.lang.Integer.parseInt(s)
    case _ => fail("Expected an integer")
  }
  private def double(value: AnyRef): Double = value match {
    case NumberToken(s) =>
      val n = java.lang.Double.parseDouble(s)
      if (!java.lang.Double.isFinite(n)) fail("Expected a finite double")
      n
    case _ => fail("Expected a number")
  }
  private def array[A](value: AnyRef, kind: String): List[A] = value match {
    case ArrayValue(`kind`, values) => values.asInstanceOf[List[A]] // Each element was checked by ArrayContext.
    case _ => fail(s"Expected $kind array")
  }

  private abstract class Context extends FContext.NoIndexFContext[AnyRef]
  private final class SingleContext extends Context {
    private var value: AnyRef = null
    def isObj: Boolean = false
    def add(s: CharSequence): Unit = value = s.toString
    def add(v: AnyRef): Unit = value = v
    def finish(): AnyRef = { pop(this); value }
  }
  private final class SkipContext(val isObj: Boolean) extends Context {
    def add(s: CharSequence): Unit = ()
    def add(v: AnyRef): Unit = ()
    def finish(): AnyRef = { pop(this); null }
  }
  private abstract class ObjectContext extends Context {
    var key: String = null
    def knownKey: Boolean
    def put(value: AnyRef): Unit
    def isObj: Boolean = true
    def add(s: CharSequence): Unit =
      if (key == null) key = s.toString else add(s.toString: AnyRef)
    def add(v: AnyRef): Unit = { put(v); key = null }
  }
  private final class BirdContext extends ObjectContext {
    private var scientificName: String = null
    private var commonNames = List.empty[String]
    private var sights = 0
    private var wingSpan = 0.0
    private var places = List.empty[Place]
    private var seen = 0
    def knownKey: Boolean = key match {
      case "scientific_name" | "common_names" | "sights" | "wing_span" | "hangs_out" => true
      case _ => false
    }
    def put(value: AnyRef): Unit = key match {
      case "scientific_name" => scientificName = string(value); seen |= 1
      case "common_names" => commonNames = array[String](value, "strings"); seen |= 2
      case "sights" => sights = integer(value); seen |= 4
      case "wing_span" => wingSpan = double(value); seen |= 8
      case "hangs_out" => places = array[Place](value, "places"); seen |= 16
      case _ => ()
    }
    def finish(): AnyRef = {
      if (seen != 31) fail("Missing required Bird fields")
      pop(this)
      Bird(scientificName, commonNames, sights, wingSpan, places)
    }
  }
  private final class PlaceContext extends ObjectContext {
    private var name: String = null
    private var id = 0
    private var latlon = List.empty[Double]
    private var description = Option.empty[String]
    private var rate = Option.empty[Int]
    private var seen = 0
    def knownKey: Boolean = key match {
      case "name" | "_id" | "latlon" | "description" | "michelin_rate" => true
      case _ => false
    }
    def put(value: AnyRef): Unit = key match {
      case "name" => name = string(value); seen |= 1
      case "_id" => id = integer(value); seen |= 2
      case "latlon" => latlon = array[Double](value, "doubles"); seen |= 4
      case "description" => description = if (value == null) None else Some(string(value))
      case "michelin_rate" => rate = if (value == null) None else Some(integer(value))
      case _ => ()
    }
    def finish(): AnyRef = {
      if (seen != 7) fail("Missing required Place fields")
      pop(this)
      Place(name, id, latlon, description, rate)
    }
  }
  private final class ArrayContext(val kind: String) extends Context {
    private val values = ListBuffer.empty[AnyRef]
    def isObj: Boolean = false
    def add(s: CharSequence): Unit = add(s.toString: AnyRef)
    def add(value: AnyRef): Unit = kind match {
      case "strings" => values += string(value)
      case "doubles" => values += java.lang.Double.valueOf(double(value))
      case "places" => value match {
        case place: Place => values += place
        case _ => fail("Expected a Place object")
      }
      case _ => fail("Unknown array type")
    }
    def finish(): AnyRef = { pop(this); ArrayValue(kind, values.toList) }
  }

  def singleContext(): FContext[AnyRef] = push(new SingleContext)
  def objectContext(): FContext[AnyRef] = stack.headOption match {
    case None | Some(_: SingleContext) => push(new BirdContext)
    case Some(a: ArrayContext) if a.kind == "places" => push(new PlaceContext)
    case Some(_: SkipContext) => push(new SkipContext(true))
    case Some(o: ObjectContext) if !o.knownKey => push(new SkipContext(true))
    case _ => fail("Unexpected object for this schema")
  }
  def arrayContext(): FContext[AnyRef] = stack.headOption match {
    case Some(b: BirdContext) if b.key == "common_names" => push(new ArrayContext("strings"))
    case Some(b: BirdContext) if b.key == "hangs_out" => push(new ArrayContext("places"))
    case Some(p: PlaceContext) if p.key == "latlon" => push(new ArrayContext("doubles"))
    case Some(_: SkipContext) => push(new SkipContext(false))
    case Some(o: ObjectContext) if !o.knownKey => push(new SkipContext(false))
    case _ => fail("Unexpected array for this schema")
  }
  def jnull: AnyRef = null
  def jfalse: AnyRef = java.lang.Boolean.FALSE
  def jtrue: AnyRef = java.lang.Boolean.TRUE
  def jnum(s: CharSequence, decIndex: Int, expIndex: Int): AnyRef =
    if (stack.headOption.exists(_.isInstanceOf[SkipContext])) null else NumberToken(s.toString)
  def jstring(s: CharSequence): AnyRef =
    if (stack.headOption.exists(_.isInstanceOf[SkipContext])) null else s.toString
}
