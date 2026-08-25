#!/usr/bin/env python3
import sys
from pathlib import Path
from PIL import Image

if len(sys.argv) != 2:
    print('usage: assert-visible-png.py <image.png>', file=sys.stderr)
    sys.exit(2)

path = Path(sys.argv[1])
img = Image.open(path).convert('RGB')
pixels = list(img.getdata())
visible = sum(1 for r, g, b in pixels if max(r, g, b) >= 12)
bright = sum(1 for r, g, b in pixels if max(r, g, b) >= 80)

# A genuine BandDrip screen contains several hundred text pixels. Keep the
# threshold low enough for antialiasing but high enough to reject a black frame.
if visible < 80 or bright < 20:
    print(f'RENDER FAILED: {path} is effectively blank ({visible} visible, {bright} bright pixels)', file=sys.stderr)
    sys.exit(1)

print(f'RENDER OK: {path} has {visible} visible and {bright} bright pixels at {img.width}x{img.height}')
