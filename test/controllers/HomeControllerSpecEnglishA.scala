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

class HomeControllerSpecEnglishA extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

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

  val sentenceA = "Mark has overcome many problems."
  val sentenceB = "He has a good chance."
  val sentenceC = "His life is so comfortable now."
  val sentenceD = "It's always darkest before the dawn."

  val paraphraseA = "Mark has overcome many troubles."
  val paraphraseB = "He has a good opportunity."
  val paraphraseC = "His lifespan is so comfortable now."
  val paraphraseD = "It's always darkest before the morning."


  //複数の主張(完全一致)
  "The specification1" should {
    "returns an appropriate response" in {
      val sentence1 = "Mark has overcome many problems."
      val paraphrase1 = "Mark has overcome many troubles."
      val sentence2 = "He has a good chance."
      val paraphrase2 = "He has a good opportunity."

      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val knowledge2= Knowledge(sentence2,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      val paraphraseKnowledge2 = Knowledge(paraphrase2,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val propositionIdForInference2 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference2 = java.util.UUID.randomUUID().toString

      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1), KnowledgeForParser(propositionIdForInference2, sentenceIdForInference2, paraphraseKnowledge2))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)      
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 5)
      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=3)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

    }
  }

  //複数の主張(部分一致)
  "The specification2" should {
    "returns an appropriate response" in {
      val sentence1 = "Mark has overcome many problems."
      val paraphrase1 = "Mark has overcome many issues."
      val sentence2 = "He has a good chance."
      val paraphrase2 = "He has a good matter."

      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val knowledge2= Knowledge(sentence2,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      val paraphraseKnowledge2 = Knowledge(paraphrase2,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val propositionIdForInference2 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference2 = java.util.UUID.randomUUID().toString

      val premiseKnowledge = List.empty[KnowledgeForParser]
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1), KnowledgeForParser(propositionIdForInference2, sentenceIdForInference2, paraphraseKnowledge2))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)      
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 5)
      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=3)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

    }
  }
  
  //一対の前提と主張(完全一致)
  "The specification3" should {
    "returns an appropriate response" in {
      val sentence1 = "Mark has overcome many problems."
      val paraphrase1 = "Mark has overcome many troubles."
      val sentence2 = "He has a good chance."
      val paraphrase2 = "He has a good opportunity."

      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val knowledge2= Knowledge(sentence2,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      val paraphraseKnowledge2 = Knowledge(paraphrase2,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val propositionIdForInference2 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference2 = java.util.UUID.randomUUID().toString

      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference2, sentenceIdForInference2, paraphraseKnowledge2))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)      
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 5)
      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=3)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

    }
  }

  //一対の前提と主張(部分一致)
  "The specification4" should {
    "returns an appropriate response" in {
      val sentence1 = "Mark has overcome many problems."
      val paraphrase1 = "Mark has overcome many issues."
      val sentence2 = "He has a good chance."
      val paraphrase2 = "He has a good matter."

      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val knowledge2= Knowledge(sentence2,"en_US", "{}", false)
      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      val paraphraseKnowledge2 = Knowledge(paraphrase2,"en_US", "{}", false)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)
      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val propositionIdForInference2 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference2 = java.util.UUID.randomUUID().toString

      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference2, sentenceIdForInference2, paraphraseKnowledge2))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)      
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 5)
      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=3)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

    }
  }
  
  //２対の前提と主張(完全一致)
  "The specification5" should {
    "returns an appropriate response" in {
      val sentence1 = "Mark has overcome many problems."
      val paraphrase1 = "Mark has overcome many troubles."
      val sentence2 = "He has a good chance."
      val paraphrase2 = "He has a good opportunity."

      val sentence3 = "His life is so comfortable now."
      val paraphrase3 = "His lifespan is so comfortable now."
      val sentence4 = "It's always darkest before the dawn."
      val paraphrase4 = "It's always darkest before the morning."

      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val knowledge2= Knowledge(sentence2,"en_US", "{}", false)
      val propositionId3 = java.util.UUID.randomUUID().toString
      val sentenceId3 = java.util.UUID.randomUUID().toString
      val knowledge3 = Knowledge(sentence3,"en_US", "{}", false)
      val propositionId4 = java.util.UUID.randomUUID().toString
      val sentenceId4 = java.util.UUID.randomUUID().toString
      val knowledge4= Knowledge(sentence4,"en_US", "{}", false)

      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      val paraphraseKnowledge2 = Knowledge(paraphrase2,"en_US", "{}", false)
      val paraphraseKnowledge3 = Knowledge(paraphrase3,"en_US", "{}", false)
      val paraphraseKnowledge4 = Knowledge(paraphrase4,"en_US", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId3, sentenceId3, knowledge3), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId4, sentenceId4, knowledge4), transversalState)

      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val propositionIdForInference2 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference2 = java.util.UUID.randomUUID().toString
      val propositionIdForInference3 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference3 = java.util.UUID.randomUUID().toString
      val propositionIdForInference4 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference4 = java.util.UUID.randomUUID().toString

      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1), KnowledgeForParser(propositionIdForInference2, sentenceIdForInference2, paraphraseKnowledge2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference3, sentenceIdForInference3, paraphraseKnowledge3), KnowledgeForParser(propositionIdForInference4, sentenceIdForInference4, paraphraseKnowledge4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)      
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 9)
      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=3)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference3, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference3, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference3, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference4, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference4, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference4, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

    }
  } 
  
  //２対の前提と主張(部分一致)
  "The specification6" should {
    "returns an appropriate response" in {
      val sentence1 = "Mark has overcome many problems."
      val paraphrase1 = "Mark has overcome many issues."
      val sentence2 = "He has a good chance."
      val paraphrase2 = "He has a good matter."

      val sentence3 = "His life is so comfortable now."
      val paraphrase3 = "His world is so comfortable now."
      val sentence4 = "It's always darkest before the dawn."
      val paraphrase4 = "It's always darkest before the sunrise."

      val propositionId1 = java.util.UUID.randomUUID().toString
      val sentenceId1 = java.util.UUID.randomUUID().toString
      val knowledge1 = Knowledge(sentence1,"en_US", "{}", false)
      val propositionId2 = java.util.UUID.randomUUID().toString
      val sentenceId2 = java.util.UUID.randomUUID().toString
      val knowledge2= Knowledge(sentence2,"en_US", "{}", false)
      val propositionId3 = java.util.UUID.randomUUID().toString
      val sentenceId3 = java.util.UUID.randomUUID().toString
      val knowledge3 = Knowledge(sentence3,"en_US", "{}", false)
      val propositionId4 = java.util.UUID.randomUUID().toString
      val sentenceId4 = java.util.UUID.randomUUID().toString
      val knowledge4= Knowledge(sentence4,"en_US", "{}", false)

      val paraphraseKnowledge1 = Knowledge(paraphrase1,"en_US", "{}", false)
      val paraphraseKnowledge2 = Knowledge(paraphrase2,"en_US", "{}", false)
      val paraphraseKnowledge3 = Knowledge(paraphrase3,"en_US", "{}", false)
      val paraphraseKnowledge4 = Knowledge(paraphrase4,"en_US", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId3, sentenceId3, knowledge3), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId4, sentenceId4, knowledge4), transversalState)

      val propositionIdForInference1 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference1 = java.util.UUID.randomUUID().toString
      val propositionIdForInference2 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference2 = java.util.UUID.randomUUID().toString
      val propositionIdForInference3 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference3 = java.util.UUID.randomUUID().toString
      val propositionIdForInference4 = java.util.UUID.randomUUID().toString
      val sentenceIdForInference4 = java.util.UUID.randomUUID().toString

      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference1, sentenceIdForInference1, paraphraseKnowledge1), KnowledgeForParser(propositionIdForInference2, sentenceIdForInference2, paraphraseKnowledge2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference3, sentenceIdForInference3, paraphraseKnowledge3), KnowledgeForParser(propositionIdForInference4, sentenceIdForInference4, paraphraseKnowledge4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge, ActionModeType.DEDUCTION_MODE.index)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze", transversalState)      
      val updatedAsosJson = TestUtils.analyzeByBaseDeductionUnit(json, transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(updatedAsosJson))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val verifyingEdgesList: List[VerifyingEdges] = Json.parse(jsonResult).as[List[VerifyingEdges]]
      assert(verifyingEdgesList.map(x => x.coveredPropositionEdges.size).sum == 9)
      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference1, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=3)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference2, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference3, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference3, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference3, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

      TestUtils.checkMatchedBothSide(json = json, sentenceId = sentenceIdForInference4, verifyingEdgesList=verifyingEdgesList, correctSize=0)   
      TestUtils.checkMatchedOneSide(json = json, sentenceId = sentenceIdForInference4, verifyingEdgesList=verifyingEdgesList, correctSize=2)   
      TestUtils.checkNoMatch(json = json, sentenceId = sentenceIdForInference4, verifyingEdgesList=verifyingEdgesList, correctSize=0)   

    }
  }  
  
}
