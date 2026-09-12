"""Ги прави сите големини на логото во frontend/public/brand/ од мастер сликата."""

import pathlib

from PIL import Image

HERE = pathlib.Path(__file__).resolve().parent
MASTER = HERE / 'focuslab-logo-master.png'
OUT = HERE.parent / 'frontend' / 'public' / 'brand'

SIZES = (32, 48, 128, 192, 256)

master = Image.open(MASTER).convert('RGBA')
OUT.mkdir(parents=True, exist_ok=True)

for size in SIZES:
    # LANCZOS: побрз филтер ги губи тенките линии наместо да ги стопи.
    out = master.resize((size, size), Image.LANCZOS)
    path = OUT / f'focuslab-logo-{size}.png'
    out.save(path, optimize=True)
    print(f'{path.name}: {path.stat().st_size / 1024:.1f} kB')
