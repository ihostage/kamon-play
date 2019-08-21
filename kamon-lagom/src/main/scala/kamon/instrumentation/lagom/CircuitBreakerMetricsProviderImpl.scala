package kamon.instrumentation.lagom

import java.util.concurrent.CopyOnWriteArrayList

import akka.actor.ActorSystem
import com.lightbend.lagom.internal.spi.{CircuitBreakerMetrics, CircuitBreakerMetricsProvider}
import javax.inject.{Inject, Singleton}
import LagomMetrics.{CircuitBreakerInstruments, defaultTags}
import play.api.Logger

@Singleton
class CircuitBreakerMetricsProviderImpl @Inject() (val system: ActorSystem) extends CircuitBreakerMetricsProvider {

  private val metrics = new CopyOnWriteArrayList[CircuitBreakerMetricsImpl]

  override def start(breakerId: String): CircuitBreakerMetrics = {
    val m = new CircuitBreakerMetricsImpl(breakerId, this)
    metrics.add(m)
    m
  }

  private[kamon] def remove(m: CircuitBreakerMetricsImpl): Unit =
    metrics.remove(m)

}

object CircuitBreakerMetricsImpl {
  final val Open = 1
  final val HalfOpen = 2
  final val Closed = 3
}

class CircuitBreakerMetricsImpl(val breakerId: String, provider: CircuitBreakerMetricsProviderImpl)
  extends CircuitBreakerMetrics {
  import CircuitBreakerMetricsImpl._

  private val log = Logger(getClass)

  private val cbInstruments = new CircuitBreakerInstruments(breakerId, defaultTags)

  override def onOpen(): Unit = {
    cbInstruments.state.update(Open)
    log.warn(s"Circuit breaker [${breakerId}] open")
  }

  override def onClose(): Unit = {
    cbInstruments.state.update(Closed)
    log.info(s"Circuit breaker [${breakerId}] closed")
  }

  override def onHalfOpen(): Unit = {
    cbInstruments.state.update(HalfOpen)
    log.info(s"Circuit breaker [${breakerId}] half-open")
  }

  override def onCallSuccess(elapsedNanos: Long): Unit = {
    cbInstruments.successCount.increment();
    cbInstruments.latency.record(elapsedNanos)
    cbInstruments.timer.record(elapsedNanos)
  }

  override def onCallFailure(elapsedNanos: Long): Unit = {
    cbInstruments.failureCount.increment()
    cbInstruments.latency.record(elapsedNanos)
    cbInstruments.failureTimer.record(elapsedNanos)
  }

  override def onCallTimeoutFailure(elapsedNanos: Long): Unit = {
    cbInstruments.timeoutFailureCount.increment()
    cbInstruments.latency.record(elapsedNanos)
    cbInstruments.failureTimer.record(elapsedNanos)
  }

  override def onCallBreakerOpenFailure(): Unit = {
    cbInstruments.breakerOpenFailureCount.increment()
    cbInstruments.latency.record(1L)
    cbInstruments.failureTimer.record(1L)
  }

  override def stop(): Unit = {
    cbInstruments.remove()
    provider.remove(this)
  }

}
