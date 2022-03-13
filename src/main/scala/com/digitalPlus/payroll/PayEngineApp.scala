package com.digitalPlus.payroll

import com.digitalPlus.payroll.domain.adt.{Employee, Ontario}
import com.digitalPlus.payroll.domain.configuration.taxTableLayer
import com.digitalPlus.payroll.domain.payRollCalculationService
import com.digitalPlus.payroll.domain.payRollCalculationService.payrollEngineLayer
import zio.logging.slf4j.Slf4jLogger
import zio.{ExitCode, URIO, ZIO}
import zio.logging.{LogAnnotation, Logging}
import com.digitalPlus.payroll.domain.configuration

object PayEngineApp extends zio.App{

  val logFormat = "[correlation-id = %s] %s"

  val loggingLayer =
    Slf4jLogger.make{(context, message) =>
      val correlationId = LogAnnotation.CorrelationId.render(
        context.get(LogAnnotation.CorrelationId)
      )
      logFormat.format(correlationId, message)
    }
  val appLayers = loggingLayer ++ taxTableLayer >>> payrollEngineLayer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
      val employList = (1 to 500000 map(n => Employee(s"EMP-$n",Ontario("NB"),1890.90))).toList
      payRollCalculationService.generatePaySlips(employList)
      .provideLayer(appLayers).catchAll(t => ZIO.succeed(t.printStackTrace()).map(_ => ExitCode.failure))
      .map { u =>
        println("PayRoll Done ")
        ExitCode.success
      }
  }

}
