# Shuffle Score Bot
I'm a Reddit bot for /r/pokemonshuffle. I look for comments detailing competition, escalation battle, and other stage runs and generate tables with everyone's aggregate info. The syntax tries to be flexible, here are several examples.

## Basic Usage

### Competition Run
```
!comp
Team: M-Ttar (Lv10), A-Ninetales (Lv12, SL4), Vanilluxe (Lv15, SL5 Shot Out), Silvally (Lv15, SL5)
Items: +5 Moves, MS, APU
Score: 124,018 
!end
```

### Escalation Battle
```
!eb 100
Team: M-Bee (Lv7, SL5 Swap++, 12/12), A-Greninja, Flygon, blank    
Items: DD
Moves left: 1
!end
```

### Normal Stage
```
!run Arcanine
Team: SMCX (SL1, 15/15), Lando-T (SL5), Flygon (Lv15), Dugtrio (Block Shot)     
Items: none
Time left: 40
!end
```

## Syntax Overview
* A block of text within a Reddit comment with your run information is referred to as "run details".
* The syntax is case-insensitive.
* Whitespace does not generally matter.
* Your run details may span multiple lines.
* You may have multiple run details in a single Reddit comment, but each one needs its own header, sections, and footer.
* No extraneous text is allowed within run details.
* `!comp`, `!eb <number>`, or `!run <name>` are run headers. You must include one of these.
* `!end` is the run footer. You must include this.
* The following are section headers: `team:`, `items:`, `score:`, `moves left:`, and `time left:`.

### Team section
A team consists of one or more Pokemon, separated by commas.
```
Team: SMCX, Dragonite, Kyurem-B, Zygarde-C
```

Pokemon stats are put in parenthesis, separated by commas. Level, skill, and MSUs are all optional and can be listed in any order.
```
M-Diancie (Lv15, SL2 Mega Boost+, 10/10)
M-Diancie (Lv15, SL2 Mega Boost+)
M-Diancie (SL2 Mega Boost+, 10/10)
M-Diancie (Lv15, 10/10)
```

#### Pokemon Level
All of the following are valid:
```
M-Diancie (Lv15)
M-Diancie (Lv 15)
M-Diancie (15)
```

#### Pokemon Skill
All of the following are valid:
```
M-Diancie (SL2 Mega Boost+)
M-Diancie (Mega Boost+ SL2)
M-Diancie (SL2)
M-Diancie (Mega Boost+)
```

#### MSUs
All of the following are valid:
```
M-Diancie (10/10)
M-Diancie (10 / 10)
```
