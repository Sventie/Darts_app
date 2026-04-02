# Darts App – Statistics Specification

This document defines all player statistics tracked in the app.  
Purpose: Implementation reference for another system (UI details are excluded).

---

## Data Model Prerequisites

The statistics assume the following underlying data:

- **Game** – A finished darts match (`finished_at` must be set).
- **GameParticipant** – A player's entry in a game, with a `placement` (1 = winner).
- **Round** – One turn of up to 3 darts. Has flags:
  - `was_bust` – The player exceeded their remaining score; round scores do not count.
  - `is_winning_round` – The player reached exactly 0 (checkout); this round counts separately.
  - `round_number` – 1-based index of the round within the game.
  - `score_before` – The player's remaining score at the start of this round.
- **DartThrow** – A single dart. Has:
  - `field` – The number hit (1–20, 25 = Bull, 50 = Bullseye, 0 = miss/out-of-bounds).
  - `multiplier` – `SINGLE`, `DOUBLE`, or `TRIPLE`.
  - `score_value` – The actual points this dart scored (field × multiplier factor).
  - `is_padding` – `true` if the dart was a "filler" throw to complete a round that ended early (e.g. after checkout); these darts are **always excluded** from all statistics.

---

## Global Filtering Rules

These rules apply to **every** statistic unless explicitly stated otherwise:

| Rule | Applies to |
|------|------------|
| Exclude padding darts (`is_padding = true`) | All dart-level stats |
| Only count finished games (`finished_at IS NOT NULL`) | All game-level stats |
| Exclude bust rounds from scoring averages | Avg per dart, avg per round, highest round, rounds < 10 |
| Exclude winning/checkout rounds from scoring averages | Avg per dart, avg per round |

---

## Statistics

### 1. Gespielte Spiele *(Games Played)*

**Type:** Integer  
**Definition:** Total number of finished games in which the player participated.

---

### 2. Siege *(Wins)*

**Type:** Integer  
**Definition:** Number of finished games where the player finished in 1st place (`placement = 1`).

---

### 3. 2. Platz *(2nd Place)*

**Type:** Integer  
**Definition:** Number of finished games where the player finished in 2nd place (`placement = 2`).  
**Condition:** Only counted in games with **3 or more** participants.

---

### 4. 3. Platz *(3rd Place)*

**Type:** Integer  
**Definition:** Number of finished games where the player finished in 3rd place (`placement = 3`).  
**Condition:** Only counted in games with **4 or more** participants.

---

### 5. Darts gesamt *(Total Darts Thrown)*

**Type:** Integer  
**Definition:** Total number of non-padding darts thrown across all games.

---

### 6. Ø Punkte/Dart *(Average Score Per Dart)*

**Type:** Decimal (e.g. `32.47`)  
**Definition:** Arithmetic mean of all individual dart `score_value`s.  
**Exclusions:** Padding darts, darts thrown in bust rounds, darts thrown in winning/checkout rounds.

---

### 7. Ø Punkte/Runde *(Average Score Per Round)*

**Type:** Decimal (e.g. `57.12`)  
**Definition:** Arithmetic mean of per-round totals (sum of up to 3 darts per round).  
**Exclusions:** Bust rounds, winning/checkout rounds (padding darts excluded from sums).

---

### 8. First 9 Ø *(First 9 Average)*

**Type:** Decimal (e.g. `154.00`)  
**Definition:**
1. For each game, sum the scores of the player's **first 3 rounds** (rounds 1, 2, 3).
2. Average those sums across all games.

**Note:** This is a standard darts metric representing expected score after 9 darts.  
**Exclusions:** Padding darts are excluded from round sums; no exclusion for bust/winning rounds (they are part of the first 3 rounds regardless).

---

### 9. Höchstes Checkout *(Highest Checkout)*

**Type:** Integer  
**Definition:** The highest `score_before` value recorded across all winning/checkout rounds (`is_winning_round = true`).  
**Meaning:** The largest remaining score a player successfully checked out in a single round.

---

### 10. Höchste Runde *(Highest Round)*

**Type:** Integer  
**Definition:** The highest total score scored in a single non-bust round (sum of up to 3 dart `score_value`s in one round).  
**Exclusions:** Bust rounds (padding darts excluded from sums).

---

### 11. Double-Quote *(Double Hit Rate)*

**Type:** Percentage string (e.g. `"12.34%"`)  
**Definition:** Share of darts that hit a double field.

```
doubleHits / totalDarts × 100
```

Where `doubleHits` = darts with `multiplier = DOUBLE`.  
`totalDarts` = all non-padding darts.

---

### 12. Triple-Quote *(Triple Hit Rate)*

**Type:** Percentage string (e.g. `"8.21%"`)  
**Definition:** Share of darts that hit a triple field.

```
tripleHits / totalDarts × 100
```

Where `tripleHits` = darts with `multiplier = TRIPLE`.

---

### 13. Out of Bounce *(Out-of-Bounds Rate)*

**Type:** Percentage string (e.g. `"5.00%"`)  
**Definition:** Share of darts that missed the board entirely.

```
missedDarts / totalDarts × 100
```

Where `missedDarts` = darts with `field = 0`.

---

### 14. Runden < 10 *(Rounds Under 10)*

**Type:** Percentage string (e.g. `"15.00%"`)  
**Definition:** Share of non-bust rounds in which the player scored fewer than 10 points.

```
roundsWithTotal < 10 / allNonBustRounds × 100
```

**Exclusions:** Bust rounds are not counted in numerator or denominator.

---

### 15. Bust-Quote *(Bust Rate)*

**Type:** Percentage string (e.g. `"20.00%"`)  
**Definition:** Share of checkout attempts that resulted in a bust.

```
bustRounds / (bustRounds + winningRounds) × 100
```

- `bustRounds` = rounds with `was_bust = true`
- `winningRounds` = rounds with `is_winning_round = true`
- Together they represent all rounds where the player attempted to finish.

---

### 16. Best Buddy

**Type:** String (player name)  
**Definition:** The other player who appeared most frequently in the same finished games as this player.  
**Tie-breaking:** Not specified – first result returned is used.

---

### 17. Erzfeind *(Rival)*

**Type:** String (player name)  
**Definition:** The opponent who finished **ahead of** this player most often across all shared finished games.

- "Ahead" means the opponent's `placement < this player's placement`.
- Only games where both players have a non-null `placement` are counted.

---

### 18. Easy Win

**Type:** String (player name)  
**Definition:** The opponent who this player finished **ahead of** most often across all shared finished games.

- "Ahead" means this player's `placement < opponent's placement`.
- Only games where both players have a non-null `placement` are counted.

---

## Additional: Training Statistics

These statistics apply only to dedicated **training sessions** (not regular games).

### Training Dispersion *(Accuracy Score)*

**Type:** Float in range `[0.0, 1.0]`  
**Definition:** Root-mean-square distance between actual hit positions and target positions, normalized to the dartboard radius.

```
dispersion = clamp(sqrt(mean((dx² + dy²) for each throw)) / R_DOUBLE_OUT, 0, 1)
```

Where:
- `dx = actualNx - targetNx`, `dy = actualNy - targetNy` (normalized board coordinates)
- `R_DOUBLE_OUT = 0.894` (normalized radius of the double ring outer edge)

**Interpretation:** `0.0` = perfect accuracy. `1.0` = average miss distance equals a full board radius.

---

### Field Frequency *(Hit Distribution)*

**Type:** List of `(field, multiplier, count)` entries  
**Definition:** For each combination of `field` (1–25) and `multiplier` (`SINGLE`, `DOUBLE`, `TRIPLE`), the total number of times that segment was hit.  
**Exclusions:** Bust rounds and padding darts are excluded.

---

### Hit Positions *(Heatmap Data)*

**Type:** List of normalized `(x, y)` coordinates  
**Definition:** The physical tap coordinates for every non-padding dart throw, normalized to board dimensions.  
**Usage:** Used to visualize throw density as a heatmap on the dartboard.  
**Exclusions:** Throws without recorded coordinates are excluded.

---

## Quick Reference

| # | Name (DE) | Name (EN) | Type | Key Rule |
|---|-----------|-----------|------|----------|
| 1 | Gespielte Spiele | Games Played | Int | Finished games only |
| 2 | Siege | Wins | Int | placement = 1 |
| 3 | 2. Platz | 2nd Place | Int | placement = 2, ≥3 players |
| 4 | 3. Platz | 3rd Place | Int | placement = 3, ≥4 players |
| 5 | Darts gesamt | Total Darts | Int | Excl. padding |
| 6 | Ø Punkte/Dart | Avg/Dart | Decimal | Excl. bust + checkout rounds |
| 7 | Ø Punkte/Runde | Avg/Round | Decimal | Excl. bust + checkout rounds |
| 8 | First 9 Ø | First 9 Avg | Decimal | Sum of rounds 1–3, averaged over games |
| 9 | Höchstes Checkout | Highest Checkout | Int | Max score_before on winning round |
| 10 | Höchste Runde | Highest Round | Int | Max round total, excl. bust |
| 11 | Double-Quote | Double Rate | % | doubles / total darts |
| 12 | Triple-Quote | Triple Rate | % | triples / total darts |
| 13 | Out of Bounce | OOB Rate | % | field=0 / total darts |
| 14 | Runden < 10 | Rounds Under 10 | % | rounds<10 / non-bust rounds |
| 15 | Bust-Quote | Bust Rate | % | busts / (busts + wins) |
| 16 | Best Buddy | Best Buddy | Name | Most shared games |
| 17 | Erzfeind | Rival | Name | Lost to most often |
| 18 | Easy Win | Easy Win | Name | Beat most often |
