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
]


def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    return urllib.request.urlopen(req, timeout=30).read()


def main():
    out = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable-nodpi"
    out.mkdir(parents=True, exist_ok=True)
    manifest = []
    for pid, name in POKEMON:
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
        dest = out / ("pkm_%03d.png" % pid)
        dest.write_bytes(data)
        manifest.append({"id": pid, "name": name, "source": img_url, "bytes": len(data)})
        print("%3d %s %d bytes  %s" % (pid, name, len(data), img_url))
    (Path(__file__).resolve().parent / "image-manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print("OK: %d images in %s" % (len(manifest), out))


if __name__ == "__main__":
    main()
