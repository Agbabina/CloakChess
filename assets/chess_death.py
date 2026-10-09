"""
chess_death.py - turn ANY RGBA sprite PNG into a death-animation strip.

Usage:
  python chess_death.py piece in.png out.png [--dust R,G,B]     -> 6 frames
  python chess_death.py boss  in.png out.png [--accent R,G,B]   -> 10 frames

Output is a horizontal strip, every frame the same size as the input sprite,
so it loads straight into a libGDX Animation<TextureRegion>.
"""
import random
import sys
import numpy as np
from PIL import Image

CRACK = (24, 18, 30, 255)


def load(path):
    return np.array(Image.open(path).convert("RGBA"))


def save_strip(frames, path):
    h, w = frames[0].shape[:2]
    img = Image.new("RGBA", (w * len(frames), h), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        img.paste(Image.fromarray(f), (i * w, 0))
    img.save(path)


def shift(a, dx, dy):
    out = np.zeros_like(a)
    h, w = a.shape[:2]
    ys = slice(max(0, dy), min(h, h + dy))
    yd = slice(max(0, -dy), min(h, h - dy))
    xs = slice(max(0, dx), min(w, w + dx))
    xd = slice(max(0, -dx), min(w, w - dx))
    out[ys, xs] = a[yd, xd]
    return out


def flash(a):
    out = a.copy()
    out[a[..., 3] > 0, :3] = 255
    return out


def mix(c1, c2, k):
    k = max(0.0, min(1.0, k))
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * k)) for i in range(3))


def crack_paths(a, seed, n=6, steps=(6, 11)):
    rng = random.Random(seed)
    ys, xs = np.nonzero(a[..., 3] > 0)
    lim = np.percentile(ys, 60)
    cand = np.nonzero(ys <= lim)[0]
    paths = []
    for _ in range(n):
        k = int(cand[rng.randrange(len(cand))])
        x, y = int(xs[k]), int(ys[k])
        dx = rng.choice([-1, 1])
        path = []
        for _ in range(rng.randint(*steps)):
            path.append((x, y))
            r = rng.random()
            if r < 0.55:
                y += 1
            elif r < 0.85:
                x += dx
            else:
                x -= dx
        paths.append(path)
    return paths


def apply_cracks(a, paths, frac, glow=None):
    out = a.copy()
    h, w = a.shape[:2]
    for p in paths:
        n = int(round(len(p) * frac))
        for i, (x, y) in enumerate(p[:n]):
            if 0 <= x < w and 0 <= y < h and out[y, x, 3] > 0:
                if glow is not None and i % 2 == 0:
                    out[y, x] = (*glow, 255)
                else:
                    out[y, x] = CRACK
    return out


def split_frame(a, seed):
    rng = random.Random(seed)
    h, w = a.shape[:2]
    ys, xs = np.nonzero(a[..., 3] > 0)
    cx = int(round(xs.mean()))
    L = np.zeros_like(a)
    R = np.zeros_like(a)
    for y in range(h):
        c = cx + rng.choice([-1, 0, 1])
        L[y, :c] = a[y, :c]
        R[y, c:] = a[y, c:]
    out = shift(L, -1, 0)
    r = shift(R, 1, 0)
    m = r[..., 3] > 0
    out[m] = r[m]
    return out


def chunk_state(a, mode, seed, size, n_shards):
    rng = random.Random(seed)
    ys, xs = np.nonzero(a[..., 3] > 0)
    if mode == "block":
        ids = (ys // size) * 1000 + (xs // size)
    else:
        k = rng.sample(range(len(ys)), min(n_shards, len(ys)))
        sx, sy = xs[k], ys[k]
        d = (xs[:, None] - sx[None, :]) ** 2 + (ys[:, None] - sy[None, :]) ** 2
        ids = d.argmin(1)
    chunks = {}
    for i, (x, y) in enumerate(zip(xs, ys)):
        chunks.setdefault(int(ids[i]), []).append((int(x), int(y)))
    return chunks, (xs.mean(), ys.mean())


def render_chunks(a, t, style, seed=1):
    """Break the sprite into chunks/shards and simulate them t frames after the break."""
    h, w = a.shape[:2]
    g_ = style.get
    chunks, (cx, cy) = chunk_state(a, g_("mode", "block"), seed, g_("size", 4), g_("shards", 9))
    rng = random.Random(seed * 7 + 1)
    ys, xs = np.nonzero(a[..., 3] > 0)
    thr, pr = {}, {}
    for x, y in zip(xs, ys):
        thr[(int(x), int(y))] = rng.random()
        pr[(int(x), int(y))] = rng.random()
    out = np.zeros_like(a)
    fl = g_("floor")
    for cid in sorted(chunks):
        pts = chunks[cid]
        mx = sum(p[0] for p in pts) / len(pts)
        my = sum(p[1] for p in pts) / len(pts)
        o, up, g = g_("out", 0.6), g_("up", 0.0), g_("g", 0.6)
        delay = g_("delay_fn", lambda x, y: 0)(mx, my)
        burn = False
        sp = g_("special")
        if sp:
            ov = sp(mx, my)
            if ov:
                o = ov.get("out", o)
                up = ov.get("up", up)
                g = ov.get("g", g)
                delay = ov.get("delay", delay)
                burn = ov.get("burn", False)
        jit = g_("jit", 0.4)
        vx = (mx - cx) / (w / 2) * o * 2 + rng.uniform(-jit, jit)
        vy = (my - cy) / (h / 2) * o + up + rng.uniform(-jit, jit)
        te = max(0.0, t - delay)
        dx = int(round(vx * te * 1.5))
        dy = int(round(vy * te + 0.5 * g * te * te))
        for (x, y) in pts:
            nx, ny = x + dx, y + dy
            sq = g_("squash", 0.0)
            if sq and fl is not None:
                ny = int(round(fl - (fl - ny) * max(0.25, 1 - sq * te)))
            atfloor = False
            if fl is not None and dy > 0 and ny > fl:
                ny = fl - int(pr[(x, y)] * 3)
                atfloor = True
            d = g_("dissolve", 0.0)
            if atfloor:
                d *= g_("keep", 1.0)
            if thr[(x, y)] < d:
                continue
            col = tuple(int(v) for v in a[y, x, :3])
            if burn:
                col = mix(col, (255, 140, 30), te / 1.5)
                col = mix(col, (30, 20, 24), (te - 2) / 3)
            ash = g_("ash")
            if ash and te > 0:
                col = mix(col, ash, te / 4)
            gl = g_("glint", 0.0)
            if gl and pr[(x, y)] < gl:
                col = (235, 250, 255)
            if 0 <= nx < w and 0 <= ny < h:
                out[ny, nx] = (*col, 255)
    n = g_("sparks", 0)
    if n:
        cols = g_("spark_cols", [(255, 255, 255)])
        srng = random.Random(seed * 13 + int(t * 10))
        x0, x1 = max(0, int(xs.min()) - 2), min(w - 1, int(xs.max()) + 2)
        y0, y1 = max(0, int(ys.min()) - 3), min(h - 1, int(ys.max()) + 2)
        for _ in range(n):
            x, y = srng.randint(x0, x1), srng.randint(y0, y1)
            out[y, x] = (*srng.choice(cols), 255)
    return out


def death_piece(a, dust=(110, 100, 120), seed=3):
    """6 frames: flash, rise, cracks, split, chunks, dust."""
    paths = crack_paths(a, seed, 4, (5, 9))
    ac = apply_cracks(a, paths, 1.0)
    st = dict(size=3, out=0.5, g=0.6, jit=0.3, spark_cols=[dust, (200, 195, 210)])
    return [
        flash(a),
        shift(a, 0, -1),
        ac,
        split_frame(ac, seed),
        render_chunks(ac, 2, dict(st, dissolve=0.4), seed),
        render_chunks(ac, 3, dict(st, dissolve=0.85, sparks=5), seed),
    ]


def death_boss(a, style, accent=(255, 160, 60), seed=7):
    """10 frames: flash, recoil, cracks x3, then 5 frames of break-up."""
    paths = crack_paths(a, seed, 7)
    f = [
        flash(a),
        shift(a, -1, 1),
        apply_cracks(flash(a), paths, 0.35),
        shift(apply_cracks(a, paths, 0.7), 1, 0),
        shift(apply_cracks(a, paths, 1.0, glow=accent), -1, 0),
    ]
    diss = [0, 0.1, 0.35, 0.65, 0.9]
    sparks = [0, 2, 6, 6, 3]
    src = apply_cracks(a, paths, 1.0, glow=accent)
    for i in range(5):
        f.append(render_chunks(src, i + 1, dict(style, dissolve=diss[i], sparks=sparks[i]), seed))
    return f


def _rgb(s):
    return tuple(int(v) for v in s.split(","))


if __name__ == "__main__":
    if len(sys.argv) < 4 or sys.argv[1] not in ("piece", "boss"):
        print(__doc__)
        sys.exit(1)
    kind, src, dst = sys.argv[1:4]
    sprite = load(src)
    if kind == "piece":
        dust = _rgb(sys.argv[5]) if len(sys.argv) > 5 and sys.argv[4] == "--dust" else (110, 100, 120)
        save_strip(death_piece(sprite, dust), dst)
    else:
        acc = _rgb(sys.argv[5]) if len(sys.argv) > 5 and sys.argv[4] == "--accent" else (255, 160, 60)
        save_strip(death_boss(sprite, dict(size=4, out=0.6, g=0.6, spark_cols=[acc, (255, 255, 255)]), acc), dst)
