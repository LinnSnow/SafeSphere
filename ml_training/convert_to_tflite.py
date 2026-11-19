"""
SafeSphere - Person 2 (ML)
Convert the fine-tuned model to TensorFlow Lite and copy everything into the Android project.

Run (after train_classifier.py):
    python convert_to_tflite.py

The .tflite model takes two int32 inputs of shape [1, max_len]  (input_ids, attention_mask)
and returns one float of shape [1, 1]: the probability (0..1) that the text is adult-toned.
"""
import os

os.environ.setdefault("TF_USE_LEGACY_KERAS", "1")
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import argparse
import json
import shutil
from pathlib import Path

import numpy as np
import tensorflow as tf
from transformers import TFAutoModelForSequenceClassification

HERE = Path(__file__).resolve().parent
REPO_ROOT = HERE.parent  # ml_training/ sits next to app/ and backend/ in the repo


def build_serving_fn(model, max_len):
    @tf.function(input_signature=[
        tf.TensorSpec([1, max_len], tf.int32, name="input_ids"),
        tf.TensorSpec([1, max_len], tf.int32, name="attention_mask"),
    ])
    def serve(input_ids, attention_mask):
        logits = model(input_ids=input_ids, attention_mask=attention_mask, training=False).logits
        # softmax -> [P(normal), P(adult)]; keep only P(adult) so Kotlin gets the 0-1 score directly
        return {"explicit_score": tf.nn.softmax(logits, axis=-1)[:, 1:2]}

    return serve


def convert(serve, model, quantize, allow_select_ops):
    converter = tf.lite.TFLiteConverter.from_concrete_functions([serve.get_concrete_function()], model)
    if quantize:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]  # weights float32 -> int8 (about 4x smaller)
    ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    if allow_select_ops:
        ops.append(tf.lite.OpsSet.SELECT_TF_OPS)
    converter.target_spec.supported_ops = ops
    return converter.convert()


def input_index(interpreter, keyword):
    for d in interpreter.get_input_details():
        if keyword in d["name"]:
            return d["index"]
    raise RuntimeError(f"no TFLite input containing '{keyword}'")


def tflite_score(interpreter, ids, mask):
    interpreter.set_tensor(input_index(interpreter, "input_ids"), ids)
    interpreter.set_tensor(input_index(interpreter, "attention_mask"), mask)
    interpreter.invoke()
    return float(interpreter.get_tensor(interpreter.get_output_details()[0]["index"])[0][0])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(HERE / "output"))
    ap.add_argument("--assets-dir", default=str(REPO_ROOT / "app" / "app" / "src" / "main" / "assets"),
                    help="Android assets folder (default matches your repo layout)")
    ap.add_argument("--no-quantize", action="store_true", help="keep float32 weights (bigger, slightly more exact)")
    args = ap.parse_args()

    out = Path(args.out)
    tok_dir = out / "tokenizer"
    cfg = json.loads((tok_dir / "tokenizer_config.json").read_text())
    max_len = cfg["max_len"]

    model = TFAutoModelForSequenceClassification.from_pretrained(out / "hf_model")
    serve = build_serving_fn(model, max_len)

    try:
        tflite_bytes = convert(serve, model, not args.no_quantize, allow_select_ops=False)
    except Exception as err:  # noqa: BLE001
        print("Built-in ops only failed:", str(err)[:300])
        print("Retrying with SELECT_TF_OPS (larger app: needs tensorflow-lite-select-tf-ops).")
        tflite_bytes = convert(serve, model, not args.no_quantize, allow_select_ops=True)

    tflite_path = out / "hinglish_classifier.tflite"
    tflite_path.write_bytes(tflite_bytes)
    print(f"TFLite model: {tflite_path}  ({len(tflite_bytes) / 1e6:.1f} MB)")

    # ---- sanity check: TFLite output must match the original model -----------
    parity =json.loads((tok_dir / "tokenizer_parity_tests.json").read_text())
    interp = tf.lite.Interpreter(model_path=str(tflite_path))
    interp.allocate_tensors()

    diffs = []
    for item in parity[:25]:
        ids = np.array([item["input_ids"]], dtype=np.int32)
        mask = (ids != cfg["pad_id"]).astype(np.int32)
        ref = tf.nn.softmax(model(input_ids=ids, attention_mask=mask, training=False).logits, axis=-1).numpy()[0, 1]
        got = tflite_score(interp, ids, mask)
        diffs.append(abs(float(ref) - got))
    print(f"Max |Keras - TFLite| score difference over {len(diffs)} sentences: {max(diffs):.4f}")
    if max(diffs) > 0.05:
        print("WARNING: difference is large. Re-run with --no-quantize and compare.")

    # ---- copy into the Android project ---------------------------------------
    destinations = [Path(args.assets_dir)]
    alt_dest = REPO_ROOT / "app" / "src" / "main" / "assets"
    if alt_dest.parent.exists() and alt_dest not in destinations:
        destinations.append(alt_dest)

    for assets in destinations:
        (assets / "tokenizer").mkdir(parents=True, exist_ok=True)
        shutil.copy(tflite_path, assets / "hinglish_classifier.tflite")
        for name in ("vocab.txt", "tokenizer_config.json", "tokenizer_parity_tests.json"):
            shutil.copy(tok_dir / name, assets / "tokenizer" / name)
        print(f"Copied model + tokenizer files to {assets}")


if __name__ == "__main__":
    main()
