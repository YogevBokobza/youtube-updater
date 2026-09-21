# youtube-updater

אפליקציית אנדרואיד קטנה ל-head unit ברכב (אנדרואיד 12, ללא root) שמנטרת גרסאות
חדשות של **YouTube Morphe** ו-**MicroG RE** ב-GitHub, שולחת התראה, ומאפשרת
להוריד ולהתקין את הגרסה החדשה מתוך האפליקציה — בלי להיכנס לדפדפן.

## מה זה עושה

- בודק ברקע **כל 6 שעות** (WorkManager) אם יצאה גרסה חדשה.
- שולח **התראה** כשיש עדכון (פעם אחת לכל גרסה).
- מתוך האפליקציה: הורדת ה-APK (עם אימות SHA-256) והתקנה דרך מתקין המערכת.
- בלי root מוצג דיאלוג אישור התקנה אחד בכל עדכון — זו מגבלת אנדרואיד, לא באג.

## מקורות העדכון

| אפליקציה | מאגר | Asset |
|----------|------|-------|
| YouTube Morphe | `j-hc/revanced-magisk-module` (סריקת releases) | `youtube-morphe-v<ver>-all.apk` |
| MicroG RE | `MorpheApp/MicroG-RE` (`releases/latest`) | `microg-<ver>.apk` |

מוגדרים ב-`app/src/main/java/com/yogev/youtubeupdater/data/UpdateSource.kt`.

### אימות שם ה-package של YouTube Morphe

ברירת המחדל היא `app.revanced.android.youtube`. אם באפליקציה הגרסה המותקנת מוצגת
כ"לא מותקן" למרות שהיא מותקנת — שם ה-package שונה. בדוק אותו במכשיר
(הגדרות ← אפליקציות ← YouTube Morphe) ועדכן את `packageName` של `YOUTUBE` בקובץ הנ"ל.
(של MicroG כבר אומת: `app.revanced.android.gms`.)

## בנייה אוטומטית והפצה (GitHub Actions)

הבנייה רצה בענן — אין צורך ב-Android Studio מקומי.

1. צור repo חדש בשם `youtube-updater` ודחוף אליו את הקוד:
   ```bash
   git remote add origin https://github.com/<user>/youtube-updater.git
   git push -u origin main
   ```

2. צור keystore לחתימה (פעם אחת). חתימה יציבה נדרשת כדי שהמעדכן יוכל לעדכן את עצמו:
   ```bash
   keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 \
     -validity 10000 -alias youtube-updater
   ```

3. המר את ה-keystore ל-base64:
   - Linux/macOS: `base64 -w0 release.jks`
   - Windows PowerShell:
     `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))`

4. ב-GitHub: **Settings → Secrets and variables → Actions**, הוסף:
   - `KEYSTORE_BASE64` — הפלט מסעיף 3
   - `KEYSTORE_PASSWORD` — סיסמת ה-keystore
   - `KEY_ALIAS` — `youtube-updater`
   - `KEY_PASSWORD` — סיסמת המפתח

   > אם לא מגדירים secrets, הבנייה עדיין עובדת אבל חותמת ב-debug key
   > (טוב לבדיקה ראשונה; לא מאפשר עדכון-עצמי חלק בהמשך).

5. פרסם גרסה כדי להפעיל בנייה + Release:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   ה-APK יופיע תחת **Releases** (וגם כ-artifact של ה-workflow).

## התקנה על ה-head unit

1. הורד את ה-APK מדף ה-Releases והתקן.
2. באנדרואיד תתבקש לאשר "התקנה ממקורות לא ידועים" עבור האפליקציה — אשר.
3. פתח את האפליקציה, לחץ **בדוק עכשיו**, ולכל אפליקציה עם עדכון לחץ **עדכן**.

## הגדרות (בתוך האפליקציה)

- **GitHub Token** (אופציונלי) — מעלה את מגבלת הבקשות ל-GitHub (60/שעה ללא token).
- **כלול pre-release** — כבוי כברירת מחדל.

## מגבלות ידועות

- ללא root: דיאלוג אישור התקנה בכל עדכון (אין דרך לעקוף באנדרואיד 12 ללא root/Shizuku).
- עדכון-במקום (בלי איבוד דאטה) עובד כל עוד החתימה של הבילד החדש זהה למותקן —
  מתקיים כי j-hc ו-MorpheApp חותמים עקבי בין בילדים.
