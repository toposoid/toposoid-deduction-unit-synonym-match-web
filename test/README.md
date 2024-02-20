# toposoid-deduction-unit-synonym-match-web Test Pattern

| TestNo | Deduciton's Pattern | Registed Knowledge | Premise Macth | Craim Match | Rigorous Evaluation of Claims |
| - | - | - | - | - | - | 
|1 | Claim:A | Claim:A | FALSE | TRUE | FALSE |
|2 | Claim:A | A → B | FALSE | FALSE | FALSE | 
|3 | Claim:B | A → B | FALSE | FALSE | FALSE | 
|4 | Claim:A | B → A,   B | FALSE | TRUE | FALSE | 
|5 | A → B | A | FALSE | FALSE | FALSE | 
|6 | A → B | B | FALSE | TRUE | FALSE | 
|7 | A → B | A → B | FALSE | FALSE | FALSE | 
|8 | A → B | A → B,   A | TRUE | TRUE | TRUE |
|8A | A → B | A, B| TRUE | TRUE | FALSE |
|9 | (A,B) → C | A | FALSE | FALSE | FALSE |
|10 | (A,B) → C | B | FALSE | FALSE | FALSE |
|11 | (A,B) → C | C | FALSE | TRUE | FALSE |
|12 | (A,B) → C | A → C | FALSE | FALSE | FALSE |
|13 | (A,B) → C | B → C | FALSE | FALSE | FALSE |
|14 | (A,B) → C | (A, B) → C | FALSE | FALSE | FALSE |
|15 | (A,B) → C | (A, B) → C、C | FALSE | TRUE | FALSE |
|16 | (A,B) → C | A, (A, B) → C | FALSE | FALSE | FALSE |
|17 | (A,B) → C | A,B  (A, B) → C | TRUE | TRUE | TRUE |
|17A | (A,B) → C | A,B, C | TRUE | TRUE | FALSE |
|18 | A → B, C | A | FALSE | FALSE | FALSE |
|19 | A → B, C | B | FALSE | FALSE | FALSE |
|20 | A → B, C | C | FALSE | FALSE | FALSE |
|21 | A → B, C | B, C | FALSE | TRUE | FALSE |
|22 | A → B, C | A → B,C | FALSE | FALSE | FALSE |
|23 | A → B, C | A, A → B,C | TRUE | TRUE | TRUE |
|23A | A → B, C | A, B,C | TRUE | TRUE | FALSE |
|24 | A → B, C | A → B | FALSE | FALSE | FALSE |
|25 | A → B, C | A, A → B | FALSE | FALSE | FALSE |
|26 | A → B, C | A, A → C | FALSE | FALSE | FALSE |
|27 | A,B → C,D | A | FALSE | FALSE | FALSE |
|28 | A,B → C,D | B | FALSE | FALSE | FALSE |
|29 | A,B → C,D | C | FALSE | FALSE | FALSE |
|30 | A,B → C,D | D | FALSE | FALSE | FALSE |
|31 | A,B → C,D | A,B | FALSE | FALSE | FALSE |
|32 | A,B → C,D | C,D | FALSE | TRUE | FALSE |
|33 | A,B → C,D | A → C | FALSE | FALSE | FALSE |
|34 | A,B → C,D | (A, B)　→ C | FALSE | FALSE | FALSE |
|35 | A,B → C,D | A → (C,D) | FALSE | FALSE | FALSE |
|36 | A,B → C,D | (A, B)　→ (C,D) | FALSE | FALSE | FALSE |
|37 | A,B → C,D | A, B (A, B)　→ (C,D) | TRUE | TRUE | TRUE |
|37A | A,B → C,D | A, B, C, D | TRUE | TRUE | FALSE |
|38 | A,B → C,D | A,  (A, B)　→ (C,D) | FALSE | FALSE | FALSE |
|39 | A,B → C,D | C,  (A, B)　→ (C,D) | FALSE | FALSE | FALSE |
|40 | A,B → C,D | A, C,  (A, B)　→ (C,D) | FALSE | FALSE | FALSE |