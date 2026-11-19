"""
SafeSphere - Person 2 (ML)
Fine-tune a small BERT-style model to detect adult-toned text (English + Hinglish).

Run (from the ml_training/ folder):
    python train_classifier.py --data dataset/dataset.csv

Everything it produces goes into ml_training/output/:
    hf_model/                         the fine-tuned model (input for convert_to_tflite.py)
    tokenizer/vocab.txt               SAME vocab the Android app must use
    tokenizer/tokenizer_config.json   max length, special-token ids, threshold
    tokenizer/tokenizer_parity_tests.json   sentences + the exact token ids Python produced
    train.csv / val.csv / test.csv    the splits (test.csv is the HELD-OUT set)
    metrics.json                      precision, recall, F1, FPR ... for your results section
    confusion_matrix.png, pr_curve.png
"""
import os

# Must be set BEFORE tensorflow is imported (Hugging Face TF models need Keras 2).
os.environ.setdefault("TF_USE_LEGACY_KERAS", "1")
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import argparse
import json
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.metrics import confusion_matrix, precision_recall_curve, roc_auc_score
from sklearn.model_selection import train_test_split
from transformers import AutoTokenizer, TFAutoModelForSequenceClassification

HERE = Path(__file__).resolve().parent
SEED = 42

# Fixed sentences used to prove Python and Kotlin tokenize identically.
PARITY_SENTENCES = [
    "kya kar rahi ho?",
    "Bhai notes bhej dena physics ke",
    "send me your photo!!!",
    "Café ÉCOLE naïve",
    "hello... how are you??",
    "tu bahut sexy lag rahi hai",
    "   extra    spaces   and\ttabs\nnewlines  ",
    "OTP is 483920, do not share",
    "emoji test 😀 mixed text",
    "I'm sure it's fine, don't worry (really)",
    "",
    "a " * 300,  # very long -> must truncate identically
]


# --------------------------------------------------------------------------- data
def load_dataset(path: Path) -> pd.DataFrame:
    df = pd.read_csv(path)
    if not {"text", "label"}.issubset(df.columns):
        raise SystemExit("CSV must have two columns named: text,label")
    df = df[["text", "label"]].dropna()
    df["text"] = df["text"].astype(str).str.strip()
    df = df[df["text"] != ""]
    df["label"] = df["label"].astype(int)
    bad = set(df["label"].unique()) - {0, 1}
    if bad:
        raise SystemExit(f"labels must be 0 (normal) or 1 (adult), found: {bad}")
    # Remove duplicates BEFORE splitting, otherwise the same sentence can land in
    # both train and test and your scores look better than they really are.
    df = df.assign(_key=df["text"].str.lower()).drop_duplicates("_key").drop(columns="_key")
    return df.reset_index(drop=True)


def split_dataset(df: pd.DataFrame):
    train, temp = train_test_split(df, test_size=0.2, stratify=df["label"], random_state=SEED)
    val, test = train_test_split(temp, test_size=0.5, stratify=temp["label"], random_state=SEED)
    return train.reset_index(drop=True), val.reset_index(drop=True), test.reset_index(drop=True)


# ------------------------------------------------------------------------ metrics
def metrics_at(y_true, probs, threshold):
    pred = (np.asarray(probs) > threshold).astype(int)  # strict '>' - same rule the app uses
    tn, fp, fn, tp = confusion_matrix(y_true, pred, labels=[0, 1]).ravel()
    precision = tp / (tp + fp) if (tp + fp) else 0.0
    recall = tp / (tp + fn) if (tp + fn) else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) else 0.0
    fpr = fp / (fp + tn) if (fp + tn) else 0.0
    return {
        "threshold": round(float(threshold), 3),
        "TP": int(tp), "FP": int(fp), "TN": int(tn), "FN": int(fn),
        "precision": round(float(precision), 4),
        "recall": round(float(recall), 4),
        "f1": round(float(f1), 4),
        "false_positive_rate": round(float(fpr), 4),
        "accuracy": round(float((tp + tn) / len(y_true)), 4),
    }


def choose_threshold(y_val, p_val, max_fpr):
    """Best F1 on the VALIDATION set while keeping false-positive rate <= max_fpr."""
    best, best_f1 = None, -1.0
    for thr in np.arange(0.05, 0.96, 0.01):
        m = metrics_at(y_val, p_val, thr)
        if m["false_positive_rate"] <= max_fpr and m["f1"] > best_f1:
            best, best_f1 = float(thr), m["f1"]
    if best is None:  # nothing satisfied the FPR cap -> fall back to plain best F1
        for thr in np.arange(0.05, 0.96, 0.01):
            m = metrics_at(y_val, p_val, thr)
            if m["f1"] > best_f1:
                best, best_f1 = float(thr), m["f1"]
    return round(best, 2)


# -------------------------------------------------------------------------- model
def encode(tok, texts, max_len):
    enc = tok(list(texts), truncation=True, padding="max_length", max_length=max_len, return_tensors="np")
    return {
        "input_ids": enc["input_ids"].astype("int32"),
        "attention_mask": enc["attention_mask"].astype("int32"),
    }


def predict_probs(model, inputs, batch_size=32):
    logits = model.predict(inputs, batch_size=batch_size, verbose=0).logits
    return tf.nn.softmax(logits, axis=-1).numpy()[:, 1]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default=str(HERE / "dataset" / "dataset.csv"))
    ap.add_argument("--model", default="distilbert-base-uncased",
                    help="any WordPiece (vocab.txt) BERT-family model, e.g. google/muril-base-cased")
    ap.add_argument("--max_len", type=int, default=128)
    ap.add_argument("--epochs", type=int, default=4)
    ap.add_argument("--batch", type=int, default=16)
    ap.add_argument("--lr", type=float, default=3e-5)
    ap.add_argument("--max_fpr", type=float, default=0.05,
                    help="false-positive-rate cap used when auto-choosing the threshold")
    ap.add_argument("--threshold", type=float, default=None,
                    help="force a threshold (e.g. 0.7). Default: pick on validation set")
    ap.add_argument("--out", default=str(HERE / "output"))
    args = ap.parse_args()

    tf.keras.utils.set_random_seed(SEED)
    out = Path(args.out)
    (out / "tokenizer").mkdir(parents=True, exist_ok=True)

    # 1. data -----------------------------------------------------------------
    df = load_dataset(Path(args.data))
    train, val, test = split_dataset(df)
    train.to_csv(out / "train.csv", index=False)
    val.to_csv(out / "val.csv", index=False)
    test.to_csv(out / "test.csv", index=False)
    print(f"Rows: total={len(df)} train={len(train)} val={len(val)} test={len(test)}")
    print("Class balance (1 = adult):", df["label"].value_counts().to_dict())

    # 2. tokenizer ------------------------------------------------------------
    tok = AutoTokenizer.from_pretrained(args.model)
    vocab_path = tok.save_vocabulary(str(out / "tokenizer"))[0]
    if Path(vocab_path).name != "vocab.txt":
        raise SystemExit("This model has no WordPiece vocab.txt - the Kotlin tokenizer needs one.")

    x_train = encode(tok, train["text"], args.max_len)
    x_val = encode(tok, val["text"], args.max_len)
    x_test = encode(tok, test["text"], args.max_len)

    # 3. model ----------------------------------------------------------------
    try:
        model = TFAutoModelForSequenceClassification.from_pretrained(args.model, num_labels=2)
    except OSError:  # repo only has PyTorch weights
        model = TFAutoModelForSequenceClassification.from_pretrained(args.model, num_labels=2, from_pt=True)

    counts = train["label"].value_counts()
    class_weight = {c: len(train) / (2 * counts[c]) for c in (0, 1)}  # rarer class counts more
    print("Class weights:", class_weight)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=args.lr),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
        metrics=["accuracy"],
    )
    model.fit(
        x_train, train["label"].values,
        validation_data=(x_val, val["label"].values),
        epochs=args.epochs, batch_size=args.batch, class_weight=class_weight,
        callbacks=[tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=1, restore_best_weights=True)],
    )

    # 4. threshold + evaluation on the HELD-OUT test set ----------------------
    p_val = predict_probs(model, x_val)
    p_test = predict_probs(model, x_test)
    chosen = args.threshold if args.threshold is not None else choose_threshold(val["label"].values, p_val, args.max_fpr)
    y_test = test["label"].values

    results = {
        "model": args.model,
        "max_len": args.max_len,
        "dataset": {"total": len(df), "train": len(train), "val": len(val), "test": len(test),
                    "class_counts": {str(k): int(v) for k, v in df["label"].value_counts().items()}},
        "chosen_threshold": chosen,
        "test_at_chosen_threshold": metrics_at(y_test, p_test, chosen),
        "test_at_0.7": metrics_at(y_test, p_test, 0.7),
        "test_roc_auc": round(float(roc_auc_score(y_test, p_test)), 4) if len(set(y_test)) == 2 else None,
    }
    (out / "metrics.json").write_text(json.dumps(results, indent=2))
    print(json.dumps(results, indent=2))

    # plots
    m = results["test_at_chosen_threshold"]
    cm = np.array([[m["TN"], m["FP"]], [m["FN"], m["TP"]]])
    fig, ax = plt.subplots(figsize=(4, 4))
    ax.imshow(cm, cmap="Blues")
    for i in range(2):
        for j in range(2):
            ax.text(j, i, cm[i, j], ha="center", va="center", fontsize=14)
    ax.set_xticks([0, 1], ["pred normal", "pred adult"])
    ax.set_yticks([0, 1], ["actual normal", "actual adult"])
    ax.set_title(f"Test confusion matrix (thr={chosen})")
    fig.tight_layout()
    fig.savefig(out / "confusion_matrix.png", dpi=150)
    plt.close(fig)

    if len(set(y_test)) == 2:
        pr, rc, _ = precision_recall_curve(y_test, p_test)
        fig, ax = plt.subplots(figsize=(5, 4))
        ax.plot(rc, pr)
        ax.set_xlabel("Recall")
        ax.set_ylabel("Precision")
        ax.set_title("Precision-Recall curve (test)")
        fig.tight_layout()
        fig.savefig(out / "pr_curve.png", dpi=150)
        plt.close(fig)

    # 5. save everything the converter + Android app need ---------------------
    model.save_pretrained(out / "hf_model")

    cfg = {
        "max_len": args.max_len,
        "do_lower_case": bool(tok.init_kwargs.get("do_lower_case", True)),
        "cls_id": tok.cls_token_id, "sep_id": tok.sep_token_id,
        "pad_id": tok.pad_token_id, "unk_id": tok.unk_token_id,
        "threshold": chosen,
    }
    (out / "tokenizer" / "tokenizer_config.json").write_text(json.dumps(cfg, indent=2))

    sample = list(test["text"].head(15))
    parity = []
    for text in PARITY_SENTENCES + sample:
        ids = tok(text, truncation=True, padding="max_length", max_length=args.max_len)["input_ids"]
        parity.append({"text": text, "input_ids": ids})
    (out / "tokenizer" / "tokenizer_parity_tests.json").write_text(json.dumps(parity, ensure_ascii=False, indent=1))

    print(f"\nDone. Next: python convert_to_tflite.py   (outputs are in {out})")


if __name__ == "__main__":
    main()
