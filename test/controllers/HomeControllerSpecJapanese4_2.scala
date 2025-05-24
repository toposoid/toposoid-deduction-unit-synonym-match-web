/*
 * Copyright 2021 Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import akka.util.Timeout
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common._
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import com.ideal.linked.toposoid.test.utils.TestUtils
import io.jvm.uuid.UUID
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, status, _}
import play.api.test._

import scala.concurrent.duration.DurationInt

class HomeControllerSpecJapanese4_2 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

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

  val sentenceA = "太郎はある調査を進めてきた。"
  val sentenceB = "太郎は秀逸な発案をした。"
  val sentenceC = "それは人事改善の措置だった。"
  val sentenceD = "太郎は素晴らしい評価を得た。"

  val paraphraseA = "太郎はある分析を進めてきた。"
  val paraphraseB = "太郎は秀逸な提案をした。"
  val paraphraseC = "それは人事改善の対策だった。"
  val paraphraseD = "太郎は素晴らしい評価を受けた。"

 
  /*
  "The specification31" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      //val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      //val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1))
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2))
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification32" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      //val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      //val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge3))
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge4))

      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification33" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      //val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      //val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge1)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId1, sentenceId2, knowledge3)),
        List.empty[PropositionRelation]
      )
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification34" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      //val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), KnowledgeForParser(propositionId1, sentenceId2, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId1, sentenceId3, knowledge3)),
        List.empty[PropositionRelation])
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification35" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      //val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge1)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId1, sentenceId2, knowledge3), KnowledgeForParser(propositionId1, sentenceId3, knowledge4)),
        List(PropositionRelation("AND", 0,1)))
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }
  */
  "The specification36" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), KnowledgeForParser(propositionId1, sentenceId2, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId1, sentenceId3, knowledge3), KnowledgeForParser(propositionId1, sentenceId4, knowledge4)),
        List(PropositionRelation("AND", 0,1)))
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification37" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val propositionId3 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val sentenceId5 = UUID.random.toString
      val sentenceId6 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId3, sentenceId3, knowledge1), KnowledgeForParser(propositionId3, sentenceId4, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId3, sentenceId5, knowledge3), KnowledgeForParser(propositionId3, sentenceId6, knowledge4)),
        List(PropositionRelation("AND", 0,1)))
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 2)
    }
  }

  "The specification37A" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val propositionId3 = UUID.random.toString
      val propositionId4 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val sentenceId5 = UUID.random.toString
      val sentenceId6 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA, "ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB, "ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC, "ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD, "ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA, "ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB, "ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC, "ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD, "ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId3, sentenceId3, knowledge3), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId4, sentenceId4, knowledge4), transversalState)

      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification38" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val sentenceId5 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId2, sentenceId2, knowledge1), KnowledgeForParser(propositionId2, sentenceId3, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId2, sentenceId4, knowledge3), KnowledgeForParser(propositionId2, sentenceId5, knowledge4)),
        List(PropositionRelation("AND", 0,1)))
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification39" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val sentenceId5 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge3), transversalState)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId2, sentenceId2, knowledge1), KnowledgeForParser(propositionId2, sentenceId3, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId2, sentenceId4, knowledge3), KnowledgeForParser(propositionId2, sentenceId5, knowledge4)),
        List(PropositionRelation("AND", 0,1)))
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()

      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification40" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val propositionId3 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val sentenceId5 = UUID.random.toString
      val sentenceId6 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"ja_JP", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"ja_JP", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"ja_JP", "{}", false)
      val knowledge4 = Knowledge(sentenceD,"ja_JP", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"ja_JP", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"ja_JP", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"ja_JP", "{}", false)
      val paraphrase4 = Knowledge(paraphraseD,"ja_JP", "{}", false)

      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1), transversalState)
      TestUtilsEx.registerSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge3), transversalState)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId3, sentenceId3, knowledge1), KnowledgeForParser(propositionId3, sentenceId4, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId3, sentenceId5, knowledge3), KnowledgeForParser(propositionId3, sentenceId6, knowledge4)),
        List(PropositionRelation("AND", 0,1)))
      TestUtils.registerData(knowledgeSentenceSetForParser, transversalState)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase4))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"), "analyze", transversalState)
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json", TRANSVERSAL_STATE.str -> transversalStateJson)
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

}
