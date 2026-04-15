/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import org.apache.pekko.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{SentenceType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentence, InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import com.ideal.linked.toposoid.test.utils.TestUtils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentAsString, contentType, defaultAwaitTimeout, status, _}
import play.api.test.{FakeRequest, _}
//import io.jvm.uuid.UUID

import scala.concurrent.duration.DurationInt
import com.ideal.linked.toposoid.common.ActionModeType
import com.ideal.linked.toposoid.protocol.model.base.VerifyingEdges

class HomeControllerSpecJapanese extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")
  val transversalStateJson:String = Json.toJson(transversalState).toString()

  before {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
    Thread.sleep(1000)
  }

  override def beforeAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def afterAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]

  
  val sentenceB = "太郎は秀逸な発案をした。"
  val sentenceC = "それは人事改善の措置だった。"

  
  val paraphraseB = "太郎は秀逸な提案をした。"
  val paraphraseC = "それは人事改善の対策だった。"

  //片側対象、片側一致
  "The specification1" should {
    "returns an appropriate response" in {
      val sentence1 = "太郎はある調査を進めてきた。"
      val paraphrase1 = "太郎はある分析を進めてきた。"
      
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"ja_JP", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val updatedAsosJson = TestUtilsEx.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 2)
      TestUtilsEx.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtilsEx.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtilsEx.checkNeverMatched(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
    }
  }
  //片側対象、片側不一致
  "The specification2" should {
    "returns an appropriate response" in {
      val sentence1 = "太郎はある調査を進めてきた。"
      val paraphrase1 = "太郎はある考察を進めてきた。"
      
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"ja_JP", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val updatedAsosJson = TestUtilsEx.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]   
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 2)   
      TestUtilsEx.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0) 
      TestUtilsEx.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)     
      TestUtilsEx.checkNeverMatched(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)
    }
  }
  //両側対象、両側一致
  "The specification3" should {
    "returns an appropriate response" in {
      val sentence1 = "太郎はある調査を進めてきた。"
      val paraphrase1 = "太郎はある分析を推し進めてきた。"
      
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"ja_JP", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val updatedAsosJson = TestUtilsEx.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 3)
      TestUtilsEx.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=3)  
      TestUtilsEx.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      TestUtilsEx.checkNeverMatched(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
    }
  }
  //両側対象、片側のみ一致
  "The specification4" should {
    "returns an appropriate response" in {
      val sentence1 = "太郎はある調査を進めてきた。"
      val paraphrase1 = "太郎はある考察を推し進めてきた。"
      
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"ja_JP", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val updatedAsosJson = TestUtilsEx.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 3)      
      TestUtilsEx.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)   
      TestUtilsEx.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)         
      TestUtilsEx.checkNeverMatched(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)
    }
  }

  //両側対象、否定一致
  "The specification5" should {
    "returns an appropriate response" in {
      val sentence1 = "太郎はある調査を進めなかった。"
      val paraphrase1 = "太郎はある分析を推し進めなかった。"
      
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"ja_JP", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val updatedAsosJson = TestUtilsEx.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 3)
      TestUtilsEx.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=3)  
      TestUtilsEx.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      TestUtilsEx.checkNeverMatched(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
    }
  }

  //両側対象、否定不一致
  "The specification6" should {
    "returns an appropriate response" in {
      val sentence1 = "太郎はある調査を進めてきた。"
      val paraphrase1 = "太郎はある分析を推し進めなかった。"
      
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"ja_JP", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val updatedAsosJson = TestUtilsEx.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 1)
      TestUtilsEx.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)  
      TestUtilsEx.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      TestUtilsEx.checkNeverMatched(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
    }
  }

  //全て被覆できないケース
  "The specification7" should {
    "returns an appropriate response" in {
      val sentence1 = "太郎はある調査を進めてきた。"
      val paraphrase1 = "太郎はある考察を深めた。"
      
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"ja_JP", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val updatedAsosJson = TestUtilsEx.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 3)
      TestUtilsEx.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)  
      TestUtilsEx.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)  
      TestUtilsEx.checkNeverMatched(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=1)  
    }
  }  

  /*
  "The specification5" should {
    "returns an appropriate response" in {
      val sentence1 = "やはり太郎はある調査を進めてきた。"
      val paraphrase1 = "太郎はある分析を進めてきた。"
      
      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"ja_JP", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"ja_JP", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val updatedAsosJson = TestUtilsEx.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      TestUtilsEx.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)      
    }
  }
  */

}
