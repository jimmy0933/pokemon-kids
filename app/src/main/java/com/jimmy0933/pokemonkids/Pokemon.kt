package com.jimmy0933.pokemonkids

/** One Pokemon. [drawable] is the bundled artwork resource name, e.g. "pkm_025".
 * [heightM] is height in metres, [weightKg] is weight in kilograms, [intro] is a
 * short child-friendly introduction read aloud in the Pokedex. */
data class Pokemon(
    val id: Int,
    val name: String,
    val type: String,
    val drawable: String,
    val heightM: Double,
    val weightKg: Double,
    val intro: String,
)

/** Whole numbers print without a decimal ("6" not "6.0"); others keep one decimal. */
fun formatMeasure(value: Double): String =
    if (value == kotlin.math.floor(value)) value.toInt().toString() else value.toString()

/** The full spoken introduction for the Pokedex detail dialog, e.g.
 * "皮卡丘，電屬性，身高 0.4 公尺，體重 6 公斤。臉頰兩邊有儲存電力的小囊袋……" */
fun Pokemon.speakText(): String =
    "${name}，${type}屬性，身高 ${formatMeasure(heightM)} 公尺，體重 ${formatMeasure(weightKg)} 公斤。${intro}"

/** The 30 Pokemon in v1, in play order. Names, numbers, heights, weights and the
 * ideas behind the intros are from the Taiwan official Pokedex
 * (https://tw.portal-pokemon.com/play/pokedex/), cross-checked against PokeAPI.
 * Intros are rewritten in friendly words for a 4-year-old. */
val ALL_POKEMON: List<Pokemon> = listOf(
    // Round 1 — the most famous starters
    Pokemon(1, "妙蛙種子", "草", "pkm_001", 0.7, 6.9,
        "背上背著一顆種子，會吸收種子裡的營養慢慢長大。種子長大開花的時候，牠也會變得更厲害！"),
    Pokemon(4, "小火龍", "火", "pkm_004", 0.6, 8.5,
        "尾巴上有一團小火焰，那是牠生命力的象徵。牠心情好的時候，火焰會燒得旺旺的。"),
    Pokemon(7, "傑尼龜", "水", "pkm_007", 0.5, 9.0,
        "背上背著一個硬硬的甲殼，就像穿了一件盔甲。牠會從嘴巴吐出強力的水泡泡。"),
    Pokemon(25, "皮卡丘", "電", "pkm_025", 0.4, 6.0,
        "臉頰兩邊有儲存電力的小囊袋。開心的時候，臉頰還會發出啪滋啪滋的電光！"),
    Pokemon(133, "伊布", "一般", "pkm_133", 0.3, 6.5,
        "身體裡藏著好多種進化的可能，可以變成很多不同的樣子。牠的毛摸起來蓬蓬鬆鬆的。"),
    Pokemon(143, "卡比獸", "一般", "pkm_143", 2.1, 460.0,
        "除了睡覺以外，幾乎一直在吃東西，是個超級大胃王。吃飽了就呼呼大睡，誰也叫不醒牠。"),
    // Round 2
    Pokemon(39, "胖丁", "一般", "pkm_039", 0.5, 5.5,
        "有一雙圓圓的大眼睛，唱起歌來很好聽，聽到的人都會舒服地睡著。"),
    Pokemon(52, "喵喵", "一般", "pkm_052", 0.4, 4.2,
        "白天都在睡大頭覺，晚上眼睛亮亮的，喜歡在自己的地盤散步。"),
    Pokemon(54, "可達鴨", "水", "pkm_054", 0.8, 19.6,
        "常常頭痛，頭痛得厲害的時候，就會使出很神奇的力量。"),
    Pokemon(35, "皮皮", "妖精", "pkm_035", 0.6, 7.5,
        "滿月的夜晚，皮皮們會聚在一起跳舞。被月光照到的時候，身體還會輕輕浮起來。"),
    Pokemon(37, "六尾", "火", "pkm_037", 0.6, 9.9,
        "有六條漂亮的尾巴。遇到比自己強的對手，會假裝受傷，趁機逃走，是個小聰明。"),
    Pokemon(58, "卡蒂狗", "火", "pkm_058", 0.7, 19.0,
        "非常勇敢，就算對手比自己大很多，也一點都不害怕，是個可靠的好夥伴。"),
    // Round 3
    Pokemon(63, "凱西", "超能力", "pkm_063", 0.9, 19.5,
        "連睡覺的時候都在使用超能力，所以常常分不清楚牠到底是醒著還是在做夢。"),
    Pokemon(92, "鬼斯", "幽靈", "pkm_092", 1.3, 0.1,
        "身體像一團紫色的霧，會飄來飄去。牠喜歡躲在暗暗的地方，和朋友玩捉迷藏。"),
    Pokemon(104, "可拉可拉", "地面", "pkm_104", 0.4, 6.5,
        "頭上戴著一頂骨頭做的帽子，手裡還拿著一根小骨頭棒，是一個很勇敢的小傢伙。"),
    Pokemon(129, "鯉魚王", "水", "pkm_129", 0.9, 10.0,
        "力氣小小的，跳得也不高，但牠每天都很努力。長大以後，牠會變成非常厲害的寶可夢喔！"),
    Pokemon(131, "乘龍", "水", "pkm_131", 2.5, 220.0,
        "會把人載在背上，帶著大家渡過大海。心情好的時候，還會用美妙的聲音唱歌。"),
    Pokemon(147, "迷你龍", "龍", "pkm_147", 1.8, 3.3,
        "喜歡在瀑布下面反覆脫皮，每脫一次皮，身體就長大一點。"),
    // Round 4
    Pokemon(10, "綠毛蟲", "蟲", "pkm_010", 0.3, 2.9,
        "頭上的觸角會放出強烈的氣味，趕走想欺負牠的敵人。"),
    Pokemon(16, "波波", "一般", "pkm_016", 0.3, 1.8,
        "方向感超級好，不管飛到多遠的地方，都能自己找到回家的路。"),
    Pokemon(19, "小拉達", "一般", "pkm_019", 0.3, 3.5,
        "門牙會一直不停地長，所以要常常啃東西，把牙齒磨得剛剛好。"),
    Pokemon(41, "超音蝠", "毒", "pkm_041", 0.8, 7.5,
        "沒有眼睛，看不見東西，可是會用嘴巴發出超音波，來知道周圍有什麼。"),
    Pokemon(50, "地鼠", "地面", "pkm_050", 0.2, 0.8,
        "住在地底下一公尺深的地方挖洞前進，靠啃樹根生活。偶爾會探出頭來，跟大家打招呼。"),
    Pokemon(74, "小拳石", "岩石", "pkm_074", 0.4, 20.0,
        "不動的時候看起來就像一顆小石頭。不小心踩到牠的話，牠會揮著拳頭生氣喔！"),
    // Round 5
    Pokemon(79, "呆呆獸", "水", "pkm_079", 1.2, 36.0,
        "動作慢吞吞的，常常在發呆，個性很悠哉。就算被打到，也要過好幾秒才會發現。"),
    Pokemon(94, "耿鬼", "幽靈", "pkm_094", 1.5, 40.5,
        "喜歡躲在別人的影子裡，突然跳出來嚇你一跳。其實牠只是想跟大家玩！"),
    Pokemon(95, "大岩蛇", "岩石", "pkm_095", 8.8, 210.0,
        "身體有八點八公尺這麼長，是用一顆顆大石頭連起來的。牠會在地底挖洞，身體越挖越結實。"),
    Pokemon(103, "椰蛋樹", "草", "pkm_103", 2.0, 120.0,
        "被叫做會走路的熱帶雨林。頭上的每顆果實都有一張臉，而且各有各的想法。"),
    Pokemon(123, "飛天螳螂", "蟲", "pkm_123", 1.5, 56.0,
        "手臂像兩把鋒利的鐮刀，割開草叢前進。動作快到眼睛都快看不清楚！"),
    Pokemon(150, "超夢", "超能力", "pkm_150", 2.0, 122.0,
        "是科學家用基因做出來的寶可夢，擁有非常強大的超能力。牠一直在尋找，自己到底是誰。"),
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
