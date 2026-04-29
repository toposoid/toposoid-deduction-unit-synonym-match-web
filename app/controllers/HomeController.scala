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
import com.ideal.linked.toposoid.protocol.model.base.MatchedKnowledgeNode
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeFeatureReference
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeBaseSynonymNode
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.DeductionQuery

//case class DeductionQuery(query:String,relationMatchState:RelationMatchState, sourceAlias:String, destinationAlias:String,isSourceConfirmed:Boolean, isDestinationConfirmed:Boolean)

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
            coveredPropositionEdges = DeductionUtils.analyzeGraphKnowledge(getQeuries, aso, transversalState)
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


  //Featureの対象が動詞なのか、名詞なのかでクエリを否定するのを入れるか入れないかを決めれば良いのでは？
  //現在、Surfaceが両方含むケースがあるのでそれが問題。
  
  private def getQeuries(edge:KnowledgeBaseEdge, aso:AnalyzedSentenceObject, transversalState:TransversalState):List[DeductionQuery] = {
    
    val sentenceIds = aso.deductionResult.coveredPropositionEdges.foldLeft(List.empty[String]){
      (acc, x) =>
        acc ++ x.sourceNode.matchedKnowledgeNodes.map(y => "'" + y.sentenceId + "'")
    }.distinct

    val sentenceIdFilterQuery = sentenceIds.size match {
      case 0 => ""
      case _ => "AND n1.sentenceId IN [%s]".format(sentenceIds.mkString(","))
    }
    
    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNode = aso.nodeMap.get(sourceKey).get.asInstanceOf[KnowledgeBaseNode]
    val destinationNode = aso.nodeMap.get(targetKey).get.asInstanceOf[KnowledgeBaseNode]
    val nodeType: String = ToposoidUtils.getNodeType(SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
    //SourceSideがすでにOKの場合 
    val query1 = "MATCH (n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE n1.surface=\"%s\" AND e.caseName='%s' AND n2.isDenialWord='%s' AND n2ext.nodeName=\"%s\" %s RETURN n1, e, n2ext".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.surface, edge.caseStr, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.normalizedName, sentenceIdFilterQuery)
    //DestinationSideがすでにOKの場合
    val query2 = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s) WHERE n2.surface=\"%s\" AND e.caseName='%s' AND n1.isDenialWord='%s' AND n1ext.nodeName=\"%s\" %s RETURN n1ext, e, n2".format(nodeType, nodeType, destinationNode.predicateArgumentStructure.surface, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.normalizedName, sentenceIdFilterQuery)
    //両サイドともOKでない場合かつ、両サイド結果としてOKになる場合
    val query3 = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE e.caseName='%s' AND n1.isDenialWord='%s' AND n2.isDenialWord='%s' AND n1ext.nodeName=\"%s\" AND n2ext.nodeName=\"%s\" %s RETURN n1ext, e, n2ext".format(nodeType, nodeType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.normalizedName, destinationNode.predicateArgumentStructure.normalizedName, sentenceIdFilterQuery)
    //両サイドともOKでない場合かつ、Sourceのみ結果としてOKになる場合
    val query4 = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE e.caseName='%s' AND n1.isDenialWord='%s' AND n2.isDenialWord='%s' AND n1ext.nodeName=\"%s\" %s RETURN n1ext, e, n2".format(nodeType, nodeType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.normalizedName, sentenceIdFilterQuery)
    //両サイドともOKでない場合かつ、Destinationのみ結果としてOKになる場合
    val query5 = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE e.caseName='%s' AND n1.isDenialWord='%s' AND n2.isDenialWord='%s' AND n2ext.nodeName=\"%s\" %s RETURN n1ext, e, n2ext".format(nodeType, nodeType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.normalizedName, sentenceIdFilterQuery)

    val haveFeatureOnSource = sourceNode.localContext.knowledgeFeatureReferences.filter(x => List(FeatureType.IMAGE.index, FeatureType.TABLE.index).contains(x.featureType)).size > 0
    val haveFeatureOnDestination = destinationNode.localContext.knowledgeFeatureReferences.filter(x => List(FeatureType.IMAGE.index, FeatureType.TABLE.index).contains(x.featureType)).size > 0

    //命題のFeatureNodeのペアをどう持つかで、仮に表層テキスト単位でマッチしても判断を先送りする必要がある。RelationMatchStateを指定している意味。
    (haveFeatureOnSource, haveFeatureOnDestination) match
      case (false, false) => {
        List(
          DeductionQuery(query1, RelationMatchState.MATCHED_BOTH, "n1", "n2ext", true, false),
          DeductionQuery(query2, RelationMatchState.MATCHED_BOTH, "n1ext", "n2", false, true),
          DeductionQuery(query3, RelationMatchState.MATCHED_BOTH, "n1ext", "n2ext", false, false),
          DeductionQuery(query4, RelationMatchState.MATCHED_SOURCE_NODE_ONLY, "n1ext", "n2", false, false),
          DeductionQuery(query5, RelationMatchState.MATCHED_TARGET_NODE_ONLY, "n1", "n2ext", false, false)
        )      
      }
      case (true, true) => {
        List(
          DeductionQuery(query1, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2ext", true, false),
          DeductionQuery(query2, RelationMatchState.NOT_MATCHED_BOTH, "n1ext", "n2", false, true),
          DeductionQuery(query3, RelationMatchState.NOT_MATCHED_BOTH, "n1ext", "n2ext", false, false),
          DeductionQuery(query4, RelationMatchState.NOT_MATCHED_BOTH, "n1ext", "n2", false, false),
          DeductionQuery(query5, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2ext", false, false)
        )      
      }
      case (true, false) => {
        List(
          DeductionQuery(query1, RelationMatchState.MATCHED_TARGET_NODE_ONLY, "n1", "n2ext", true, false),
          DeductionQuery(query2, RelationMatchState.MATCHED_TARGET_NODE_ONLY, "n1ext", "n2", false, true),
          DeductionQuery(query3, RelationMatchState.MATCHED_TARGET_NODE_ONLY, "n1ext", "n2ext", false, false),
          DeductionQuery(query4, RelationMatchState.NOT_MATCHED_BOTH, "n1ext", "n2", false, false),
          DeductionQuery(query5, RelationMatchState.MATCHED_TARGET_NODE_ONLY, "n1", "n2ext", false, false)

        )      
      }
      case (false, true) => {
        List(
          DeductionQuery(query1, RelationMatchState.MATCHED_SOURCE_NODE_ONLY, "n1", "n2ext", true, false),
          DeductionQuery(query2, RelationMatchState.MATCHED_SOURCE_NODE_ONLY, "n1ext", "n2", false, true),
          DeductionQuery(query3, RelationMatchState.MATCHED_SOURCE_NODE_ONLY, "n1ext", "n2ext", false, false),
          DeductionQuery(query4, RelationMatchState.MATCHED_SOURCE_NODE_ONLY, "n1ext", "n2", false, false),
          DeductionQuery(query5, RelationMatchState.NOT_MATCHED_BOTH, "n1", "n2ext", false, false)
        )      
      }
  }
}
/*
  private def analyzeGraphKnowledge(getQeuries:(KnowledgeBaseEdge, Map[String, KnowledgeBaseNode]) => List[DeductionQuery], edges: List[KnowledgeBaseEdge], aso:AnalyzedSentenceObject, transversalState:TransversalState):List[CoveredPropositionEdge] = {    
    val futures: List[Future[Option[CoveredPropositionEdge]]] = edges.foldLeft(List.empty[Future[Option[CoveredPropositionEdge]]]){
      (acc, edge) => {
        val deductionQueries = getQeuries(edge, aso.nodeMap)        
        acc :+ Future(analyzeEdge(0, deductionQueries, edge, aso, Neo4JUtilsImpl(), transversalState))
      }
    }    
    val combinedFuture: Future[List[Option[CoveredPropositionEdge]]] = Future.sequence(futures)
    val result = Await.result(combinedFuture, Duration.Inf)    
    result.flatten
  }

  private def analyzeEdge(idx:Int, deductionQueries:List[DeductionQuery],edge:KnowledgeBaseEdge, aso:AnalyzedSentenceObject, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState):Option[CoveredPropositionEdge] = {
    val nodeMap = aso.nodeMap
    val sourceNode = nodeMap.get(edge.sourceId).get.asInstanceOf[KnowledgeBaseNode]
    val destinationNode = nodeMap.get(edge.destinationId).get.asInstanceOf[KnowledgeBaseNode]

    //引数のisSourceConfirmed, isDestinationConfirmedとdeductionQueries(idx)のisSourceConfirmed, isDestinationConfirmedが同じかをチェックする。
    //チェックNGの場合は、analyzeEdge(idx+1, deductionQueries, edge, nodeMap, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)    
    val coveredPropositionEdges = aso.deductionResult.coveredPropositionEdges.filter(x => {
      x.sourceNode.terminalId.equals(edge.sourceId) && x.destinationNode.terminalId.equals(edge.destinationId)
    })
    val (isSourceConfirmed, isDestinationConfirmed, coveredPropositionEdge) = coveredPropositionEdges.size match {
      case 0 => (false, false, None) //BaseMatch用
      case _ => (coveredPropositionEdges.head.sourceNode.isConfirmed, coveredPropositionEdges.head.destinationNode.isConfirmed, Option(coveredPropositionEdges.head))
    }
    //クエリを実行する必要のない場合は、早めに判断し次のクエリを実行を促す。
    if(!deductionQueries(idx).isSourceConfirmed == isSourceConfirmed || !deductionQueries(idx).isDestinationConfirmed == isDestinationConfirmed){
      if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
      else coveredPropositionEdge  
    }else{

      val sourceMorphemes = sourceNode.predicateArgumentStructure.morphemes
      val destinationMorphemes = destinationNode.predicateArgumentStructure.morphemes
      val isVerbOrNounOnSource = sourceNode.localContext.lang match {
        case "ja_JP" =>  sourceMorphemes.filter(x => x.split(",").toList.contains("動詞")).size > 0 || sourceMorphemes.filter(x => x.split(",").toList.contains("名詞")).size > 0
        case "en_US" => sourceMorphemes.filter(x => x.split(",").toList.contains("VERB")).size > 0  || sourceMorphemes.filter(x => x.split(",").toList.contains("NOUN")).size > 0
      }
      val isVerbOrNounOnDestination = destinationNode.localContext.lang match {
        case "ja_JP" =>  destinationMorphemes.filter(x => x.split(",").toList.contains("動詞")).size > 0 || destinationMorphemes.filter(x => x.split(",").toList.contains("名詞")).size > 0
        case "en_US" => destinationMorphemes.filter(x => x.split(",").toList.contains("VERB")).size > 0  || destinationMorphemes.filter(x => x.split(",").toList.contains("NOUN")).size > 0
      }

      deductionQueries(idx).relationMatchState match {
        case RelationMatchState.MATCHED_BOTH => {        
          analyze(idx, deductionQueries, edge, nodeMap, neo4JUtils, transversalState) match {
            case Some(x) => Option(x)
            case _ => {
              if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
              else coveredPropositionEdge
            }}
        }
        case RelationMatchState.MATCHED_SOURCE_NODE_ONLY => {
          if(isVerbOrNounOnDestination){
            analyze(idx, deductionQueries, edge, nodeMap, neo4JUtils, transversalState) match {
              case Some(x) => Option(x)
              case _ => {
                if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                else coveredPropositionEdge  
              }}          
          }else {
            if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
            else coveredPropositionEdge        
          }
        }
        case RelationMatchState.MATCHED_TARGET_NODE_ONLY => {
          if(isVerbOrNounOnSource) {
            analyze(idx, deductionQueries, edge, nodeMap, neo4JUtils, transversalState) match {
              case Some(x) => Option(x)
              case _ => {
                if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                else coveredPropositionEdge  
              }}          
          }else {
            if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
            else coveredPropositionEdge        
          }
        }
        case RelationMatchState.NOT_MATCHED_BOTH => {
          if(isVerbOrNounOnSource && isVerbOrNounOnDestination){
            analyze(idx, deductionQueries, edge, nodeMap, neo4JUtils, transversalState) match {
              case Some(x) => Option(x)
              case _ => {
                if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
                else coveredPropositionEdge  
              }}          
          }else {
            if(idx + 1 < deductionQueries.size) analyzeEdge(idx+1, deductionQueries, edge, aso, neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState)
            else coveredPropositionEdge        
          }
        }
      }
    }
  }
  
  private def analyze(idx:Int, deductionQueries:List[DeductionQuery],edge:KnowledgeBaseEdge, nodeMap: Map[String, KnowledgeBaseNode], neo4JUtils:Neo4JUtilsImpl, transversalState:TransversalState):Option[CoveredPropositionEdge] = {
    val deductionUnitName = conf.getString("TOPOSOID_DEDUCTION_UNIT_NAME")
    val jsonStr: String = neo4JUtils.getCypherQueryResult(deductionQueries(idx).query, "", transversalState)
    //If there is even one that does not match, it is useless to search further
    if (!jsonStr.equals("""{"records":[]}""")) {
      //ヒットするものがある場合
      val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]      
      Option(DeductionUtils.getCoveredPropositionEdge(edge, deductionQueries(idx).sourceAlias, deductionQueries(idx).destinationAlias, nodeMap,  neo4jRecords, deductionQueries(idx).relationMatchState, deductionUnitName))        
    }else{
      None
    }
  }
}
*/
  /*
  private def analyzeEdge(edge:KnowledgeBaseEdge, aso:AnalyzedSentenceObject, transversalState:TransversalState):Option[CoveredPropositionEdge] = {

    val nodeMap: Map[String, KnowledgeBaseNode] =  aso.nodeMap    
    val deductionResult:DeductionResult = aso.deductionResult
    val neo4JUtils = Neo4JUtilsImpl()
    val sourceKey = edge.sourceId
    val targetKey = edge.destinationId
    val sourceNode = nodeMap.get(sourceKey).get.asInstanceOf[KnowledgeBaseNode]
    val destinationNode = nodeMap.get(targetKey).get.asInstanceOf[KnowledgeBaseNode]
    val deductionUnitName = conf.getString("TOPOSOID_DEDUCTION_UNIT_NAME")
    
    //sentenceIdも絞り込めるがどうするか？  
    val coveredPropositionEdges = aso.deductionResult.coveredPropositionEdges.filter(x => {
      x.sourceNode.terminalId.equals(sourceKey) && x.destinationNode.terminalId.equals(targetKey)
    })

    if(coveredPropositionEdges.size == 0) {
      None
    }else{
      val coveredPropositionEdge = coveredPropositionEdges.head
      val nodeType: String = ToposoidUtils.getNodeType(SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
    
      if(coveredPropositionEdge.sourceNode.isConfirmed && !coveredPropositionEdge.destinationNode.isConfirmed){
        val sourceAlias = "n1"
        val destinationAlias = "n2ext"
        val querySourceOnly = "MATCH (n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE n1.surface=\"%s\" AND e.caseName='%s' AND n2.isDenialWord='%s' AND n2ext.nodeName=\"%s\" RETURN n1, e, n2ext".format(nodeType, nodeType, sourceNode.predicateArgumentStructure.surface, edge.caseStr, destinationNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.normalizedName)
        logger.debug(querySourceOnly)
        val jsonStr: String = neo4JUtils.getCypherQueryResult(querySourceOnly, "", transversalState)
        //If there is even one that does not match, it is useless to search further
        if (!jsonStr.equals("""{"records":[]}""")) {
          //ヒットするものがある場合
          val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
          //Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH))     
          Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH, deductionUnitName))     
        }else{
          Option(coveredPropositionEdge)
        }
      }else if(!coveredPropositionEdge.sourceNode.isConfirmed && coveredPropositionEdge.destinationNode.isConfirmed){
        val sourceAlias = "n1ext"
        val destinationAlias = "n2"
        val queryTargetOnly = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s) WHERE n2.surface=\"%s\" AND e.caseName='%s' AND n1.isDenialWord='%s' AND n1ext.nodeName=\"%s\" RETURN n1ext, e, n2".format(nodeType, nodeType, destinationNode.predicateArgumentStructure.surface, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.normalizedName)
        logger.debug(queryTargetOnly)
        val jsonStr: String = neo4JUtils.getCypherQueryResult(queryTargetOnly, "", transversalState)
        //If there is even one that does not match, it is useless to search further
        if (!jsonStr.equals("""{"records":[]}""")) {
          //ヒットするものがある場合
          val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
          //Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH))     
          Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH, deductionUnitName))     
        }else{
          Option(coveredPropositionEdge)
        }
      }else if(!coveredPropositionEdge.sourceNode.isConfirmed && !coveredPropositionEdge.destinationNode.isConfirmed){
        val sourceAlias = "n1ext"
        val destinationAlias = "n2ext"
        val queryBothReplacement = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE e.caseName='%s' AND n1.isDenialWord='%s' AND n2.isDenialWord='%s' AND n1ext.nodeName=\"%s\" AND n2ext.nodeName=\"%s\" RETURN n1ext, e, n2ext".format(nodeType, nodeType, edge.caseStr, sourceNode.predicateArgumentStructure.isDenialWord, destinationNode.predicateArgumentStructure.isDenialWord, sourceNode.predicateArgumentStructure.normalizedName, destinationNode.predicateArgumentStructure.normalizedName)
              logger.debug(queryBothReplacement)
        val jsonStr: String = neo4JUtils.getCypherQueryResult(queryBothReplacement, "", transversalState)
        //If there is even one that does not match, it is useless to search further
        if (!jsonStr.equals("""{"records":[]}""")) {
          //ヒットするものがある場合
          val neo4jRecords: Neo4jRecords = Json.parse(jsonStr).as[Neo4jRecords]
          //Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH))     
          Option(DeductionUtils.getCoveredPropositionEdge(edge, sourceAlias, destinationAlias, nodeMap,  neo4jRecords, RelationMatchState.MATCHED_BOTH, deductionUnitName))     
        }else{
          Option(coveredPropositionEdge)
        }
      }else{
        Option(coveredPropositionEdge)
      }    
    }
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
  */


  /*
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

  private def getMatchedKnowledgeNodes(
    edge: KnowledgeBaseEdge, 
    serchedKnowledgeNodes:List[KnowledgeBaseNode | KnowledgeBaseSynonymNode | KnowledgeFeatureReference], 
    proopsitionNode: KnowledgeBaseNode,
    featureInfoList:List[MatchedFeatureInfo]):List[MatchedKnowledgeNode]= {
    
    serchedKnowledgeNodes.map(x => {
      x match {
        case a:KnowledgeBaseNode => {
          MatchedKnowledgeNode(
              propositionId = a.propositionId,
              sentenceId = a.sentenceId,
              nodeId = a.nodeId,
              caseNameOnEdge = edge.caseStr,
              isDenialWord = a.predicateArgumentStructure.isDenialWord,
              nodeType = a.predicateArgumentStructure.nodeType,
              featureInfoList = List.empty[MatchedFeatureInfo]
            )
        }
        case b:KnowledgeBaseSynonymNode => {
          MatchedKnowledgeNode(
              propositionId = b.propositionId,
              sentenceId = b.sentenceId,
              nodeId = b.nodeId,
              caseNameOnEdge = edge.caseStr,
              isDenialWord = proopsitionNode.predicateArgumentStructure.isDenialWord,
              nodeType = proopsitionNode.predicateArgumentStructure.nodeType, 
              featureInfoList = List.empty[MatchedFeatureInfo]
            )
        }
        case c:KnowledgeFeatureReference => {
          MatchedKnowledgeNode(
              propositionId = c.propositionId,
              sentenceId = c.sentenceId,
              nodeId = c.featureId,
              caseNameOnEdge = edge.caseStr,
              isDenialWord = proopsitionNode.predicateArgumentStructure.isDenialWord,
              nodeType = proopsitionNode.predicateArgumentStructure.nodeType, 
              featureInfoList = List.empty[MatchedFeatureInfo]
            )
        }
      }
    })
    
  }
  
  private def getCoveredPropositionEdge(edge: KnowledgeBaseEdge, sourceAlias:String, destinationAlias:String, nodeMap:Map[String, KnowledgeBaseNode], neo4jRecords: Neo4jRecords, relationMatchState:RelationMatchState):CoveredPropositionEdge = {
    //一旦どちらかのノードが埋まっていれば推論を進めるものとする。
    
    val (isConfirmedSource, isConfirmedDestination)= relationMatchState match {
        case RelationMatchState.MATCHED_BOTH => (true, true)
        case RelationMatchState.MATCHED_SOURCE_NODE_ONLY => (true, false)
        case RelationMatchState.MATCHED_TARGET_NODE_ONLY => (false, true)
        case RelationMatchState.NOT_MATCHED_BOTH => (false, false)
    } 

    val sourceNodeSurface = nodeMap.get(edge.sourceId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface
    val destinationNodeSurface = nodeMap.get(edge.destinationId).get.asInstanceOf[KnowledgeBaseNode].predicateArgumentStructure.surface

    val sourceKnowledgeNodes = neo4jRecords.records.map(x => x.filter(y => y.key == sourceAlias).map(
      z => List(z.value.localNode, z.value.synonymNode, z.value.featureNode).flatten.head)).flatten

    val destinationKnowledgeNodes = neo4jRecords.records.map(x => x.filter(y => y.key == destinationAlias).map(
      z => List(z.value.localNode, z.value.synonymNode, z.value.featureNode).flatten.head)).flatten

    val sourceMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = sourceAlias match {
      case "" => List.empty[MatchedKnowledgeNode]
      case _ => getMatchedKnowledgeNodes(edge, sourceKnowledgeNodes, nodeMap.get(edge.sourceId).get, List.empty[MatchedFeatureInfo])
    }

    val destinationMatchedKnowledgeNodes:List[MatchedKnowledgeNode] = destinationAlias match {
      case "" =>  List.empty[MatchedKnowledgeNode]
      case _ => getMatchedKnowledgeNodes(edge, destinationKnowledgeNodes, nodeMap.get(edge.destinationId).get, List.empty[MatchedFeatureInfo])
    }

    //val knowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = List.empty[KnowledgeBaseSideInfo]
    /*
    val knowledgeBaseSideInfoList:List[KnowledgeBaseSideInfo] = (sourceMatchedKnowledgeNodes:::destinationMatchedKnowledgeNodes).map(x => {   
      //TODO:すでにある deductionUnitsを追加しないといけない。                 
      KnowledgeBaseSideInfo(propositionId=x.propositionId, sentenceId=x.sentenceId , featureInfoList = List.empty[MatchedFeatureInfo], deductionUnits = List("exact-match"))
    }).distinct
    */
    //isConfirmed:Boolean, deductionUnit:String
    val sourceNode = CoveredPropositionNode(terminalId = edge.sourceId, terminalSurface = sourceNodeSurface, terminalUrl = "", matchedKnowledgeNodes=sourceMatchedKnowledgeNodes, isConfirmedSource, "exact-match")
    val destinationNode = CoveredPropositionNode(terminalId = edge.destinationId, terminalSurface = destinationNodeSurface, terminalUrl = "", matchedKnowledgeNodes=destinationMatchedKnowledgeNodes, isConfirmedDestination, "exact-match")
    //val knowledgeBaseSideInfo = KnowledgeBaseSideInfo(propositionId = , sentenceId = , featureInfoList = List.empty[MatchedFeatureInfo])
    CoveredPropositionEdge(sourceNode = sourceNode, destinationNode = destinationNode)
  }
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