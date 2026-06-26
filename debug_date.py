from datetime import datetime

# Reproduce what migrate_legacy.py does
time_str = "Apr 16, 2025 6:02 AM"
dt = datetime.strptime(time_str.strip(), "%b %d, %Y %I:%M %p")
date_ms = int(dt.timestamp() * 1000)
formatted = datetime.fromtimestamp(date_ms / 1000).strftime("%Y-%m-%d %H:%M:%S")
print(f"Original: {time_str}")
print(f"Parsed dt: {dt}")
print(f"date_ms: {date_ms}")
print(f"Formatted: {formatted}")
print(f"format: {datetime.fromtimestamp(date_ms/1000)}")

# Check the actual CSV file
with open(r'D:\Android projec\importable_transactions.csv', 'r', encoding='utf-8') as f:
    first_line = f.readline()  # header
    second_line = f.readline()  # first data row
    print(f"\nRaw CSV row 1: {repr(second_line[:200])}")
    
    # Find the date field by counting fields
    # Parse manually like parseCsvLine
    fields = []
    cur = ""
    in_q = False
    for ch in second_line:
        if ch == '"':
            in_q = not in_q
        elif ch == ',' and not in_q:
            fields.append(cur)
            cur = ""
        else:
            cur += ch
    fields.append(cur)
    
    print(f"Field 7 (date): {repr(fields[7])}")
    print(f"Field 8 (amount): {repr(fields[8])}")
    print(f"Field 9 (type): {repr(fields[9])}")
    print(f"Field 10 (note): {repr(fields[10][:50])}")
