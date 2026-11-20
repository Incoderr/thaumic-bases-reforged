# Thaumic Bases Reforged v0.1.54-alpha · Minecraft 1.21.1

## Русский

Экспериментальный обратный перенос нашей версии 0.1.54-alpha+26.3 на 1.21.1. Запуск проверен на клиенте и выделенном сервере; пользователь подтвердил работу в отдельном профиле 1.21.1. Полная проверка всех механик ещё продолжается. Основа — порт **Thaumic Bases** для **Thaumcraft 4: Reforged**.

- Магические растения, декоративные блоки, шипы и пирожидкость.
- Браслеты с поддержкой набалдашников. Ихоровый браслет — неофициальное дополнение этого порта, доступное при установленном Thaumic Tinkerer.
- Револьвер с зарядкой от банки с Telum и 15 отдельными исследованиями для его улучшений.
- Коса Белоглазого, табак и трубки, костёр тауматурга и другие предметы оригинального аддона.
- Статьи, рецепты и связи исследований в Таумономиконе. Интерфейс и исследования доступны на русском и английском языках.

## English

Experimental 1.21.1 backport of our 0.1.54-alpha+26.3 release. Client and dedicated-server startup have been tested, and the user confirmed it works in a separate 1.21.1 profile. Full gameplay validation is still ongoing. Based on **Thaumic Bases** for **Thaumcraft 4: Reforged**.

- Magical plants, decorative blocks, spikes, and Pyrofluid.
- Bracelets with wand focus support. The Ichor Bracelet is an unofficial addition made for this port and is available when Thaumic Tinkerer is installed.
- A revolver fueled by a jar of Telum, with 15 separate research entries for its upgrades.
- The White-Eyed One’s Scythe, tobacco and pipes, the Thaumaturge’s Campfire, and more content from the original add-on.
- Thaumonomicon entries, recipes, and research prerequisites. The interface and research entries are available in English and Russian.

## Установка / Installation

Скачайте JAR из [релиза для 1.21.1](https://github.com/Justevian/thaumic-bases-reforged/releases/tag/v0.1.54-alpha%2B1.21.1) и поместите его в папку `mods` профиля 1.21.1.
Download the JAR from the [1.21.1 release](https://github.com/Justevian/thaumic-bases-reforged/releases/tag/v0.1.54-alpha%2B1.21.1) and place it in your 1.21.1 profile’s `mods` folder.

- Minecraft **1.21.1**
- NeoForge **21.1.256+** (также проверен запуск на / also launched on **21.1.267**)
- Thaumcraft 4: Reforged **1.1.3**
- TerraBlender **1.21.1-4.1.0.1**
- Curios **9.5.1+1.21.1** (зависимость ядра / core dependency)

Thaumic Tinkerer нужен только для ихорового браслета.  
Thaumic Tinkerer is only required for the Ichor Bracelet.

## Авторы / Credits

- **Modbder** — оригинальный Thaumic Bases / original Thaumic Bases ([исходный проект / original project](https://github.com/Modbder/ThaumicBases), [лицензия версии 1.7.10 / original 1.7.10 license](https://www.curseforge.com/minecraft/mc-mods/thaumic-bases/license))
- **Azanor** — оригинальный Thaumcraft / original Thaumcraft
- **overseas (notlikethiss)** — [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged)

## Сборка и проверка / Building and testing

Нужен JDK 21. Положите в `libs` следующие официальные файлы (не входят в архив исходников):
JDK 21 is required. Place these official dependencies in `libs` (not included in the source archive):

- `thaumcraft-4-reforged-1.1.3-1.21.1.jar` — [official core release](https://github.com/notlikethiss/thaumcraft-4-reforged/releases/tag/1.21.1)
- `TerraBlender-neoforge-1.21.1-4.1.0.1.jar` — [TerraBlender](https://modrinth.com/mod/terrablender/versions)
- `curios-neoforge-9.5.1+1.21.1.jar` — [Curios](https://modrinth.com/mod/curios/versions)

```shell
./gradlew build
./gradlew runServer -PportSmoke=true
./gradlew runClient -PclientSmoke=true
```

В Windows используйте `gradlew.bat`. Для тестового сервера сначала примите EULA в `run/eula.txt`.
On Windows use `gradlew.bat`. Accept the Minecraft EULA in `run/eula.txt` before running server checks.

Проверки запускаются в отдельном каталоге `run` и не входят в выпускаемый JAR. Сервер проверяет загрузку рецептов, данные браслетов и сохранение блочных сущностей; клиент — модели предметов и выбор моделей браслетов. Тестовые запуски автоматически завершаются. Для обычного запуска не передавайте флаги `portSmoke`/`clientSmoke`.

Checks use the isolated `run` directory and are excluded from the release JAR. Server checks cover recipe loading, bracelet data and block entity persistence; client checks cover item models and bracelet model selection. Test runs stop automatically. Omit the smoke flags for regular development runs.

Не проверены: полное прохождение исследований и всех механик, внешний вид браслетов на руке и совместимость с большой сборкой. Перенос миров с 26.3 на 1.21.1 не поддерживается.
Full research/gameplay progression, first-person bracelet appearance and large modpack compatibility still require manual testing. Downgrading 26.3 worlds is unsupported.
