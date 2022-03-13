package com.digitalPlus.payroll.domain

import zio.{UIO, ZIO}

import scala.annotation.tailrec

object adt {

  sealed trait Province{
    val code: String
  }

  case class Ontario(code: String) extends Province
  case class Alberta(code: String) extends Province
  case class Quebec(code: String) extends Province
  case class BC(code: String) extends  Province
  case class Manitoba(code: String) extends Province
  case class NovaScotia(code: String) extends Province
  case class PrinceEdwardIsland(code: String) extends Province
  case class Newfoundland(code: String) extends Province
  case class NewBrunswick(code: String) extends Province

  sealed trait Deduction{
    val code: String
    val amount: BigDecimal
  }

  case class IncomeTax(code: String, amount: BigDecimal) extends Deduction

 sealed trait Earnings{
    val amount: BigDecimal
    val code : String
  }

  case class Basic(code: String,  amount: BigDecimal) extends Earnings

  case class Claim(code: String,  amount: BigDecimal) extends  Earnings

  case class  OverTime(code: String,  amount: BigDecimal) extends Earnings

  case class Allowance(code: String,  amount: BigDecimal) extends Earnings

  case class Others(code: String,  amount: BigDecimal) extends  Earnings

  case class Bonus(code: String,  amount: BigDecimal) extends  Earnings


  case class Employee(empCode: String, province: Province, grossPay: BigDecimal,deductions: List[Deduction] = List.empty, earnings: List[Earnings] = List.empty )


  case class PaySlip(netPay: BigDecimal, totalDeductionAmt: BigDecimal, totalEarningAmt: BigDecimal, deductions: List[Deduction], earnings: List[Earnings])


  case class IncomeTaxBracketRate(percentageRate: Double, lowerBoundIncomeLimit: BigDecimal, upperBoundIncomeLimit: BigDecimal)


   case class TaxBracket(lowerBound: BigDecimal, upperBound: BigDecimal, taxRate: Double)


  case class IncomeTaxBracketRateTable(code: String, taxRateBrackets: List[TaxBracket],highestRate: Double)


   case class TaxTables(taxes: List[IncomeTaxBracketRateTable])

  def incomeTaxBracketRateCalculator(taxRates: List[TaxBracket], income: BigDecimal, upperTaxPer: Double) : UIO[BigDecimal] ={
    val isIncomeWithinTaxBracketRange: (BigDecimal,BigDecimal,BigDecimal) => Boolean = (lowerBound,upperBound,incomeEarn) => (incomeEarn >= lowerBound  &&  incomeEarn <= upperBound)
    @tailrec
    def incomeTaxRateTailRec(taxRatesList: List[TaxBracket], rate: Double) : Double ={
      if(taxRatesList.isEmpty) rate
      else{
        val hd = taxRatesList.head
        if(isIncomeWithinTaxBracketRange(hd.lowerBound,hd.upperBound,income)) hd.taxRate
        else incomeTaxRateTailRec(taxRatesList.tail,rate)
      }
    }
     ZIO.succeed(((incomeTaxRateTailRec(taxRates,upperTaxPer) / 100) * income))

  }


}
