import csv
from datetime import datetime

test_path = r'D:\Android projec\test_output.csv'

# Test with exact same pattern as migrate_legacy.py
with open(test_path, 'w', encoding='utf-8', newline='') as f:
    writer = csv.writer(f, quoting=csv.QUOTE_ALL)
    
    headers = ['id', 'account_id', 'category_id', 'sub_category_id',
        'goal_id', 'peer_contact_id', 'tag_ids', 'date', 'amount',
        'type', 'note', 'description', 'receipt_path', 'recurring_id',
        'split_data', 'investment_platform', 'expected_return_date',
        'created_at', 'is_recurring', 'is_split_parent', 'is_split_child',
        'parent_transaction_id', 'is_transfer', 'to_account_id']
    
    writer.writerow(headers)
    
    out_row = [
        "",  # id
        "Cash",  # account_id
        "Food & Dining",  # category_id
        "Provision",  # sub_category_id
        "",  # goal_id
        "",  # peer_contact_id
        "",  # tag_ids
        "2025-04-16 06:02:00",  # date
        175.0,  # amount
        "expense",  # type
        "",  # note
        "",  # description
        "",  # receipt_path
        "",  # recurring_id
        "",  # split_data
        "",  # investment_platform
        "",  # expected_return_date
        "2025-04-16 06:02:00",  # created_at
        "false",  # is_recurring
        "false",  # is_split_parent
        "false",  # is_split_child
        "",  # parent_transaction_id
        "false",  # is_transfer
        "",  # to_account_id
    ]
    writer.writerow(out_row)

# Read it back
with open(test_path, 'r', encoding='utf-8') as f:
    content = f.read()
    
print('Written file content:')
print(repr(content))
print()
print('Line 1:', repr(content.split('\n')[0]))
print('Line 2:', repr(content.split('\n')[1]))
