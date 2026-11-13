// AGP 9 компилирует Kotlin сам (встроенный Kotlin), поэтому плагин kotlin-android не подключаем.
// Плагин Compose задаёт версию Kotlin для всего проекта.
plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
