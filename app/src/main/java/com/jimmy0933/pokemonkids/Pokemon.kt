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

/** The 60 Pokemon in v1.2 (30 in v1, +30 in v1.2), in play order. Names, numbers,
 * heights, weights and the ideas behind the intros are from the Taiwan official
 * Pokedex (https://tw.portal-pokemon.com/play/pokedex/), cross-checked against
 * PokeAPI. Intros are rewritten in friendly words for a 4-year-old. */
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
    // Round 6
    Pokemon(2, "妙蛙草", "草", "pkm_002", 1.0, 13.0,
        "沐浴在陽光下越久，背上的花苞就會長得越大。牠最喜歡曬太陽了！"),
    Pokemon(5, "火恐龍", "火", "pkm_005", 1.1, 19.0,
        "揮動燃燒的尾巴時，周圍會變得暖呼呼的。牠是一隻很熱情的寶可夢！"),
    Pokemon(8, "卡咪龜", "水", "pkm_008", 1.0, 22.5,
        "蓬蓬鬆鬆的長尾巴是長壽的象徵，特別受到爺爺奶奶的喜愛。牠游起泳來又快又穩！"),
    Pokemon(12, "巴大蝶", "蟲", "pkm_012", 1.1, 32.0,
        "翅膀上有漂亮的花紋，飛起來的時候，閃亮亮的鱗粉會像小星星一樣飄下來。"),
    Pokemon(18, "大比鳥", "一般", "pkm_018", 1.5, 39.5,
        "有著亮麗的羽毛，很多訓練家都被牠頭上美麗的羽毛吸引。牠飛得又高又快！"),
    Pokemon(20, "拉達", "一般", "pkm_020", 0.7, 18.5,
        "遇到危險的時候，牠會用後腳的小蹼游過大海，逃到別的島嶼去。牠可是游泳小高手！"),
    // Round 7
    Pokemon(24, "阿柏怪", "毒", "pkm_024", 3.5, 65.0,
        "肚子上有像臉一樣有趣的花紋。牠把身體捲起來，就像一條長長的繩子。"),
    Pokemon(26, "雷丘", "電", "pkm_026", 0.8, 30.0,
        "尾巴像接地線一樣把電導入地面，所以自己不會被電到。牠的臉頰也能發出啪滋啪滋的電光！"),
    Pokemon(27, "穿山鼠", "地面", "pkm_027", 0.6, 12.0,
        "喜歡挖很深的洞住在裡面。遇到危險時，牠會把身體捲成一團，靜靜地等危險過去。"),
    Pokemon(31, "尼多后", "毒", "pkm_031", 1.3, 60.0,
        "身上長滿堅硬的鱗片，會用身體把對手撞開。保護孩子的時候，牠會變得最勇敢！"),
    Pokemon(36, "皮可西", "妖精", "pkm_036", 1.3, 40.0,
        "是住在安靜深山裡的小妖精，害羞不喜歡被看見。滿月的夜晚，牠會和朋友們一起跳舞。"),
    Pokemon(38, "九尾", "火", "pkm_038", 1.1, 19.9,
        "有九根漂亮的尾巴，傳說每一根尾巴都藏著不同的神奇力量。牠活了很久很久，是個聰明又優雅的寶可夢。"),
    // Round 8
    Pokemon(40, "胖可丁", "一般", "pkm_040", 1.0, 12.0,
        "毛皮摸起來細細滑滑的。別惹牠生氣喔，生氣的時候牠會一直膨脹起來！"),
    Pokemon(42, "大嘴蝠", "毒", "pkm_042", 1.6, 55.0,
        "肚子餓的時候，牠會張開大大的嘴巴。牠沒有眼睛，靠耳朵聽聲音來找路。"),
    Pokemon(46, "派拉斯", "蟲", "pkm_046", 0.3, 5.4,
        "背上長著兩朵小蘑菇，喜歡潮濕的地方。牠會把蘑菇的孢子灑在樹根上，和大樹做朋友。"),
    Pokemon(51, "三地鼠", "地面", "pkm_051", 0.7, 33.3,
        "有三顆頭，會輪流工作，把周圍的泥土挖得鬆鬆軟軟的。牠們挖洞挖得好快！"),
    Pokemon(55, "哥達鴨", "水", "pkm_055", 1.7, 76.6,
        "游泳的時候手腳划得好快，額頭還會閃閃發光。牠是游泳健將！"),
    Pokemon(59, "風速狗", "火", "pkm_059", 1.9, 155.0,
        "在草原上奔跑的樣子非常帥氣，連畫裡的人都被牠迷住了。牠跑起來像風一樣快！"),
    // Round 9
    Pokemon(61, "蚊香蝌蚪", "水", "pkm_061", 1.0, 20.0,
        "兩條腿很有力氣，雖然能在陸地上生活，但牠更喜歡待在水裡游泳。肚子上的花紋像蚊香一樣捲捲的。"),
    Pokemon(65, "胡地", "超能力", "pkm_065", 1.5, 48.0,
        "手裡拿著一根湯匙，那是牠用超能力變出來的，全世界只有一根！牠的頭腦超級聰明。"),
    Pokemon(68, "怪力", "格鬥", "pkm_068", 1.6, 130.0,
        "有四隻強壯的手臂，可以從各個方向快速出拳。牠力氣超大，可以一次搬走好多東西！"),
    Pokemon(71, "大食花", "草", "pkm_071", 1.7, 15.5,
        "頭上開著一朵大大的花，會散發甜甜的香味。牠搖晃花朵的時候，就像在跳舞一樣。"),
    Pokemon(76, "隆隆岩", "岩石", "pkm_076", 1.4, 300.0,
        "身上穿著像岩石一樣堅硬的殼，每年脫一次皮，身體就變得更大。牠滾起來的時候像一顆大石球！"),
    Pokemon(78, "烈焰馬", "火", "pkm_078", 1.7, 95.0,
        "燃燒的鬃毛隨風飄揚，可以用每小時兩百四十公里的速度在大草原上奔跑。牠跑起來的時候就像一道火光！"),
    // Round 10
    Pokemon(80, "呆殼獸", "水", "pkm_080", 1.6, 78.5,
        "去海裡捕食的時候，大舌貝咬住了牠的尾巴，於是牠們就變成好朋友，一起生活了。牠還是慢吞吞、很悠哉的樣子。"),
    Pokemon(83, "大蔥鴨", "一般", "pkm_083", 0.8, 15.0,
        "把翅膀拿的蔥當成劍一樣揮舞，是一隻很會用劍的寶可夢。牠最寶貝那根蔥了，走到哪都帶著。"),
    Pokemon(84, "嘟嘟", "一般", "pkm_084", 1.4, 39.2,
        "有兩顆長得一模一樣的頭，牠們配合得天衣無縫。跑步的時候，兩顆頭會一起看著前方。"),
    Pokemon(86, "小海獅", "水", "pkm_086", 1.1, 90.0,
        "頭上有很堅硬的角，可以用頭撞破冰山來開路。牠喜歡在冰冷的海水裡游泳。"),
    Pokemon(113, "吉利蛋", "一般", "pkm_113", 1.1, 34.6,
        "心地非常善良，會生出營養滿分的蛋，分給受傷的寶可夢和人類吃。大家都好喜歡牠！"),
    Pokemon(130, "暴鯉龍", "水", "pkm_130", 6.5, 235.0,
        "雖然看起來很兇，其實牠以前是小小的鯉魚王，努力長大才變成這麼厲害的寶可夢！牠在水裡游得非常快。"),
)

const val ROUND_SIZE = 6

/** Derived from the list size, so adding Pokemon grows the game automatically:
 * 30 Pokemon -> 5 rounds, 60 Pokemon -> 10 rounds. */
val TOTAL_ROUNDS = ALL_POKEMON.size / ROUND_SIZE

/** The rounds, each a different set of 6 Pokemon. */
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
