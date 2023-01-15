# toposoid-deduction-unit-synonym-match-web
This is a WEB API that works as a microservice within the Toposoid project.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))
This microservice provides the ability to identify and match the synonyms of the entered text with the knowledge graph. 

[![Unit Test And Build Image Action](https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web/actions/workflows/action.yml/badge.svg?branch=main)](https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web/actions/workflows/action.yml)

<img width="1033" src="https://user-images.githubusercontent.com/82787843/212532038-cf5d5ddd-69ef-4775-89bf-fdfe99721cc4.png">


## Requirements
* Docker version 20.10.x, or later
* docker-compose version 1.22.x

### Memory requirements
* Required: at least 8GB of RAM (The maximum heap memory size of the JVM is set to 6G (Application: 4G, Neo4J: 2G))
* Required: 30G or higher　of HDD

## Setup
```bssh
docker-compose up -d
```
It takes more than 20 minutes to pull the Docker image for the first time.
## Usage
```bash
curl -X POST -H "Content-Type: application/json" -d '{
    "analyzedSentenceObjects": [
        {
            "nodeMap": {
                "e7182758-3672-46f0-9eb7-e69659a5c336-3": {
                    "nodeId": "e7182758-3672-46f0-9eb7-e69659a5c336-3",
                    "propositionId": "268ee5ef-7341-4c0e-99a8-d562ead3a94c",
                    "currentId": 3,
                    "parentId": -1,
                    "isMainSection": true,
                    "surface": "進めてきた。",
                    "normalizedName": "進める",
                    "dependType": "D",
                    "caseType": "文末",
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
                    "isDenialWord": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "すすめる",
                    "surfaceYomi": "すすめてきた。",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "lang": "ja_JP",
                    "extentText": "{}"
                },
                "e7182758-3672-46f0-9eb7-e69659a5c336-2": {
                    "nodeId": "e7182758-3672-46f0-9eb7-e69659a5c336-2",
                    "propositionId": "268ee5ef-7341-4c0e-99a8-d562ead3a94c",
                    "currentId": 2,
                    "parentId": 3,
                    "isMainSection": false,
                    "surface": "分析を",
                    "normalizedName": "分析",
                    "dependType": "D",
                    "caseType": "ヲ格",
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
                    "isDenialWord": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "ぶんせき",
                    "surfaceYomi": "ぶんせきを",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "lang": "ja_JP",
                    "extentText": "{}"
                },
                "e7182758-3672-46f0-9eb7-e69659a5c336-1": {
                    "nodeId": "e7182758-3672-46f0-9eb7-e69659a5c336-1",
                    "propositionId": "268ee5ef-7341-4c0e-99a8-d562ead3a94c",
                    "currentId": 1,
                    "parentId": 2,
                    "isMainSection": false,
                    "surface": "ある",
                    "normalizedName": "有る",
                    "dependType": "D",
                    "caseType": "連格",
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
                    "isDenialWord": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "ある",
                    "surfaceYomi": "ある",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "lang": "ja_JP",
                    "extentText": "{}"
                },
                "e7182758-3672-46f0-9eb7-e69659a5c336-0": {
                    "nodeId": "e7182758-3672-46f0-9eb7-e69659a5c336-0",
                    "propositionId": "268ee5ef-7341-4c0e-99a8-d562ead3a94c",
                    "currentId": 0,
                    "parentId": 3,
                    "isMainSection": false,
                    "surface": "太郎は",
                    "normalizedName": "太郎",
                    "dependType": "D",
                    "caseType": "未格",
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
                    "isDenialWord": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "たろう",
                    "surfaceYomi": "たろうは",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "lang": "ja_JP",
                    "extentText": "{}"
                }
            },
            "edgeList": [
                {
                    "sourceId": "e7182758-3672-46f0-9eb7-e69659a5c336-2",
                    "destinationId": "e7182758-3672-46f0-9eb7-e69659a5c336-3",
                    "caseStr": "ヲ格",
                    "dependType": "D",
                    "logicType": "-",
                    "lang": "ja_JP"
                },
                {
                    "sourceId": "e7182758-3672-46f0-9eb7-e69659a5c336-1",
                    "destinationId": "e7182758-3672-46f0-9eb7-e69659a5c336-2",
                    "caseStr": "連格",
                    "dependType": "D",
                    "logicType": "-",
                    "lang": "ja_JP"
                },
                {
                    "sourceId": "e7182758-3672-46f0-9eb7-e69659a5c336-0",
                    "destinationId": "e7182758-3672-46f0-9eb7-e69659a5c336-3",
                    "caseStr": "未格",
                    "dependType": "D",
                    "logicType": "-",
                    "lang": "ja_JP"
                }
            ],
            "sentenceType": 1,
            "sentenceId": "e7182758-3672-46f0-9eb7-e69659a5c336",
            "lang": "ja_JP",
            "deductionResultMap": {
                "0": {
                    "status": false,
                    "matchedPropositionIds": [],
                    "deductionUnit": ""
                },
                "1": {
                    "status": false,
                    "matchedPropositionIds": [],
                    "deductionUnit": ""
                }
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
