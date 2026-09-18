# ScotBank

A simple banking web application built in Java, developed as part of the CS217 coursework. It demonstrates user authentication, KYC (Know Your Customer) verification, and account management for investment holdings such as equities and ETFs, with performance metrics.

## Features

- User login and logout with session-based authentication
- KYC verification flow for new users
- Account management for investors, including equity and ETF holdings
- Server-rendered pages using Handlebars templates

##  Stack

- **Java** — core application language
- **[Jooby](https://jooby.io)** — lightweight web framework
- **Handlebars** — server-side templating engine
- **Maven** — build and dependency management

## Project Structure
 
<details>
<summary>Click to expand</summary>
<pre>
src/main/java/
├── io.jooby.helper/
│   └── UniRestExtension        # Jooby extension wrapping the UniRest HTTP client
└── uk.co.asepstrath.bank/
    ├── controllers/             # Route controllers
    │   ├── AccountController
    │   ├── KYCController
    │   ├── LoginController
    │   └── LogoutController
    ├── models/                  # Domain models
    │   ├── Equity
    │   ├── ETF
    │   ├── Investor
    │   ├── PricePoint
    │   └── Transaction
    ├── repositories/
    │   └── AccountRepository
    ├── services/
    │   ├── AccountService
    │   ├── ApiService
    │   ├── CapitalGainsService
    │   ├── CategorisationService
    │   ├── DataSyncService
    │   ├── KYCService
    │   └── TransactionProcessor
    ├── scotbank/                 # Account type implementations
    │   ├── CashAccount
    │   └── InvestmentAccount
    ├── Account
    ├── App                       # Jooby application entry point
    ├── AppLifecycleManager
    ├── Constants
    └── DatabaseInitialiser
src/main/resources/
├── assets/                       # Static assets
│   ├── styles.css
│   ├── portfolio.css
│   ├── Scotbank_Logo.png
│   └── Scot_Favicon.png
└── views/                        # Handlebars templates
    ├── login.hbs
    ├── create.hbs
    ├── forgot.hbs
    ├── kyc.hbs
    ├── account.hbs
    ├── portfolio.hbs
    ├── deposit.hbs
    └── withdraw.hbs
</pre>
 
</details>

## Getting Started

### Prerequisites

- JDK 17 or later
- Maven (or the bundled wrapper, `./mvnw`)

### Build

```bash
./mvnw clean install
```

### Run

Run the application's main class directly from IntelliJ, or via Maven:

```bash
./mvnw compile exec:java
```

By default the app starts on `http://localhost:8080`.
