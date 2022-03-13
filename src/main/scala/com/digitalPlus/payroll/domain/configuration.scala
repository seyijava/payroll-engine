package com.digitalPlus.payroll.domain

import com.digitalPlus.payroll.domain.adt.{IncomeTaxBracketRateTable, TaxTables}
import com.typesafe.config.ConfigFactory
import zio.config.magnolia.DeriveConfigDescriptor
import zio.config.typesafe.TypesafeConfigSource
import zio.{Has, Task, ZIO, ZLayer}

object configuration {

  type TaxTableConfiguration = Has[TaxTableConfig]

  trait Service{
    def incomeTaxBracketRateTableLookup(code: String) : Task[IncomeTaxBracketRateTable]
    def incomeTaxBracketRateTableList: Task[List[IncomeTaxBracketRateTable]]
  }

  val descriptor = DeriveConfigDescriptor.descriptor[TaxTables]

  case class TaxTableConfig() extends Service {
    private lazy val taxTableConfigMap: Task[Map[String,IncomeTaxBracketRateTable]] =
     for{
        taxTables <- loadTaxTable()
        taxTableMap = taxTables.taxes.map(taxTable => (taxTable.code -> taxTable)).toMap
      }yield taxTableMap

    private  def loadTaxTable(): Task[TaxTables] = {
      for {
        rawConfig <- ZIO.effect(ConfigFactory.load().getConfig("taxTable"))
        configSource <- ZIO.fromEither(TypesafeConfigSource.fromTypesafeConfig(rawConfig))
        config <- ZIO.fromEither(zio.config.read(descriptor.from(configSource)))
      } yield config
    }
    override def incomeTaxBracketRateTableLookup(code: String): Task[IncomeTaxBracketRateTable] =
      for{map <- taxTableConfigMap }yield map(code)

    override def incomeTaxBracketRateTableList: Task[List[IncomeTaxBracketRateTable]] =
      for{ incomeTaxTables <- loadTaxTable() }yield incomeTaxTables.taxes

  }

   val  taxTableLayer: ZLayer[Any, Throwable, TaxTableConfiguration] = ZLayer.succeed(TaxTableConfig())

    def incomeTaxBracketRateTableLookup(code: String) : ZIO[TaxTableConfiguration,Throwable,IncomeTaxBracketRateTable]
    = ZIO.accessM(_.get.incomeTaxBracketRateTableLookup(code))

    def incomeTaxBracketRateTableList : ZIO[TaxTableConfiguration,Throwable,List[IncomeTaxBracketRateTable]] =
      ZIO.accessM(_.get.incomeTaxBracketRateTableList)

}