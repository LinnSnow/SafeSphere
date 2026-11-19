"""
SafeSphere - Person 2 (ML)
Merge one or more raw CSVs (public datasets + your own) into dataset/dataset.csv (text,label).

Each --source is  path:text_column:label_column:positive_values
  positive_values = comma-separated label values that mean ADULT (-> 1). Everything else -> 0.

Example:
  python prepare_dataset.py \
    --source raw/my_labeled.csv:text:label:1 \
    --source raw/hasoc_hinglish.csv:comment:task_1:HOF
"""
import argparse
from pathlib import Path

import pandas as pd

HERE = Path(__file__).resolve().parent


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", action="append", required=True)
    ap.add_argument("--out", default=str(HERE / "dataset" / "dataset.csv"))
    args = ap.parse_args()

    frames = []
    for spec in args.source:
        path, text_col, label_col, positives = spec.split(":")
        df = pd.read_csv(path)
        positives = {p.strip() for p in positives.split(",")}
        part = pd.DataFrame({
            "text": df[text_col].astype(str).str.strip(),
            "label": df[label_col].astype(str).str.strip().isin(positives).astype(int),
        })
        print(f"{path}: {len(part)} rows, {part['label'].sum()} adult")
        frames.append(part)

    merged = pd.concat(frames, ignore_index=True)
    merged = merged[merged["text"].str.len() > 0]
    merged = merged.assign(_k=merged["text"].str.lower()).drop_duplicates("_k").drop(columns="_k")
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    merged.to_csv(args.out, index=False)
    print(f"Wrote {len(merged)} rows to {args.out}  |  class counts: {merged['label'].value_counts().to_dict()}")
    print("REMINDER: a 'hate/offensive' label is NOT the same as 'adult/sexual'. Review the positives by hand.")


if __name__ == "__main__":
    main()
