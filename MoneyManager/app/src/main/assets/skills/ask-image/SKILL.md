---
name: ask-image
description: Analyzes a receipt or payment screenshot image to extract transaction details with per-field confidence levels. Never hallucinate missing data.
---

# Receipt Image Analysis Skill

## Purpose
Extract structured transaction data from a receipt image. Tag each field with a confidence level so the app can flag uncertain entries for user review.

## Output Format
Return ONLY a valid JSON object matching this schema:
```json
{
  "typeId": "expense|income|transfer",
  "amount": 0.00,
  "categoryName": "string",
  "accountName": "string",
  "peerContactName": "string or null",
  "description": "string or null",
  "date": "YYYY-MM-DD or null",
  "confidence": {
    "amount": "high|medium|low",
    "typeId": "high|medium|low",
    "date": "high|medium|low",
    "merchant": "high|medium|low"
  },
  "needs_review": false,
  "flags": []
}
```

## Field Extraction Rules

### amount
- Use TOTAL or GRAND TOTAL — never a subtotal or single line item.
- Include decimal places (e.g. 150.50, not 150).
- If multiple totals appear (tax inclusive vs exclusive), use the larger (final) amount.

### typeId
- Most receipts → `expense`.
- Cashback, salary credit, refund receipts → `income`.
- Wallet top-up or bank-to-bank → `transfer`.

### date
- Look for date in receipt header or footer.
- Return as `YYYY-MM-DD`. Return `null` if not readable.

### description
- Short merchant name or purpose, max 40 characters.
- Return `null` if genuinely unclear.

## Confidence Levels
- `high`: clearly printed, unambiguous value
- `medium`: readable but could be misread (e.g. faded ink, small font)
- `low`: blurry, partially cut off, or absent

Set `needs_review: true` if ANY of `amount`, `typeId`, or `date` is `low` confidence.

## No-Hallucination Rule
**NEVER invent or guess values.** If a field is unclear:
1. Set the field to `null`
2. Mark its confidence as `"low"`
3. Add a descriptive flag to the `flags` array

### Common flags
- `"blurry_image"` — image is out of focus
- `"partial_receipt"` — receipt is cut off
- `"amount_unclear"` — total not readable
- `"date_missing"` — no date on receipt
- `"multiple_items"` — receipt has multiple line items (only extract the total)
- `"foreign_currency"` — non-INR amounts detected

## Tool Calling Order
1. `getMerchantCategory(merchant)` — check past category for this merchant first
2. `searchCategories(keyword)` — if no merchant history found
3. `getDefaultAccount()` — unless an account/card number is printed on the receipt
4. `searchPeers(name)` — only if a named person appears (e.g. "Paid to: Rahul")
