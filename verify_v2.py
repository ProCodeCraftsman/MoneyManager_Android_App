import csv
from datetime import datetime

fpath = r'D:\Android projec\importable_transactions_v2.csv'

with open(fpath, 'r', encoding='utf-8') as f:
    content = f.read()

print(f'File size: {len(content)} bytes')
print(f'File starts with quotes: {content.startswith(chr(34))}')
print(f'First 100 chars: {repr(content[:100])}')

# Check if quoting is correct
header_line = content.split('\n')[0]
print(f'Header line: {repr(header_line)}')
print(f'Header has quotes: {"\"" in header_line}')

# Parse properly
reader = csv.reader(content.splitlines())
header = next(reader)
print(f'\nHeader has {len(header)} columns')
print(f'Header: {header}')

# Check first 3 data rows
for i, row in enumerate(reader):
    if i >= 3:
        break
    if len(row) >= 24:
        print(f'\nRow {i+1}:')
        print(f'  id={repr(row[0])}')
        print(f'  account={repr(row[1])}')
        print(f'  category={repr(row[2])}')
        print(f'  sub_category={repr(row[3])}')
        print(f'  date={repr(row[7])}')
        print(f'  amount={repr(row[8])}')
        print(f'  type={repr(row[9])}')
        print(f'  note={repr(row[10][:60])}')
        print(f'  created_at={repr(row[17])}')
        print(f'  is_transfer={repr(row[22])}')
        print(f'  to_account={repr(row[23])}')

# Check ALL dates parse correctly
reader = csv.reader(open(fpath, 'r', encoding='utf-8'))
next(reader)
all_ok = True
bad_dates = []
for row in reader:
    if len(row) >= 24:
        date_str = row[7].strip()
        try:
            datetime.strptime(date_str, "%Y-%m-%d %H:%M:%S")
        except:
            all_ok = False
            bad_dates.append((date_str, row[9], row[1]))
            if len(bad_dates) >= 5:
                break

print(f'\nAll 1123 dates parse correctly: {all_ok}')
if not all_ok:
    print(f'Sample bad dates:')
    for d, t, a in bad_dates:
        print(f'  date={repr(d)}, type={t}, account={a}')
else:
    print('All dates in YYYY-MM-DD HH:MM:SS format ✓')
    
# Check multi-line notes
reader = csv.reader(open(fpath, 'r', encoding='utf-8'))
next(reader)
multi = 0
for row in reader:
    if len(row) > 10 and '\n' in row[10]:
        multi += 1
print(f'\nRecords with newlines in notes: {multi}')
