package ca.shiftfocus.lib.concurrent

import scala.concurrent.Future
import scalaz.{\/-, \/}

trait Serialized {
  /**
   * Given a collection, and some function to map over the collection whose result type is a future disjunction,
   * this function runs the given function over the collection sequentially. Use this when you would have a list of
   * Futures that cannot be run in parallel, for example if they will all run on the same database connection.
   *
   * {{{
   *   val fooList: IndexedSeq[Foo] = Vector(foo1, foo2, foo3)
   *   def barFunc(foo: Foo): Future[Bar]
   *
   *   // The resultant list will be generated sequentially with no parallel operations
   *   val futureBarList: Future[IndexedSeq[Bar]] = serializedT(fooList)(barFunc)
   * }}}
   *
   * @param collection
   * @param fn
   * @tparam E
   * @tparam R
   * @tparam L
   * @return
   */
  def serialized[E, R, L[E] <: IndexedSeq[E]](collection: L[E])(fn: E => Future[R]): Future[IndexedSeq[R]] = {
    collection.foldLeft(Future(IndexedSeq.empty[R])) { (fAccumulated, nextItem) =>
      for {
        accumulated <- fAccumulated
        nextResult <- fn(nextItem)
      }
      yield accumulated :+ nextResult
    }
  }
}