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


import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects, DeductionResult, MatchedFeatureInfo, MatchedPropositionInfo}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecordMap, Neo4jRecords}
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, ToposoidUtils}
import com.ideal.linked.toposoid.deduction.common.{FacadeForAccessNeo4J}
import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.deduction.common.FacadeForAccessNeo4J.{getCypherQueryResult, havePremiseNode}
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
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with LazyLogging{

  def execute() = Action(parse.json) { request =>
    try {
      val json = request.body
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
      val asos: List[AnalyzedSentenceObject] = analyzedSentenceObjects.analyzedSentenceObjects
      val result: List[AnalyzedSentenceObject] = asos.foldLeft(List.empty[AnalyzedSentenceObject]) {
        (acc, x) => acc :+ analyze(x, acc)
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
   * This function analyzes whether the entered text exactly matches.
   *
   * @param aso
   * @param asos
   * @return
   */
  private def analyze(aso: AnalyzedSentenceObject, asos: List[AnalyzedSentenceObject]): AnalyzedSentenceObject = {

    val (searchResults, propositionIdInfoList) = aso.edgeList.foldLeft((List.empty[List[Neo4jRecordMap]], List.empty[MatchedPropositionInfo])) {
      (acc, x) => analyzeGraphKnowledge(x, aso.nodeMap, aso.knowledgeFeatureNode.sentenceType, acc)
    }
    if (propositionIdInfoList.size == 0) return aso
    val result = checkFinal(propositionIdInfoList, aso, searchResults)

    //This process requires that the Premise has already finished in calculating the DeductionResult
    if (aso.knowledgeFeatureNode.sentenceType == CLAIM.index) {
      val premiseDeductionResults: List[DeductionResult] = asos.map(x => x.deductionResultMap.get(PREMISE.index.toString).get)
      //If there is no deduction result that makes premise true, return the process.
      if (premiseDeductionResults.filter(_.status).size == 0) return result
      asos.filter(x => x.knowledgeFeatureNode.sentenceType == PREMISE.index).size match {
        case 0 => result
        case _ => {
          //val premiseDeductionResults: List[DeductionResult] = asos.map(x => x.deductionResultMap.get(PREMISE.index.toString).get)
          val matchedPropositionInfoList: List[MatchedPropositionInfo] = premiseDeductionResults.map(_.matchedPropositionInfoList).flatten
          val premisePropositionIds: Set[String] = matchedPropositionInfoList.map(_.propositionId).toSet
          val claimPropositionIds: Set[String] = result.deductionResultMap.get(CLAIM.index.toString).get.matchedPropositionInfoList.map(_.propositionId).toSet[String]

          //There must be at least one Claim that corresponds to at least one Premise proposition.
          (premisePropositionIds & claimPropositionIds).size - premisePropositionIds.size match {
            case 0 => {
              val originalDeductionResult: DeductionResult = result.deductionResultMap.get(CLAIM.index.toString).get
              val updateDeductionResult: DeductionResult = DeductionResult(
                status = originalDeductionResult.status,
                matchedPropositionInfoList = originalDeductionResult.matchedPropositionInfoList,
                deductionUnit = originalDeductionResult.deductionUnit,
                havePremiseInGivenProposition = true
              )
              AnalyzedSentenceObject(
                nodeMap = result.nodeMap,
                edgeList = result.edgeList,
                knowledgeFeatureNode = result.knowledgeFeatureNode,
                deductionResultMap = result.deductionResultMap.updated(CLAIM.index.toString, updateDeductionResult))
            }
            case _ => result
          }
        }
      }
    } else {
      result
    }
  }

  /**
   * final check
   *
   * @param targetMatchedPropositionInfoList
   * @param aso
   * @param searchResults
   * @return
   */
  private def checkFinal(targetMatchedPropositionInfoList: List[MatchedPropositionInfo], aso: AnalyzedSentenceObject, searchResults: List[List[Neo4jRecordMap]]): AnalyzedSentenceObject = {
    //The targetMatchedPropositionInfoList contains duplicate propositionIds.
    if (targetMatchedPropositionInfoList.size < aso.edgeList.size) return aso
    //Pick up the most frequent propositionId
    val dupFreq = targetMatchedPropositionInfoList.groupBy(identity).filter(x => x._2.size >= aso.edgeList.size)
    if (dupFreq.size == 0) return aso

    val minFreqSize = dupFreq.mapValues(_.size).minBy(_._2)._2
    val propositionIdsHavingMinFreq: List[MatchedPropositionInfo] = targetMatchedPropositionInfoList.groupBy(identity).mapValues(_.size).filter(_._2 == minFreqSize).map(_._1).toList
    logger.debug(propositionIdsHavingMinFreq.toString())

    val coveredPropositionInfoList = propositionIdsHavingMinFreq
    //Does the chosen proposalId have a premise? T
    //he coveredPropositionInfoList contains a mixture of those that are established only by Claims and those that have Premise.
    val propositionInfoListHavingPremise: List[MatchedPropositionInfo] = coveredPropositionInfoList.filter(havePremiseNode(_))
    val propositionInfoListOnlyClaim: List[MatchedPropositionInfo] = coveredPropositionInfoList.filterNot(x => propositionInfoListHavingPremise.map(y => y.propositionId).contains(x.propositionId))

    val finalPropositionInfoList: List[MatchedPropositionInfo] = propositionInfoListHavingPremise.size match {
      case 0 => propositionInfoListOnlyClaim
      case _ => propositionInfoListOnlyClaim ::: checkClaimHavingPremise(propositionInfoListHavingPremise)
    }

    if (finalPropositionInfoList.size == 0) return aso

    val status = true
    //selectedPropositions includes trivialClaimsPropositionIds
    val deductionResult: DeductionResult = new DeductionResult(status, finalPropositionInfoList, "exact-match")
    val updateDeductionResultMap = aso.deductionResultMap.updated(aso.knowledgeFeatureNode.sentenceType.toString, deductionResult)
    AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.knowledgeFeatureNode, updateDeductionResultMap)

  }

  /**
   *
   * @param propositionId
   * @return
   */
  private def havePremise(propositionId: String): Boolean = {
    val query = "MATCH (n:PremiseNode)-[*]-(m:ClaimNode) WHERE m.propositionId ='%s'  RETURN (n)".format(propositionId)
    val jsonStr: String = getCypherQueryResult(query, "n")
    val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
    neo4jRecords.records.size match {
      case 0 => false
      case _ => true
    }
  }

  /**
   *
   * @param targetMatchedPropositionInfoList
   * @return
   */
  private def checkClaimHavingPremise(targetMatchedPropositionInfoList: List[MatchedPropositionInfo]): List[MatchedPropositionInfo] = {
    //Pick up a node with the same surface layer as the Premise connected from Claim as x
    //Search for the one that has the corresponding ClaimId and has a premise
    targetMatchedPropositionInfoList.foldLeft(List.empty[MatchedPropositionInfo]) {
      (acc, x) => {
        val query = "MATCH (n1:PremiseNode)-[e:PremiseEdge]->(n2:PremiseNode) WHERE n1.propositionId='%s' AND n2.propositionId='%s' RETURN n1, e, n2".format(x.propositionId, x.propositionId)
        val jsonStr = FacadeForAccessNeo4J.getCypherQueryResult(query, "x")
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        val resultMatchedPropositionInfoList = neo4jRecords.records.size match {
          case 0 => List.empty[MatchedPropositionInfo]
          case _ => checkOnlyClaimNodes(neo4jRecords, targetMatchedPropositionInfoList)
        }
        acc ::: resultMatchedPropositionInfoList
      }
    }
  }

  /**
   *
   * @param neo4jRecords
   * @param targetMatchedPropositionInfoList
   * @return
   */
  private def checkOnlyClaimNodes(neo4jRecords: Neo4jRecords, targetMatchedPropositionInfoList: List[MatchedPropositionInfo]): List[MatchedPropositionInfo] = {

    val claimMatchedPropositionInfo: List[MatchedPropositionInfo] = neo4jRecords.records.foldLeft(List.empty[MatchedPropositionInfo]) {
      (acc, x) => {
        val surface1: String = x(0).value.logicNode.predicateArgumentStructure.surface
        val caseStr: String = x(1).value.logicEdge.caseStr
        val surface2: String = x(2).value.logicNode.predicateArgumentStructure.surface
        val query = "MATCH (n1:ClaimNode)-[e:ClaimEdge]->(n2:ClaimNode) WHERE n1.surface='%s' AND e.caseName='%s' AND n2.surface='%s' RETURN n1, e, n2".format(surface1, caseStr, surface2)
        val jsonStr: String = getCypherQueryResult(query, "")
        val neo4jRecordsForClaim: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        val additionalMatchedPropositionInfo = neo4jRecordsForClaim.records.foldLeft(List.empty[MatchedPropositionInfo]) {
          (acc2, x2) => {
            val propositionId = x2.head.value.logicNode.propositionId
            val sentenceId = x2.head.value.logicNode.sentenceId
            val matchedFeatureInfo = MatchedFeatureInfo(sentenceId, 1)
            acc2 :+ MatchedPropositionInfo(propositionId, List(matchedFeatureInfo))
          }
        }
        acc ::: additionalMatchedPropositionInfo
      }
    }
    //Checkpoint
    //・Are there all claims corresponding to premise?
    //・Does the obtained result have more propositionIds than the number of neo4jRecords records?得られてた結果でneo4jRecordsのレコード数と同数以上のpropositionIdを持つものが存在するかどうか？
    //・Multiple claims can guarantee one Premise, so it is not necessarily =, but there must be more Claims than the number of Premises.
    if (claimMatchedPropositionInfo.size < neo4jRecords.records.size) return List.empty[MatchedPropositionInfo]

    //val candidates: List[MatchedPropositionInfo] = claimMatchedPropositionInfo.groupBy(identity).mapValues(_.size).map(_._1).toList
    val candidates: List[MatchedPropositionInfo] = claimMatchedPropositionInfo.distinct
    //candidatesは、propositionId上の重複はない。
    if (candidates.size == 0) return List.empty[MatchedPropositionInfo]
    //ensure there are no Premise. only claim!
    val finalChoice: List[MatchedPropositionInfo] = candidates.filterNot(x => havePremise(x.propositionId))
    finalChoice.size match {
      case 0 => List.empty[MatchedPropositionInfo]
      case _ => finalChoice ::: targetMatchedPropositionInfoList
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
  private def analyzeGraphKnowledge(edge: KnowledgeBaseEdge, nodeMap: Map[String, KnowledgeBaseNode], sentenceType: Int, accParent: (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo])): (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo]) = {

    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNode = nodeMap.get(sourceKey).getOrElse().asInstanceOf[KnowledgeBaseNode]
    val destinationNode = nodeMap.get(targetKey).getOrElse().asInstanceOf[KnowledgeBaseNode]

    val initAcc: (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo]) = sentenceType match {
      case PREMISE.index => {
        val (searchResults, matchedPropositionInfoList) = searchMatchRelation(sourceNode, destinationNode, edge.caseStr, CLAIM.index)
        if (matchedPropositionInfoList.size == 0) return accParent
        (accParent._1 ::: searchResults, accParent._2 ::: matchedPropositionInfoList)
      }
      case _ => accParent
    }
    val (searchResults, matchedPropositionInfoList) = searchMatchRelation(sourceNode, destinationNode, edge.caseStr, sentenceType)
    print("check")
    (initAcc._1 ::: searchResults, initAcc._2 ::: matchedPropositionInfoList)

  }


  /**
   * This function searches for a subgraph that matches the predicate argument analysis result of the input sentence.
   *
   * @param sourceNode
   * @param targetNode
   * @param caseName
   * @return
   */
  private def searchMatchRelation(sourceNode: KnowledgeBaseNode, targetNode: KnowledgeBaseNode, caseName: String, sentenceType: Int): (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo]) = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType)
    //エッジの両側ノードで厳格に一致するものがあるかどうか
    val queryBoth = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.normalizedName, targetNode.predicateArgumentStructure.isDenialWord)
    logger.debug(queryBoth)
    val queryBothResultJson: String = getCypherQueryResult(queryBoth, "")
    if (!queryBothResultJson.equals("""{"records":[]}""")) {
      //ヒットするものがある場合
      getMatchedPropositionInfo(Json.parse(queryBothResultJson).as[Neo4jRecords], "n1", "n2")
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
    //return (axiomIds, searchResults)
  }

  /**
   * This function gets the proposition ID contained in the result of querying Neo4J
   *
   * @param neo4jRecords
   * @param sourceKey
   * @param tragetKey
   * @return
   */
  private def getMatchedPropositionInfo(neo4jRecords: Neo4jRecords, sourceKey: String, tragetKey: String): (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo]) = {

    val (searchResults, matchPropositionInfoList) = neo4jRecords.records.foldLeft((List.empty[List[Neo4jRecordMap]], List.empty[MatchedPropositionInfo])) {
      (acc, x) => {
        x.head.value.logicNode.propositionId match {
          case "" => {
            val matchPropositionInfo = MatchedPropositionInfo(x.head.value.synonymNode.propositionId, List(MatchedFeatureInfo(x.head.value.synonymNode.sentenceId, 1)))
            (acc._1 :+ x, acc._2 :+ matchPropositionInfo)
          }
          case _ => {
            val matchPropositionInfo = MatchedPropositionInfo(x.head.value.logicNode.propositionId, List(MatchedFeatureInfo(x.head.value.logicNode.sentenceId, 1)))
            (acc._1 :+ x, acc._2 :+ matchPropositionInfo)
          }
        }
      }
    }
    (searchResults, matchPropositionInfoList)
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
  private def checkSynonymNode(sourceNode: KnowledgeBaseNode, targetNode: KnowledgeBaseNode, caseName: String, relationMatchState: RelationMatchState, sentenceType: Int): (List[List[Neo4jRecordMap]], List[MatchedPropositionInfo]) = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType)
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
      (List.empty[List[Neo4jRecordMap]], List.empty[MatchedPropositionInfo])
    } else {
      relationMatchState match {
        case MATCHED_SOURCE_NODE_ONLY => {
          getMatchedPropositionInfo(Json.parse(resultJson).as[Neo4jRecords], "n1", "sn2")
        }
        case MATCHED_TARGET_NODE_ONLY => {
          getMatchedPropositionInfo(Json.parse(resultJson).as[Neo4jRecords], "sn1", "n2")
        }
        case NOT_MATCHED => {
          getMatchedPropositionInfo(Json.parse(resultJson).as[Neo4jRecords], "sn1", "sn2")
        }
      }
    }
  }

}
