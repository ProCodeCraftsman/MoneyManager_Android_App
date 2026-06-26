import csv
import re
from datetime import datetime

LEGACY_CSV = r"D:\Android projec\export_05_06_26_827.csv"
OUTPUT_CSV = r"D:\Android projec\importable_v2.csv"

# Account name mapping (legacy → new app)
ACCOUNT_MAP = {
    "Cash": "Cash",
    "Scapia card": "Scapia FB",
    "Scapia FB": "Scapia FB",
    "Axis Bank": "Flipkart Axis",
    "Flipkart Axis": "Flipkart Axis",
    "Super Money": "Super Money Rupay",
    "Super Money Rupay": "Super Money Rupay",
    "ICICI Saphiro": "ICICI Saphiro",
    "Savings": "Savings",
}

# Legacy categories that are MISCLASSIFIED as expense — actual type override
# Key: legacy_category → (actual_type, seed_category_name, notes)
TYPE_OVERRIDE = {
    "investment": ("savings", "Mutual Funds", "RE-CLASSIFIED: investment/savings — was miscategorized as expense"),
    "Lend": ("lend", None, "RE-CLASSIFIED: lend — was miscategorized as expense"),
}

# Legacy category → Seed category name (for expense/income/savings)
# We try to map as closely as possible to the seeder categories
CATEGORY_MAP = {
    # Expense categories
    "Food Takeaway": ("Food & Dining", "Take Away"),
    "food dining out": ("Food & Dining", "Restaurants/Dining Out"),
    "Shopping groceries": ("Food & Dining", "Provision"),
    "Car": ("Transport", "Fuel (Petrol/Diesel)"),
    "Fuel": ("Transport", "Fuel (Petrol/Diesel)"),
    "Public Transportation": ("Transport", "Public Transit (Train/Bus)"),
    "Private Transportation": ("Transport", "Ride-Hailing"),
    "Credit Card EMI": ("Bills & Utilities", "Mobile & Internet"),  # mixed — could also be Financial
    "Home": ("Family & Social", "Sent to Family"),
    "Social": ("Family & Social", "Social Outings"),
    "Health": ("Health", None),  # parent category
    "Entertainment": ("Entertainment", None),
    "Clothing": ("Shopping", "Clothing & Shoes"),
    "personal care and cloths": ("Personal Care", None),
    "insurance": ("Insurance", None),
    "Insurance": ("Insurance", None),
    "utilities": ("Bills & Utilities", None),
    "Electronics": ("Shopping", "Electronics"),
    "Rent": ("Bills & Utilities", "Rent"),
    "Loan": ("Financial & Legal", None),
    "loan Return": ("Financial & Legal", None),
    "accommodation": ("Vacation", "Hotel / Accommodation"),
    "hotel": ("Vacation", "Hotel / Accommodation"),
    "  -  ": (None, None),  # transfer placeholder category
    # Income categories
    "Salary": ("Salary", None),
    "Refunds": ("Refunds & Cashbacks", None),
}

# Sub-category refinement based on notes keywords
NOTE_CATEGORY_OVERRIDES = [
    (r"(?i)\b(petrol|diesel|fuel|gasoline)\b", ("Transport", "Fuel (Petrol/Diesel)")),
    (r"(?i)\b(train|railway|rail|metro|bus|ksrtc)\b", ("Transport", "Public Transit (Train/Bus)")),
    (r"(?i)\b(uber|ola|rapido|taxi|auto)\b", ("Transport", "Ride-Hailing")),
    (r"(?i)\b(parking)\b", ("Transport", "Parking & Tolls")),
    (r"(?i)\b(medicine|pharmacy|medic|tablet|capsule|vitamin|sompraz|ors)\b", ("Health", "Pharmacy / Medicine")),
    (r"(?i)\b(hospital|doctor|clinic|check.?up|op\b|consultation|xray|x-ray|admission)\b", ("Health", "Doctor & Hospital")),
    (r"(?i)\b(groceries?|provision|vegetables?|veggies?|fruits?|milk|egg|bread|rice|chapathi|flour|oil|spice)\b", ("Food & Dining", "Provision")),
    (r"(?i)\b(chicken?|fish|meat|egg|mutton|beef|pork|seafood|prawn)\b", ("Food & Dining", "Meat & Fish")),
    (r"(?i)\b(restaurant|cafe|dining|dinner|lunch|breakfast|hotel|paragon|thalappakatti|azad|biriyani|mandi|shawaya|kfc)\b", ("Food & Dining", "Restaurants/Dining Out")),
    (r"(?i)\b(swiggy|zomato|online.?delivery|order|delivery)\b", ("Food & Dining", "Online Delivery")),
    (r"(?i)\b(snack|bakery|cake|pastry|bun|cookie|chips)\b", ("Food & Dining", "Snacks & Bakery")),
    (r"(?i)\b(take.?away|takeout|parcel)\b", ("Food & Dining", "Take Away")),
    (r"(?i)\b(movie|cinema|theater|film|iffk)\b", ("Entertainment", "Movies")),
    (r"(?i)\b(flight|airport|airline|goa|pune|ticket)\b", ("Transport", "Flights")),
    (r"(?i)\b(sip|mutual.?fund|groww|indmoney|nps|ppf|fd\b|fixed.?deposit|stock|share|etf|bond|wint.?wealth)\b", ("Mutual Funds", None)),
    (r"(?i)\b(education.?loan|student.?loan)\b", ("Education", "Student Loan EMI")),
    (r"(?i)\b(insurance|policy|cover)\b", ("Insurance", None)),
    (r"(?i)\b(cloth|shirt|pant|dress|zudio|hm|fashion|wear|tshirt)\b", ("Shopping", "Clothing & Shoes")),
    (r"(?i)\b(mobile|phone|recharge|data.?pack|airtel|jio|broadband|internet|wifi|kerala.?vision)\b", ("Bills & Utilities", "Mobile & Internet")),
    (r"(?i)\b(electricity|water.?bill|gas.?bill)\b", ("Bills & Utilities", None)),
    (r"(?i)\b(rent|rental)\b", ("Bills & Utilities", "Rent")),
    (r"(?i)\b(charity|donation|transgender)\b", ("Family & Social", "Charity / Donation")),
    (r"(?i)\b(gift)\b", ("Family & Social", "Gifts")),
    (r"(?i)\b(salary)\b", ("Salary", None)),
    (r"(?i)\b(refund|cashback|reimbursement)\b", ("Refunds & Cashbacks", None)),
    (r"(?i)\b(fd\b|fixed.?deposit|sbi.?fd|hdfc.?fd)\b", ("Fixed Deposit", None)),
    (r"(?i)\b(scooter|bike|dio|motorcycle)\b", ("Transport", "Bike / Scooter")),
    (r"(?i)\b(maintenance|service|repair)\b", ("Transport", "Vehicle Maintenance")),
    (r"(?i)\b(lens.?kart|specs|glasses|frame)\b", ("Personal Care", None)),
    (r"(?i)\b(hostel|accommodation|whoopers)\b", ("Vacation", "Hotel / Accommodation")),
    (r"(?i)\b(liquor|alcohol|beer|wine|whisky)\b", ("Entertainment", None)),
    (r"(?i)\b(swimming|binale)\b", ("Entertainment", None)),
    (r"(?i)\b(toy|kiyu)\b", ("Family & Social", "Gifts")),
    (r"(?i)\b(amazon|flipkart)\b", ("Shopping", "Online Shopping")),
    (r"(?i)\b(emi|installment)\b", ("Financial & Legal", "Bank Fees & Charges")),
    (r"(?i)\b(lend|loan.?return|pay.?back)\b", ("Financial & Legal", None)),
    (r"(?i)\b(achan|amma|family|parents?)\b", ("Family & Social", "Sent to Family")),
    (r"(?i)\b(achu|anil|aswathy|rahul|swaroop|sagar|vrinda|arun|niranjan|koottu|siddhar|jisha|jaisha|ettan)\b", ("Family & Social", "Social Outings")),
    (r"(?i)\b(train.?ticket|vande.?bharat)\b", ("Transport", "Public Transit (Train/Bus)")),
    (r"(?i)\b(web|domain|hosting|cloud|subscription|claude|open.?ai)\b", ("Bills & Utilities", "Subscriptions (OTT/Music)")),
    (r"(?i)\b(medicine|medic|health.?checkup|check.?up)\b", ("Health", None)),
]

def parse_legacy_datetime(time_str):
    try:
        dt = datetime.strptime(time_str.strip(), "%b %d, %Y %I:%M %p")
        return int(dt.timestamp() * 1000)
    except:
        try:
            dt = datetime.strptime(time_str.strip(), "%b %d, %Y %I:%M:%S %p")
            return int(dt.timestamp() * 1000)
        except:
            return int(datetime.now().timestamp() * 1000)

def resolve_account(account_str):
    account_str = account_str.strip()
    # Handle transfer format "Cash->XYZ"
    if "->" in account_str:
        parts = account_str.split("->", 1)
        src = ACCOUNT_MAP.get(parts[0].strip(), parts[0].strip())
        dst = ACCOUNT_MAP.get(parts[1].strip(), parts[1].strip())
        return src, dst, True
    return ACCOUNT_MAP.get(account_str, account_str), None, False

def resolve_category(legacy_cat, legacy_type, note, amount):
    legacy_cat = legacy_cat.strip()

    # If transfer
    if legacy_type == "(*) Transfer":
        return None, None, "transfer"

    # Income
    if legacy_type == "(+) Income":
        cat = CATEGORY_MAP.get(legacy_cat, (legacy_cat, None))
        return cat[0], cat[1], "income"

    # Expense — check if miscategorized
    if legacy_cat in TYPE_OVERRIDE:
        actual_type, seed_cat, flag = TYPE_OVERRIDE[legacy_cat]
        if legacy_cat == "investment":
            # Refine by note
            note_lower = note.lower()
            if any(w in note_lower for w in ["sip", "mutual", "indmoney", "groww"]):
                return "Mutual Funds", None, "savings"
            elif any(w in note_lower for w in ["nps"]):
                return "NPS", None, "savings"
            elif any(w in note_lower for w in ["fd", "fixed deposit", "sbi fd", "hdfc"]):
                return "Fixed Deposit", None, "savings"
            elif any(w in note_lower for w in ["stock", "share", "happiest mind", "iirctc", "equity"]):
                return "Stocks", None, "savings"
            elif any(w in note_lower for w in ["bond", "wint wealth"]):
                return "Bonds", None, "savings"
            elif any(w in note_lower for w in ["kiyu", "kiyu fund", "ettan emi"]):
                return "Other Savings", None, "savings"
            elif any(w in note_lower for w in ["gold"]):
                return "Gold & Silver", None, "savings"
            elif any(w in note_lower for w in ["ppf"]):
                return "PPF", None, "savings"
            elif any(w in note_lower for w in ["crypto", "bitcoin"]):
                return "Crypto", None, "savings"
            return "Mutual Funds", None, "savings"
        elif legacy_cat == "Lend":
            return None, None, "lend"

    # Regular expense
    # Try note-based refinement first
    for pattern, (parent_cat, sub_cat) in NOTE_CATEGORY_OVERRIDES:
        if re.search(pattern, note):
            return parent_cat, sub_cat, "expense"

    # Fall back to category map
    if legacy_cat in CATEGORY_MAP:
        parent_cat, sub_cat = CATEGORY_MAP[legacy_cat]
        return parent_cat, sub_cat, "expense"

    # Fallback: use legacy category as-is (will require manual mapping)
    return legacy_cat, None, "expense"


def main():
    with open(LEGACY_CSV, "r", encoding="utf-8-sig") as f:
        reader = csv.reader(f)
        header = next(reader)  # TIME,TYPE,AMOUNT,CATEGORY,ACCOUNT,NOTES

        rows = list(reader)

    # Output headers matching importTransactionsFromCsv
    output_headers = [
        "id", "account_id", "category_id", "sub_category_id",
        "goal_id", "peer_contact_id", "tag_ids", "date", "amount",
        "type", "note", "description", "receipt_path", "recurring_id",
        "split_data", "investment_platform", "expected_return_date",
        "created_at", "is_recurring", "is_split_parent", "is_split_child",
        "parent_transaction_id", "is_transfer", "to_account_id"
    ]

    savings_entries = []
    lend_entries = []
    expense_entries = []
    income_entries = []
    transfer_entries = []

    with open(OUTPUT_CSV, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f, quoting=csv.QUOTE_ALL)
        writer.writerow(output_headers)

        for idx, row in enumerate(rows):
            if len(row) < 6:
                continue

            time_str = row[0].strip()
            legacy_type = row[1].strip()
            amount_str = row[2].strip()
            legacy_cat = row[3].strip() if len(row) > 3 else ""
            account_str = row[4].strip() if len(row) > 4 else "Cash"
            note = row[5].strip() if len(row) > 5 else ""
            # Strip newlines/CR from all text fields — csv.lines() in the app breaks on them
            note = re.sub(r"[\n\r]+", " ", note)

            # Parse amount
            try:
                amount = float(amount_str)
            except:
                continue

            # Parse date
            date_ms = parse_legacy_datetime(time_str)
            created_at_ms = date_ms  # use transaction date as created_at

            # Resolve account
            src_account, dst_account, is_transfer = resolve_account(account_str)

            # Resolve type and category
            parent_cat, sub_cat, tx_type = resolve_category(legacy_cat, legacy_type, note, amount)

            # Build output row
            out_row = [
                "",  # id (auto-generate)
                src_account,  # account_id (name, will be resolved by import)
                parent_cat if parent_cat else "",  # category_id (name)
                sub_cat if sub_cat else "",  # sub_category_id
                "",  # goal_id
                "",  # peer_contact_id
                "",  # tag_ids
                datetime.fromtimestamp(date_ms / 1000).strftime("%Y-%m-%d %H:%M:%S"),
                amount,
                tx_type,
                note,
                "",  # description
                "",  # receipt_path
                "",  # recurring_id
                "",  # split_data
                "",  # investment_platform
                "",  # expected_return_date
                datetime.fromtimestamp(created_at_ms / 1000).strftime("%Y-%m-%d %H:%M:%S"),
                "false",  # is_recurring
                "false",  # is_split_parent
                "false",  # is_split_child
                "",  # parent_transaction_id
                "true" if is_transfer else "false",
                dst_account if dst_account else "",
            ]

            writer.writerow(out_row)

            # Categorize for summary
            if tx_type == "savings":
                savings_entries.append((time_str, amount, legacy_cat, note, legacy_type))
            elif tx_type == "lend":
                lend_entries.append((time_str, amount, legacy_cat, note, legacy_type))
            elif tx_type == "expense":
                expense_entries.append(amount)
            elif tx_type == "income":
                income_entries.append(amount)
            elif tx_type == "transfer":
                transfer_entries.append(amount)

    # Write summary to file
    summary_path = r"D:\Android projec\migration_summary.txt"
    with open(summary_path, "w", encoding="utf-8") as sf:
        sf.write("=" * 80 + "\n")
        sf.write("MIGRATION SUMMARY\n")
        sf.write("=" * 80 + "\n")
        sf.write(f"Total entries processed: {len(rows)}\n")
        sf.write(f"  Expenses: {len(expense_entries)} (total: Rs.{sum(expense_entries):.2f})\n")
        sf.write(f"  Income: {len(income_entries)} (total: Rs.{sum(income_entries):.2f})\n")
        sf.write(f"  Transfers: {len(transfer_entries)} (total: Rs.{sum(transfer_entries):.2f})\n")
        sf.write(f"  Savings/Investments: {len(savings_entries)} (total: Rs.{sum(s[1] for s in savings_entries):.2f})\n")
        sf.write(f"  Lend: {len(lend_entries)} (total: Rs.{sum(s[1] for s in lend_entries):.2f})\n")
        sf.write("\n")
        sf.write("=" * 80 + "\n")
        sf.write("MISCLASSIFIED SAVINGS/INVESTMENT ENTRIES\n")
        sf.write("(Originally recorded as '(-) Expense' -- corrected to type='savings')\n")
        sf.write("=" * 80 + "\n")
        for entry in savings_entries:
            sf.write(f"  [{entry[0]}] Rs.{entry[1]:>10.2f} | {entry[2]:25s} | {entry[3][:60]}\n")
        sf.write("\n")
        sf.write("=" * 80 + "\n")
        sf.write("MISCLASSIFIED LEND ENTRIES\n")
        sf.write("(Originally recorded as '(-) Expense' -- corrected to type='lend')\n")
        sf.write("=" * 80 + "\n")
        for entry in lend_entries:
            sf.write(f"  [{entry[0]}] Rs.{entry[1]:>10.2f} | {entry[2]:25s} | {entry[3][:60]}\n")
        sf.write("\n")
        sf.write(f"Output written to: {OUTPUT_CSV}\n")
        sf.write(f"Total amount mis-categorized as expense but actually savings: Rs.{sum(s[1] for s in savings_entries):.2f}\n")
        sf.write(f"Total amount mis-categorized as expense but actually lend: Rs.{sum(s[1] for s in lend_entries):.2f}\n")

    # Print to console (ASCII-safe)
    print("=" * 80)
    print("MIGRATION SUMMARY")
    print("=" * 80)
    print(f"Total entries processed: {len(rows)}")
    print(f"  Expenses: {len(expense_entries)} (total: Rs.{sum(expense_entries):.2f})")
    print(f"  Income: {len(income_entries)} (total: Rs.{sum(income_entries):.2f})")
    print(f"  Transfers: {len(transfer_entries)} (total: Rs.{sum(transfer_entries):.2f})")
    print(f"  Savings/Investments: {len(savings_entries)} (total: Rs.{sum(s[1] for s in savings_entries):.2f})")
    print(f"  Lend: {len(lend_entries)} (total: Rs.{sum(s[1] for s in lend_entries):.2f})")
    print()
    print(f"Detailed summary written to: {summary_path}")
    print(f"Output CSV written to: {OUTPUT_CSV}")
    print(f"MISCLASSIFIED savings amount: Rs.{sum(s[1] for s in savings_entries):.2f}")
    print(f"MISCLASSIFIED lend amount: Rs.{sum(s[1] for s in lend_entries):.2f}")

if __name__ == "__main__":
    main()
