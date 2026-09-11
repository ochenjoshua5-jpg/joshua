import pathlib, os, shutil, re
root = pathlib.Path('.')

# 1) core metadata and identity
p = root / 'settings.gradle.kts'
t = p.read_text(encoding='utf-8')
t = t.replace('rootProject.name = "YumaPlayer"', 'rootProject.name = "OJ Music Player"')
p.write_text(t, encoding='utf-8')

p = root / 'app' / 'build.gradle.kts'
t = p.read_text(encoding='utf-8')
t = t.replace('namespace = "moe.rukamori.archivetune"', 'namespace = "com.ochenjoshua.ojmusicplayer"')
t = t.replace('applicationId = "re.muwmix.yumaplayer"', 'applicationId = "com.ochenjoshua.ojmusicplayer"')
p.write_text(t, encoding='utf-8')

p = root / 'app' / 'src' / 'main' / 'AndroidManifest.xml'
t = p.read_text(encoding='utf-8')
t = t.replace('xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:tools=', 'xmlns:android="http://schemas.android.com/apk/res/android"\n    package="com.ochenjoshua.ojmusicplayer"\n    xmlns:tools=')
t = t.replace('android:scheme="yumaplayer"', 'android:scheme="ojmusicplayer"')
p.write_text(t, encoding='utf-8')

# 2) app name and strings
p = root / 'app' / 'src' / 'debug' / 'res' / 'values' / 'app_name.xml'
t = p.read_text(encoding='utf-8')
t = t.replace('Yuma Debug', 'OJ Music Player Debug')
p.write_text(t, encoding='utf-8')

p = root / 'app' / 'src' / 'main' / 'res' / 'values' / 'strings.xml'
t = p.read_text(encoding='utf-8')
t = t.replace('YumaPlayer', 'OJ Music Player')
t = t.replace('Yuma Player', 'OJ Music Player')
t = t.replace('Your playlist is ready for its first song ✨', 'Your playlist is ready for its first song ✨')
# add a few warm texts in the main English resources without touching others
if '<string name="app_name">' not in t:
    t = t.replace('</resources>', '    <string name="app_name">OJ Music Player</string>\n</resources>')
# add requested creator labels
if '<string name="creator_ochen_joshua">' not in t:
    t = t.replace('</resources>', '    <string name="creator_ochen_joshua">Created by Ochen Joshua</string>\n    <string name="made_by_ochen_joshua">Made by Ochen Joshua</string>\n    <string name="find_your_tune">Finding your tune...</string>\n    <string name="vibe_added">Added to your vibe!</string>\n    <string name="oops_error">Oops — something went wrong. Try again?</string>\n</resources>')
p.write_text(t, encoding='utf-8')

# 3) replace package path references in every Kotlin/Java/XML/Gradle file
for f in list(root.rglob('*.kt')) + list(root.rglob('*.java')) + list(root.rglob('*.xml')) + list(root.rglob('*.kts')) + list(root.rglob('*.gradle')):
    try:
        t = f.read_text(encoding='utf-8')
    except Exception:
        continue
    nt = t.replace('moe.rukamori.archivetune', 'com.ochenjoshua.ojmusicplayer').replace('re.muwmix.yumaplayer', 'com.ochenjoshua.ojmusicplayer').replace('YumaPlayer', 'OJ Music Player').replace('Yuma Player', 'OJ Music Player')
    if nt != t:
        f.write_text(nt, encoding='utf-8')

# 4) adjust package header in remaining Kotlin files at or below source path under new tree
for f in list((root/'app'/'src'/'main'/'kotlin'/'com'/'ochenjoshua'/'ojmusicplayer').rglob('*.kt')):
    try:
        t = f.read_text(encoding='utf-8')
    except Exception:
        continue
    nt = t.replace('package moe.rukamori.archivetune', 'package com.ochenjoshua.ojmusicplayer')
    if nt != t:
        f.write_text(nt, encoding='utf-8')

# 5) update a few theme files to requested palette/typography names and style
p = root / 'app' / 'src' / 'main' / 'kotlin' / 'com' / 'ochenjoshua' / 'ojmusicplayer' / 'ui' / 'theme' / 'Type.kt'
t = p.read_text(encoding='utf-8')
t = t.replace('val AppFontFamily = FontFamily(Font(R.font.poppins))', 'val AppFontFamily = FontFamily(Font(R.font.poppins))\nval NunitoFontFamily = FontFamily(Font(R.font.nunito_regular))')
t = t.replace('val AppTypography = buildTypography(AppFontFamily)', 'val AppTypography = buildTypography(AppFontFamily)')
# keep compile-safe default assignment for now
p.write_text(t, encoding='utf-8')

p = root / 'app' / 'src' / 'main' / 'kotlin' / 'com' / 'ochenjoshua' / 'ojmusicplayer' / 'ui' / 'theme' / 'Theme.kt'
t = p.read_text(encoding='utf-8')
t = t.replace('val DefaultThemeColor = Color(0xFFED5564)', 'val DefaultThemeColor = Color(0xFFFF6FC8)')
p.write_text(t, encoding='utf-8')

# 6) About screen: add creator credit line in the existing About UI function, no overdo
p = root / 'app' / 'src' / 'main' / 'kotlin' / 'com' / 'ochenjoshua' / 'ojmusicplayer' / 'ui' / 'screens' / 'settings' / 'AboutScreen.kt'
t = p.read_text(encoding='utf-8')
if 'Created by Ochen Joshua' not in t:
    # Add a creator text in the top header area of the About success content
    t = t.replace('AboutScreenContent(', 'AboutScreenContent(')
    t = t.replace('private fun AboutSuccessContent(', 'private fun AboutSuccessContent(')
    # Insert a short text block in the list after the about title in the About screen content header area
    if 'Created by Ochen Joshua' not in t:
        t = t.replace('title = {\n                    Text(\n                        text = stringResource(R.string.about),', 'title = {\n                    Text(\n                        text = stringResource(R.string.about),')
        # Add a small creator credit line in the About screen success content while retaining structure
        t = t.replace('AboutSuccessContent(', 'AboutCreatorSplash(/* Ochen Joshua credit */)\nAboutSuccessContent(')
        t = t.replace('private fun AboutSuccessContent(', 'private fun AboutCreatorSplash(/* Ochen Joshua credit */) {\n    Text(text = "Created by Ochen Joshua", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)\n}\n\n@Composable\nprivate fun AboutSuccessContent(')

p.write_text(t, encoding='utf-8')

# 7) Settings builder: add developer line in settings somewhere subtle
p = root / 'app' / 'src' / 'main' / 'kotlin' / 'com' / 'ochenjoshua' / 'ojmusicplayer' / 'ui' / 'screens' / 'settings' / 'SettingsDataBuilders.kt'
t = p.read_text(encoding='utf-8')
t = t.replace('subtitle = "v${BuildConfig.VERSION_NAME}",', 'subtitle = "v${BuildConfig.VERSION_NAME} • Ochen Joshua",')
p.write_text(t, encoding='utf-8')

print('OJ rebrand script complete')
