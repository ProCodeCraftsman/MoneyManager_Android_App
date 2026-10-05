# SMS Pattern Catalog

Per-sender coverage of the built-in rules (`domain/sms/BuiltInSmsRules.kt`), measured on the transaction-like templates of one real inbox (scrubbed corpus: `app/src/test/resources/sms/txn_templates.json`). Counts are messages in that inbox, not a global frequency. Regenerate the table after adding rules (see `SMS-ANALYZER.md`).

**Corpus result:** 226 templates / 1317 messages; every template is either matched by a rule or classified as a non-transaction (OTP, promo, reminder, failed, info) (100% of messages). Enforced by `SmsCorpusTest` (threshold 90%).

> **Caveat:** the rules were written against this same corpus, so 100% shows there is no regression, not that unseen messages will parse. Real-world coverage is measured by re-running the analyzer on a phone after install.

## Coverage by sender

| Sender | Messages | Templates | Rules |
|---|---:|---:|---|
| `FEDSCP` | 416 | 12 | `fedscp.declined`, `fedscp.fuel-waiver`, `fedscp.card-payment`, `fedscp.refund`, `fedscp.refund-alt`, `fedscp.card-spend` |
| `HDFCBK` | 157 | 18 | `hdfc.upi-sent`, `hdfc.card-spent`, `hdfc.credit-alert`, `hdfc.debited`, `hdfc.deposited`, `hdfc.card-payment`, `hdfc.card-payment-online` |
| `UTKSPR` | 144 | 4 | `utkarsh.card-debit`, `utkarsh.card-payment` |
| `ICICIT` | 97 | 36 | `icici.card-spent-using`, `icici.card-spent-at`, `icici.card-upi`, `icici.card-payment` |
| `SBIUPI` | 94 | 19 | `info.upi-mandate-created`, `sbi.upi-credit` |
| `AXISBK` | 69 | 30 | `info.card-credit-balance`, `info.card-cashback-earned`, `axis.card-spent-a`, `axis.card-spent-b`, `axis.card-payment`, `axis.cashback-credited` |
| `CBSSBI` | 67 | 4 | `sbi.cbs-debit`, `sbi.cbs-credit`, `sbi.cbs-td-closure` |
| `SLCBNK` | 42 | 4 | `reminder.money-request`, `slice.card-spent`, `slice.repayment`, `slice.refund` |
| `SBIPSG` | 31 | 18 | `sbi.neft-credit`, `sbi.imps-credit` |
| `ABCAMC` | 29 | 1 | `mf.absl-sip` |
| `SBYONO` | 18 | 1 | `sbi.yono-transfer` |
| `PHONPE` | 13 | 10 | `reminder.money-request` |
| `EPFOHO` | 12 | 12 | `info.epf-contribution` |
| `IPRUMF` | 10 | 1 | `mf.iprumf-sip` |
| `TATAMF` | 10 | 1 | `mf.tata-sip` |
| `RZRPAY` | 10 | 1 | `info.settlement` |
| `SWIGGY` | 10 | 5 | `info.refund-initiated` |
| `IRSMSA` | 9 | 5 | `irctc.rwallet-debit`, `irctc.rwallet-credit` |
| `JIOPAY` | 9 | 6 | _generic / AI fallback_ |
| `IRCTCI` | 8 | 2 | _generic / AI fallback_ |
| `PTNNPS` | 7 | 1 | `nps.contribution` |
| `SBIMFM` | 7 | 2 | `mf.sbi-purchase` |
| `SBIMFD` | 5 | 1 | `mf.sbi-purchase` |
| `EDLAMC` | 5 | 2 | `mf.edelweiss-purchase` |
| `NPSCRA` | 3 | 1 | `nps.contribution` |
| `ICCSLM` | 3 | 2 | `rd.cooperative-society` |
| `POLBAZ` | 3 | 3 | `policybazaar.payment` |
| `SUPRMY` | 3 | 3 | `reminder.money-request` |
| `HDFCMF` | 2 | 1 | `mf.hdfc-purchase` |
| `AIRINF` | 2 | 1 | _generic / AI fallback_ |
| `DSPAMC` | 2 | 1 | `mf.dsp-purchase` |
| `SAMCCC` | 2 | 1 | `samsung.service` |
| `650022` | 2 | 1 | `airtel.prepaid-bill` |
| `MOAMCL` | 2 | 2 | `mf.motilal-subscription`, `mf.motilal-purchase` |
| `ICICIP` | 1 | 1 | _generic / AI fallback_ |
| `INDANE` | 1 | 1 | `info.lpg-subsidy-notice` |
| `IONEMG` | 1 | 1 | _generic / AI fallback_ |
| `MOBINR` | 1 | 1 | _generic / AI fallback_ |
| `FEDBNK` | 1 | 1 | `federal.upi-debit` |
| `SNITCH` | 1 | 1 | `snitch.purchase` |
| `JIOFBR` | 1 | 1 | `jiofiber.payment` |
| `SBIBNK` | 1 | 1 | `sbi.it-refund` |
| `MMTRIP` | 1 | 1 | `info.refund-initiated` |
| `KRD24X` | 1 | 1 | _generic / AI fallback_ |
| `VAAHAN` | 1 | 1 | `vahan.puc` |
| `FEDFIB` | 1 | 1 | `federal.fund-transfer` |
| `ITDCPC` | 1 | 1 | `itd.challan` |
| `FVNFNV` | 1 | 1 | _generic / AI fallback_ |

Senders marked _generic / AI fallback_ send non-transactions (promos, reminders, failures) that the generic classifier rejects, or one-off wording that goes to the AI path.

## Non-transaction rules

| Rule | What it stops |
|---|---|
| `fedscp.declined` | Declined card transaction (FAILED) |
| `info.refund-initiated` | Swiggy / MakeMyTrip "refund initiated" (not yet credited) |
| `info.settlement` | Razorpay merchant settlements |
| `info.epf-contribution` | EPFO passbook notices |
| `info.upi-mandate-created` | SBI UPI mandate created |
| `info.card-credit-balance`, `info.card-cashback-earned` | Axis informational notices |
| `info.lpg-subsidy-notice` | LPG subsidy income-tax notice |
| `reminder.money-request` | PhonePe / SuperMoney / slice "has requested money" |
| (generic) | AutoPay scheduled, statement generated, payment due, amount payable, "Ignore if paid" -> REMINDER; unsuccessful / failed / declined -> FAILED; OTP; loan / offer / voucher / upgrade wording -> PROMO |

## Natures

| Nature | Rules |
|---|---|
| `CARD_REPAYMENT` | `fedscp.card-payment`, `hdfc.card-payment`, `hdfc.card-payment-online`, `utkarsh.card-payment`, `icici.card-payment`, `axis.card-payment`, `slice.repayment` |
| `CASHBACK` | `fedscp.fuel-waiver`, `axis.cashback-credited` |
| `REFUND` | `fedscp.refund`, `fedscp.refund-alt`, `slice.refund` |
| `INVESTMENT` | all `mf.*`, `nps.contribution`, `rd.cooperative-society` |

## Known gaps

- One-off merchant wording that no rule covers (new shops, new insurers) goes to the generic parser, then the AI path.
- Merchant strings are bank-truncated (`KERALA VISI`, `LULU INTERNATIO`); the merchant-category memory keys on the truncated text.
- HDFC `Info:` strings for UPI debits are cleaned (`UPI-<ref>-` and VPA domains stripped) but may still carry noise.
- Formats were inferred from masked templates; real-message edge cases will appear on the first on-device run. Re-run the analyzer to see them.
