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
    val sourcePas = sourceNode.predicateArgumentStructure
    val destinationPas = destinationNode.predicateArgumentStructure
    val nodeType: String = ToposoidUtils.getNodeType(SentenceType.CLAIM.index, ScopeType.LOCAL.index, FeatureType.PREDICATE_ARGUMENT.index)
    //SourceSideがすでにOKの場合 
    val query1 = "MATCH (n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE e.caseName='%s' AND n2.isDenialWord='%s' AND n2.modalityType='%s' AND n2ext.nodeName=\"%s\" %s RETURN n1, e, n2ext".format(nodeType, nodeType, edge.caseStr, destinationPas.isDenialWord, destinationPas.modalityType, destinationPas.normalizedName, sentenceIdFilterQuery)
    //DestinationSideがすでにOKの場合
    val query2 = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s) WHERE n1.isDenialWord='%s' AND n1.modalityType='%s' AND n1ext.nodeName=\"%s\" AND e.caseName='%s' %s RETURN n1ext, e, n2".format(nodeType, nodeType, sourcePas.isDenialWord, sourcePas.modalityType, sourcePas.normalizedName, edge.caseStr, sentenceIdFilterQuery)
    //両サイドともOKでない場合かつ、両サイド結果としてOKになる場合
    val query3 = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE  n1.isDenialWord='%s' AND n1.modalityType='%s' AND n1ext.nodeName=\"%s\" AND e.caseName='%s' AND n2.isDenialWord='%s' AND n2.modalityType='%s' AND n2ext.nodeName=\"%s\" %s RETURN n1ext, e, n2ext".format(nodeType, nodeType, sourcePas.isDenialWord, sourcePas.modalityType, sourcePas.normalizedName, edge.caseStr, destinationPas.isDenialWord, destinationPas.modalityType, destinationPas.normalizedName, sentenceIdFilterQuery)
    //両サイドともOKでない場合かつ、Sourceのみ結果としてOKになる場合
    val query4 = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE n1.isDenialWord='%s' AND n1.modalityType='%s' AND n1ext.nodeName=\"%s\" AND e.caseName='%s' AND n2.isDenialWord='%s' AND n2.modalityType='%s' %s RETURN n1ext, e, n2".format(nodeType, nodeType, sourcePas.isDenialWord, sourcePas.modalityType, sourcePas.normalizedName, edge.caseStr, destinationPas.isDenialWord, destinationPas.modalityType, sentenceIdFilterQuery)
    //両サイドともOKでない場合かつ、Destinationのみ結果としてOKになる場合
    val query5 = "MATCH (n1ext:SynonymNode)-[e1ext:SynonymEdge]-(n1:%s)-[e]->(n2:%s)-[e2ext:SynonymEdge]-(n2ext:SynonymNode) WHERE n1.isDenialWord='%s' AND n1.modalityType='%s' AND e.caseName='%s' AND n2.isDenialWord='%s' AND n2.modalityType='%s' AND n2ext.nodeName=\"%s\" %s RETURN n1ext, e, n2ext".format(nodeType, nodeType, sourcePas.isDenialWord, sourcePas.modalityType, edge.caseStr, destinationPas.isDenialWord, destinationPas.modalityType, destinationPas.normalizedName, sentenceIdFilterQuery)

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
