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
    (63, "凱西"), (92, "鬼斯"), (104, "可拉可拉"), (129, "鯉魚王"), (131, "乘龍"), (147, "迷你龍"),
    (10, "綠毛蟲"), (16, "波波"), (19, "小拉達"), (41, "超音蝠"), (50, "地鼠"), (74, "小拳石"),
    (79, "呆呆獸"), (94, "耿鬼"), (95, "大岩蛇"), (103, "椰蛋樹"), (123, "飛天螳螂"), (150, "超夢"),
    # v1.2 — rounds 6-10
    (2, "妙蛙草"), (5, "火恐龍"), (8, "卡咪龜"), (12, "巴大蝶"), (18, "大比鳥"), (20, "拉達"),
    (24, "阿柏怪"), (26, "雷丘"), (27, "穿山鼠"), (31, "尼多后"), (36, "皮可西"), (38, "九尾"),
    (40, "胖可丁"), (42, "大嘴蝠"), (46, "派拉斯"), (51, "三地鼠"), (55, "哥達鴨"), (59, "風速狗"),
    (61, "蚊香蝌蚪"), (65, "胡地"), (68, "怪力"), (71, "大食花"), (76, "隆隆岩"), (78, "烈焰馬"),
    (80, "呆殼獸"), (83, "大蔥鴨"), (84, "嘟嘟"), (86, "小海獅"), (113, "吉利蛋"), (130, "暴鯉龍"),
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
