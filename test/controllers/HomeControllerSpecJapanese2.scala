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
import com.ideal.linked.toposoid.common.ToposoidUtils
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.parser.InputSentence
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, status, _}
import play.api.test.{FakeRequest, _}

import scala.concurrent.duration.DurationInt

class HomeControllerSpecJapanese2 extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with DefaultAwaitTimeout with Injecting {

  before {
    Neo4JAccessor.delete()
  }

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override implicit def defaultAwaitTimeout: Timeout = 600.seconds
  val controller: HomeController = inject[HomeController]

/*太郎はある調査を進めてきた。
  太郎はある分析を進めてきた。

太郎は秀逸な発案をした。
太郎は秀逸な提案をした。

それは人事改善の対策だった。
それは人事改善の措置だった。

太郎は素晴らしい評価を得た。
太郎は素晴らしい評価を受けた。
*/


  "The specification21" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false)), List(Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false), Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 2)
    }
  }

  "The specification22" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false), Knowledge("それは人事改善の対策だった。","ja_JP", "{}")),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false)), List(Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false), Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification23" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false), Knowledge("それは人事改善の対策だった。","ja_JP", "{}")),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false)), List(Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false), Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 2)
    }
  }

  "The specification24" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false)), List(Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false), Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification25" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false)), List(Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false), Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification26" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false)), List(Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false), Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification27" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification28" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification29" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 1)
    }
  }

  "The specification30" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎は素晴らしい評価を得た。","ja_JP", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 1)
    }
  }

  "The specification31" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification32" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎は素晴らしい評価を得た。","ja_JP", "{}", false)))
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 2)
    }
  }

  "The specification33" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification34" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}"), Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification35" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を得た。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification36" should {
    "returns an appropriate response" in {
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を得た。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification37" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を得た。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 2)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 2)
    }
  }

  "The specification38" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を得た。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 0)
    }
  }

  "The specification39" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を得た。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 1)
    }
  }

  "The specification40" should {
    "returns an appropriate response" in {
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false)))
      Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false)))
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎はある調査を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}", false)),
        List.empty[PropositionRelation],
        List(Knowledge("それは人事改善の対策だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を得た。","ja_JP", "{}", false)),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)
      val inputSentence = Json.toJson(InputSentence(List(Knowledge("太郎はある分析を進めてきた。","ja_JP", "{}", false), Knowledge("太郎は秀逸な提案をした。","ja_JP", "{}", false)), List(Knowledge("それは人事改善の措置だった。","ja_JP", "{}", false), Knowledge("太郎は素晴らしい評価を受けた。","ja_JP", "{}", false)))).toString()
      val json = ToposoidUtils.callComponent(inputSentence, conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001", "analyze")
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))
      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 1)
    }
  }



}


/*
  "The specification1" should {
    "returns an appropriate response" in {
      val json = """{
                   |    "analyzedSentenceObjects": [
                   |        {
                   |            "nodeMap": {
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-3": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 3,
                   |                    "parentId": -1,
                   |                    "isMainSection": true,
                   |                    "surface": "した。",
                   |                    "normalizedName": "する",
                   |                    "dependType": "D",
                   |                    "caseType": "文末",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "する",
                   |                    "surfaceYomi": "した。",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-2": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 2,
                   |                    "parentId": 3,
                   |                    "isMainSection": false,
                   |                    "surface": "提案を",
                   |                    "normalizedName": "提案",
                   |                    "dependType": "D",
                   |                    "caseType": "ヲ格",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "提案": "抽象物"
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "ていあん",
                   |                    "surfaceYomi": "ていあんを",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-1": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-1",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 1,
                   |                    "parentId": 2,
                   |                    "isMainSection": false,
                   |                    "surface": "秀逸な",
                   |                    "normalizedName": "秀逸だ",
                   |                    "dependType": "D",
                   |                    "caseType": "連格",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "しゅういつだ",
                   |                    "surfaceYomi": "しゅういつな",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-0": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-0",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 0,
                   |                    "parentId": 3,
                   |                    "isMainSection": false,
                   |                    "surface": "太郎は",
                   |                    "normalizedName": "太郎",
                   |                    "dependType": "D",
                   |                    "caseType": "未格",
                   |                    "namedEntity": "PERSON:太郎",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "たろう",
                   |                    "surfaceYomi": "たろうは",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                }
                   |            },
                   |            "edgeList": [
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "caseStr": "ヲ格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                },
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-1",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "caseStr": "連格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                },
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-0",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "caseStr": "未格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                }
                   |            ],
                   |            "sentenceType": 1,
                   |            "deductionResultMap": {
                   |                "0": {
                   |                    "status": false,
                   |                    "matchedPropositionIds": [],
                   |                    "deductionUnit": ""
                   |                },
                   |                "1": {
                   |                    "status": false,
                   |                    "matchedPropositionIds": [],
                   |                    "deductionUnit": ""
                   |                }
                   |            }
                   |        }
                   |    ]
                   |}""".stripMargin
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))

      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("1").get.status).size == 1)
    }
  }

  "The specification2" should {
    "returns an appropriate response" in {
      val json = """{
                   |    "analyzedSentenceObjects": [
                   |        {
                   |            "nodeMap": {
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-3": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 3,
                   |                    "parentId": -1,
                   |                    "isMainSection": true,
                   |                    "surface": "した。",
                   |                    "normalizedName": "する",
                   |                    "dependType": "D",
                   |                    "caseType": "文末",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "する",
                   |                    "surfaceYomi": "した。",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 0,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-2": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 2,
                   |                    "parentId": 3,
                   |                    "isMainSection": false,
                   |                    "surface": "提案を",
                   |                    "normalizedName": "提案",
                   |                    "dependType": "D",
                   |                    "caseType": "ヲ格",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "提案": "抽象物"
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "ていあん",
                   |                    "surfaceYomi": "ていあんを",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 0,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-1": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-1",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 1,
                   |                    "parentId": 2,
                   |                    "isMainSection": false,
                   |                    "surface": "秀逸な",
                   |                    "normalizedName": "秀逸だ",
                   |                    "dependType": "D",
                   |                    "caseType": "連格",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "しゅういつだ",
                   |                    "surfaceYomi": "しゅういつな",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 0,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-0": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-0",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 0,
                   |                    "parentId": 3,
                   |                    "isMainSection": false,
                   |                    "surface": "太郎は",
                   |                    "normalizedName": "太郎",
                   |                    "dependType": "D",
                   |                    "caseType": "未格",
                   |                    "namedEntity": "PERSON:太郎",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "たろう",
                   |                    "surfaceYomi": "たろうは",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 0,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                }
                   |            },
                   |            "edgeList": [
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "caseStr": "ヲ格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                },
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-1",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "caseStr": "連格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                },
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-0",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "caseStr": "未格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                }
                   |            ],
                   |            "sentenceType": 0,
                   |            "deductionResultMap": {
                   |                "0": {
                   |                    "status": false,
                   |                    "matchedPropositionIds": [],
                   |                    "deductionUnit": ""
                   |                },
                   |                "1": {
                   |                    "status": false,
                   |                    "matchedPropositionIds": [],
                   |                    "deductionUnit": ""
                   |                }
                   |            }
                   |        }
                   |    ]
                   |}""".stripMargin
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))

      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 1)
    }
  }

  "The specification3" should {
    "returns an appropriate response" in {
      Neo4JAccessor.delete()
      val knowledgeSentenceSet = KnowledgeSentenceSet(
        List(Knowledge("太郎は秀逸な発案をした。","ja_JP", "{}")),
        List.empty[PropositionRelation],
        List(Knowledge("太郎は脚光を浴びた。","ja_JP", "{}")),
        List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSet)

      val json = """{
                   |    "analyzedSentenceObjects": [
                   |        {
                   |            "nodeMap": {
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-3": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 3,
                   |                    "parentId": -1,
                   |                    "isMainSection": true,
                   |                    "surface": "した。",
                   |                    "normalizedName": "する",
                   |                    "dependType": "D",
                   |                    "caseType": "文末",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "する",
                   |                    "surfaceYomi": "した。",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 0,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-2": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 2,
                   |                    "parentId": 3,
                   |                    "isMainSection": false,
                   |                    "surface": "提案を",
                   |                    "normalizedName": "提案",
                   |                    "dependType": "D",
                   |                    "caseType": "ヲ格",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "提案": "抽象物"
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "ていあん",
                   |                    "surfaceYomi": "ていあんを",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 0,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-1": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-1",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 1,
                   |                    "parentId": 2,
                   |                    "isMainSection": false,
                   |                    "surface": "秀逸な",
                   |                    "normalizedName": "秀逸だ",
                   |                    "dependType": "D",
                   |                    "caseType": "連格",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "しゅういつだ",
                   |                    "surfaceYomi": "しゅういつな",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 0,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                },
                   |                "781d77ce-746f-4d86-8392-dacae3f6c9d2-0": {
                   |                    "nodeId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-0",
                   |                    "propositionId": "781d77ce-746f-4d86-8392-dacae3f6c9d2",
                   |                    "currentId": 0,
                   |                    "parentId": 3,
                   |                    "isMainSection": false,
                   |                    "surface": "太郎は",
                   |                    "normalizedName": "太郎",
                   |                    "dependType": "D",
                   |                    "caseType": "未格",
                   |                    "namedEntity": "PERSON:太郎",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {
                   |                        "": ""
                   |                    },
                   |                    "domains": {
                   |                        "": ""
                   |                    },
                   |                    "isDenialWord": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "たろう",
                   |                    "surfaceYomi": "たろうは",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 0,
                   |                    "lang": "ja_JP",
                   |                    "extentText": "{}"
                   |                }
                   |            },
                   |            "edgeList": [
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "caseStr": "ヲ格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                },
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-1",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-2",
                   |                    "caseStr": "連格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                },
                   |                {
                   |                    "sourceId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-0",
                   |                    "destinationId": "781d77ce-746f-4d86-8392-dacae3f6c9d2-3",
                   |                    "caseStr": "未格",
                   |                    "dependType": "D",
                   |                    "logicType": "-",
                   |                    "lang": "ja_JP"
                   |                }
                   |            ],
                   |            "sentenceType": 0,
                   |            "deductionResultMap": {
                   |                "0": {
                   |                    "status": false,
                   |                    "matchedPropositionIds": [],
                   |                    "deductionUnit": ""
                   |                },
                   |                "1": {
                   |                    "status": false,
                   |                    "matchedPropositionIds": [],
                   |                    "deductionUnit": ""
                   |                }
                   |            }
                   |        }
                   |    ]
                   |}""".stripMargin
      val fr = FakeRequest(POST, "/execute")
        .withHeaders("Content-type" -> "application/json")
        .withJsonBody(Json.parse(json))

      val result = call(controller.execute(), fr)
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val jsonResult: String = contentAsJson(result).toString()
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(jsonResult).as[AnalyzedSentenceObjects]
      assert(analyzedSentenceObjects.analyzedSentenceObjects.filter(_.deductionResultMap.get("0").get.status).size == 0)
    }
  }
   */