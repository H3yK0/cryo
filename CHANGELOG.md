# Changelog

**English** · [Português](docs/CHANGELOG.pt-BR.md)

## 1.5 — voice on your phone, a financial engine, smart alerts and bills that match your statement

Everything built since 1.4, in one release. All of it still runs on your phone, with no internet.

**New look**
- **New icon**: the square C, now golden, with thickness and a thread of light
- **Depth across the app**: soft cards on a grouped background, grouped lists, a main button with volume and a light vibration on touch
- Cryo's own illustrations instead of keyboard emoji; large text (200%) checked on the main screens

**Log faster**
- **Voice understood on your phone** (Whisper, running locally; the audio never becomes a file): a live "echo" screen shows the words as you speak. It understands spoken numbers, corrections ("32, no, 35"), your own nicknames and several entries in one sentence. Phones that cannot run it use the phone's own offline recogniser
- **The voice button sits in the middle of the bottom bar**, on every tab: tap to speak, **hold to type**

**Understand your money**
- **Financial engine**: finds what repeats by itself (salary, rent, subscriptions, a new price, a late payment), shows the next 3, 6 and 12 months, flags out-of-pattern and repeated charges, gives a financial health score (0–100, in six parts), the interest hidden in your debts with a plan to pay them off, the real return of your investments and when each goal gets there
- **A month forecast whose range keeps its promise**: several simple methods, weighed by how well each one would have predicted your own past months. "Between X and Y, in 8 of 10 scenarios" is checked against your past before it is shown, and the forecast says what weighed most
- **Budgets**: how each category should close the month and the chance of going over its limit, a monthly overview, and **moving budget between categories for this month only**
- **Investments, "if it keeps going like before"**: where they could be in 1, 3 and 5 years with your usual contributions

**Alerts that help and never nag**
- **Smart alerts, one at a time**: a bill due tomorrow (with **Paid** and **Remind me tomorrow** right in the notification), a budget close to its limit, a card statement closing, payday, weekly and monthly summaries. At most one regular alert a day, at two times only
- **Daily reminder** at the time you choose, only if you logged nothing that day
- **Animated notifications** with a sticker drawn for each situation, and **shopping mode, live** (Android 16 Live Updates and the Now Bar on One UI 8)
- **Celebration moments**: goal reached, budget at 80% or over (with a tip on where to cover it from), logging streaks, how last month closed
- **Cryo's own sounds**, soft and muffled with a thread of ice crystal on top. They follow your phone's silent mode and can be turned off

**Bills, cards and your bank statement**
- **Bills match your bank statement**: when you import a statement, Cryo recognises the line that pays a bill ("NETFLIX.COM 59.99" is your Netflix bill), links it instead of creating a second entry, and the bill shows as paid. The forecast no longer counts that bill twice
- **Tidy up what was already imported**: Plan › Bills shows "payments found" and "paid twice" suggestions, one tap each, always with undo. "Paid" looks for the payment in your statement before creating a new one, and undoing never deletes an imported entry
- **Create the bill later**: add a bill and Cryo asks whether entries you already have are that bill. It also learns your bank's nickname for it ("DEB AUT CIA ENERG" = Electricity), keeping only a hash
- **Credit card refunds, by hand** (requested by a user): a **Refund** button on the card screen, or choose the card for an income entry. A refund lowers the statement, and editing one keeps it on the card
- **Card statement totals that match your bank** (requested by a user): editing a card purchase no longer moves it to another statement. You can **choose the statement** a purchase, refund or payment belongs to, and **Adjust total** records the difference to what your bank shows as a visible, deletable adjustment

**Yours**
- **Your profile**: the circle on Home is a button that opens a profile panel (it replaces the gear). Use your initial, a Cryo drawing, an emoji or **your own photo** (picked with Android's photo picker, no new permission; it stays on this phone only)
- **"Learn Cryo"**: a local guide that notices what you have not used yet and shows one tip at a time, at most one a day and three a week, with **Show me** (it opens the right screen and highlights the spot) and **Not now**. A list with all 18 lessons shows what you already use. Plain rules running on your phone, no AI; the guide only stores counts and dates

**Protected**
- **Password-protected backups** (optional, AES-256): the password is asked for when restoring and is never stored. Reminders show no name or amount on the lock screen
- **A firmer lock**: every fresh start asks to unlock, and turning the lock on confirms it once. On Android 8 to 12, the app is hidden from screenshots and recent apps while the lock is on
- **Fixes from a full code review**: a protected backup is never saved without protection, damaged or oversized backup files are refused with a clear message, and the spreadsheet export no longer runs formulas typed into descriptions
- Every bug fixed so far now has a test that fails if it comes back (about 940 automated tests)

- **Bills are not all monthly** (requested by a user): choose the rhythm — every month, every 2, 3 or 6 months, or once a year (vehicle tax, insurance, yearly subscriptions) — set the first due date, split each cycle into instalments, type the one-off price to see how much the instalments add in interest, and give the bill an end date. Templates in your language fill in the name, rhythm and day
- **Pick any card statement**: the chips show the nearby ones and "Other…" opens the whole list of the card's statements, with each total, so old purchases land where your bank put them
- **The forecast answers in one line** whether your balance goes below zero, from when, and what the lowest expected balance is
- **Support**: Settings › About links to Buy Me a Coffee. The app still has no internet permission — your browser opens the page
- The undo bar no longer covers the "+" button, and floating buttons now have a name for screen readers
- Database version 5: bills gained rhythm, instalments, one-off price and end date. Existing bills stay monthly, exactly as they were

- **Balance that matches the bank, by itself**: a statement only lists what moved in the period; what was already in the account is its opening balance. The PDF reader now reads the opening and closing balances, the import sets the account's opening balance on its own (no entry is created) and the final screen shows the steps the way the bank does — balance before, money in, money out, balance at the end — next to Cryo's balance on the same day, with undo. It works with OFX too (the opening balance is worked out from the file's own balance), and reopening a statement that is already in Cryo fixes the balance without importing anything again. When some difference is left, one tap on "Match the bank balance" settles it.
- **Delete a whole import**: "Imports you made" lists each import with what it brought, and removes it in one go, with undo. Deleting removes only what came from that file
- **Refund a transaction**: one tap creates the opposite entry, with the original kept in your history
- **Statement reading fixed in three places** (found with a real bank statement): a company tax number next to the amount no longer becomes part of it, the page header no longer moves every later entry to the first day of the period, and a dash inside a description no longer flips an expense into income. With these, a real statement now matches the bank to the cent
- Settings › Alerts now updates as soon as you change something, instead of after leaving the screen

**Good to know**
- New permissions: **microphone** (asked only on your first tap on the voice button) and **promoted notifications** (Android 16 live updates; it asks you nothing). Still no internet permission
- The app grows from about 6 MB to about 65 MB, because the speech model ships inside it
- Database version 4 (monthly budget moves, no-spend days, goal illustrations); old backups still open

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
