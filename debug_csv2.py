import csv, os

fpath = r'D:\Android projec\importable_transactions.csv'
print('File size:', os.path.getsize(fpath))

with open(fpath, 'r', encoding='utf-8') as f:
    content = f.read(1000)

print('Contains double-quote chars?', '"' in content)
print('First 200 bytes repr:')
print(repr(content[:200]))
print()

# Count quote characters
quote_count = content.count('"')
print(f'Total double-quote chars in first 1000 bytes: {quote_count}')

# Show first 5 actual lines
f.seek(0)
lines = f.readlines()
print(f'\nTotal raw lines: {len(lines)}')
print('First line (header):')
print(repr(lines[0][:200]))
print('Second line:')
print(repr(lines[1][:200]))
