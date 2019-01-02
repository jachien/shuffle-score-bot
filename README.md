# Shuffle Score Bot
I'm /u/shufflescorebot, a Reddit bot for /r/pokemonshuffle. I look for comments detailing competition, escalation battle, and other stage runs and generate tables with everyone's aggregate info. It might be easiest to understand what I do if you look at [my comment history](https://www.reddit.com/user/shufflescorebot/comments/). The syntax tries to be flexible, here are several examples.

## Contents
* [Basic Usage](#basic-usage)
  * [Competitions](#competitions)
  * [Escalation Battles](#escalation-battles)
  * [Main / Special / Expert Stages](#main--special--expert-stages)
* [Syntax Overview](#syntax-overview)
  * [Team Section](#team-section)
    * [Level](#pokemon-level)
    * [Skill](#pokemon-skill)
    * [MSUs](#msus)
  * [Items Section](#items-section)
    * [Itemless Runs](#itemless-runs)
    * [Items and Aliases](#items-and-aliases)
    * [Multiple Jewels](#multiple-jewels)

## Basic Usage

### Competitions
```
!comp  
Team: M-Ttar (Lv10), A-Ninetales (Lv12, SL4), Vanilluxe (Lv15, SL5 Shot Out), Silvally (Lv15, SL5)  
Items: +5 Moves, MS, APU  
Score: 124,018  
!end  
```

### Escalation Battles
```
!roster M-Bee (Lv7, Swap++ SL5, 12/12), SMCX (SL1, 15/15), A-Greninja (Lv15, UP SL5), Flygon (Lv15, Shot Out SL5) !end

!eb 50  
Team: SMCX, A-Greninja, Flygon, blank  
Items: none  
Moves left: 5  
!end  

!eb 100  
Team: M-Bee, A-Greninja, Flygon, blank  
Items: DD  
Moves left: 1  
!end  
```

### Main / Special / Expert Stages
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
M-Diancie (Lvl15)
M-Diancie (Lvl 15)
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

### Items section
Items consist of one or more items, separated by commas.
```
Items: M+5, MS, DD, APU, C-1, Jewel
```

#### Itemless runs
You can specify that no items were used.
```
Items: none
```

If you do not include an `items:` section will report "unknown items" for your entry and rank it below all runs that specified items used for non-competition stages.

#### Items and Aliases
The following are all supported within the `items:` section.

* None
* Itemless
* No Items

---

* Moves +5
* M+5
* +5 Moves
* +5

---

* Time +10
* T+10
* +10 Secs
* +10 Seconds
* +10

---

* MS
* Mega Start

---

* DD
* Disruption Delay

---

* APU
* AP+
* Attack Power Up
* Attack Up

---

* C-1

---

* Jewel

#### Multiple Jewels
You can include multiple Jewels.
```
Items: M+5, MS, DD, APU, C-1, Jewel, Jewel
```
