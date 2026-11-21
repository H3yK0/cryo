<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="96" alt="Cryo icon">
</p>

<h1 align="center">Cryo</h1>

<p align="center"><b>Your money, crystal clear.</b><br>
A personal finance app for Android: simple to use, complete, offline and open source.</p>

<p align="center">
  <b>English</b> ·
  <a href="docs/readme/README.pt-BR.md">Português</a> ·
  <a href="docs/readme/README.es.md">Español</a> ·
  <a href="docs/readme/README.fr.md">Français</a> ·
  <a href="docs/readme/README.de.md">Deutsch</a> ·
  <a href="docs/readme/README.it.md">Italiano</a> ·
  <a href="docs/readme/README.ja.md">日本語</a> ·
  <a href="docs/readme/README.ko.md">한국어</a> ·
  <a href="docs/readme/README.zh-CN.md">中文</a> ·
  <a href="docs/readme/README.hi.md">हिन्दी</a> ·
  <a href="docs/readme/README.ru.md">Русский</a> ·
  <a href="docs/readme/README.uk.md">Українська</a> ·
  <a href="docs/readme/README.ar.md">العربية</a> ·
  <a href="docs/readme/README.he.md">עברית</a>
</p>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="19%" alt="Home">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="19%" alt="Quick entry">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="19%" alt="End-of-month forecast">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="19%" alt="Budgets">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="19%" alt="Dark mode">
</p>

## What it does

- **Log by typing or speaking.** Type or say "lunch 12.50 on debit" or "tv 500 in 10 installments on credit card". Cryo understands the amount, the category, the account, the date and the installments. You just check and save.
- **A financial engine on your phone.** The end-of-month forecast comes with a range checked against your own past, and its number is always the sum of the lines that explain it — including your bills, your budgets and what you invested. Cryo finds your salary, rent and subscriptions on its own, answers **"How will I end the year?"** with red signals on the days that need attention, tells you whether your **goals fit** at your current pace, flags unusual and repeated charges, gives a financial health score (right next to your balance), shows the interest built into your debts with a payoff plan, and the real yearly return of your investments. All calculated offline.
- **Spending heatmap and money flow.** See which days you spend the most, where the money came from and where it went.
- **Import bank statements.** OFX, CSV, spreadsheet (XLSX) or PDF: you review the list before anything goes in, with suggested categories, what is already in Cryo unchecked and credit card bill payments recognized.
- **Credit cards.** Bills, available limit and installment purchases, each installment on the right bill.
- **Debts.** Loans, financing, agreements and money you owe someone, kept apart from the rest: how much is left, late installments, how much of your income goes to installments and when you will be debt-free.
- **Planning.** Recurring bills (monthly, yearly or in instalments) with reminders, upcoming bills in the Statement and skipping a month, a budget per category — or **planned from your income** — that warns you when your bills do not fit, and savings goals.
- **A statement per account**, like your bank's: each entry with the balance after it and the balance at the end of each day. In the main Statement, each entry shows its account, and you can filter by one.
- **Investments and transfers** between accounts.
- **14 languages and your currency.** English, Portuguese, Spanish, French, German, Italian, Japanese, Korean, Chinese, Hindi, Russian, Ukrainian, Arabic and Hebrew (right to left), in the currency you choose (dollar, euro, pound, yen, won, rupee, real, rouble, hryvnia, shekel, riyal and more). Logging by phrase and importing statements work in all of them.
- **A quick animated tour** shows how to use the app the first time you open it (and again whenever you want, in Settings).
- **Material You**, light and dark themes and smooth animations that follow your phone's "Remove animations" setting.

## Privacy

- Your data stays on your phone. **The app has no internet permission.**
- No sign-up, no ads, no tracking.
- Lock with fingerprint, face or your phone's screen lock.
- **Automatic backup** to a folder on your phone or to your cloud (Google Drive, Nextcloud and others), daily, weekly or monthly. Cryo writes the file through Android's file picker, and the cloud app is the one that uploads it.
- **Password-protected backups** (optional): the files are encrypted with AES-256 and can't be read without your password. The key stays in Android's secure keystore.
- Backup to a `.json` file and export to a `.csv` spreadsheet, whenever you want.

**Voice that stays on your phone.** Voice is understood on the phone itself by Whisper (an open speech model); the audio never becomes a file. The model is downloaded once with your browser — Cryo has no internet permission — and Cryo only accepts the official file, checking its SHA-256 ([the models](docs/VOZ-MODELOS.md)). Without it, your phone's own offline recogniser is used. Typing the phrase always works.

## Install

- **F-Droid:** [f-droid.org/packages/io.github.h3yk0.cryo](https://f-droid.org/packages/io.github.h3yk0.cryo/)
- **GitHub:** download `Cryo-<version>.apk` from the [Releases](https://github.com/H3yK0/cryo/releases) page.

Both are the same file, signed with the same key: F-Droid builds the code again and only publishes it if the result is identical to the APK here (reproducible build). So you can update from either one without uninstalling.

## Build from source

Requirements: JDK 21 and the Android SDK (platform 36). The published APK is built with JDK 21, the same one F-Droid uses.

```bash
./gradlew assembleRelease     # unsigned APK in app/build/outputs/apk/release/
./gradlew testDebugUnitTest   # automated tests (rules, database, backup, import and screens)
```

To sign with your own key, create `keystore/keystore.properties` with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. The `keystore/` folder never goes into the repository.

To regenerate the screenshots of the F-Droid page and of these READMEs (`fastlane/` folder), in the 14 languages:

```bash
./gradlew testDebugUnitTest --tests '*StoreScreenshotsTest' --tests '*StoreAssetsTest' -PstoreAssets=true
```

### How the code is organized

| Folder | Contents |
|---|---|
| `domain/` | Rules without Android: balances, bills and installments, debts, forecast, money flow, the phrase interpreter (`PhraseParser.kt`, `PhraseLang.kt`), languages and currencies |
| `importer/` | Statement reading (OFX, CSV, XLSX, PDF and the test JSON) and the review before importing |
| `data/` | Database (Room), settings, backup, default categories and sample data |
| `ui/` | Screens in Jetpack Compose with Material 3 |
| `notify/` | Reminders for recurring bills and debt installments (WorkManager) |

The project documents (requirements in [docs/PRD.md](docs/PRD.md), visual guide, statement import) are written in Portuguese, the author's language.

## Contributing

Suggestions and bug reports are welcome in [Issues](https://github.com/H3yK0/cryo/issues), in any of the 14 languages.

- **Translations:** the 13 translations were made with the help of AI. If you speak one of these languages and find something odd, please open an issue: native review is very welcome.
- **Statements from your bank:** the app has a **developer mode** (tap the version 7 times in Settings › About) with a test mode that uses a separate database and sample data, sample statements in every format and a reading report that can be shared in an anonymous version. If your bank's statement is not read correctly, you can help without sending your data: see [docs/IMPORTACAO.md](docs/IMPORTACAO.md#ajudar-a-melhorar-a-leitura) (in Portuguese).

## Support

Cryo is free and has no ads, no tracking and no internet access. If it helps you and you feel like buying me a coffee, it is very welcome — and entirely optional.

<a href="https://www.buymeacoffee.com/H3YK0" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me a Coffee" height="48"></a>

## License

Copyright © 2026 Hanry Franco

This program is free software: you can redistribute it and/or modify it under the terms of the [GNU Affero General Public License](LICENSE) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Up to version 1.3, Cryo was distributed under the GNU General Public License v3 or later. It is distributed in the hope that it will be useful, but **without any warranty**.
