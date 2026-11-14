#!/usr/bin/env python3
"""Download Pokemon artwork from the Taiwan official Pokedex detail pages.

Source: https://tw.portal-pokemon.com/play/pokedex/NNNN/
The main artwork URL is embedded in the page JSON as "file_name".
Images are bundled in the app for private home use only.
If this ever fails, fall back to PokeAPI official-artwork for the same IDs.
"""
import json
import re
import sys
import urllib.request
from pathlib import Path

POKEMON = [
    (1, "妙蛙種子"), (4, "小火龍"), (7, "傑尼龜"), (25, "皮卡丘"), (133, "伊布"), (143, "卡比獸"),
    (39, "胖丁"), (52, "喵喵"), (54, "可達鴨"), (35, "皮皮"), (37, "六尾"), (58, "卡蒂狗"),
    (63, "凱西"), (92, "鬼斯"), (104, "卡拉卡拉"), (129, "鯉魚王"), (131, "拉普拉斯"), (147, "迷你龍"),
    (10, "綠毛蟲"), (16, "波波"), (19, "小拉達"), (41, "超音蝠"), (50, "地鼠"), (74, "小拳石"),
    (79, "呆呆獸"), (94, "耿鬼"), (95, "大岩蛇"), (103, "椰蛋樹"), (123, "飛天螳螂"), (150, "超夢"),
    # v1.2 — rounds 6-10
    (2, "妙蛙草"), (5, "火恐龍"), (8, "卡咪龜"), (12, "巴大蝶"), (18, "大比鳥"), (20, "拉達"),
    (24, "阿柏怪"), (26, "雷丘"), (27, "穿山鼠"), (31, "尼多后"), (36, "皮可西"), (38, "九尾"),
    (40, "胖可丁"), (42, "大嘴蝠"), (46, "派拉斯"), (51, "三地鼠"), (55, "哥達鴨"), (59, "風速狗"),
    (61, "蚊香君"), (65, "胡地"), (68, "怪力"), (71, "大食花"), (76, "隆隆岩"), (78, "烈焰馬"),
    (80, "呆殼獸"), (83, "大蔥鴨"), (84, "嘟嘟"), (86, "小海獅"), (113, "吉利蛋"), (130, "暴鯉龍"),
    # v1.3 — rounds 11-20 (note: 61 is 蚊香君, fixed in v1.3; 60 蚊香蝌蚪 added here)
    (3, "妙蛙花"), (6, "噴火龍"), (9, "水箭龜"), (134, "水伊布"), (135, "雷伊布"), (136, "火伊布"),
    (144, "急凍鳥"), (145, "閃電鳥"), (146, "火焰鳥"), (149, "快龍"), (151, "夢幻"), (132, "百變怪"),
    (11, "鐵甲蛹"), (13, "獨角蟲"), (14, "鐵殼蛹"), (15, "大針蜂"), (17, "比比鳥"), (21, "烈雀"),
    (22, "大嘴雀"), (23, "阿柏蛇"), (28, "穿山王"), (29, "尼多蘭"), (30, "尼多娜"), (32, "尼多朗"),
    (33, "尼多力諾"), (34, "尼多王"), (43, "走路草"), (44, "臭臭花"), (45, "霸王花"), (47, "派拉斯特"),
    (48, "毛球"), (49, "摩魯蛾"), (53, "貓老大"), (56, "猴怪"), (57, "火爆猴"), (60, "蚊香蝌蚪"),
    (62, "蚊香泳士"), (64, "勇基拉"), (66, "腕力"), (67, "豪力"), (69, "喇叭芽"), (70, "口呆花"),
    (72, "瑪瑙水母"), (73, "毒刺水母"), (75, "隆隆石"), (77, "小火馬"), (81, "小磁怪"), (82, "三合一磁怪"),
    (85, "嘟嘟利"), (87, "白海獅"), (88, "臭泥"), (90, "大舌貝"), (91, "刺甲貝"), (93, "鬼斯通"),
    (96, "催眠貘"), (98, "大鉗蟹"), (100, "霹靂電球"), (102, "蛋蛋"), (105, "嘎啦嘎啦"), (108, "大舌頭"),
]


def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    return urllib.request.urlopen(req, timeout=30).read()


def main():
    out = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable-nodpi"
    out.mkdir(parents=True, exist_ok=True)
    manifest_path = Path(__file__).resolve().parent / "image-manifest.json"
    manifest = []
    if manifest_path.exists():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    seen = {entry["id"] for entry in manifest}
    for pid, name in POKEMON:
        dest = out / ("pkm_%03d.png" % pid)
        if pid in seen and dest.exists() and dest.read_bytes().startswith(b"\x89PNG"):
            print("%3d %s already downloaded, skipping" % (pid, name))
            continue
        page = fetch("https://tw.portal-pokemon.com/play/pokedex/%04d/" % pid).decode("utf-8", "ignore")
        m = re.search(r"file_name.{0,10}(/images/pokedex/[0-9a-f]+\.png)", page)
        if not m:
            print("FAIL %d %s: no file_name found" % (pid, name), file=sys.stderr)
            sys.exit(1)
        img_url = "https://tw.portal-pokemon.com" + m.group(1)
        data = fetch(img_url)
        if not data.startswith(b"\x89PNG"):
            print("FAIL %d %s: not PNG" % (pid, name), file=sys.stderr)
            sys.exit(1)
        dest.write_bytes(data)
        manifest.append({"id": pid, "name": name, "source": img_url, "bytes": len(data)})
        print("%3d %s %d bytes  %s" % (pid, name, len(data), img_url))
    manifest.sort(key=lambda e: e["id"])
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print("OK: %d images in %s" % (len(manifest), out))


if __name__ == "__main__":
    main()
