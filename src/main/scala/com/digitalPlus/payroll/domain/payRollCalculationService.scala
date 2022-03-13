package com.digitalPlus.payroll.domain

import zio.logging.slf4j._
import com.digitalPlus.payroll.domain.adt.{Deduction, Earnings, Employee, IncomeTax, PaySlip, Province, incomeTaxBracketRateCalculator}
import com.digitalPlus.payroll.domain.configuration.{TaxTableConfig, TaxTableConfiguration}
import zio.logging.{Logger, Logging}
import zio.{Has, Task, UIO, ZIO, ZLayer}

import zio.logging._
object payRollCalculationService {

  type  PayDayCalculationServiceEngine = Has[PayDayCalculationService]

  val FEDTAXCODE = "FED"

  type EmployeePaySlip = (Employee, PaySlip)

  type EmployeesPaySips = List[ (Employee, PaySlip)]


  type FederalTax  = (Double,BigDecimal) => UIO[BigDecimal]


  type ProvincialTax = (Province,BigDecimal) => UIO[BigDecimal]

  type EarningsCalculation =  List[Earnings] => UIO[BigDecimal]

  type IncomeTaxCalculation =  (ProvincialTax, FederalTax) => UIO[BigDecimal]

  type DeductionCalculation =  List[Deduction] => UIO[BigDecimal]


  val earningsComputation: EarningsCalculation = (earnings) => ZIO.succeed(earnings.foldLeft(BigDecimal(0.0))((amt,earning) => amt + earning.amount))

  val deductionComputation: DeductionCalculation = (deductions) => ZIO.succeed(deductions.foldLeft(BigDecimal(0.0))((amt,deduction) => amt + deduction.amount))


  class PayDayCalculationService(logger: Logger[String], taxTableConfig: TaxTableConfig){


    def generatePaySlips(employees: List[Employee]): Task[EmployeesPaySips] = ZIO.foreachParN(20)(employees)(emp => generatePaySlip(emp))

    def generatePaySlip(employee: Employee): Task[EmployeePaySlip] =
      for{

          _ <- logger.info(s"Computing and Generating PaySlip for Employee ${employee.empCode}")
            provinceTaxBracketRate <- taxTableConfig.incomeTaxBracketRateTableLookup(employee.province.code)
            fedTaxBracketRate <- taxTableConfig.incomeTaxBracketRateTableLookup(FEDTAXCODE)
            fedTax <- incomeTaxBracketRateCalculator(fedTaxBracketRate.taxRateBrackets,employee.grossPay,fedTaxBracketRate.highestRate)
            provinceTax <- incomeTaxBracketRateCalculator(provinceTaxBracketRate.taxRateBrackets,employee.grossPay,provinceTaxBracketRate.highestRate)
            fexIncomeTax = IncomeTax(FEDTAXCODE, fedTax)
            provinceIncomeTax = IncomeTax(employee.province.code, provinceTax)
            empDeductions = employee.deductions ++ List(provinceIncomeTax, fexIncomeTax)
            totalEarningsAmount <- earningsComputation(employee.earnings)
            totalDeductionsAmount <- deductionComputation(empDeductions)
            pay = (employee.grossPay - totalDeductionsAmount) + totalEarningsAmount
            paySlip = PaySlip(pay, totalDeductionsAmount, totalEarningsAmount, empDeductions, employee.earnings)
          _ <- logger.info(s"Payslip ${paySlip}")
        }yield(employee,paySlip)

  }

  val payrollEngineLayer : ZLayer[Logging with TaxTableConfiguration, Throwable, PayDayCalculationServiceEngine] =
    ZLayer.fromServices[Logger[String], TaxTableConfig,PayDayCalculationService] {(logger, config) => new PayDayCalculationService(logger, config)}


  def generatePaySlip(employee: Employee): ZIO[PayDayCalculationServiceEngine,Throwable,EmployeePaySlip] =
    ZIO.accessM(_.get.generatePaySlip(employee))


  def generatePaySlips(employees: List[Employee]): ZIO[PayDayCalculationServiceEngine,Throwable,EmployeesPaySips] =
    ZIO.accessM(_.get.generatePaySlips(employees))

}





