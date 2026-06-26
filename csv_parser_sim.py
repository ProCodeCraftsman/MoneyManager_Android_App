def parse_csv_line(line):
    """Simulate the Kotlin parseCsvLine function"""
    result = []
    current = ""
    in_quotes = False
    
    for char in line:
        if char == '"':
            in_quotes = not in_quotes
        elif char == ',' and not in_quotes:
            result.append(current)
            current = ""
        else:
            current += char
    
    result.append(current)
    return result
