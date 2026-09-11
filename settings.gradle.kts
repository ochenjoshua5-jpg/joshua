. Do only this task. Do not make changes to any other file.

The GitHub Actions build is failing with this exact error:

text
e: [ksp] /home/runner/work/joshua/joshua/app/src/main/kotlin/com/ochenjoshua/ojmusicplayer/db/MusicDatabase.kt:393: 
Schema '2.json' required for migration was not found at the schema out folder. Cannot generate auto migrations.
This repeats for 2.json through 21.json.

Root cause: The Room database is at version 21 with auto-migrations from versions 2 through 21. The historical schema JSON files (normally in app/schemas/) are missing because they were never included in this fork. Room cannot generate migrations without them.

Since this is a fresh app with no existing users and no existing databases on any device, migrations are not needed at all.

Task 1: Reset the Room database to version 1
Open app/src/main/kotlin/com/ochenjoshua/ojmusicplayer/db/MusicDatabase.kt.

In the @Database(...) annotation, change:

version = 21 → version = 1

Remove the entire autoMigrations = [...] parameter from the @Database annotation (including all entries and the surrounding brackets).

If exportSchema = true is set, change it to exportSchema = false.

Find where the database is built (look for Room.databaseBuilder — usually in a Hilt module like DatabaseModule.kt or inside a companion object in MusicDatabase.kt).

Add .fallbackToDestructiveMigration() to the builder chain. If the method is deprecated in the current Room version, use .fallbackToDestructiveMigration(dropAllTables = true) instead.

Search the entire project for any code that references the database version number 21 (comments, tests, etc.) and update them to 1 for consistency. Do NOT change anything else.

Also search for Migration classes defined in the project (files with Migration( or object MIGRATION_ in them) and comment them out or remove them from the builder — they no longer apply.

Task 2: Fix the missing translation resource
The build log also shows:

text
warn: removing resource com.ochenjoshua.ojmusicplayer.debug:string/about_position_yuki without required default value.
Search app/src/main/res/ for any file containing about_position_yuki.

The string exists in a locale-specific strings.xml (e.g., values-ja/strings.xml), but not in the default app/src/main/res/values/strings.xml.

Add the string to the default values/strings.xml with the same or a reasonable English value. For example:
<string name="about_position_yuki">Yuki</string>

Do not delete the locale-specific version.

Task 3: Output
When done, output:

The full corrected @Database(...) annotation block from MusicDatabase.kt.

The full corrected database builder chain.

Confirmation that you added the missing about_position_yuki string to values/strings.xml.

A list of every file you modified with a one-line summary.

Print exactly: ROOM FIXED AND READY TO PUSH

Do not touch the workflow file, libs.versions.toml, AndroidManifest.xml, or any other file. Only the files described above.@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("androidx")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral {
            mavenContent {
                releasesOnly()
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "JitPack"
                    setUrl("https://jitpack.io")
                }
            }
            filter {
                includeGroup("com.github.therealbush")
                includeGroup("com.github.TeamNewPipe")
            }
        }
    }
}

// F-Droid doesn't support foojay-resolver plugin
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
// }

rootProject.name = "OJ Music Player"
include(":app")
include(":core")
include(":lyrics:kugou")
include(":lyrics:lrclib")
include(":lyrics:simpmusic")
include(":lyrics:paxsenix")
include(":lyrics:betterlyrics")
include(":lyrics:unison")
include(":lyrics:youlyplus")
include(":lastfm")
include(":canvas")
include(":shazamkit")
include(":spotifycore")
include(":flaccore")
include(":moriextractor")
include(":morideobfuscator")

// Use a local copy of NewPipe Extractor by uncommenting the lines below.
// We assume, that ArchiveTune and NewPipe Extractor have the same parent directory.
// If this is not the case, please change the path in includeBuild().
//
// For this to work you also need to change the implementation in core/build.gradle.kts
// to one which does not specify a version.
// From:
//      implementation(libs.newpipe.extractor)
// To:
//      implementation("com.github.TeamNewPipe:NewPipeExtractor")
// includeBuild("../NewPipeExtractor") {
//    dependencySubstitution {
//        substitute(module("com.github.TeamNewPipe:NewPipeExtractor")).using(project(":extractor"))
//    }
// }
