# toposoid-deduction-unit-synonym-match-web
This is a WEB API that works as a microservice within the Toposoid project.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))
This microservice provides the ability to identify and match the synonyms of the entered text with the knowledge graph. 

[![Unit Test And Build Image Action](https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web/actions/workflows/action.yml/badge.svg?branch=main)](https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web/actions/workflows/action.yml)

* API Image
  * Input
  * <img width="1023" src="https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web/assets/82787843/952bb8c5-b5ab-4635-9500-a749a02af8c0">
  * Output
  * <img width="1020" src="https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web/assets/82787843/33d060dd-9564-4fc6-bcf9-f2a67a792503">

  
## Requirements
* Docker version 20.10.x, or later
* docker-compose version 1.22.x
* The following microservices must be running
    * scala-data-accessor-neo4j-web
    * neo4j

## Recommended Environment For Standalone
* Required: at least 8GB of RAM
* Required: at least 3.21G of HDD(Total required Docker Image size)

## Setup For Standalone
```bssh
docker-compose up
```

The first startup takes a long time until docker pull finishes.

## Usage
```bash
# Please refer to the following for information on registering data to try searching.
# ref. https://github.com/toposoid/toposoid-knowledge-register-web
#for example
curl -X POST -H "Content-Type: application/json" -d '{
    "premiseList": [],
    "premiseLogicRelation": [],
    "claimList": [
        {
            "sentence": "太郎はある調査を進めてきた。",
            "lang": "ja_JP",
            "extentInfoJson": "{}",
            "isNegativeSentence": false,
            "knowledgeForImages":[]
        }
    ],
    "claimLogicRelation": [
    ]
}' http://localhost:9002/regist

#Deduction
curl -X POST -H "Content-Type: application/json" -d '{
    "analyzedSentenceObjects": [
        {
            "nodeMap": {
                "90778169-e5b6-4ac9-a22b-0e112391e63d-3": {
                    "nodeId": "90778169-e5b6-4ac9-a22b-0e112391e63d-3",
                    "propositionId": "27c8c3df-1879-4db7-bb2d-3ba9cf5430f0",
                    "sentenceId": "90778169-e5b6-4ac9-a22b-0e112391e63d",
                    "predicateArgumentStructure": {
                        "currentId": 3,
                        "parentId": -1,
                        "isMainSection": true,
                        "surface": "進めてきた。",
                        "normalizedName": "進める",
                        "dependType": "D",
                        "caseType": "文末",
                        "isDenialWord": false,
                        "isConditionalConnection": false,
                        "normalizedNameYomi": "すすめる",
                        "surfaceYomi": "すすめてきた。",
                        "modalityType": "-",
                        "parallelType": "-",
                        "nodeType": 1,
                        "morphemes": [
                            "動詞,*,母音動詞,タ系連用テ形",
                            "接尾辞,動詞性接尾辞,カ変動詞,タ形",
                            "特殊,句点,*,*"
                        ]
                    },
                    "localContext": {
                        "lang": "ja_JP",
                        "namedEntity": "",
                        "rangeExpressions": {
                            "": {}
                        },
                        "categories": {
                            "": ""
                        },
                        "domains": {
                            "": ""
                        },
                        "knowledgeFeatureReferences": []
                    }
                },
                "90778169-e5b6-4ac9-a22b-0e112391e63d-2": {
                    "nodeId": "90778169-e5b6-4ac9-a22b-0e112391e63d-2",
                    "propositionId": "27c8c3df-1879-4db7-bb2d-3ba9cf5430f0",
                    "sentenceId": "90778169-e5b6-4ac9-a22b-0e112391e63d",
                    "predicateArgumentStructure": {
                        "currentId": 2,
                        "parentId": 3,
                        "isMainSection": false,
                        "surface": "分析を",
                        "normalizedName": "分析",
                        "dependType": "D",
                        "caseType": "ヲ格",
                        "isDenialWord": false,
                        "isConditionalConnection": false,
                        "normalizedNameYomi": "ぶんせき",
                        "surfaceYomi": "ぶんせきを",
                        "modalityType": "-",
                        "parallelType": "-",
                        "nodeType": 1,
                        "morphemes": [
                            "名詞,サ変名詞,*,*",
                            "助詞,格助詞,*,*"
                        ]
                    },
                    "localContext": {
                        "lang": "ja_JP",
                        "namedEntity": "",
                        "rangeExpressions": {
                            "": {}
                        },
                        "categories": {
                            "分析": "抽象物"
                        },
                        "domains": {
                            "分析": "科学・技術"
                        },
                        "knowledgeFeatureReferences": []
                    }
                },
                "90778169-e5b6-4ac9-a22b-0e112391e63d-1": {
                    "nodeId": "90778169-e5b6-4ac9-a22b-0e112391e63d-1",
                    "propositionId": "27c8c3df-1879-4db7-bb2d-3ba9cf5430f0",
                    "sentenceId": "90778169-e5b6-4ac9-a22b-0e112391e63d",
                    "predicateArgumentStructure": {
                        "currentId": 1,
                        "parentId": 2,
                        "isMainSection": false,
                        "surface": "ある",
                        "normalizedName": "有る",
                        "dependType": "D",
                        "caseType": "連格",
                        "isDenialWord": false,
                        "isConditionalConnection": false,
                        "normalizedNameYomi": "ある",
                        "surfaceYomi": "ある",
                        "modalityType": "-",
                        "parallelType": "-",
                        "nodeType": 1,
                        "morphemes": [
                            "動詞,*,子音動詞ラ行,基本形"
                        ]
                    },
                    "localContext": {
                        "lang": "ja_JP",
                        "namedEntity": "",
                        "rangeExpressions": {
                            "": {}
                        },
                        "categories": {
                            "": ""
                        },
                        "domains": {
                            "": ""
                        },
                        "knowledgeFeatureReferences": []
                    }
                },
                "90778169-e5b6-4ac9-a22b-0e112391e63d-0": {
                    "nodeId": "90778169-e5b6-4ac9-a22b-0e112391e63d-0",
                    "propositionId": "27c8c3df-1879-4db7-bb2d-3ba9cf5430f0",
                    "sentenceId": "90778169-e5b6-4ac9-a22b-0e112391e63d",
                    "predicateArgumentStructure": {
                        "currentId": 0,
                        "parentId": 3,
                        "isMainSection": false,
                        "surface": "太郎は",
                        "normalizedName": "太郎",
                        "dependType": "D",
                        "caseType": "未格",
                        "isDenialWord": false,
                        "isConditionalConnection": false,
                        "normalizedNameYomi": "たろう",
                        "surfaceYomi": "たろうは",
                        "modalityType": "-",
                        "parallelType": "-",
                        "nodeType": 1,
                        "morphemes": [
                            "名詞,人名,*,*",
                            "助詞,副助詞,*,*"
                        ]
                    },
                    "localContext": {
                        "lang": "ja_JP",
                        "namedEntity": "PERSON",
                        "rangeExpressions": {
                            "": {}
                        },
                        "categories": {
                            "": ""
                        },
                        "domains": {
                            "": ""
                        },
                        "knowledgeFeatureReferences": []
                    }
                }
            },
            "edgeList": [
                {
                    "sourceId": "90778169-e5b6-4ac9-a22b-0e112391e63d-2",
                    "destinationId": "90778169-e5b6-4ac9-a22b-0e112391e63d-3",
                    "caseStr": "ヲ格",
                    "dependType": "D",
                    "parallelType": "-",
                    "hasInclusion": false,
                    "logicType": "-"
                },
                {
                    "sourceId": "90778169-e5b6-4ac9-a22b-0e112391e63d-1",
                    "destinationId": "90778169-e5b6-4ac9-a22b-0e112391e63d-2",
                    "caseStr": "連格",
                    "dependType": "D",
                    "parallelType": "-",
                    "hasInclusion": false,
                    "logicType": "-"
                },
                {
                    "sourceId": "90778169-e5b6-4ac9-a22b-0e112391e63d-0",
                    "destinationId": "90778169-e5b6-4ac9-a22b-0e112391e63d-3",
                    "caseStr": "未格",
                    "dependType": "D",
                    "parallelType": "-",
                    "hasInclusion": false,
                    "logicType": "-"
                }
            ],
            "knowledgeBaseSemiGlobalNode": {
                "nodeId": "90778169-e5b6-4ac9-a22b-0e112391e63d",
                "propositionId": "27c8c3df-1879-4db7-bb2d-3ba9cf5430f0",
                "sentenceId": "90778169-e5b6-4ac9-a22b-0e112391e63d",
                "sentence": "太郎はある分析を進めてきた。",
                "sentenceType": 1,
                "localContextForFeature": {
                    "lang": "ja_JP",
                    "knowledgeFeatureReferences": []
                }
            },
            "deductionResult": {
                "status": false,
                "coveredPropositionResults": [],
                "havePremiseInGivenProposition": false
            }
        }
    ]
}' http://localhost:9102/execute
```

# Note
* This microservice uses 9102 as the default port.
* If you want to run in a remote environment or a virtual environment, change PRIVATE_IP_ADDRESS in docker-compose.yml according to your environment.
* The memory allocated to Neo4J can be adjusted with NEO4J_dbms_memory_heap_max__size in docker-compose.yml.

## License
toposoid/toposoid-deduction-unit-synonym-match-web is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

## Author
* Makoto Kubodera([Linked Ideal LLC.](https://linked-ideal.com/))

Thank you!
