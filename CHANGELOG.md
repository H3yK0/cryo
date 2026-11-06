# Changelog

**English** · [Português](docs/CHANGELOG.pt-BR.md)

## 1.4 — 10 languages, currencies and a new look

- **10 languages**: Brazilian Portuguese, English, Spanish, French, German, Italian, Japanese, Korean, Chinese (simplified) and Hindi. Cryo follows your phone's language (English if it is not one of the 10), and you can switch right away in Settings › Language and currency
- **App currency**: real, dollar, euro, pound, yen, won, yuan, rupee and more (20 in total), written the way each country writes money. The default comes from your phone's country. Switching the currency does not convert amounts: it only changes the symbol (Cryo has no internet and no exchange rates)
- **Phrase and voice entry in every language**: "lunch 25 on debit", "almuerzo 12,50 con tarjeta de débito", "コーヒー500円", "점심 12000원 카드로", "午饭50元", "चाय 20 रुपये नकद". It understands amounts like 1,234.56, 1万5千円 and 3만원, dates in each country's format and installments ("10 installments", "12回払い", "分12期")
- **Import statements from other countries**: column names in the 10 languages, month-before-day dates (US) detected from the file itself, amounts in each country's format, files in Shift_JIS, EUC-KR/CP949 and GB18030, PDFs in other layouts and card bill payments recognized in other languages too
- **New, tidier look** with Cryo's colors: a colored header on Home, clean sections and shortcuts. There are now 4 tabs: Home, Activity, Plan and Insights; the Wallet (accounts, cards, investments and debts) opens from Home
- **Restore a backup on first launch**: people switching phones start with their data right away
- **How to use Cryo**: a 5-page animated tour shows, on first launch, how to log by phrase, the monthly forecast, cards and installments, planning and privacy, in your language and currency. You can see it again in Settings › About
- **Smooth animations**: screens slide when opening and going back, tabs switch with a fade, the balance counts up to the new value, bars and charts grow as they appear and Activity animates what comes in and goes out. With Android's "Remove animations" on, everything shows up immediately
- Default categories are created in the app's language, with each country's keywords; existing users' categories do not change
- The store page (F-Droid) is also in the 10 languages, with screenshots in each of them
- **GitHub page in the 10 languages**, with English as the main one
- Sample data in test mode is scaled to each currency (a salary in yen or won looks real)
- **License**: starting with this version, Cryo is distributed under the GNU AGPL v3 or later (up to 1.3, GNU GPL v3 or later). Still free software, no ads and no internet

## 1.3 — statement import and developer mode

- **Import bank statements** (Settings › Your data, or the ↥ button on an account or card screen): OFX, CSV, XLSX spreadsheets (and the ".xls" many banks export) and PDF, read on the phone itself
- Before importing, you review the list: a suggested category on each line (from keywords and from what you have logged before), and you can change the description or the category, or leave a line out
- What is already in Cryo stays unchecked: transactions imported before and those that look like something already there (same amount, up to 3 days apart), including the card bill payment that already came in through the card statement
- Card bills: choose which bill the purchases go into (or go by each purchase's date); if the bank exports purchases as positive amounts, Cryo flips the signs itself and tells you
- A card bill payment in the account statement becomes a card payment (the spending is not counted twice); refunds become a credit on the bill
- CSV and spreadsheets: Cryo finds the columns by itself, even with the bank's name above the table; when it is not sure, it asks. Bills with the installment in its own column or with purchases in dollars (like C6's) work too. Password-protected PDFs also work (the password is not stored)
- After importing: comparison with the balance reported by the bank and a button to undo everything
- **Developer mode** (tap the version 7 times in Settings › About): a test mode with a separate database and sample data, sample statements in every format, a test JSON and a reading report with an anonymous version to ask for help without showing your data. Nothing from test mode reaches your real data
- Reminders and automatic backup keep taking care of the real data even in test mode
- Fix: the fingerprint or password lock now also appears when Android closes the app in the background and the screen comes back by itself
- After unlocking, the app goes back to exactly the screen you were on (it used to go back to Home)
- With the lock on, nothing of the app shows behind the lock screen (not even a window that was open), and Cryo's thumbnail in Recents does not show your data (Android 13 or newer)

## 1.2 — automatic backup

- **Automatic backup** (Settings › Automatic backup): Cryo saves your data by itself, every day, week or month
- To a folder on the phone: always replace the same file or keep several copies (5, 10, 30 or all); only the oldest automatic copies are deleted, other files in the folder are never touched
- To the cloud: a file on Google Drive, OneDrive, Nextcloud or another cloud installed on the phone, updated with each backup. The cloud app is the one that uploads the file: Cryo still has no internet permission
- Folder and cloud can be used together, and the "Back up now" button saves right away
- If nothing changed since the last backup, no identical copy is written
- Safeguards: automatic backup only replaces or deletes files this phone wrote; when you pick a place that already has another backup, Cryo shows both and asks (and you can restore from there, handy when switching phones); if another phone changes the file, or if many transactions suddenly disappear, the backup pauses and warns you
- Notification when a backup cannot be saved
- In Settings, "Make a backup" is now called "Save a copy now"

## 1.1 — debts and archive

- New **Debts** area: loans, financing, agreements, credit card/overdraft and money owed to people
- Shows how much is left, paid and late installments, how much of your income goes to installments and when you will be debt-free
- Paying an installment with interest or a fee, paying ahead, settling with a discount and correcting the balance
- "I borrowed" goes into the account without counting as income
- Installments show up in Plan › Bills, in the monthly forecast, in Home alerts and in reminders
- Phrase entry understands "paid the motorcycle installment" and "borrowed 300 from John"
- Net worth now subtracts debts
- New archive for goals (Plan › Goals › Archived goals) and for accounts, cards and investments (Wallet › Archived), with an option to restore
- Backups from version 1.0 keep working
- Reproducible build: the APK on GitHub and the one on F-Droid are identical and signed with the same key

## 1.0 — first public version

- Phrase and voice entry ("lunch 12.50 on debit", "tv 1200 in 10 installments on credit card")
- Overview with balance, net worth and the month's summary
- End-of-month forecast and a daily spending limit
- Spending heatmap and money flow
- Credit cards with bills, limit and installment purchases
- Recurring bills with due-date reminders
- Budget per category with warnings at 80% and 100%
- Goals and savings pots, investments and transfers between accounts
- Lock with fingerprint, face or password; mode to hide amounts
- Backup to a file and export to a spreadsheet (CSV)
- Material You, light and dark themes
