# Changelog

**English** · [Português](docs/CHANGELOG.pt-BR.md)

## 1.8 — a new forecast engine, checked against your own past

- **New forecast engine, built from scratch** (still all on your phone, no AI model): seven simple methods each look at your past in a different way, and Cryo weighs them by how well each one would have predicted your own past months. In tests with invented histories, the month forecast makes ~7% smaller errors than 1.7, and up to 57% smaller for people who do one big monthly shop
- **The range keeps its promise**: "between X and Y, in 8 of 10 scenarios" is now checked against your past months before it is shown (in tests it held 80% of the time, up from 68%), and the card says how it went: "checked against your past: right in 10 of 12 months"
- **The forecast explains itself**: what weighed most in the calculation, and whether this time of year usually costs you more (only when that repeated in the two previous years)
- **Budgets**: each category shows how it should close the month and the chance of going over its limit; the "at this pace" warnings use the new forecast
- **Investments, "if it keeps going like before"**: the return of the investment itself, its ups and downs, the biggest drop, where it could be in 1, 3 and 5 years with your usual contributions, and the chance of ending above what you put in. It is based on the past, never a promise
- **Security and reliability fixes** from a full code review: a password-protected backup is never saved without protection; damaged or oversized backup files are refused with a clear message; spreadsheet export no longer runs formulas typed in descriptions; a fingerprint sensor error only unlocks phones that have no screen lock; on Android 8 to 12, screenshots of Cryo are blocked while the lock is on; huge amounts no longer wrap around into other numbers; tapping "Paid" twice records one payment; deleting a category also removes its monthly budget moves; each notification opens its own screen
- No new permissions and no database change

## 1.7 — smart alerts, sounds and the voice button within thumb reach

- **The voice button now sits in the middle of the bottom bar**, on every tab, where your thumb reaches (like Samsung's One UI): tap to speak, **hold to type**. The phrase field on Home is now for typing only
- **Smart alerts, one at a time**: a bill due tomorrow (with **Paid** and **Remind me tomorrow** right in the notification), a budget close to or over its limit, a category on pace to go over, a card statement closing tomorrow, payday ("Log salary" with the usual amount), a late salary, a subscription that got more expensive, a duplicate charge, unusual spending, a balance about to go negative, a goal almost reached, a last installment, a better financial health score, your week on Sunday and how the month closed
- **Never annoying**: at most one regular alert a day, only at two times (morning and your reminder time), nothing for new users besides bills; types you keep dismissing come less often. Everything is calculated on your phone
- **Daily reminder at the time you choose** (Cryo asks once): only if you logged nothing that day, with **No-spend day** and **Log** buttons
- **Animated notifications**: each alert comes as a message from Cryo with an animated sticker drawn for the situation (Android plays animations only in this conversation style). No keyboard emoji anywhere: all images are Cryo's own
- **Shopping mode, live**: "Going shopping, up to $300" shows a live notification with how much is gone and how much is left (Android 16 Live Updates and the Now Bar on One UI 8)
- **Celebration moments**: goal reached (full screen, with ice crystals), budget at 80% or over (the bar crosses the line and the card shakes, with a tip on where to cover it from), logging streaks (3, 7, 14, 30... days) and how last month closed
- **Monthly spending overview** with every budgeted category, plus categories without a budget; **move budget between categories for this month only**, with undo
- **Cryo's own sounds** ("ice crystal" sounds made from scratch): saving, money in, warnings, achievements, voice start and end. They always follow your phone's silent mode and can be turned off
- **Settings › Alerts and sounds**: Friendly or Discreet tone, the reminder time, each alert type with **See example**, and sounds
- **Voice**: the "go ahead" sound is no longer recorded together with what you say; developer mode shows the timing and has an experimental faster mode to test
- Large text (200%) checked on the main screens
- Database version 4 (monthly budget moves, no-spend days, goal illustrations); old backups still open, and goals get the illustration matching their old emoji
- New permission: **promoted notifications** (Android 16 live updates; it asks you nothing)

## 1.6 — voice, understood on your phone

- **Voice entry inside Cryo, with Whisper** (an open speech-recognition model, built into the app): what you say is understood on the phone itself, with no internet. The audio only lives in memory and is erased right away; Cryo still has no internet permission
- **Words appear while you speak**, with an echo animation: the microphone grows with your voice, each syllable sends out a ring, and what Cryo understood (amount, category, card, date) pops up as little cards. It stops by itself when you stop talking
- **Knows your names**: your accounts, cards, categories and goals help it hear right ("Roxo", "Pet Shop")
- **Understands the way people talk** (Portuguese first): "dezoito e noventa" or "18 e 90" = 18.90, "trinta e dois e cinquenta", "cento e vinte", "dois mil e quinhentos", "conto", nicknames like "roxinho" for your "Roxo" card
- **Several entries at once**: "lunch 30 and uber 15" becomes two entries to check; the card or date said once counts for all
- **Spoken corrections**: "32, no, 35" keeps 35
- Choose the recognizer in Settings › Quick entry › Speech recognition: Whisper (default) or your phone's own (on-device). Phones that can't run Whisper use the phone's recognizer
- The app is bigger (about 65 MB) because the voice model comes inside it
- New permission: **microphone**, asked only the first time you tap the mic, with an explanation

## 1.5 — financial engine, protected backups and a new look

- **Financial engine on your phone** (all offline): the end-of-month forecast now shows a range and the chance of ending in the red, from 2,000 scenarios simulated with your own history, and makes ~38% smaller errors than before when tested against past months
- **Recurring payments found automatically**: salary, rent, subscriptions; Cryo warns when one gets more expensive or when your salary is late, and turns them into bills with one tap
- **Coming months**: your balance day by day for 3, 6 or 12 months, with card bills, installments, bills and debts, the tightest day and the chance of going negative
- **Financial health score** (0 to 100) in 6 explained parts, with what to do to improve; **out of the ordinary** spending and **repeated charges**; category pace ("at this pace, Food goes over the limit around day 24") and smarter budget suggestions
- **Debts**: the interest built into each debt, a payoff plan (highest interest first or smallest debt first) with how much you save, and the fair price to pay installments early
- **Investments**: real yearly return counting each deposit on its date, optionally compared with inflation; **goals** show when you'll get there
- **Learned categories**: Cryo learns how you categorize and helps when importing statements and logging by phrase
- **Password-protected backups** (optional, AES-256, key kept in Android's secure keystore); bill reminders show no names or amounts on the lock screen
- 500 extra guarantee tests (stability, scalability, performance, resources and security)
- **New icon**: the square C is now gold, with thickness and a touch of light, on the same Cryo blue. The little star is gone. Chosen by comparing 11 designs with research on visual attention
- **Depth across the app**: content sits on cards with soft shadows over a "grouped" background, like the best iPhone apps; the Home header uses the icon's gradient; shortcuts and the main button have volume and shrink with a spring when touched
- **Grouped lists**: each day in Activity, a card's bill and the account history form a single card; Settings is grouped like the iPhone's
- **Haptic feedback**: confirming, switching on and off, changing tabs and segments. It follows your phone's "touch vibration" setting and needs no new permission
- Cryo's icon shows up inside the app (welcome, lock screen and About)

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
