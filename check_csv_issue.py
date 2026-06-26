# Check for the real issue - multi-line notes breaking csv.lines()
with open(r'D:\Android projec\importable_transactions.csv', 'rb') as f:
    data = f.read()

# Show a problematic segment
text = data.decode('utf-8')

# Find records with newlines inside notes
in_quotes = False
newlines_in_quotes = 0
segments = []
for i, b in enumerate(data):
    ch = chr(b)
    if ch == '"':
        in_quotes = not in_quotes
    if ch == '\n' and in_quotes:
        newlines_in_quotes += 1
        if newlines_in_quotes <= 3:
            # Show context around this newline
            start = max(0, i-120)
            end = min(len(data), i+120)
            segments.append(text[start:end])

print(f"Total embedded newlines: {newlines_in_quotes}")
print()
for i, seg in enumerate(segments):
    print(f"=== Embedded newline {i+1} ===")
    # Replace \n with \\n for display
    display = seg.replace('\n', '\\n\n')
    print(display)
    print()

# Now simulate what csv.lines() does (splitting by \n) and how parseCsvLine handles it
lines = text.split('\n')
print(f"Total lines from split(): {len(lines)}")
print(f"Expected records (header + 1123 data): 1124")

valid = 0
skipped = 0
from csv_parser_sim import parse_csv_line
for i, line in enumerate(lines):
    parts = parse_csv_line(line)
    if len(parts) >= 22:
        valid += 1
        # Check if date is valid
        if valid <= 3:
            print(f"  Valid line {i}: date={parts[7]}, type={parts[9]}, amt={parts[8]}")
    else:
        skipped += 1
        
print(f"\nValid records (>=22 cols): {valid}")
print(f"Skipped records (<22 cols): {skipped}")

# Now check what happens with the date parsing
print("\n=== Date parsing simulation ===")
from datetime import datetime
date_failures = 0
date_success = 0
for i, line in enumerate(lines):
    parts = parse_csv_line(line)
    if len(parts) >= 22:
        try:
            dt = datetime.strptime(parts[7].strip(), "%Y-%m-%d %H:%M:%S")
            date_success += 1
        except:
            date_failures += 1
            if date_failures <= 5:
                print(f"  FAILED: line {i}, date field = '{parts[7]}'")

print(f"Date parse success: {date_success}, failures: {date_failures}")
