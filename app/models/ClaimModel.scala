package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System,SystemDataStore,DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class Claim (
    _id: BSONObjectID,
    docnum: Int,
    p: PersonDetail,  // Applicant
    ed: ExpenseDetail,      // Expense Detail
    wf: ClaimFormWorkflow,            // Workflow
    wfs: ClaimFormWorkflowStatus,    // Workflow Status
    wfat: ClaimFormWorkflowAssignTo,
    wfa: ClaimFormWorkflowAction,
    wdadat: ClaimFormWorkflowActionDate,
    sys: Option[System]
)

case class ExpenseDetail (
    rdat: Option[DateTime],    // Receipt Date
    cat: String,               // Category
    glc: String,               // GL Code
    amt: CurrencyAmount,        // Claim Amount
    er: Double,                 // Exchange Rate
    aamt: CurrencyAmount,      // Approve Amount
    gstamt: TaxDetail,        // GST Amount
    iamt:  CurrencyAmount,      // Item Amount
    d: String                  // Description
)

case class TaxDetail (
    cn: String,          // Company
    crnum: String,       // Company Register Number
    tnum: String,        // Tax Number
    tamt: CurrencyAmount // Tax Amount
)

case class ClaimFormWorkflow (
    papr: PersonDetail,    // Pending Approver
    s: String               // Status
)

case class ClaimFormWorkflowStatus (
    s1: String,            // Status
    s2: String,
    s3: String,
    s4: String,
    s5: String,
    s6: String,
    s7: String,
    s8: String,
    s9: String,
    s10: String
)

case class ClaimFormWorkflowAssignTo (
    at1: PersonDetail,      // Assigned To
    at2: PersonDetail,
    at3: PersonDetail,
    at4: PersonDetail,
    at5: PersonDetail,
    at6: PersonDetail,
    at7: PersonDetail,
    at8: PersonDetail,
    at9: PersonDetail,
    at10: PersonDetail
)

case class ClaimFormWorkflowAction (
    a1: String,            // Action
    a2: String,
    a3: String,
    a4: String,
    a5: String,
    a6: String,
    a7: String,
    a8: String,
    a9: String,
    a10: String
)

case class ClaimFormWorkflowActionDate (
    ad1: Option[DateTime],      // Action Date
    ad2: Option[DateTime],
    ad3: Option[DateTime],
    ad4: Option[DateTime],
    ad5: Option[DateTime],
    ad6: Option[DateTime],
    ad7: Option[DateTime],
    ad8: Option[DateTime],
    ad9: Option[DateTime],
    ad10: Option[DateTime]
)

object ClaimModel {
  
  // Use Reader to deserialize document automatically
  implicit object SystemBSONReader extends BSONDocumentReader[System] {
    def read(p_doc: BSONDocument): System = {
      System(
          p_doc.getAs[String]("eid").map(v => v),
          p_doc.getAs[BSONDateTime]("cdat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("mdat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[String]("mby").map(v => v),
          p_doc.getAs[BSONDateTime]("ddat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[String]("dby").map(v => v),
          p_doc.getAs[BSONDateTime]("ll").map(dt => new DateTime(dt.value))
      )
    }
  }
  
  implicit object CurrencyAmountBSONReader extends BSONDocumentReader[CurrencyAmount] {
    def read(p_doc: BSONDocument): CurrencyAmount = {
      CurrencyAmount(
          p_doc.getAs[String]("ccy").get,
          p_doc.getAs[Double]("amt").get
      )
    }
  }
  
  implicit object PersonDetailBSONReader extends BSONDocumentReader[PersonDetail] {
    def read(p_doc: BSONDocument): PersonDetail = {
      PersonDetail(
          p_doc.getAs[String]("n").get,
          p_doc.getAs[String]("id").get
      )
    }
  }
  
  implicit object ClaimFormWorkflowActionDateBSONReader extends BSONDocumentReader[ClaimFormWorkflowActionDate] {
    def read(p_doc: BSONDocument): ClaimFormWorkflowActionDate = {
      ClaimFormWorkflowActionDate(
          p_doc.getAs[BSONDateTime]("ad1").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad2").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad3").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad4").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad5").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad6").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad7").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad8").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad9").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("ad10").map(dt => new DateTime(dt.value))
      )
    }
  }

  implicit object ClaimFormWorkflowActionBSONReader extends BSONDocumentReader[ClaimFormWorkflowAction] {
    def read(p_doc: BSONDocument): ClaimFormWorkflowAction = {
      ClaimFormWorkflowAction(
          p_doc.getAs[String]("a1").get,
          p_doc.getAs[String]("a2").get,
          p_doc.getAs[String]("a3").get,
          p_doc.getAs[String]("a4").get,
          p_doc.getAs[String]("a5").get,
          p_doc.getAs[String]("a6").get,
          p_doc.getAs[String]("a7").get,
          p_doc.getAs[String]("a8").get,
          p_doc.getAs[String]("a9").get,
          p_doc.getAs[String]("a10").get
      )
    }
  }
  
  implicit object ClaimFormWorkflowAssignToBSONReader extends BSONDocumentReader[ClaimFormWorkflowAssignTo] {
    def read(p_doc: BSONDocument): ClaimFormWorkflowAssignTo = {
      ClaimFormWorkflowAssignTo(
          p_doc.getAs[PersonDetail]("at1").get,
          p_doc.getAs[PersonDetail]("at2").get,
          p_doc.getAs[PersonDetail]("at3").get,
          p_doc.getAs[PersonDetail]("at4").get,
          p_doc.getAs[PersonDetail]("at5").get,
          p_doc.getAs[PersonDetail]("at6").get,
          p_doc.getAs[PersonDetail]("at7").get,
          p_doc.getAs[PersonDetail]("at8").get,
          p_doc.getAs[PersonDetail]("at9").get,
          p_doc.getAs[PersonDetail]("at10").get
      )
    }
  }
  
  implicit object ClaimFormWorkflowStatusBSONReader extends BSONDocumentReader[ClaimFormWorkflowStatus] {
    def read(p_doc: BSONDocument): ClaimFormWorkflowStatus = {
      ClaimFormWorkflowStatus(
          p_doc.getAs[String]("s1").get,
          p_doc.getAs[String]("s2").get,
          p_doc.getAs[String]("s3").get,
          p_doc.getAs[String]("s4").get,
          p_doc.getAs[String]("s5").get,
          p_doc.getAs[String]("s6").get,
          p_doc.getAs[String]("s7").get,
          p_doc.getAs[String]("s8").get,
          p_doc.getAs[String]("s9").get,
          p_doc.getAs[String]("s10").get
      )
    }
  }
  
  implicit object ClaimFormWorkflowBSONReader extends BSONDocumentReader[ClaimFormWorkflow ] {
    def read(p_doc: BSONDocument): ClaimFormWorkflow = {
      ClaimFormWorkflow(
          p_doc.getAs[PersonDetail]("papr").get,
          p_doc.getAs[String]("s").get
      )
    }
  }
  
  implicit object TaxDetailBSONReader extends BSONDocumentReader[TaxDetail] {
    def read(p_doc: BSONDocument): TaxDetail = {
      TaxDetail(
          p_doc.getAs[String]("cn").get,
          p_doc.getAs[String]("crnum").get,
          p_doc.getAs[String]("tnum").get,
          p_doc.getAs[CurrencyAmount]("tamt").get
      )
    }
  }
  
  implicit object ExpenseDetailBSONReader extends BSONDocumentReader[ExpenseDetail] {
    def read(p_doc: BSONDocument): ExpenseDetail = {
      ExpenseDetail(
          p_doc.getAs[BSONDateTime]("rdat").map(dt => new DateTime(dt.value )),
          p_doc.getAs[String]("cat").get,
          p_doc.getAs[String]("glc").get,
          p_doc.getAs[CurrencyAmount]("amt").get,
          p_doc.getAs[Double]("er").get,
          p_doc.getAs[CurrencyAmount]("aamt").get,
          p_doc.getAs[TaxDetail]("gstamt").get,
          p_doc.getAs[CurrencyAmount]("iamt").get,
          p_doc.getAs[String]("d").get
      )
    }
  }
  
  implicit object ClaimBSONReader extends BSONDocumentReader[Claim] {
    def read(p_doc: BSONDocument): Claim = {
      Claim(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[Int]("docnum").get,
          p_doc.getAs[PersonDetail]("p").get,
          p_doc.getAs[ExpenseDetail]("ed").get,
          p_doc.getAs[ClaimFormWorkflow]("wf").get,
          p_doc.getAs[ClaimFormWorkflowStatus]("wfs").get,
          p_doc.getAs[ClaimFormWorkflowAssignTo]("wfat").get,
          p_doc.getAs[ClaimFormWorkflowAction]("wfa").get,
          p_doc.getAs[ClaimFormWorkflowActionDate]("wdadat").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object SystemBSONWriter extends BSONDocumentWriter[System] {
    def write(p_doc: System): BSONDocument = {
      BSONDocument(
          "eid" -> p_doc.eid,
          "cdat" -> p_doc.cdat.map(date => BSONDateTime(date.getMillis)),
          "mdat" -> p_doc.mdat.map(date => BSONDateTime(date.getMillis)),
          "mby" -> p_doc.mby,
          "ddat" -> p_doc.ddat.map(date => BSONDateTime(date.getMillis)),
          "dby" -> p_doc.dby,
          "ll" -> p_doc.ll.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
  
  implicit object CurrencyAmountBSONWriter extends BSONDocumentWriter[CurrencyAmount] {
    def write(p_doc: CurrencyAmount): BSONDocument = {
      BSONDocument(
          "ccy" -> p_doc.ccy,
          "amt" -> p_doc.amt

      )     
    }
  }
  
  implicit object PersonDetailBSONWriter extends BSONDocumentWriter[PersonDetail] {
    def write(p_doc: PersonDetail): BSONDocument = {
      BSONDocument(
          "n" -> p_doc.n,
          "id" -> p_doc.id
      )     
    }
  }
  
  implicit object ClaimFormWorkflowActionDateBSONWriter extends BSONDocumentWriter[ClaimFormWorkflowActionDate] {
    def write(p_doc: ClaimFormWorkflowActionDate): BSONDocument = {
      BSONDocument(
          "ad1" -> p_doc.ad1.map(date => BSONDateTime(date.getMillis)),
          "ad2" -> p_doc.ad2.map(date => BSONDateTime(date.getMillis)),
          "ad3" -> p_doc.ad3.map(date => BSONDateTime(date.getMillis)),
          "ad4" -> p_doc.ad4.map(date => BSONDateTime(date.getMillis)),
          "ad5" -> p_doc.ad5.map(date => BSONDateTime(date.getMillis)),
          "ad6" -> p_doc.ad6.map(date => BSONDateTime(date.getMillis)),
          "ad7" -> p_doc.ad7.map(date => BSONDateTime(date.getMillis)),
          "ad8" -> p_doc.ad8.map(date => BSONDateTime(date.getMillis)),
          "ad9" -> p_doc.ad9.map(date => BSONDateTime(date.getMillis)),
          "ad10" -> p_doc.ad10.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
  
  implicit object ClaimFormWorkflowActionBSONWriter extends BSONDocumentWriter[ClaimFormWorkflowAction] {
    def write(p_doc: ClaimFormWorkflowAction): BSONDocument = {
      BSONDocument(
          "a1" -> p_doc.a1,
          "a2" -> p_doc.a2,
          "a3" -> p_doc.a3,
          "a4" -> p_doc.a4,
          "a5" -> p_doc.a5,
          "a6" -> p_doc.a6,
          "a7" -> p_doc.a7,
          "a8" -> p_doc.a8,
          "a9" -> p_doc.a9,
          "a10" -> p_doc.a10
      )     
    }
  }
  
  implicit object ClaimFormWorkflowAssignToBSONWriter extends BSONDocumentWriter[ClaimFormWorkflowAssignTo] {
    def write(p_doc: ClaimFormWorkflowAssignTo): BSONDocument = {
      BSONDocument(
          "at1" -> p_doc.at1,
          "at2" -> p_doc.at2,
          "at3" -> p_doc.at3,
          "at4" -> p_doc.at4,
          "at5" -> p_doc.at5,
          "at6" -> p_doc.at6,
          "at7" -> p_doc.at7,
          "at8" -> p_doc.at8,
          "at9" -> p_doc.at9,
          "at10" -> p_doc.at10
      )     
    }
  }
  
  implicit object ClaimFormWorkflowStatusBSONWriter extends BSONDocumentWriter[ClaimFormWorkflowStatus] {
    def write(p_doc: ClaimFormWorkflowStatus): BSONDocument = {
      BSONDocument(
          "s1" -> p_doc.s1,
          "s2" -> p_doc.s2,
          "s3" -> p_doc.s3,
          "s4" -> p_doc.s4,
          "s5" -> p_doc.s5,
          "s6" -> p_doc.s6,
          "s7" -> p_doc.s7,
          "s8" -> p_doc.s8,
          "s9" -> p_doc.s9,
          "s10" -> p_doc.s10
      )     
    }
  }
  
  implicit object ClaimFormWorkflowBSONWriter extends BSONDocumentWriter[ClaimFormWorkflow] {
    def write(p_doc: ClaimFormWorkflow): BSONDocument = {
      BSONDocument(
          "papr" -> p_doc.papr,
          "s" -> p_doc.s
      )     
    }
  }
  
  implicit object TaxDetailBSONWriter extends BSONDocumentWriter[TaxDetail] {
    def write(p_doc: TaxDetail): BSONDocument = {
      BSONDocument(
          "cn" -> p_doc.cn,
          "crnum" -> p_doc.crnum,
          "tnum" -> p_doc.tnum,
          "tamt" -> p_doc.tamt
      )     
    }
  }
  
  implicit object ExpenseDetailBSONWriter extends BSONDocumentWriter[ExpenseDetail] {
    def write(p_doc: ExpenseDetail): BSONDocument = {
      BSONDocument(
          "rdat" -> p_doc.rdat.map(date => BSONDateTime(date.getMillis)),
          "cat" -> p_doc.cat,
          "glc" -> p_doc.glc,
          "amt" -> p_doc.amt,
          "er" -> p_doc.er,
          "aamt" -> p_doc.aamt,
          "gstamt" -> p_doc.gstamt,
          "iamt" -> p_doc.iamt,
          "d" -> p_doc.d
      )     
    }
  }
  
  implicit object ClaimBSONWriter extends BSONDocumentWriter[Claim] {
    def write(p_doc: Claim): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "docnum" -> p_doc.docnum,
          "p" -> p_doc.p,
          "ed" -> p_doc.ed,
          "wf" -> p_doc.wf,
          "wfs" -> p_doc.wfs,
          "wfat" -> p_doc.wfat,
          "wfa" -> p_doc.wfa,
          "wdadat" -> p_doc.wdadat,
          "sys" -> p_doc.sys
      )     
    }
  }
  
  private val col = DbConnUtility.claim_db.collection("claim")
    
  val doc = Claim(
      _id = BSONObjectID.generate,
      docnum = 0,
      p = PersonDetail(n="", id=""),
      ed = ExpenseDetail(rdat=Some(new DateTime()), cat="", glc="", amt=CurrencyAmount(ccy="", amt=0.0), er=1.0, aamt=CurrencyAmount(ccy="", amt=0.0), gstamt=TaxDetail(cn="", crnum="", tnum="", tamt=CurrencyAmount(ccy="", amt=0.0)), iamt=CurrencyAmount(ccy="", amt=0.0), d=""),
      wf = ClaimFormWorkflow(papr=PersonDetail(n="", id=""), s="New"),
      wfs = ClaimFormWorkflowStatus(s1="", s2="", s3="", s4="", s5="", s6="", s7="", s8="", s9="", s10=""),
      wfat = ClaimFormWorkflowAssignTo(at1=PersonDetail(n="", id=""), at2=PersonDetail(n="", id=""), at3=PersonDetail(n="", id=""), at4=PersonDetail(n="", id=""), at5=PersonDetail(n="", id=""), at6=PersonDetail(n="", id=""), at7=PersonDetail(n="", id=""), at8=PersonDetail(n="", id=""), at9=PersonDetail(n="", id=""), at10=PersonDetail(n="", id="")),
      wfa = ClaimFormWorkflowAction(a1="", a2="", a3="", a4="", a5="", a6="", a7="", a8="", a9="", a10=""),
      wdadat = ClaimFormWorkflowActionDate(ad1=None, ad2=None, ad3=None, ad4=None, ad5=None, ad6=None, ad7=None, ad8=None, ad9=None, ad10=None),
      sys = None
  )
  
  private def updateSystem(p_doc:Claim) = {
    val eid = p_doc.sys.get.eid.getOrElse(None)
    val cdat = p_doc.sys.get.cdat.getOrElse(None)
    val mdat = p_doc.sys.get.mdat.getOrElse(None)
    val mby = p_doc.sys.get.mby.getOrElse(None)
    val ddat = p_doc.sys.get.ddat.getOrElse(None)
    val dby = p_doc.sys.get.dby.getOrElse(None)
    val ll = p_doc.sys.get.ll.getOrElse(None)
    val sys_doc = System(
        eid = if (eid!=None) {Some(p_doc.sys.get.eid.get)} else {None},
        cdat = if (cdat!=None) {Some(p_doc.sys.get.cdat.get)} else {None},
        mdat = if (mdat!=None) {Some(p_doc.sys.get.mdat.get)} else {None},
        mby = if (mby!=None) {Some(p_doc.sys.get.mby.get)} else {None},
        ddat = if (ddat!=None) {Some(p_doc.sys.get.ddat.get)} else {None},
        dby = if (dby!=None) {Some(p_doc.sys.get.dby.get)} else {None},
        ll= if (ll!=None) {Some(p_doc.sys.get.ll.get)} else {None}
    ) 
    sys_doc
  }
  
  // Insert new document
  def insert(p_doc:Claim, p_eid:String="", p_request:RequestHeader=null)= {
    val future = col.insert(p_doc.copy(sys = SystemDataStore.creation(p_eid,p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  def update(p_query:BSONDocument, p_doc:Claim, p_request:RequestHeader) = {
    val future = col.update(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), p_doc.copy(sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
		
  // Soft deletion by setting deletion flag in document
  def remove(p_query:BSONDocument, p_request:RequestHeader) = {
    for {
      docs <- this.find(p_query, p_request)
    } yield {
      docs.foreach { doc => 
        val future = col.update(BSONDocument("_id" -> doc._id, "sys.ddat"->BSONDocument("$exists"->false)), doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc), p_request)))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {}
        }
      }
    }
  }
  
  // Delete document
  def removePermanently(p_query:BSONDocument) = {
    val future = col.remove(p_query)
  }
	
  // Find all documents
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[Claim](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[Claim](ReadPreference.primary).collect[List]()
  }
	
  // Find and sort all documents using session
  def find(p_query:BSONDocument, p_sort:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).sort(p_sort).cursor[Claim](ReadPreference.primary).collect[List]()
  }
  
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[Claim]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[Claim]
  }
  
}
