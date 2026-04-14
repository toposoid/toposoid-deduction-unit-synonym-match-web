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

import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.common.{Neo4JUtilsImpl, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.PropositionRelation
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.test.utils.TestUtils
import play.api.libs.json.Json
import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.toposoid.protocol.model.base.VerifyingEdges
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObject
import com.ideal.linked.toposoid.protocol.model.base.DeductionResult

object TestUtilsEx {
  val neo4JUtils = new Neo4JUtilsImpl()
  def deleteNeo4JAllData(transversalState:TransversalState): Unit = {
    val query = "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r"
    neo4JUtils.executeQuery(query, transversalState)
  }

  def executeQueryAndReturn(query: String, transversalState: TransversalState): Neo4jRecords = {
    neo4JUtils.executeQueryAndReturn(query, transversalState)
  }

  def registerSingleClaim(knowledgeForParser: KnowledgeForParser, transversalState: TransversalState): Unit = {
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List.empty[KnowledgeForParser],
      List.empty[PropositionRelation],
      List(knowledgeForParser),
      List.empty[PropositionRelation])
    TestUtils.registerData(knowledgeSentenceSetForParser, transversalState, addVectorFlag = false)
  }

  def analyzeByBaseDeductionUnit(asosJson:String, transversalState: TransversalState):String = {
    val json = ToposoidUtils.callComponent(asosJson, conf.getString("TOPOSOID_DEDUCTION_UNIT1_HOST"), conf.getString("TOPOSOID_DEDUCTION_UNIT1_PORT"), "execute", transversalState)
    val verifyingEdges = Json.parse(json).as[List[VerifyingEdges]]
    val analyzedSentenceObjects = Json.parse(asosJson).as[AnalyzedSentenceObjects]
    val asos = analyzedSentenceObjects.analyzedSentenceObjects
    
    val updatedAsos = asos.foldLeft(List.empty[AnalyzedSentenceObject]){
      (acc, x) => {
        val coveredPropositionEdges = verifyingEdges.filter(y => y.sentenceId.equals(x.knowledgeBaseSemiGlobalNode.sentenceId)).head.coveredPropositionEdges
        val updatedDeductionReult = DeductionResult(
          status = x.deductionResult.status, 
          authenticityType = x.deductionResult.authenticityType, 
          coveredPropositionEdges = coveredPropositionEdges, 
          evidenceKnowledgeList = x.deductionResult.evidenceKnowledgeList, 
          havePremiseInGivenProposition = x.deductionResult.havePremiseInGivenProposition, 
          deductionPhaseType = x.deductionResult.deductionPhaseType
        )        
        acc :+ AnalyzedSentenceObject(x.nodeMap, x.edgeList, x.knowledgeBaseSemiGlobalNode, updatedDeductionReult)
      }
    }
    Json.toJson(AnalyzedSentenceObjects(updatedAsos, analyzedSentenceObjects.deductionConfiguration)).toString
    
  }

  def checkMatchedBothSide(json:String, sentenceId:String, verifyingEdgesList:List[VerifyingEdges], correctSize:Int ):Unit = {

      val evalA:VerifyingEdges = verifyingEdgesList.filter(x => x.sentenceId.equals(sentenceId)).head
      val coveredEdges = evalA.coveredPropositionEdges.filter(x => x.destinationNode.isConfirmed && x.sourceNode.isConfirmed)
      assert(coveredEdges.size == correctSize)

      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json).as[AnalyzedSentenceObjects]
      //両側被覆エッジに含まれるノードのチェック
      val targetAso = analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceId.equals(sentenceId)).head      
      coveredEdges.foreach(x => {
        assert(targetAso.nodeMap.get(x.sourceNode.terminalId).get.predicateArgumentStructure.surface.equals(x.sourceNode.terminalSurface))
        assert(targetAso.nodeMap.get(x.destinationNode.terminalId).get.predicateArgumentStructure.surface.equals(x.destinationNode.terminalSurface))        
      })

      val sentenceIds = coveredEdges.foldLeft(List.empty[String]){
        (acc, x) => {        
          val sourceKnowledgeSentenceIds = x.sourceNode.matchedKnowledgeNodes.foldLeft(Set.empty[String]){(acc2, y) => {
            acc2 + y.sentenceId
          }}        
          val destinationKnowledgeSentenceIds = x.destinationNode.matchedKnowledgeNodes.foldLeft(Set.empty[String]){(acc2, y) => {
            acc2 + y.sentenceId
          }}
          val targetSentenceIds = sourceKnowledgeSentenceIds & destinationKnowledgeSentenceIds 
          assert(targetSentenceIds.size > 0)
          acc ::: targetSentenceIds.toList
        }
      }            
      assert(sentenceIds.groupBy(identity).filter(x => x._2.size >= correctSize).size > 0)
  }


  def checkMatchedOneSide(json:String, sentenceId:String, verifyingEdgesList:List[VerifyingEdges], correctSize:Int ):Unit = {

      val evalA:VerifyingEdges = verifyingEdgesList.filter(x => x.sentenceId.equals(sentenceId)).head
      val coveredEdges = evalA.coveredPropositionEdges.filter(x => (x.destinationNode.isConfirmed || x.sourceNode.isConfirmed) && !(x.destinationNode.isConfirmed && x.sourceNode.isConfirmed))
      assert(coveredEdges.size == correctSize)

      val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(json).as[AnalyzedSentenceObjects]
      //両側被覆エッジに含まれるノードのチェック
      val targetAso = analyzedSentenceObjects.analyzedSentenceObjects.filter(x => x.knowledgeBaseSemiGlobalNode.sentenceId.equals(sentenceId)).head      
      coveredEdges.foreach(x => {
        if(x.sourceNode.isConfirmed){
          assert(targetAso.nodeMap.get(x.sourceNode.terminalId).get.predicateArgumentStructure.surface.equals(x.sourceNode.terminalSurface))
        }
        if(x.destinationNode.isConfirmed){
          assert(targetAso.nodeMap.get(x.destinationNode.terminalId).get.predicateArgumentStructure.surface.equals(x.destinationNode.terminalSurface))        
        }        
      })

      val sentenceIds = coveredEdges.foldLeft(List.empty[String]){
        (acc, x) => {           
          val sourceKnowledgeSentenceIds = x.sourceNode.isConfirmed match {
            case true => {
              x.sourceNode.matchedKnowledgeNodes.foldLeft(Set.empty[String]){(acc2, y) => {
                acc2 + y.sentenceId
              }}
            }
            case _ => {
              Set.empty[String]
            }
          }
          val destinationKnowledgeSentenceIds = x.destinationNode.isConfirmed match {
            case true => {
              x.destinationNode.matchedKnowledgeNodes.foldLeft(Set.empty[String]){(acc2, y) => {
                acc2 + y.sentenceId
              }}
            }
            case _ => {
              Set.empty[String]
            }
          }
          val targetSentenceIds = sourceKnowledgeSentenceIds | destinationKnowledgeSentenceIds 
          if((x.sourceNode.isConfirmed || x.destinationNode.isConfirmed) && !(x.sourceNode.isConfirmed && x.destinationNode.isConfirmed) ){
            assert(targetSentenceIds.size > 0)
          }        
          acc ::: targetSentenceIds.toList
        }
      }      
      assert(sentenceIds.groupBy(identity).filter(x => x._2.size >= correctSize).size > 0)
  }
}
