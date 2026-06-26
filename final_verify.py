import csv
from datetime import datetime
from collections import Counter

fpath = r'D:\Android projec\importable_v2.csv'

with open(fpath, 'r', encoding='utf-8') as f:
    content = f.read()

print(f'File size: {len(content)} bytes')
print(f'Properly quoted: {content.startswith(chr(34))}')
print(f'Header: {repr(content.split(chr(10))[0][:100])}')

# Parse and verify
reader = csv.reader(open(fpath, 'r', encoding='utf-8'))
next(reader)  # header

dates_ok = 0
dates_fail = 0
total = 0
multi_note = 0

for row in reader:
    total += 1
    if len(row) < 24:
        continue
    
    # Check date
    try:
        datetime.strptime(row[7], "%Y-%m-%d %H:%M:%S")
        dates_ok += 1
    except:
        dates_fail += 1
        print(f'BAD DATE: row {total}: date={repr(row[7])}, type={row[9]}')
    
    # Check for newlines in notes
    if '\n' in row[10] or '\r' in row[10]:
        multi_note += 1

print(f'\nTotal rows: {total}')
print(f'Dates OK: {dates_ok}')
print(f'Dates FAILED: {dates_fail}')
print(f'Records with newlines in notes: {multi_note}')

# Month distribution
reader = csv.reader(open(fpath, 'r', encoding='utf-8'))
next(reader)
months = Counter()
for row in reader:
    if len(row) >= 24 and len(row[7]) >= 7:
        months[row[7][:7]] += 1

print('\nMonth distribution:')
for m, c in sorted(months.items()):
    print(f'  {m}: {c} records')

print(f'\nUnique months: {len(months)}')
print(f'Total: {sum(months.values())}')
