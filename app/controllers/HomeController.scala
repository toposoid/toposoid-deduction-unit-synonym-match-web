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


import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects, CoveredPropositionEdge, CoveredPropositionNode, KnowledgeBaseSideInfo, MatchedFeatureInfo}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecords}
import com.ideal.linked.toposoid.common.{CLAIM, LOCAL, PREDICATE_ARGUMENT, PREMISE, ToposoidUtils}
import com.ideal.linked.toposoid.deduction.common.DeductionUnitController
import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.deduction.common.FacadeForAccessNeo4J.getCypherQueryResult
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}

import javax.inject._
import play.api._
import play.api.libs.json.Json
import play.api.mvc._

import scala.util.{Failure, Success, Try}

sealed abstract class RelationMatchState(val index: Int)
final case object MATCHED_SOURCE_NODE_ONLY extends RelationMatchState(0)
final case object MATCHED_TARGET_NODE_ONLY extends RelationMatchState(1)
final case object NOT_MATCHED extends RelationMatchState(2)

/**
 * This controller creates an `Action` to determine if the text you enter matches, provided that the knowledge graph and synonyms are equated.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with DeductionUnitController with LazyLogging {

  def execute() = Action(parse.json) { request =>
    try {
      val json = request.body
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
      val asos: List[AnalyzedSentenceObject] = analyzedSentenceObjects.analyzedSentenceObjects
      val result: List[AnalyzedSentenceObject] = asos.foldLeft(List.empty[AnalyzedSentenceObject]) {
        (acc, x) => acc :+ analyze(x, acc, "synonym-match", List.empty[Int])
      }
      Ok(Json.toJson(AnalyzedSentenceObjects(result))).as(JSON)
    } catch {
      case e: Exception => {
        logger.error(e.toString, e)
        BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
      }
    }
  }


  /**
   * This function is a sub-function of analyze
   *
   * @param edge
   * @param nodeMap
   * @param sentenceType
   * @param accParent
   * @return
   */
  def analyzeGraphKnowledge(edge: KnowledgeBaseEdge, aso:AnalyzedSentenceObject, accParent: List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)]): List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = {

    val nodeMap: Map[String, KnowledgeBaseNode] =  aso.nodeMap
    val sentenceType = aso.knowledgeBaseSemiGlobalNode.sentenceType
    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNode = nodeMap.get(sourceKey).getOrElse().asInstanceOf[KnowledgeBaseNode]
    val destinationNode = nodeMap.get(targetKey).getOrElse().asInstanceOf[KnowledgeBaseNode]

    val initAcc: List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = sentenceType match {
      case PREMISE.index => {
        accParent ::: searchMatchRelation(sourceNode, destinationNode, edge.caseStr, CLAIM.index)
      }
      case _ => accParent
    }
    initAcc ::: searchMatchRelation(sourceNode, destinationNode, edge.caseStr, sentenceType)

  }


  /**
   * This function searches for a subgraph that matches the predicate argument analysis result of the input sentence.
   *
   * @param sourceNode
   * @param targetNode
   * @param caseName
   * @return
   */
  private def searchMatchRelation(sourceNode: KnowledgeBaseNode, targetNode: KnowledgeBaseNode, caseName: String, sentenceType: Int): List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, PREDICATE_ARGUMENT.index)
    val sourceSurface = sourceNode.predicateArgumentStructure.surface
    val targetSurface = targetNode.predicateArgumentStructure.surface
    //エッジの両側ノードで厳格に一致するものがあるかどうか
    val queryBoth = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.normalizedName, targetNode.predicateArgumentStructure.isDenialWord)
    logger.debug(queryBoth)
    val queryBothResultJson: String = getCypherQueryResult(queryBoth, "")
    if (!queryBothResultJson.equals("""{"records":[]}""")) {
      //ヒットするものがある場合
      getKnowledgeBaseSideInfo(Json.parse(queryBothResultJson).as[Neo4jRecords], sourceNode, targetNode)
    } else {
      //ヒットするものがない場合
      //上記でヒットしない場合、エッジの片側ノード（Source）で厳格に一致するものがあるかどうか
      val querySourceOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName)
      logger.debug(querySourceOnly)
      val querySourceOnlyResultJson: String = getCypherQueryResult(querySourceOnly, "")
      if (!querySourceOnlyResultJson.equals("""{"records":[]}""")) {
        //TargetをSynonymに置き換えられる可能性あり
        checkSynonymNode(sourceNode, targetNode, caseName, MATCHED_SOURCE_NODE_ONLY, sentenceType)
      } else {
        //上記でヒットしない場合、エッジの片側ノード（Target）で厳格に一致するものがあるかどうか
        val queryTargetOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, caseName, targetNode.predicateArgumentStructure.normalizedName, targetNode.predicateArgumentStructure.isDenialWord)
        logger.debug(queryTargetOnly)
        val queryTargetOnlyResultJson: String = getCypherQueryResult(queryTargetOnly, "")
        if (!queryTargetOnlyResultJson.equals("""{"records":[]}""")) {
          //SourceをSynonymに置き換えられる可能性あり
          checkSynonymNode(sourceNode, targetNode, caseName, MATCHED_TARGET_NODE_ONLY, sentenceType)
        } else {
          //もしTargetとSourceをSynonymに置き換えられれば、OK
          checkSynonymNode(sourceNode, targetNode, caseName, NOT_MATCHED, sentenceType)
        }
      }
    }
  }

  /**
   * This function gets the proposition ID contained in the result of querying Neo4J
   *
   * @param neo4jRecords
   * @param sourceKey
   * @param tragetKey
   * @return
   */
  private def getKnowledgeBaseSideInfo(neo4jRecords: Neo4jRecords, sourceProblemNode: KnowledgeBaseNode, targetProblemNode: KnowledgeBaseNode): List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = {
    neo4jRecords.records.foldLeft(List.empty[(KnowledgeBaseSideInfo, CoveredPropositionEdge)]) {
      (acc, x) => {
        val knowledgeBaseSideInfo = x.head.value.synonymNode match {
          case Some(y) => {
            KnowledgeBaseSideInfo(y.propositionId, y.sentenceId, List(MatchedFeatureInfo(y.sentenceId, 1)))
          }
          case _ => {
            KnowledgeBaseSideInfo(x.head.value.localNode.get.propositionId, x.head.value.localNode.get.sentenceId, List(MatchedFeatureInfo(x.head.value.localNode.get.sentenceId, 1)))
          }
        }
        val sourceNode = CoveredPropositionNode(terminalId = sourceProblemNode.nodeId, terminalSurface = sourceProblemNode.predicateArgumentStructure.surface, terminalUrl = "")
        val destinationNode = CoveredPropositionNode(terminalId = targetProblemNode.nodeId, terminalSurface = targetProblemNode.predicateArgumentStructure.surface, terminalUrl = "")
        val coveredPropositionEdge = CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
        acc :+ (knowledgeBaseSideInfo, coveredPropositionEdge)
      }
    }
  }

  /**
   * Check if it is logically valid even if replaced with synonyms
   *
   * @param sourceNode
   * @param targetNode
   * @param caseName
   * @param relationMatchState
   * @return
   */
  private def checkSynonymNode(sourceNode: KnowledgeBaseNode, targetNode: KnowledgeBaseNode, caseName: String, relationMatchState: RelationMatchState, sentenceType: Int): List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, PREDICATE_ARGUMENT.index)
    val query = relationMatchState match {
      case MATCHED_SOURCE_NODE_ONLY => {
        "MATCH (n1:%s)-[e]-(n2:%s)<-[se:SynonymEdge]-(sn2:SynonymNode) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.isDenialWord='%s' AND sn2.nodeName='%s' RETURN n1, e, sn2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.isDenialWord, targetNode.predicateArgumentStructure.normalizedName)
      }
      case MATCHED_TARGET_NODE_ONLY => {
        "MATCH (sn1:SynonymNode)-[se:SynonymEdge]->(n1:%s)-[e]-(n2:%s) WHERE sn1.nodeName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN sn1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.normalizedName, targetNode.predicateArgumentStructure.isDenialWord)
      }
      case NOT_MATCHED => {
        "MATCH (sn1:SynonymNode)-[se1:SynonymEdge]->(n1:%s)-[e]-(n2:%s)<-[se2:SynonymEdge]-(sn2:SynonymNode) WHERE sn1.nodeName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.isDenialWord='%s' AND sn2.nodeName='%s' RETURN sn1, e, sn2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.isDenialWord, targetNode.predicateArgumentStructure.normalizedName)
      }
    }
    val resultJson: String = getCypherQueryResult(query, "")
    logger.debug(query)
    if (resultJson.equals("""{"records":[]}""")) {
      List.empty[(KnowledgeBaseSideInfo, CoveredPropositionEdge)]
    } else {
      getKnowledgeBaseSideInfo(Json.parse(resultJson).as[Neo4jRecords], sourceNode, targetNode)
    }
  }

}
