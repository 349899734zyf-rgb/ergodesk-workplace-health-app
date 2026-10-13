"""
Builds app/src/main/assets/motion_timeline.csv from raw MotionSense CSVs.

MotionSense (Kaggle) records iPhone sensors at 50 Hz with the phone in the
front trouser pocket. Each raw file holds one activity; the label is only in
the folder name (sit_5, std_6, wlk_7, jog_9). Download these files from
Kaggle and save them flat in tools/motionsense_raw/ as
<folder>_<file>.csv, e.g. sit_5_sub_1.csv:

    sit_5/sub_1.csv, std_6/sub_1.csv, wlk_7/sub_1.csv, jog_9/sub_1.csv

This script cuts every file into 1-second windows (50 samples), keeps two
features per window and stitches segments into one labelled timeline that
SensorDataSimulator replays (and loops) in the app:

    pitch          mean attitude.pitch (rad), roughly the thigh angle
    accMagnitude   mean |userAcceleration| (g), how much the body moves

Usage: python3 tools/build_motion_timeline.py
"""

import csv
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW_DIR = ROOT / "tools" / "motionsense_raw"
OUT_FILE = ROOT / "app" / "src" / "main" / "assets" / "motion_timeline.csv"

SAMPLES_PER_SECOND = 50

# Labels must match PostureState in the app. Walking and jogging both map
# to MOVING because ErgoDesk only cares whether the user left the desk.
LABELS = {
    "sit_5": "SITTING",
    "std_6": "STANDING",
    "wlk_7": "MOVING",
    "jog_9": "MOVING",
}

# (raw file, first second, length in seconds) in playback order. A segment
# longer than its file wraps around to the file's start, so the 205-second
# sitting recording can fill a realistic desk bout.
# The story: sit past the 30-minute threshold, walk, stand, a shorter sit,
# a jog, then a long sit that passes even the relaxed 45-minute interval.
SEGMENTS = [
    ("sit_5_sub_1.csv", 0, 35 * 60),
    ("wlk_7_sub_1.csv", 0, 108),
    ("std_6_sub_1.csv", 0, 203),
    ("sit_5_sub_1.csv", 0, 20 * 60),
    ("jog_9_sub_1.csv", 0, 97),
    ("std_6_sub_1.csv", 0, 100),
    ("sit_5_sub_1.csv", 0, 50 * 60),
    ("wlk_7_sub_1.csv", 0, 108),
    ("std_6_sub_1.csv", 100, 103),
]


def one_second_windows(path):
    with open(path, newline="") as f:
        rows = list(csv.DictReader(f))

    windows = []
    for start in range(0, len(rows) - SAMPLES_PER_SECOND + 1, SAMPLES_PER_SECOND):
        window = rows[start:start + SAMPLES_PER_SECOND]
        pitch = sum(float(r["attitude.pitch"]) for r in window) / len(window)
        acc = sum(
            math.sqrt(sum(float(r[f"userAcceleration.{axis}"]) ** 2 for axis in "xyz"))
            for r in window
        ) / len(window)
        windows.append((pitch, acc))
    return windows


def main():
    cache = {}
    timeline = []

    for name, first, length in SEGMENTS:
        if name not in cache:
            cache[name] = one_second_windows(RAW_DIR / name)
        windows = cache[name]
        label = LABELS[name.rsplit("_", 2)[0]]
        for offset in range(length):
            pitch, acc = windows[(first + offset) % len(windows)]
            timeline.append((pitch, acc, label))

    OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT_FILE, "w", newline="") as f:
        writer = csv.writer(f, lineterminator="\n")
        writer.writerow(["second", "pitch", "accMagnitude", "label"])
        for second, (pitch, acc, label) in enumerate(timeline):
            writer.writerow([second, f"{pitch:.3f}", f"{acc:.3f}", label])

    print(f"Wrote {len(timeline)} seconds to {OUT_FILE.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
