package ca.shiftfocus.lib.concurrent

import scalaz._
import scala.concurrent.{ExecutionContext, Future}

/**
 * This trait provides several utility functions that can be used with Futures in
 * conjunction with scalaz.\/ and its scalaz.EitherT monad transformer.
 *
 * @tparam A is the failure type to use on the left-hand side of disjunctions.
 *           NB: the left-hand type is invariant in the EitherT transformer.
 */
trait Lifting[A] extends FutureMonad with Serialized {

  implicit def liftFun[X, T <% X, U](f: (X) => U): (T) => U = {
    def g(t: T) = f(t)
    g _
  }

  class Pipe[T](t: T) {
    def pipeTo[X, U](f: (X) => U)(implicit ev: T => X) = f(t)
  }
  implicit def pipeTo[T](t: T) = new Pipe(t: T)

  type ErrorOr[B] = EitherT[Future, A, B]
  type ReaderTE[C, B] = ReaderT[ErrorOr, C, B]
  object ReaderTE extends KleisliInstances {
    def apply[C, B](f: C => ErrorOr[B]): ReaderTE[C, B] = Kleisli[ErrorOr, C, B](f)
  }

  /**
   * An implicit conversion to automatically call ".run" on EitherT monads when
   * it can be inferred that you want to unwrap the transformer.
   * 
   * @param eithert the EitherT monad transformer to unwrap/run
   * @tparam B the right-hand type of the disjunction
   * @return an unwrapped Future disjunction
   */
  implicit def eitherRunner[B](eithert: EitherT[Future, A, B]): Future[\/[A, B]] = eithert.run

  /**
   * Lift a wrapped future disjunction into an EitherT.
   *
   * @tparam B
   * @return
   */
  def lift[B] = EitherT.eitherT[Future, A, B] _

  /**
   * Given a list of future disjunctions, lift them into an EitherT transformer that stores
   * the first error encountered, or a list of all right-hand-side data.
   *
   * @param interList
   * @tparam B
   * @return
   */
  def liftSeq[B](interList: IndexedSeq[Future[\/[A, B]]])(implicit ec: ExecutionContext): EitherT[Future, A, IndexedSeq[B]] = {
    liftSeq(Future.sequence(interList))
  }

  /**
   * Given a future list of disjunctions, lift it into an EitherT transformer that stores
   * the first error encountered, or a list of all right-hand-side data.
   *
   * @param fIntermediate
   * @tparam B
   * @return
   */
  def liftSeq[B](fIntermediate: Future[IndexedSeq[\/[A, B]]])(implicit ec: ExecutionContext): EitherT[Future, A, IndexedSeq[B]] = {
    val result = fIntermediate.map { intermediate =>
      if (intermediate.filter(_.isLeft).nonEmpty) -\/(intermediate.filter(_.isLeft).head.swap.toOption.get)
      else \/-(intermediate.map(_.toOption.get))
    }
    lift(result)
  }

  /**
   * Given a boolean condition, and an instance of the left-hand failure type, build an EitherT transformer
   * that contains Unit when the condition is true, or contains the failure when the condition is false. Useful
   * for making short-circuiting assertions in for-comprehensions.
   *
   * Example usage:
   * {{{
   *   for {
   *     foo <- lift(fooRepository.find(fooId))
   *     _    <- predicate (foo.name == "Bar") (FooError("Foo can't be named Bar"))
   *     // The for-comprehension only continues if the predicate's condition was true. Otherwise it
   *     // short-circuits and evaluates to the FooError.
   *   } yield foo
   * }}}
   *
   * @param condition the boolean condition to test
   * @param fail the failure to return when the condition is false
   * @return a [[scalaz.EitherT]]
   */
  def predicate(condition: Boolean)(fail: A): EitherT[Future, A, Unit] = {
    val result = Future.successful {
      if (condition) \/-(())
      else -\/(fail)
    }
    lift(result)
  }

  /**
   * Given a future boolean condition, and an instance of the left-hand failure type, build an EitherT transformer
   * that contains Unit when the condition is true, or contains the failure when the condition is false. Useful
   * for making short-circuiting assertions in for-comprehensions.
   *
   * Example usage:
   * {{{
   *   for {
   *     foo <- lift(fooRepository.find(fooId))
   *     _    <- predicate (foo.name == "Bar") (FooError("Foo can't be named Bar"))
   *     // The for-comprehension only continues if the predicate's condition was true. Otherwise it
   *     // short-circuits and evaluates to the FooError.
   *   } yield foo
   * }}}
   *
   * @param fCondition the boolean condition to test, wrapped in a Future
   * @param fail the failure to return when the condition is false
   * @return a [[scalaz.EitherT]]
   */
//  def predicate(fCondition: Future[Boolean])(fail: A)(implicit ec: ExecutionContext): EitherT[Future, A, Unit] = {
//    lift {
//      fCondition.map { condition =>
//        if (condition) \/-(())
//        else -\/(fail)
//      }
//    }
//  }
  def predicate(fCondition: Future[\/[A, Boolean]])(fail: A)(implicit ec: ExecutionContext): EitherT[Future, A, Unit] = {
    lift {
      fCondition.map(_.map {
        case true => \/-(())
        case false => -\/(fail)
      })
    }
  }

  /**
   * Optionally call a function to return a value, depending on whether the optional parameter is defined or not.
   * If it is not, instead return a default value.
   *
   * @param anOption the optional parameter
   * @param f the function to call with the optional parameter
   * @param default the default value to return if the optional parameter is not defined
   * @tparam I the type of the optional parameter
   * @tparam B the success type returned by f
   * @return
   */
  def optionally[I, B](anOption: Option[I])(f: I => EitherT[Future, A, B])(default: B): EitherT[Future, A, B] = lift {
    anOption match {
      case Some(thing) => f(thing)
      case None => Future successful \/-(default)
    }
  }

  def readerOptional[I, C, B](anOption: Option[I])(f: I => ReaderTE[C, B])(default: B): ReaderTE[C, B] = ReaderTE { conn: C => lift {
    anOption match {
      case Some(thing) => f(thing)(conn)
      case None => Future successful \/-(default)
    }
  }}

  /**
   * Given a collection, and some function to map over the collection whose result type is a future disjunction,
   * this function runs the given function over the collection sequentially. Use this when you would have a list of
   * Futures that cannot be run in parallel, for example if they will all run on the same database connection.
   *
   * {{{
   *   val fooList: IndexedSeq[Foo] = Vector(foo1, foo2, foo3)
   *   def barFunc(foo: Foo): Future[\/[Error, Bar]]
   *
   *   // The resultant list will be generated sequentially with no parallel operations
   *   val futureBarList: Future[\/[Error, IndexedSeq[Bar]] = serializedT(fooList)(barFunc)
   * }}}
   *
   * @param collection the list of objects to be mapped
   * @param fn the function to be applied to each element of the list
   * @tparam E the starting type of the elements in the list
   * @tparam R the transformed type of the elements in the list
   * @tparam L the exact type of list, which must be an indexed sequence
   * @return
   */
  def serializedT[E, R, L[E] <: IndexedSeq[E]](collection: L[E])(fn: E => Future[\/[A, R]])(implicit ec: ExecutionContext): Future[\/[A, IndexedSeq[R]]] = {
    val empty: Future[\/[A, IndexedSeq[R]]] = Future.successful(\/-(IndexedSeq.empty[R]))
    collection.foldLeft(empty) { (fAccumulated, nextItem) =>
      val iteration = for {
        accumulated <- lift(fAccumulated)
        nextResult <- lift(fn(nextItem))
      }
      yield accumulated :+ nextResult
      iteration.run
    }
  }
}
