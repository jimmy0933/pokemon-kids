# 寶可夢收集遊戲（Pokemon Kids）

4 歲以上小朋友玩的 Android 遊戲：看圖翻牌配對，配對到的寶可夢就收進圖鑑。
全程離線、不用登入、沒有廣告、沒有課金。

> ⚠️ **公開 repo 注意**：Jimmy 2026-11-14 明確選擇這個 repo 用公開（public）。
> 遊戲本身只給自家小朋友玩，不上架 Play 商店；但 repo 公開表示裡面附的寶可夢圖片
> 任何人都能看到／下載。寶可夢角色與圖片的版權屬於其權利人，本專案未經官方授權，
> 僅供家中私人使用，不要拿去公開散布或商用。

## 玩法

- **主頁**：兩個大按鈕 — ▶ 玩遊戲、📖 我的圖鑑，下面顯示「已收集 X / 30」。
- **翻牌配對**：每關 6 隻寶可夢、12 張卡（3×4），點卡翻開，配對成功就留下來，
  還有音效和中文語音唸名字；配錯不會扣分，卡片蓋回去再試一次。
- **收集**：配對成功的寶可夢立刻進圖鑑，存在手機本機（DataStore），關掉重開還在。
  圖鑑裡收到的顯示彩色圖＋名字，點開可以看大圖、再聽一次名字；
  還沒收到的顯示黑色剪影＋「?」。上面有進度條「已收集 X / 30」，
  一關玩完會跳出慶祝畫面。
- **5 關、30 隻**：每關不同的一組 6 隻，玩完 5 關圖鑑就滿了。

## 30 隻寶可夢

| 關 | 寶可夢（編號） |
|---|---|
| 1 | 妙蛙種子 1、 小火龍 4、 傑尼龜 7、 皮卡丘 25、 伊布 133、 卡比獸 143 |
| 2 | 胖丁 39、 喵喵 52、 可達鴨 54、 皮皮 35、 六尾 37、 卡蒂狗 58 |
| 3 | 凱西 63、 鬼斯 92、 可拉可拉 104、 鯉魚王 129、 乘龍 131、 迷你龍 147 |
| 4 | 綠毛蟲 10、 波波 16、 小拉達 19、 超音蝠 41、 地鼠 50、 小拳石 74 |
| 5 | 呆呆獸 79、 耿鬼 94、 大岩蛇 95、 椰蛋樹 103、 飛天螳螂 123、 超夢 150 |

編號與繁體中文名以台灣寶可夢官方圖鑑為準。

## 圖片來源

角色圖從台灣寶可夢官方圖鑑下載，包在 App 裡（離線可用，執行時不連網抓圖）：
https://tw.portal-pokemon.com/play/pokedex/
例如妙蛙種子：https://tw.portal-pokemon.com/play/pokedex/0001/
下載腳本在 `tools/download_images.py`，來源清單在 `tools/image-manifest.json`。
這次 30 張全部都從官方圖鑑成功下載（630×630 PNG，共約 2.2 MB），沒有用到 PokeAPI 備援。

## 技術

- Kotlin + Jetpack Compose，`com.jimmy0933.pokemonkids`，minSdk 30、targetSdk 35、compileSdk 36
- 參考 Jimmy 其他專案（my-player）的設定：AGP 8.10.1、Kotlin 2.2.21、Gradle 8.13、Java 17、Compose BOM 2025.09.01
- 收集進度：Jetpack DataStore（Preferences），`collected_ids`
- 語音：Android 系統 TextToSpeech（繁體中文），手機沒有中文語音引擎時就靜音，遊戲照常玩
- 音效：內建 ToneGenerator 嗶聲，不用額外音檔

## 怎麼拿到 APK

1. 推到 `main` 後，GitHub Actions（`.github/workflows/debug-apk.yml`）會跑單元測試、
   編 debug APK，產物在 Actions 頁面的 `pokemon-kids-debug-apk`，下載後安裝到手機。
   APK 位置：`app/build/outputs/apk/debug/app-debug.apk`
2. 本機編：裝好 Android SDK 36 + Java 17 後，`gradle :app:assembleDebug`
   （本機沒有附 `gradlew`，用 `gradle` 8.13 指令或走 Actions 即可）

## 簽名版（給 Jimmy）

這個 repo 目前**只有 debug 版**，沒有簽名設定、沒有新建 keystore。
要做簽名 release 版時，請用你自己的 keystore 與 Secrets，做法可參考 my-player
的 `android-release.yml`（條件式 signingConfig），不要把 keystore 或密碼提交進 repo。

## 檔案結構

- `app/src/main/java/com/jimmy0933/pokemonkids/` — `MainActivity.kt`（主頁＋選關）、
  `GameScreen.kt`（翻牌配對）、`CollectionScreen.kt`（圖鑑）、`Pokemon.kt`（30 隻資料＋關卡邏輯）、
  `CollectionStore.kt`（DataStore）、`Sounds.kt`（音效）
- `app/src/main/res/drawable-nodpi/pkm_*.png` — 30 張寶可夢圖
- `app/src/test/.../PokemonDataTest.kt` — 30 隻不重複、5 關各 6 隻、每關 12 張卡是 6 對的測試
