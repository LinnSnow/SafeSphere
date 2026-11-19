# SafeSphere - ML part: setup guide (Person 2)

## 1. Python side (Google Colab, free GPU: Runtime > Change runtime type > T4)

```bash
# upload the ml_training/ folder to Colab (or git clone your repo), then:
cd ml_training
pip install -r requirements.txt

# a) Build your dataset (see "Dataset" below). For a quick smoke test use the sample:
cp dataset/sample_dataset.csv dataset/dataset.csv

# b) Train (DistilBERT multilingual handles Roman-script Hinglish better than the English-only one)
python train_classifier.py --model distilbert-base-multilingual-cased
#    or MuRIL (best for Hinglish, but ~5x bigger):  --model google/muril-base-cased

# c) Convert + copy into the Android project
python convert_to_tflite.py
```

Outputs land in `ml_training/output/` (metrics.json, confusion_matrix.png, pr_curve.png) and the
model + tokenizer files are copied to `app/app/src/main/assets/`.

If running in Colab, download `output/` and commit these to the repo:
`app/app/src/main/assets/hinglish_classifier.tflite` and `app/app/src/main/assets/tokenizer/*`.

## 2. Android side

**app/app/build.gradle.kts**
```kotlin
android {
    // keep the .tflite file uncompressed so it can be memory-mapped
    androidResources { noCompress += "tflite" }
}
dependencies {
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    // ONLY if convert_to_tflite.py said it fell back to SELECT_TF_OPS:
    // implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
```
(Your repo uses `libs.versions.toml`; you can move these into the version catalog if the team prefers.)

**AndroidManifest.xml** (inside `<manifest>` / `<application>`)
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application ...>
    <service
        android:name="com.safesphere.ml.NotificationMonitorService"
        android:exported="true"
        android:label="SafeSphere text protection"
        android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
        <intent-filter>
            <action android:name="android.service.notification.NotificationListenerService" />
        </intent-filter>
    </service>
</application>
```

**One line you must fill in:** `createLogger()` in `NotificationMonitorService.kt` - return Person 4's
`BlockEventLoggerImpl` (look at its constructor). I could see only the file names in your repo, not the code.

**Onboarding screen (Person 3/UI):** call `NotificationAccess.isGranted(context)`; if false show a button that
calls `NotificationAccess.openSettings(context)`. On Android 13+ also request `POST_NOTIFICATIONS` at runtime.

## 3. Dataset

`dataset/dataset.csv` with two columns: `text,label` (1 = adult/sexual, 0 = normal).
`sample_dataset.csv` has only ~60 rows - enough to test the pipeline, NOT enough for real results.

- Aim for 2,000+ rows (at least a few hundred adult examples), roughly 1:3 to 1:1 adult:normal.
- Include plenty of NORMAL Hinglish (school, family, friends, OTPs, delivery messages, slang/abuse that is
  not sexual). That is what keeps false positives low.
- Public sets (HASOC, code-mixed hate speech) are "offensive", not "adult". Use `prepare_dataset.py` to merge
  them but REVIEW the positives by hand and relabel; otherwise the model learns "abusive", not "adult".
- Write your label definition in the report: adult = sexual/explicit/solicitation content.

## 4. Test it works
1. Run `TokenizerParityTest` (androidTest). It must pass - this proves Kotlin == Python.
2. Install the app, grant Notification access, send yourself a test message from another app.
