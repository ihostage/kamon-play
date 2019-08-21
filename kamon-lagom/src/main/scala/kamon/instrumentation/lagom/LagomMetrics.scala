package kamon.instrumentation.lagom

import kamon.Kamon
import kamon.metric.{Counter, InstrumentGroup, MeasurementUnit, Timer}
import kamon.tag.TagSet

object LagomMetrics {

  val defaultTags = TagSet.of("component", "lagom")

  val CBLatency = Kamon.histogram(
    name = "lagom.cb.latency",
    description = "Latency of Lagom Circuit Breakers",
    unit = MeasurementUnit.time.nanoseconds
  )

  val CBState = Kamon.gauge(
    name = "lagom.cb.state",
    description = "State of Lagom Circuit Breakers"
  )

  val CBCounter = Kamon.counter(
    name = "lagom.cb.count",
    description = "Count of calls of Lagom Circuit Breakers"
  )

  val CBTimer = Kamon.timer(
    name = "lagom.cb.timer",
    description = "Timer of calls of Lagom Circuit Breakers"
  )

  class CircuitBreakerInstruments(circuitBreaker: String, tags: TagSet) extends InstrumentGroup(tags.withTag("cb", circuitBreaker)) {

    val latency = register(CBLatency)
    val state = register(CBState)
    val successCount: Counter = register(CBCounter, "success", true)
    val failureCount: Counter = register(CBCounter, "success", false)
    val timeoutFailureCount: Counter = register(
      CBCounter, TagSet.builder().add("success", false).add("fail", "timeout").build()
    )
    val breakerOpenFailureCount: Counter = register(
      CBCounter, TagSet.builder().add("success", false).add("fail", "open").build()
    )
    val timer: Timer = register(CBTimer, "success", true)
    val failureTimer: Timer = register(CBTimer, "success", false)

  }

}
