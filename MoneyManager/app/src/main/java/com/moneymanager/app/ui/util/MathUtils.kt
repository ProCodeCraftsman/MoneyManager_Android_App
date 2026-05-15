package com.moneymanager.app.ui.util

fun evaluateExpression(expression: String): Double {
    if (expression.isEmpty()) return 0.0

    return try {
        val tokens = mutableListOf<String>()
        var current = ""
        for (char in expression) {
            if (char in "+-*/") {
                if (current.isNotEmpty()) tokens.add(current)
                tokens.add(char.toString())
                current = ""
            } else {
                current += char
            }
        }
        if (current.isNotEmpty()) tokens.add(current)

        if (tokens.isEmpty()) return 0.0

        val pass1 = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "*" || token == "/") {
                val prev = pass1.removeAt(pass1.size - 1).toDouble()
                val next = tokens[i + 1].toDouble()
                val res = if (token == "*") prev * next else prev / next
                pass1.add(res.toString())
                i += 2
            } else {
                pass1.add(token)
                i++
            }
        }

        var result = pass1[0].toDouble()
        i = 1
        while (i < pass1.size) {
            val op = pass1[i]
            val next = pass1[i + 1].toDouble()
            result = if (op == "+") result + next else result - next
            i += 2
        }
        result
    } catch (e: Exception) {
        expression.toDoubleOrNull() ?: 0.0
    }
}
