package com.moneymanager.app.ui.util

/** Result of a keypad edit: the new amount text and where the caret sits in it. */
data class AmountEdit(val text: String, val cursor: Int)

/**
 * Cursor-aware edits for the calculator-style amount field. With the cursor at the end every
 * operation behaves exactly like plain append/trim, so existing keypad behaviour is unchanged.
 */
object AmountEditor {
    private const val OPERATORS = "+-*/"

    private fun Char.isOperator() = this in OPERATORS

    fun insert(text: String, rawCursor: Int, key: String): AmountEdit {
        val cursor = rawCursor.coerceIn(0, text.length)
        return when {
            key == "." -> insertDecimal(text, cursor)
            key.length == 1 && key[0].isOperator() -> insertOperator(text, cursor, key[0])
            text == "0" -> AmountEdit(key, key.length)
            else -> AmountEdit(text.substring(0, cursor) + key + text.substring(cursor), cursor + key.length)
        }
    }

    /** Removes the character before the cursor; no-op at the start. */
    fun delete(text: String, rawCursor: Int): AmountEdit {
        val cursor = rawCursor.coerceIn(0, text.length)
        if (cursor == 0) return AmountEdit(text, 0)
        val removed = text.substring(0, cursor - 1) + text.substring(cursor)
        return normalizeOperators(removed, cursor - 1)
    }

    private fun insertDecimal(text: String, cursor: Int): AmountEdit {
        var start = cursor
        while (start > 0 && !text[start - 1].isOperator()) start--
        var end = cursor
        while (end < text.length && !text[end].isOperator()) end++
        if (text.substring(start, end).contains('.')) return AmountEdit(text, cursor)
        val dot = if (cursor == start) "0." else "."
        return AmountEdit(text.substring(0, cursor) + dot + text.substring(cursor), cursor + dot.length)
    }

    private fun insertOperator(text: String, cursor: Int, op: Char): AmountEdit {
        if (cursor == 0) return AmountEdit(text, cursor)
        val before = text[cursor - 1]
        if (before.isOperator() || before == '.') {
            return AmountEdit(text.substring(0, cursor - 1) + op + text.substring(cursor), cursor)
        }
        if (cursor < text.length && text[cursor].isOperator()) {
            return AmountEdit(text.substring(0, cursor) + op + text.substring(cursor + 1), cursor + 1)
        }
        return AmountEdit(text.substring(0, cursor) + op + text.substring(cursor), cursor + 1)
    }

    /**
     * Deleting a digit can leave "5+*2" or a leading operator. Keep the later of two adjacent
     * operators (typing replaces) and drop a leading one so the expression stays evaluable.
     */
    private fun normalizeOperators(text: String, cursor: Int): AmountEdit {
        val sb = StringBuilder()
        var newCursor = cursor
        text.forEachIndexed { i, c ->
            val dropped = c.isOperator() &&
                (sb.isEmpty() || (i + 1 < text.length && text[i + 1].isOperator()))
            if (dropped) {
                if (i < cursor) newCursor--
            } else {
                sb.append(c)
            }
        }
        return AmountEdit(sb.toString(), newCursor.coerceIn(0, sb.length))
    }
}
