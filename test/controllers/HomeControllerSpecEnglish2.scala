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
import com.ideal.linked.data.accessor.neo4j.Neo4JAccessor
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, ToposoidUtils}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentence, InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, status, _}
import play.api.test.{FakeRequest, _}
import io.jvm.uuid.UUID

import scala.concurrent.duration.DurationInt

class HomeControllerSpecEnglish2 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

  before {
    Neo4JAccessor.delete()
    Thread.sleep(1000)
  }

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]
  
  val sentenceA = "Mark has overcome many problems."
  val sentenceB = "He has a good chance."
  val sentenceC = "His life is so comfortable now."

  val paraphraseA = "Mark has overcome many troubles."
  val paraphraseB = "He has a good opportunity."
  val paraphraseC = "His living is so comfortable now."

  def registSingleClaim(knowledgeForParser:KnowledgeForParser): Unit = {
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)
  }

  "The specification11" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      //val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      //val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)

      registSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge3))
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

  "The specification12" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      //val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge1)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId1, sentenceId2, knowledge3)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

  "The specification13" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      //val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge2)),
        List.empty[PropositionRelation],
        List(KnowledgeForParser(propositionId1, sentenceId2, knowledge3)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

  "The specification14" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId1, sentenceId1, knowledge1),KnowledgeForParser(propositionId1, sentenceId2, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId1, sentenceId3, knowledge3)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

  "The specification15" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString

      val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)


      registSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge3))
      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId2, sentenceId2, knowledge1),KnowledgeForParser(propositionId2, sentenceId3, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId2, sentenceId4, knowledge3)),
        List.empty[PropositionRelation])

      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

  "The specification16" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)

      registSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1))

      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId2, sentenceId2, knowledge1),KnowledgeForParser(propositionId2, sentenceId3, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId2, sentenceId4,knowledge3)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)

      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

  "The specification17" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val propositionId3 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val sentenceId5 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)

      registSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1))
      registSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2))
      val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
        List(KnowledgeForParser(propositionId3, sentenceId3, knowledge1),KnowledgeForParser(propositionId3, sentenceId4, knowledge2)),
        List(PropositionRelation("AND", 0,1)),
        List(KnowledgeForParser(propositionId3, sentenceId5,knowledge3)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)
      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 1)
    }
  }

  "The specification17A" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val propositionId2 = UUID.random.toString
      val propositionId3 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val sentenceId2 = UUID.random.toString
      val sentenceId3 = UUID.random.toString
      val sentenceId4 = UUID.random.toString
      val sentenceId5 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA, "en_US", "{}", false)
      val knowledge2 = Knowledge(sentenceB, "en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC, "en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA, "en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB, "en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC, "en_US", "{}", false)

      registSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1))
      registSingleClaim(KnowledgeForParser(propositionId2, sentenceId2, knowledge2))
      registSingleClaim(KnowledgeForParser(propositionId3, sentenceId3, knowledge3))

      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(PREMISE.index) && x.deductionResult.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceType.equals(CLAIM.index) && x.deductionResult.havePremiseInGivenProposition).size == 0)
    }
  }

  "The specification18" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      //val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      //val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)

      registSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge1))

      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

  "The specification19" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      //val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      //val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)
      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)
      registSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge2))

      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

  "The specification20" should {
    "returns an appropriate response" in {
      val propositionId1 = UUID.random.toString
      val sentenceId1 = UUID.random.toString
      //val knowledge1 = Knowledge(sentenceA,"en_US", "{}", false)
      //val knowledge2 = Knowledge(sentenceB,"en_US", "{}", false)
      val knowledge3 = Knowledge(sentenceC,"en_US", "{}", false)

      val paraphrase1 = Knowledge(paraphraseA,"en_US", "{}", false)
      val paraphrase2 = Knowledge(paraphraseB,"en_US", "{}", false)
      val paraphrase3 = Knowledge(paraphraseC,"en_US", "{}", false)

      registSingleClaim(KnowledgeForParser(propositionId1, sentenceId1, knowledge3))

      val propositionIdForInference = UUID.random.toString
      val premiseKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase1))
      val claimKnowledge = List(KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase2), KnowledgeForParser(propositionIdForInference, UUID.random.toString, paraphrase3))
      val inputSentence = Json.toJson(InputSentenceForParser(premiseKnowledge, claimKnowledge)).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"), "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
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

}

