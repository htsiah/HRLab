package models

case class PersonDetail(
    n: String,
    id: String
)

case class CurrencyAmount(
    ccy: String,
    amt: Double
)

object CommonCaseClassModel{}