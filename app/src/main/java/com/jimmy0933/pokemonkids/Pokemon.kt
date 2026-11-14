package com.jimmy0933.pokemonkids

/** One Pokemon. [drawable] is the bundled artwork resource name, e.g. "pkm_025". */
data class Pokemon(
    val id: Int,
    val name: String,
    val type: String,
    val drawable: String,
)

/** The 30 Pokemon in v1, in play order. Names and numbers are from the
 * Taiwan official Pokedex (https://tw.portal-pokemon.com/play/pokedex/). */
val ALL_POKEMON: List<Pokemon> = listOf(
    // Round 1 — the most famous starters
    Pokemon(1, "妙蛙種子", "草", "pkm_001"),
    Pokemon(4, "小火龍", "火", "pkm_004"),
    Pokemon(7, "傑尼龜", "水", "pkm_007"),
    Pokemon(25, "皮卡丘", "電", "pkm_025"),
    Pokemon(133, "伊布", "一般", "pkm_133"),
    Pokemon(143, "卡比獸", "一般", "pkm_143"),
    // Round 2
    Pokemon(39, "胖丁", "一般", "pkm_039"),
    Pokemon(52, "喵喵", "一般", "pkm_052"),
    Pokemon(54, "可達鴨", "水", "pkm_054"),
    Pokemon(35, "皮皮", "妖精", "pkm_035"),
    Pokemon(37, "六尾", "火", "pkm_037"),
    Pokemon(58, "卡蒂狗", "火", "pkm_058"),
    // Round 3
    Pokemon(63, "凱西", "超能力", "pkm_063"),
    Pokemon(92, "鬼斯", "幽靈", "pkm_092"),
    Pokemon(104, "可拉可拉", "地面", "pkm_104"),
    Pokemon(129, "鯉魚王", "水", "pkm_129"),
    Pokemon(131, "乘龍", "水", "pkm_131"),
    Pokemon(147, "迷你龍", "龍", "pkm_147"),
    // Round 4
    Pokemon(10, "綠毛蟲", "蟲", "pkm_010"),
    Pokemon(16, "波波", "一般", "pkm_016"),
    Pokemon(19, "小拉達", "一般", "pkm_019"),
    Pokemon(41, "超音蝠", "毒", "pkm_041"),
    Pokemon(50, "地鼠", "地面", "pkm_050"),
    Pokemon(74, "小拳石", "岩石", "pkm_074"),
    // Round 5
    Pokemon(79, "呆呆獸", "水", "pkm_079"),
    Pokemon(94, "耿鬼", "幽靈", "pkm_094"),
    Pokemon(95, "大岩蛇", "岩石", "pkm_095"),
    Pokemon(103, "椰蛋樹", "草", "pkm_103"),
    Pokemon(123, "飛天螳螂", "蟲", "pkm_123"),
    Pokemon(150, "超夢", "超能力", "pkm_150"),
)

const val ROUND_SIZE = 6
const val TOTAL_ROUNDS = 5

/** The 5 rounds, each a different set of 6 Pokemon. */
fun rounds(): List<List<Pokemon>> = ALL_POKEMON.chunked(ROUND_SIZE)

/** The 12 cards for a round: each Pokemon twice, shuffled. Pure function, unit-tested. */
fun roundCards(roundIndex: Int, seed: Long = 0L): List<Pokemon> {
    val round = rounds()[roundIndex]
    val cards = round + round
    return if (seed == 0L) cards.shuffled() else cards.shuffled(kotlin.random.Random(seed))
}

/** True when every Pokemon in the round has been matched. */
fun isRoundComplete(matchedIds: Set<Int>, roundIndex: Int): Boolean =
    rounds()[roundIndex].all { it.id in matchedIds }
