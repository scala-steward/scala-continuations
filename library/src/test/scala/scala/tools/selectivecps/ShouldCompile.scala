/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

import scala.collection.mutable.HashMap
import scala.util.continuations._

// https://issues.scala-lang.org/browse/SI-3620
object t3620 extends App {

  class Store[K,V] {

    trait Waiting {
      def key: K
      def inform(value: V): Unit
    }

    private val map = new HashMap[K, V]
    private var waiting: List[Waiting] = Nil

    def waitFor(k: K, f: (V => Unit)): Unit = {
      map.get(k) match {
        case Some(v) => f(v)
        case None => {
          val w = new Waiting {
            def key = k
            def inform(v: V) = f(v)
          }
          waiting = w :: waiting
        }
      }
    }


    def add(key: K, value: V): Unit = {
      map(key) = value
      val p = waiting.partition(_.key == key)
      waiting = p._2
      p._1.foreach(_.inform(value))
    }

    def required(key: K) = {
      shift {
        c: (V => Unit) => {
          waitFor(key, c)
        }
      }
    }

    def option(key: Option[K]) = {
      shift {
        c: (Option[V] => Unit) => {
          key match {
            case Some(key) => waitFor(key, (v: V) => c(Some(v)))
            case None => c(None)
          }

        }
      }
    }

  }

  val store = new Store[String, Int]

  def test(p: Option[String]): Unit = {
    reset {
      // uncommenting the following two lines makes the compiler happy!
//      val o = store.option(p)
//      println(o)
      val i = store.option(p).getOrElse(1)
      println(i)
    }
  }

  test(Some("a"))

}
