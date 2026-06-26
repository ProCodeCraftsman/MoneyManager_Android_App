import csv
from collections import Counter

INPUT = r'D:\Android projec\importable_transactions.csv'

with open(INPUT, 'r', encoding='utf-8') as f:
    content = f.read()

# Check for embedded newlines in quoted fields
in_quotes = False
embedded_newlines = 0
for ch in content:
    if ch == '"':
        in_quotes = not in_quotes
    if ch == '\n' and in_quotes:
        embedded_newlines += 1

print(f'Embedded newlines (in quoted fields): {embedded_newlines}')
print(f'Total file size: {len(content)} bytes')

# Parse properly with csv reader
reader = csv.reader(content.splitlines())
header = next(reader)
print(f'Headers ({len(header)}): {header}')

dates = []
records_with_short_columns = 0
for row in reader:
    if len(row) < 22:
        records_with_short_columns += 1
        continue
    if len(row) > 7:
        dates.append(row[7])

print(f'\nProperly parsed records (>=22 cols): {len(dates)}')
print(f'Records skipped (<22 cols): {records_with_short_columns}')

months = Counter()
for d in dates:
    if d:
        months[d[:7]] += 1

print(f'\nDate distribution:')
for m, c in sorted(months.items()):
    print(f'  {m}: {c}')

# Check records with newlines in notes
reader = csv.reader(open(INPUT, 'r', encoding='utf-8'))
next(reader)
multi_line_notes = 0
for row in reader:
    if len(row) > 10 and '\n' in row[10]:
        multi_line_notes += 1
print(f'\nRecords with multi-line notes: {multi_line_notes}')
