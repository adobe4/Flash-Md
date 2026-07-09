# Vinakili

A 100% offline personal-finance + business-invoice Android app for Tanzanian
users. Currency is TZS throughout. Native **Kotlin + Jetpack Compose** — no
WebView, no server, no login. All data lives on the device.

The app has two sections, switchable from Settings:

- **Personal (Binafsi)** — accent amber
  - **Lists (Orodha)** — debts & to-buy items, totals, CSV/JSON export
  - **Spend (Matumizi)** — daily expenses with monthly/yearly charts
  - **Balance (Salio)** — money across accounts, colour-coded, donut split
  - **Goals (Malengo)** — money goals with quick-add + wishlist
  - **Stats (Takwimu)** — monthly overview + breakdown charts
- **Business (Biashara)** — accent blue
  - **Dashboard (Dashibodi)** — revenue, unpaid, overdue, recent invoices, top clients
  - **Clients (Wateja)** — profiles, lifetime totals, per-client invoice history
  - **Invoices (Ankara)** — full editor, line items, discount/VAT, payments, **PDF export**
  - **Products (Bidhaa)** — catalogue used to build invoice line items
  - **Expenses (Gharama)** — categorised, filtered, charted
  - **Reports (Ripoti)** — profit & loss, 12-month trend, status split, overdue list

## Features

- **Bilingual**: English / Kiswahili, switchable live from Settings (first-launch picker)
- **Dark & light themes** (dark by default)
- **Deliberate palette**: amber + blue accents — no green, no purple anywhere
- **Liquid-glass design**: floating morphing dock, glass balance pill in the
  header (tap it to jump to Balance), soft glows and spring-fluid animations
- **Native invoice PDF** generated with Android's `PdfDocument`
- **One backup for everything**: a single JSON export in Settings, shared through
  the native share sheet (Drive, WhatsApp, Telegram…); importing on another
  device puts every record back where it belongs, statistics intact
- Soft-deletes everywhere; haptics reserved for long-press only

## Getting the APK

Every push builds a signed APK in GitHub Actions
(`.github/workflows/android.yml`). Grab it from:

- the **Vinakili-APK** artifact on the workflow run, or
- the **latest** GitHub release (`Vinakili-release.apk`).

On your phone, enable "Install unknown apps" for your browser/file manager,
then open the APK to install.

## Building locally

Requires the Android SDK (API 35) and JDK 17.

```bash
./gradlew assembleRelease   # -> app/build/outputs/apk/release/app-release.apk
```

The repo ships a self-signed release keystore (`app/keystore/vinakili.keystore`)
so CI can produce an installable, signed APK out of the box.
