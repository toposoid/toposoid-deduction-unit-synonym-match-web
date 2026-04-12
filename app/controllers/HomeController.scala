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


import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects, CoveredPropositionEdge, CoveredPropositionNode, KnowledgeBaseSideInfo, MatchedFeatureInfo}
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.common.{SentenceType, ScopeType, FeatureType, TRANSVERSAL_STATE, ToposoidUtils, TransversalState}
import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}

import javax.inject._
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json.JsValue

import scala.util.{Failure, Success, Try}
import com.ideal.linked.toposoid.protocol.model.base.DeductionResult
import com.ideal.linked.toposoid.common.Neo4JUtilsImpl
import com.ideal.linked.toposoid.protocol.model.base.VerifyingEdges
import com.ideal.linked.toposoid.common.DeductionUtils
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import com.ideal.linked.toposoid.common.RelationMatchState

/*
sealed abstract class RelationMatchState(val index: Int)
final case object MATCHED_SOURCE_NODE_ONLY extends RelationMatchState(0)
final case object MATCHED_TARGET_NODE_ONLY extends RelationMatchState(1)
final case object NOT_MATCHED extends RelationMatchState(2)
*/

/**
 * This controller creates an `Action` to determine if the text you enter matches, provided that the knowledge graph and synonyms are equated.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController /*with DeductionUnitController*/ with LazyLogging {

  def execute():Action[JsValue] = Action(parse.json[JsValue])  { request =>
    val transversalState = Json.parse(request.headers.get(TRANSVERSAL_STATE .str).get).as[TransversalState]
    try {
      val json = request.body
      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json.toString).as[AnalyzedSentenceObjects]
      val asos: List[AnalyzedSentenceObject] = analyzedSentenceObjects.analyzedSentenceObjects
      /*
      val result: List[AnalyzedSentenceObject] = asos.foldLeft(List.empty[AnalyzedSentenceObject]) {
        (acc, x) => acc :+ analyze(x, acc, "synonym-match", List.empty[Int], transversalState)
      }
      logger.info(ToposoidUtils.formatMessageForLogger("deduction completed.", transversalState.userId))
      Ok(Json.toJson(AnalyzedSentenceObjects(result, analyzedSentenceObjects.deductionConfiguration))).as(JSON)
      */      
      val result:List[VerifyingEdges] = asos.foldLeft(List.empty[VerifyingEdges]){
        (acc, aso) => {          
          acc :+ VerifyingEdges(
            propositionId = aso.knowledgeBaseSemiGlobalNode.propositionId,
            sentenceId = aso.knowledgeBaseSemiGlobalNode.sentenceId,
            //coveredPropositionEdges = analyzeGraphKnowledge(DeductionUtils.getUnsettledEdges(aso), aso, transversalState)
            coveredPropositionEdges = analyzeGraphKnowledge(getUnsettledEdges(aso), aso, transversalState)
          )
        }
      }
      logger.info(ToposoidUtils.formatMessageForLogger("Synonym edge analysis completed.", transversalState.userId))    
      Ok(Json.toJson(result)).as(JSON)        
    } catch {
      case e: Exception => {
        logger.error(ToposoidUtils.formatMessageForLogger(e.toString, transversalState.userId), e)
        BadRequest(Json.obj("status" -> "Error", "message" -> e.toString()))
      }
    }
  }

  def getUnsettledEdges(aso:AnalyzedSentenceObject): List[KnowledgeBaseEdge] = {
    //TODO:ロジカルエッヂを省けてる？
    val pairSetList = aso.deductionResult.coveredPropositionEdges.foldLeft(List.empty[Set[String]]){
      (acc, x) => {
        if(x.sourceNode.isConfirmed && x.destinationNode.isConfirmed){
          acc :+ Set(x.sourceNode.terminalId, x.destinationNode.terminalId)
        }else{
          acc
        }        
      }
    }
    aso.edgeList.filterNot(x => {
      val targetLink = Set(x.sourceId, x.destinationId)
      pairSetList.contains(targetLink)
    })

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
  /*
  def analyzeGraphKnowledge(edges: List[KnowledgeBaseEdge], aso:AnalyzedSentenceObject, transversalState:TransversalState):List[CoveredPropositionEdge] = {
    /*
    val nodeMap: Map[String, KnowledgeBaseNode] =  aso.nodeMap
    val sentenceType = aso.knowledgeBaseSemiGlobalNode.sentenceType
    val sourceKey = edge.sourceId 
    val targetKey = edge.destinationId
    val sourceNode = nodeMap.get(sourceKey).get.asInstanceOf[KnowledgeBaseNode]
    val destinationNode = nodeMap.get(targetKey).get.asInstanceOf[KnowledgeBaseNode]

    val initAcc: List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = sentenceType match {
      case SentenceType.PREMISE.index => {
        accParent ::: searchMatchRelation(sourceNode, destinationNode, edge.caseStr, SentenceType.CLAIM.index, transversalState)
      }
      case _ => accParent
    }
    initAcc ::: searchMatchRelation(sourceNode, destinationNode, edge.caseStr, sentenceType, transversalState)
    */
    List.empty[CoveredPropositionEdge]
  }
  */

  private def analyzeEdge(edge:KnowledgeBaseEdge, aso:AnalyzedSentenceObject, transversalState:TransversalState):Option[CoveredPropositionEdge] = {

    val nodeMap: Map[String, KnowledgeBaseNode] =  aso.nodeMap    
    val deductionResult:DeductionResult = aso.deductionResult
    val neo4JUtils = Neo4JUtilsImpl()
    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNode = nodeMap.get(sourceKey).get.asInstanceOf[KnowledgeBaseNode]
    val destinationNode = nodeMap.get(targetKey).get.asInstanceOf[KnowledgeBaseNode]
    
    //deductionResult.coveredPropositionEdges
    /*
    Source側がすでに被覆されているエッジ
    Destination側がすでに被覆されているエッジ
    両側が被覆可能なエッジ
    */

    //sentenceIdも絞り込めるがどうするか？  
    val coveredPropositionEdge = aso.deductionResult.coveredPropositionEdges.filter(x => {
      x.sourceNode.terminalId.equals(sourceKey) && x.destinationNode.terminalId.equals(targetKey)
    }).head

    val nodeType: String = ToposoidUtils.getNodeType(SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
  
    if(coveredPropositionEdge.sourceNode.isConfirmed && !coveredPropositionEdge.destinationNode.isConfirmed){
      val sourceAlias = "n1"
      val destinationAlias = "n2ext"
      val querySourceOnly = "MATCH (n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE n1.surface=\"%s\" AND e.caseName='%s' AND n2.isDenialWord='%s' AND n2ext.nodeName='%s' RETURN n1, e, n2ext".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.surface, edge.caseStr, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.normalizedName)
      logger.debug(querySourceOnly)
      val jsonStr: String = neo4JUtils.getCypherQueryResult(querySourceOnly, "", transversalState)
      //If there is even one that does not match, it is useless to search further
      if (!jsonStr.equals("""{"records":[]}""")) {
        //ヒットするものがある場合
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH))     
      }else{
        None
      }
    }else if(!coveredPropositionEdge.sourceNode.isConfirmed && coveredPropositionEdge.destinationNode.isConfirmed){
      val sourceAlias = "n1ext"
      val destinationAlias = "n2"
      val queryTargetOnly = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s) WHERE n2.surface=\"%s\" AND e.caseName='%s' AND n1.isDenialWord='%s' AND n1ext.nodeName='%s' RETURN n1ext, e, n2".format(nodeType, nodeType, destinationNode.predicateArgumentStructure.surface, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.normalizedName)
      logger.debug(queryTargetOnly)
      val jsonStr: String = neo4JUtils.getCypherQueryResult(queryTargetOnly, "", transversalState)
      //If there is even one that does not match, it is useless to search further
      if (!jsonStr.equals("""{"records":[]}""")) {
        //ヒットするものがある場合
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH))     
      }else{
        None
      }
    }else if(!coveredPropositionEdge.sourceNode.isConfirmed && !coveredPropositionEdge.destinationNode.isConfirmed){
      val sourceAlias = "n1ext"
      val destinationAlias = "n2ext"
      val queryBothReplacement = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE e.caseName='%s' AND Not e1ext:LocalEdge AND Not e2ext:LocalEdge AND n1.isDenialWord='%s' AND n2.isDenialWord='%s' RETURN n1ext, e, n2ext".format(nodeType, nodeType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.normalizedName, destinationNode.predicateArgumentStructure.normalizedName)
            logger.debug(queryBothReplacement)
      val jsonStr: String = neo4JUtils.getCypherQueryResult(queryBothReplacement, "", transversalState)
      //If there is even one that does not match, it is useless to search further
      if (!jsonStr.equals("""{"records":[]}""")) {
        //ヒットするものがある場合
        val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
        Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH))     
      }else{
        None
      }
    }else{
      None
    }

    None
  }


  private def analyzeGraphKnowledge(edges: List[KnowledgeBaseEdge], aso:AnalyzedSentenceObject, transversalState:TransversalState):List[CoveredPropositionEdge] = {
    
    

    val futures: List[Future[Option[CoveredPropositionEdge]]] = edges.foldLeft(List.empty[Future[Option[CoveredPropositionEdge]]]){
      (acc, edge) => {
        acc :+ Future(analyzeEdge(edge:KnowledgeBaseEdge, aso:AnalyzedSentenceObject, transversalState))
      }
    }    
    val combinedFuture: Future[List[Option[CoveredPropositionEdge]]] = Future.sequence(futures)
    val result = Await.result(combinedFuture, Duration.Inf)    
    result.flatten
    
  }

  /**
   * This function searches for a subgraph that matches the predicate argument analysis result of the input sentence.
   *
   * @param sourceNode
   * @param targetNode
   * @param caseName
   * @return
   */
  /*
  private def searchMatchRelation(sourceNode: KnowledgeBaseNode, targetNode: KnowledgeBaseNode, caseName: String, sentenceType: Int, transversalState:TransversalState): List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
    val sourceSurface = sourceNode.predicateArgumentStructure.surface
    val targetSurface = targetNode.predicateArgumentStructure.surface
    //エッジの両側ノードで厳格に一致するものがあるかどうか
    val queryBoth = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName, targetNode.predicateArgumentStructure.normalizedName, targetNode.predicateArgumentStructure.isDenialWord)
    logger.debug(queryBoth)
    val queryBothResultJson: String = getCypherQueryResult(queryBoth, "", transversalState)
    if (!queryBothResultJson.equals("""{"records":[]}""")) {
      //ヒットするものがある場合
      getKnowledgeBaseSideInfo(Json.parse(queryBothResultJson).as[Neo4jRecords], sourceNode, targetNode)
    } else {
      //ヒットするものがない場合
      //上記でヒットしない場合、エッジの片側ノード（Source）で厳格に一致するものがあるかどうか
      val querySourceOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE n1.normalizedName='%s' AND n1.isDenialWord='%s' AND e.caseName='%s' RETURN n1, e, n2".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.normalizedName, sourceNode.predicateArgumentStructure.isDenialWord, caseName)
      logger.debug(querySourceOnly)
      val querySourceOnlyResultJson: String = getCypherQueryResult(querySourceOnly, "", transversalState)
      if (!querySourceOnlyResultJson.equals("""{"records":[]}""")) {
        //TargetをSynonymに置き換えられる可能性あり
        checkSynonymNode(sourceNode, targetNode, caseName, MATCHED_SOURCE_NODE_ONLY, sentenceType, transversalState)
      } else {
        //上記でヒットしない場合、エッジの片側ノード（Target）で厳格に一致するものがあるかどうか
        val queryTargetOnly = "MATCH (n1:%s)-[e]-(n2:%s) WHERE e.caseName='%s' AND n2.normalizedName='%s' AND n2.isDenialWord='%s' RETURN n1, e, n2".format(nodeType, nodeType, caseName, targetNode.predicateArgumentStructure.normalizedName, targetNode.predicateArgumentStructure.isDenialWord)
        logger.debug(queryTargetOnly)
        val queryTargetOnlyResultJson: String = getCypherQueryResult(queryTargetOnly, "", transversalState)
        if (!queryTargetOnlyResultJson.equals("""{"records":[]}""")) {
          //SourceをSynonymに置き換えられる可能性あり
          checkSynonymNode(sourceNode, targetNode, caseName, MATCHED_TARGET_NODE_ONLY, sentenceType, transversalState)
        } else {
          //もしTargetとSourceをSynonymに置き換えられれば、OK
          checkSynonymNode(sourceNode, targetNode, caseName, NOT_MATCHED, sentenceType, transversalState)
        }
      }
    }
  }
  */
  /**
   * This function gets the proposition ID contained in the result of querying Neo4J
   *
   * @param neo4jRecords
   * @param sourceKey
   * @param tragetKey
   * @return
   */
  /*
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
  */
  /**
   * Check if it is logically valid even if replaced with synonyms
   *
   * @param sourceNode
   * @param targetNode
   * @param caseName
   * @param relationMatchState
   * @return
   */
  /*
  private def checkSynonymNode(sourceNode: KnowledgeBaseNode, targetNode: KnowledgeBaseNode, caseName: String, relationMatchState: RelationMatchState, sentenceType: Int, transversalState:TransversalState): List[(KnowledgeBaseSideInfo, CoveredPropositionEdge)] = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
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
    val resultJson: String = getCypherQueryResult(query, "", transversalState)
    logger.debug(query)
    if (resultJson.equals("""{"records":[]}""")) {
      List.empty[(KnowledgeBaseSideInfo, CoveredPropositionEdge)]
    } else {
      getKnowledgeBaseSideInfo(Json.parse(resultJson).as[Neo4jRecords], sourceNode, targetNode)
    }
  }
  */
}
