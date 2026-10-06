#!/usr/bin/env python3
"""Render docs/assets/quickstart.gif — compose → /login → JWT. No live stack."""
from __future__ import annotations

import math
import shutil
import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "assets" / "quickstart.gif"
W, H = 960, 540
FPS = 8
BG_PAPER = (246, 243, 236)
INK = (26, 24, 20)
MUTED = (107, 101, 96)
LINE = (221, 214, 200)
PANEL = (255, 252, 247)
ACCENT = (196, 92, 38)
CODE_BG = (28, 25, 20)
CODE_FG = (232, 226, 214)
CODE_DIM = (154, 146, 132)
GREEN = (126, 196, 120)
BAR = (40, 36, 32)


def font(path: str, size: int, index: int = 0) -> ImageFont.FreeTypeFont:
    try:
        return ImageFont.truetype(path, size, index=index)
    except OSError:
        return ImageFont.load_default()


MENLO = "/System/Library/Fonts/Menlo.ttc"
GEORGIA = "/System/Library/Fonts/Supplemental/Georgia.ttf"
PING = "/System/Library/Fonts/Supplemental/Arial.ttf"
mono = lambda s: font(MENLO, s, 0)
mono_b = lambda s: font(MENLO, s, 1)
serif = lambda s: font(GEORGIA, s)
sans = lambda s: font(PING, s)


def new_paper() -> Image.Image:
    return Image.new("RGB", (W, H), BG_PAPER)


def window(im: Image.Image, title: str, y0: int = 56) -> tuple[int, int, int, int]:
    d = ImageDraw.Draw(im)
    x0, x1 = 36, W - 36
    y1 = H - 36
    d.rounded_rectangle([x0, y0, x1, y1], 10, fill=CODE_BG, outline=BAR, width=2)
    d.rounded_rectangle([x0, y0, x1, y0 + 36], 10, fill=BAR)
    d.rectangle([x0, y0 + 22, x1, y0 + 36], fill=BAR)
    for i, c in enumerate(((255, 95, 86), (255, 189, 46), (39, 201, 63))):
        cx = x0 + 18 + i * 18
        d.ellipse([cx, y0 + 12, cx + 10, y0 + 22], fill=c)
    tw = d.textbbox((0, 0), title, font=mono(13))
    d.text((x0 + (x1 - x0 - (tw[2] - tw[0])) // 2, y0 + 11), title, font=mono(13), fill=CODE_DIM)
    return x0 + 18, y0 + 52, x1 - 18, y1 - 16


def kicker(im: Image.Image, text: str) -> None:
    d = ImageDraw.Draw(im)
    d.text((40, 18), text, font=mono(15), fill=ACCENT)


def type_prefix(full: str, t: float, duration: float) -> str:
    if duration <= 0:
        return full
    n = int(len(full) * min(1.0, max(0.0, t / duration)))
    return full[:n]


def scene_compose(t: float) -> Image.Image:
    im = new_paper()
    kicker(im, "1 / 3  ·  clone and run")
    x, y, x1, y1 = window(im, "~/aaax  —  compose")
    d = ImageDraw.Draw(im)
    cmd = "docker compose --profile stack up --build"
    shown = type_prefix(cmd, t, 3.2)
    cursor = "▋" if int(t * FPS) % 2 == 0 and t < 3.4 else ""
    d.text((x, y), "$ " + shown + cursor, font=mono(16), fill=CODE_FG)
    logs = [
        (" ✔ Container aaax-postgres  Healthy", GREEN),
        (" ✔ Container aaax-redis     Healthy", GREEN),
        (" Started AaaxApplication    :8081", CODE_FG),
        (" AAAX_LOCAL_SEED            ok", CODE_DIM),
    ]
    if t > 3.6:
        ly = y + 36
        appear = min(4, int((t - 3.6) / 0.55) + 1)
        for line, col in logs[:appear]:
            d.text((x, ly), line, font=mono(15), fill=col)
            ly += 26
    return im


def scene_login(t: float) -> Image.Image:
    im = new_paper()
    kicker(im, "2 / 3  ·  hosted  /login")
    d = ImageDraw.Draw(im)
    # browser chrome
    x0, y0, x1, y1 = 36, 56, W - 36, H - 36
    d.rounded_rectangle([x0, y0, x1, y1], 10, fill=BG_PAPER, outline=LINE, width=2)
    d.rounded_rectangle([x0, y0, x1, y0 + 40], 10, fill=(255, 252, 247))
    d.rectangle([x0, y0 + 24, x1, y0 + 40], fill=(255, 252, 247))
    for i, c in enumerate(((255, 95, 86), (255, 189, 46), (39, 201, 63))):
        cx = x0 + 18 + i * 18
        d.ellipse([cx, y0 + 14, cx + 10, y0 + 24], fill=c)
    # url
    ux0, uy0 = x0 + 90, y0 + 10
    d.rounded_rectangle([ux0, uy0, x1 - 24, uy0 + 22], 6, fill=(243, 239, 230), outline=LINE)
    d.text((ux0 + 10, uy0 + 4), "http://localhost:8081/login", font=mono(12), fill=MUTED)

    # card
    cw, ch = 420, 390
    cx0 = (W - cw) // 2
    cy0 = y0 + 58
    d.rounded_rectangle([cx0, cy0, cx0 + cw, cy0 + ch], 12, fill=PANEL, outline=LINE, width=1)
    d.text((cx0 + 28, cy0 + 22), "AAAX", font=sans(13), fill=INK)
    d.text((cx0 + 28, cy0 + 42), "Accounts · Authentication · Authorization · eXperiences", font=sans(11), fill=MUTED)
    d.text((cx0 + 28, cy0 + 72), "Sign in", font=serif(28), fill=INK)
    d.text((cx0 + 28, cy0 + 112), "Continue OAuth2 authorization.", font=sans(14), fill=MUTED)

    user = type_prefix("smoke.primary@aaax.local", max(0, t - 1.0), 2.2)
    pw = type_prefix("••••••••••••••", max(0, t - 3.6), 1.4)

    def field(label: str, value: str, fy: int, focus: bool) -> None:
        d.text((cx0 + 28, fy), label, font=sans(12), fill=INK)
        box = [cx0 + 28, fy + 20, cx0 + cw - 28, fy + 56]
        d.rounded_rectangle(box, 8, fill=(255, 255, 255), outline=ACCENT if focus else LINE, width=2 if focus else 1)
        d.text((box[0] + 12, box[1] + 8), value, font=mono(14), fill=INK)

    field("Username", user, cy0 + 150, 1.0 <= t < 3.6)
    field("Password", pw, cy0 + 220, 3.6 <= t < 5.4)

    btn = [cx0 + 28, cy0 + 300, cx0 + cw - 28, cy0 + 342]
    hot = t >= 5.6
    d.rounded_rectangle(btn, 8, fill=(168, 77, 31) if hot else ACCENT)
    bw = d.textbbox((0, 0), "Sign in", font=sans(16))
    d.text((cx0 + (cw - (bw[2] - bw[0])) // 2, cy0 + 310), "Sign in", font=sans(16), fill=(255, 250, 245))
    return im


def scene_token(t: float) -> Image.Image:
    im = new_paper()
    kicker(im, "3 / 3  ·  JWT")
    x, y, *_ = window(im, "POST /oauth2/token")
    d = ImageDraw.Draw(im)
    curl = "curl -u client:secret -d grant_type=custom-password-grant \\"
    curl2 = "     -d username=smoke.primary@aaax.local -d credentials=…"
    shown = type_prefix(curl, t, 1.8)
    d.text((x, y), shown, font=mono(14), fill=CODE_FG)
    if t > 1.6:
        d.text((x, y + 22), curl2, font=mono(14), fill=CODE_DIM)
    body = [
        "{",
        '  "token_type": "Bearer",',
        '  "expires_in": 3600,',
        '  "access_token": "eyJhbGciOiJSUzI1NiJ9…"',
        "}",
    ]
    if t > 3.0:
        ly = y + 64
        n = min(len(body), int((t - 3.0) / 0.28) + 1)
        for line in body[:n]:
            col = GREEN if "access_token" in line else CODE_FG
            d.text((x, ly), line, font=mono(16), fill=col)
            ly += 26
    if t > 5.2:
        d.text((x, y + 220), "→  your app validates JWT against /oauth2/jwks", font=mono(14), fill=ACCENT)
    return im


def frames() -> list[Image.Image]:
    plan = [
        (scene_compose, 7.5),
        (scene_login, 9.0),
        (scene_token, 8.5),
    ]
    out: list[Image.Image] = []
    for fn, dur in plan:
        n = int(dur * FPS)
        for i in range(n):
            out.append(fn(i / FPS))
    return out


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    seq = frames()
    tmp = Path(tempfile.mkdtemp(prefix="aaax-gif-"))
    try:
        for i, im in enumerate(seq):
            im.save(tmp / f"{i:04d}.png")
        palette = tmp / "palette.png"
        subprocess.check_call(
            [
                "ffmpeg", "-y", "-framerate", str(FPS), "-i", str(tmp / "%04d.png"),
                "-vf", "palettegen=max_colors=64:stats_mode=diff", str(palette),
            ],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        subprocess.check_call(
            [
                "ffmpeg", "-y", "-framerate", str(FPS), "-i", str(tmp / "%04d.png"),
                "-i", str(palette),
                "-lavfi", "paletteuse=dither=bayer:bayer_scale=3",
                "-loop", "0", str(OUT),
            ],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    kb = OUT.stat().st_size / 1024
    print(f"wrote {OUT}  {len(seq)} frames  {kb:.0f} KB")
    if kb > 2800:
        raise SystemExit(f"gif too large: {kb:.0f} KB")


if __name__ == "__main__":
    main()
