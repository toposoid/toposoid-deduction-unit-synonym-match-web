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

import com.ideal.linked.data.accessor.neo4j.Neo4JAccessor
import com.ideal.linked.toposoid.knowledgebase.regist.model.Knowledge
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, contentType, defaultAwaitTimeout, status, _}
import play.api.test.{FakeRequest, _}

class HomeControllerSpecEnglish extends PlaySpec with BeforeAndAfter with BeforeAndAfterAll with GuiceOneAppPerSuite with Injecting {

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
    Sentence2Neo4jTransformer.createGraphAuto(List(Knowledge("Life is so comfortable.","en_US", "{}")))
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  val controller: HomeController = inject[HomeController]

  "The specification2" should {
    "returns an appropriate response" in {
      val json = """{
                   |    "analyzedSentenceObjects": [
                   |        {
                   |            "nodeMap": {
                   |                "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-0": {
                   |                    "nodeId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-0",
                   |                    "propositionId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a",
                   |                    "currentId": 0,
                   |                    "parentId": 1,
                   |                    "isMainSection": true,
                   |                    "surface": "Living",
                   |                    "normalizedName": "living",
                   |                    "dependType": "-",
                   |                    "caseType": "nsubj",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {},
                   |                    "domains": {},
                   |                    "isDenial": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "",
                   |                    "surfaceYomi": "",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "en_US",
                   |                    "extentText": "{}"
                   |                },
                   |                "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-1": {
                   |                    "nodeId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-1",
                   |                    "propositionId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a",
                   |                    "currentId": 1,
                   |                    "parentId": 1,
                   |                    "isMainSection": true,
                   |                    "surface": "is",
                   |                    "normalizedName": "be",
                   |                    "dependType": "-",
                   |                    "caseType": "ROOT",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {},
                   |                    "domains": {},
                   |                    "isDenial": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "",
                   |                    "surfaceYomi": "",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "en_US",
                   |                    "extentText": "{}"
                   |                },
                   |                "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-2": {
                   |                    "nodeId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-2",
                   |                    "propositionId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a",
                   |                    "currentId": 2,
                   |                    "parentId": 3,
                   |                    "isMainSection": true,
                   |                    "surface": "so",
                   |                    "normalizedName": "so",
                   |                    "dependType": "-",
                   |                    "caseType": "advmod",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {},
                   |                    "domains": {},
                   |                    "isDenial": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "",
                   |                    "surfaceYomi": "",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "en_US",
                   |                    "extentText": "{}"
                   |                },
                   |                "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-3": {
                   |                    "nodeId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-3",
                   |                    "propositionId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a",
                   |                    "currentId": 3,
                   |                    "parentId": 1,
                   |                    "isMainSection": true,
                   |                    "surface": "comfortable",
                   |                    "normalizedName": "comfortable",
                   |                    "dependType": "-",
                   |                    "caseType": "acomp",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {},
                   |                    "domains": {},
                   |                    "isDenial": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "",
                   |                    "surfaceYomi": "",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "en_US",
                   |                    "extentText": "{}"
                   |                },
                   |                "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-4": {
                   |                    "nodeId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-4",
                   |                    "propositionId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a",
                   |                    "currentId": 4,
                   |                    "parentId": 1,
                   |                    "isMainSection": true,
                   |                    "surface": ".",
                   |                    "normalizedName": ".",
                   |                    "dependType": "-",
                   |                    "caseType": "punct",
                   |                    "namedEntity": "",
                   |                    "rangeExpressions": {
                   |                        "": {}
                   |                    },
                   |                    "categories": {},
                   |                    "domains": {},
                   |                    "isDenial": false,
                   |                    "isConditionalConnection": false,
                   |                    "normalizedNameYomi": "",
                   |                    "surfaceYomi": "",
                   |                    "modalityType": "-",
                   |                    "logicType": "-",
                   |                    "nodeType": 1,
                   |                    "lang": "en_US",
                   |                    "extentText": "{}"
                   |                }
                   |            },
                   |            "edgeList": [
                   |                {
                   |                    "sourceId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-0",
                   |                    "destinationId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-1",
                   |                    "caseStr": "nsubj",
                   |                    "dependType": "-",
                   |                    "logicType": "-",
                   |                    "lang": "en_US"
                   |                },
                   |                {
                   |                    "sourceId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-2",
                   |                    "destinationId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-3",
                   |                    "caseStr": "advmod",
                   |                    "dependType": "-",
                   |                    "logicType": "-",
                   |                    "lang": "en_US"
                   |                },
                   |                {
                   |                    "sourceId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-3",
                   |                    "destinationId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-1",
                   |                    "caseStr": "acomp",
                   |                    "dependType": "-",
                   |                    "logicType": "-",
                   |                    "lang": "en_US"
                   |                },
                   |                {
                   |                    "sourceId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-4",
                   |                    "destinationId": "a8ab4e4c-8ec3-448e-ad4c-1c41ee1f7b6a-1",
                   |                    "caseStr": "punct",
                   |                    "dependType": "-",
                   |                    "logicType": "-",
                   |                    "lang": "en_US"
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

}
