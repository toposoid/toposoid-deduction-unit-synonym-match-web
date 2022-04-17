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


import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects, DeductionResult}
import com.ideal.linked.toposoid.protocol.model.neo4j.{Neo4jRecodeUnit, Neo4jRecordMap, Neo4jRecords}
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, ToposoidUtils}
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
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController with LazyLogging{

    /**
     * This function receives the predicate argument structure analysis result of a Japanese sentence as JSON,
     * checks whether it logically matches the knowledge database on the condition that synonyms are identified,
     * and returns the result in JSON.
     * @return
     */
    def execute()  = Action(parse.json) { request =>
      try {
        val json = request.body
        val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
        val convertAnalyzedSentenceObjects = analyzedSentenceObjects.analyzedSentenceObjects.map(analyze)
        Ok(Json.toJson(AnalyzedSentenceObjects(convertAnalyzedSentenceObjects))).as(JSON)

      }catch {
        case e: Exception => {
          logger.error(e.toString, e)
          BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
        }
      }
    }

    /**
     * This function analyzes whether input logically matches the knowledge database on the condition that synonyms are identified.
     * @param aso
     * @return
     */
    private def analyze(aso:AnalyzedSentenceObject): AnalyzedSentenceObject = Try{

      val (searchResults, propositionIds) = aso.edgeList.foldLeft((List.empty[List[Neo4jRecordMap]], List.empty[String])){
        (acc, x) => analyzeGraphKnowledge(x, aso.nodeMap, aso.sentenceType, acc)
      }

      if(propositionIds.size < aso.edgeList.size) return aso
      //Pick up the most frequent propositionId
      val maxFreqSize = propositionIds.groupBy(identity).mapValues(_.size).maxBy(_._2)._2
      val propositionIdsHavingMaxFreq:List[String] = propositionIds.groupBy(identity).mapValues(_.size).filter(_._2 == maxFreqSize).map(_._1).toList
      logger.debug(propositionIdsHavingMaxFreq.toString())
      //If the number of search results with this positionId and the number of edges are equal,
      //it is assumed that they match exactly. It is no longer a partial match.
      val selectedPropositionIds =  propositionIdsHavingMaxFreq.filter(x => searchResults.filter(y =>  existALlPropositionIdEqualId(x, y)).size ==  aso.edgeList.size)
      if(selectedPropositionIds.size == 0) return aso
      val deductionResult:DeductionResult = new DeductionResult(true, selectedPropositionIds, "synonym-match")
      val updateDeductionResultMap = aso.deductionResultMap.updated(aso.sentenceType.toString, deductionResult)
      AnalyzedSentenceObject(aso.nodeMap, aso.edgeList, aso.sentenceType, updateDeductionResultMap)

    }match {
      case Success(s) => s
      case Failure(e) => throw e
    }


  /**
   * This function is a sub-function of analyze
   * @param edge
   * @param nodeMap
   * @param sentenceType
   * @param accParent
   * @return
   */
    private def analyzeGraphKnowledge(edge:KnowledgeBaseEdge, nodeMap:Map[String, KnowledgeBaseNode], sentenceType:Int, accParent:(List[List[Neo4jRecordMap]], List[String])): (List[List[Neo4jRecordMap]], List[String]) = {

      val sourceKey = edge.sourceId
      val targetKey = edge.destinationId
      val sourceNode = nodeMap.get(sourceKey).getOrElse().asInstanceOf[KnowledgeBaseNode]
      val destinationNode = nodeMap.get(targetKey).getOrElse().asInstanceOf[KnowledgeBaseNode]
      val (propositionIds, searchResults) = searchMatchRelation(sourceNode, destinationNode, edge.caseStr, sentenceType)

      if(sentenceType == PREMISE.index){
        val (propositionIds2, searchResults2) = searchMatchRelation(sourceNode, destinationNode, edge.caseStr, CLAIM.index)
        if(propositionIds2.size == 0) return (List.empty[List[Neo4jRecordMap]], List.empty[String])
        (accParent._1 ++ searchResults ++ searchResults2, accParent._2 ++ propositionIds ++ propositionIds2)
      }else{
        (accParent._1 ++ searchResults, accParent._2 ++ propositionIds)
      }
    }


    /**
     * This function searches for a subgraph that matches the predicate argument analysis result of the input sentence.
     * @param sourceNode
     * @param targetNode
     * @param caseName
     * @return
     */
    private def searchMatchRelation(sourceNode:KnowledgeBaseNode, targetNode:KnowledgeBaseNode, caseName:String, sentenceType:Int ):(List[String], List[List[Neo4jRecordMap]]) = {

      val nodeType:String = ToposoidUtils.getNodeType(sentenceType)
      //エッジの両側ノードで厳格に一致するものがあるかどうか
      val queryBoth = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType,sourceNode.normalizedName, sourceNode.isDenialWord, caseName, targetNode.normalizedName, targetNode.isDenialWord)
      logger.debug(queryBoth)
      val queryBothResultJson: String = getCypherQueryResult(queryBoth, "")
      if (!queryBothResultJson.equals("""{"records":[]}""")) {
        //ヒットするものがある場合
        getPropositionIds(Json.parse(queryBothResultJson).as[Neo4jRecords], "n1", "n2")
      }else{
        //ヒットするものがない場合
        //上記でヒットしない場合、エッジの片側ノード（Source）で厳格に一致するものがあるかどうか
        val querySourceOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.normalizedName, sourceNode.isDenialWord, caseName)
        logger.debug(querySourceOnly)
        val querySourceOnlyResultJson: String = getCypherQueryResult(querySourceOnly, "")
        if(!querySourceOnlyResultJson.equals("""{"records":[]}""")){
          //TargetをSynonymに置き換えられる可能性あり
          checkSynonymNode(sourceNode, targetNode, caseName, MATCHED_SOURCE_NODE_ONLY, sentenceType)
        }else{
          //上記でヒットしない場合、エッジの片側ノード（Target）で厳格に一致するものがあるかどうか
          val queryTargetOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType,caseName, targetNode.normalizedName, targetNode.isDenialWord)
          logger.debug(queryTargetOnly)
          val queryTargetOnlyResultJson: String = getCypherQueryResult(queryTargetOnly, "")
          if(!queryTargetOnlyResultJson.equals("""{"records":[]}""")){
            //SourceをSynonymに置き換えられる可能性あり
            checkSynonymNode(sourceNode, targetNode, caseName, MATCHED_TARGET_NODE_ONLY, sentenceType)
          }else{
            //もしTargetとSourceをSynonymに置き換えられれば、OK
            checkSynonymNode(sourceNode, targetNode, caseName, NOT_MATCHED, sentenceType)
          }
        }
      }
      //return (axiomIds, searchResults)
    }

    /**
     * This function gets the proposition ID contained in the result of querying Neo4J
     * @param neo4jRecords
     * @param sourceKey
     * @param tragetKey
     * @return
     */
    private def getPropositionIds(neo4jRecords:Neo4jRecords, sourceKey:String, tragetKey:String): (List[String], List[List[Neo4jRecordMap]]) ={
      val (searchResults, propositionIds) =neo4jRecords.records.foldLeft((List.empty[List[Neo4jRecordMap]], List.empty[String])){
        (acc, x) => { (acc._1 :+ x, acc._2 :+ x.head.value.logicNode.propositionId)}
      }
      (propositionIds, searchResults)
    }

    /**
     * Check if it is logically valid even if replaced with synonyms
     * @param sourceNode
     * @param targetNode
     * @param caseName
     * @param relationMatchState
     * @return
     */
    private def checkSynonymNode(sourceNode:KnowledgeBaseNode, targetNode:KnowledgeBaseNode,caseName:String, relationMatchState: RelationMatchState, sentenceType:Int): (List[String], List[List[Neo4jRecordMap]]) = {

      val nodeType:String = ToposoidUtils.getNodeType(sentenceType)
      val query = relationMatchState match {
        case MATCHED_SOURCE_NODE_ONLY => {
          "MATCH (n1:%s)-[e]-(n2:%s)<-[se:SynonymEdge]-(sn2:SynonymNode) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.isDenialWord='%s' AND sn2.nodeName='%s' RETURN n1, e, sn2".format(nodeType, nodeType,sourceNode.normalizedName, sourceNode.isDenialWord, caseName, targetNode.isDenialWord, targetNode.normalizedName)
        }
        case MATCHED_TARGET_NODE_ONLY => {
          "MATCH (sn1:SynonymNode)-[se:SynonymEdge]->(n1:%s)-[e]-(n2:%s) WHERE sn1.nodeName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN sn1, e, n2".format(nodeType, nodeType,sourceNode.normalizedName, sourceNode.isDenialWord, caseName, targetNode.normalizedName, targetNode.isDenialWord)
        }
        case NOT_MATCHED => {
          "MATCH (sn1:SynonymNode)-[se1:SynonymEdge]->(n1:%s)-[e]-(n2:%s)<-[se2:SynonymEdge]-(sn2:SynonymNode) WHERE sn1.nodeName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.isDenialWord='%s' AND sn2.nodeName='%s' RETURN sn1, e, sn2".format(nodeType, nodeType,sourceNode.normalizedName, sourceNode.isDenialWord, caseName, targetNode.isDenialWord, targetNode.normalizedName)
        }
      }
      val resultJson: String = getCypherQueryResult(query, "")
      logger.debug(query)
      if(resultJson.equals("""{"records":[]}""")) {
        (List.empty[String], List.empty[List[Neo4jRecordMap]])
      }else{
        relationMatchState match {
          case MATCHED_SOURCE_NODE_ONLY => {
            getPropositionIds(Json.parse(resultJson).as[Neo4jRecords], "n1", "sn2")
          }
          case MATCHED_TARGET_NODE_ONLY => {
            getPropositionIds(Json.parse(resultJson).as[Neo4jRecords], "sn1", "n2")
          }
          case NOT_MATCHED => {
            getPropositionIds(Json.parse(resultJson).as[Neo4jRecords], "sn1", "sn2")
          }
        }
      }

    }

    /**
     * Check if there is a result with only the specified ID
     * @param id
     * @param record
     * @return
     */
    private def existALlPropositionIdEqualId(id:String, record:List[Neo4jRecordMap]):Boolean = Try{
      if(record.size > 0){
        record.foreach { map: Neo4jRecordMap =>
          if (map.value.logicNode.propositionId.equals(id)) {
            return true
          }
        }
      }
      return false
    }match {
      case Failure(e) => throw e
    }

}
