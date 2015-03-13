package ca.shiftfocus.krispii.core.lib.concurrent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._

trait FutureMonad {
  implicit val futureMonad = new Monad[Future] {
    override def point[A](a: ⇒ A): Future[A] = Future(a)
    override def bind[A, B](fa: Future[A])(f: A ⇒ Future[B]): Future[B] = fa.flatMap(f)
  }
}