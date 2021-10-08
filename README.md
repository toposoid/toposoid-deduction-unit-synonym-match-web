# toposoid-deduction-unit-synonym-match-web
This is a WEB API that works as a microservice within the Toposoid project.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))
This microservice provides the ability to identify and match the synonyms of the entered text with the knowledge graph. 

[![Unit Test And Build Image Action](https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web/actions/workflows/action.yml/badge.svg?branch=main)](https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web/actions/workflows/action.yml)

<img width="1208" alt="2021-10-01 20 37 10" src="https://user-images.githubusercontent.com/82787843/135613760-7015f3f6-408a-44a3-babd-729e565697bc.png">

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
                "21687cd8-437b-48c6-bd21-210f67cc5e07-3": {
                    "nodeId": "21687cd8-437b-48c6-bd21-210f67cc5e07-3",
                    "propositionId": "21687cd8-437b-48c6-bd21-210f67cc5e07",
                    "currentId": 3,
                    "parentId": -1,
                    "isMainSection": true,
                    "surface": "した。",
                    "normalizedName": "する",
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
                    "isDenial": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "する",
                    "surfaceYomi": "した。",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "extentText": "{}"
                },
                "21687cd8-437b-48c6-bd21-210f67cc5e07-2": {
                    "nodeId": "21687cd8-437b-48c6-bd21-210f67cc5e07-2",
                    "propositionId": "21687cd8-437b-48c6-bd21-210f67cc5e07",
                    "currentId": 2,
                    "parentId": 3,
                    "isMainSection": false,
                    "surface": "発案を",
                    "normalizedName": "発案",
                    "dependType": "D",
                    "caseType": "ヲ格",
                    "namedEntity": "",
                    "rangeExpressions": {
                        "": {}
                    },
                    "categories": {
                        "発案": "抽象物"
                    },
                    "domains": {
                        "": ""
                    },
                    "isDenial": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "はつあん",
                    "surfaceYomi": "はつあんを",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "extentText": "{}"
                },
                "21687cd8-437b-48c6-bd21-210f67cc5e07-1": {
                    "nodeId": "21687cd8-437b-48c6-bd21-210f67cc5e07-1",
                    "propositionId": "21687cd8-437b-48c6-bd21-210f67cc5e07",
                    "currentId": 1,
                    "parentId": 2,
                    "isMainSection": false,
                    "surface": "秀逸な",
                    "normalizedName": "秀逸だ",
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
                    "isDenial": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "しゅういつだ",
                    "surfaceYomi": "しゅういつな",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "extentText": "{}"
                },
                "21687cd8-437b-48c6-bd21-210f67cc5e07-0": {
                    "nodeId": "21687cd8-437b-48c6-bd21-210f67cc5e07-0",
                    "propositionId": "21687cd8-437b-48c6-bd21-210f67cc5e07",
                    "currentId": 0,
                    "parentId": 3,
                    "isMainSection": false,
                    "surface": "太郎は",
                    "normalizedName": "太郎",
                    "dependType": "D",
                    "caseType": "未格",
                    "namedEntity": "PERSON:太郎",
                    "rangeExpressions": {
                        "": {}
                    },
                    "categories": {
                        "": ""
                    },
                    "domains": {
                        "": ""
                    },
                    "isDenial": false,
                    "isConditionalConnection": false,
                    "normalizedNameYomi": "たろう",
                    "surfaceYomi": "たろうは",
                    "modalityType": "-",
                    "logicType": "-",
                    "nodeType": 1,
                    "extentText": "{}"
                }
            },
            "edgeList": [
                {
                    "sourceId": "21687cd8-437b-48c6-bd21-210f67cc5e07-2",
                    "destinationId": "21687cd8-437b-48c6-bd21-210f67cc5e07-3",
                    "caseStr": "ヲ格",
                    "dependType": "D",
                    "logicType": "-"
                },
                {
                    "sourceId": "21687cd8-437b-48c6-bd21-210f67cc5e07-1",
                    "destinationId": "21687cd8-437b-48c6-bd21-210f67cc5e07-2",
                    "caseStr": "連格",
                    "dependType": "D",
                    "logicType": "-"
                },
                {
                    "sourceId": "21687cd8-437b-48c6-bd21-210f67cc5e07-0",
                    "destinationId": "21687cd8-437b-48c6-bd21-210f67cc5e07-3",
                    "caseStr": "未格",
                    "dependType": "D",
                    "logicType": "-"
                }
            ],
            "sentenceType": 1,
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
