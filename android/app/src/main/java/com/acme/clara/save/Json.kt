package com.acme.clara.save

/**
 * A tiny, dependency-free JSON reader/writer — just enough for the save system.
 *
 * We hand-roll this rather than pull in kotlinx.serialization so the whole persistence
 * layer stays a pure-JVM unit-testable island with no Gradle-plugin or Android coupling.
 * Supported value types: [Map]<String, Any?>, [List]<Any?>, [String], [Long]/[Int],
 * [Boolean], and null. Numbers always decode back as [Long].
 */
object Json {

    fun encode(value: Any?): String = StringBuilder().also { enc(value, it) }.toString()

    private fun enc(v: Any?, sb: StringBuilder) {
        when (v) {
            null -> sb.append("null")
            is String -> encStr(v, sb)
            is Boolean -> sb.append(if (v) "true" else "false")
            is Int, is Long -> sb.append(v.toString())
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, vv) in v) {
                    if (!first) sb.append(',')
                    first = false
                    encStr(k.toString(), sb); sb.append(':'); enc(vv, sb)
                }
                sb.append('}')
            }
            is List<*> -> {
                sb.append('[')
                var first = true
                for (e in v) {
                    if (!first) sb.append(',')
                    first = false
                    enc(e, sb)
                }
                sb.append(']')
            }
            else -> throw IllegalArgumentException("Json cannot encode ${v::class}")
        }
    }

    private fun encStr(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
        }
        sb.append('"')
    }

    /** Parse [text]; throws [IllegalArgumentException] on malformed input. */
    fun decode(text: String): Any? {
        val p = Parser(text)
        val v = p.value()
        p.ws()
        require(p.end()) { "trailing content at ${p.pos}" }
        return v
    }

    private class Parser(val s: String) {
        var pos = 0
        fun end() = pos >= s.length
        fun ws() { while (pos < s.length && s[pos].isWhitespace()) pos++ }

        fun value(): Any? {
            ws()
            require(!end()) { "unexpected end of input" }
            return when (s[pos]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't', 'f' -> bool()
                'n' -> nul()
                else -> num()
            }
        }

        private fun obj(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            pos++ // {
            ws()
            if (!end() && s[pos] == '}') { pos++; return m }
            while (true) {
                ws()
                val k = str()
                ws(); expect(':')
                m[k] = value()
                ws()
                when {
                    end() -> throw IllegalArgumentException("unterminated object")
                    s[pos] == ',' -> pos++
                    s[pos] == '}' -> { pos++; return m }
                    else -> throw IllegalArgumentException("expected , or } at $pos")
                }
            }
        }

        private fun arr(): List<Any?> {
            val list = ArrayList<Any?>()
            pos++ // [
            ws()
            if (!end() && s[pos] == ']') { pos++; return list }
            while (true) {
                list.add(value())
                ws()
                when {
                    end() -> throw IllegalArgumentException("unterminated array")
                    s[pos] == ',' -> pos++
                    s[pos] == ']' -> { pos++; return list }
                    else -> throw IllegalArgumentException("expected , or ] at $pos")
                }
            }
        }

        private fun str(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                require(!end()) { "unterminated string" }
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        require(!end()) { "dangling escape" }
                        when (val e = s[pos++]) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                            'u' -> {
                                val hex = s.substring(pos, pos + 4); pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> throw IllegalArgumentException("bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun bool(): Boolean =
            if (s.startsWith("true", pos)) { pos += 4; true }
            else if (s.startsWith("false", pos)) { pos += 5; false }
            else throw IllegalArgumentException("bad literal at $pos")

        private fun nul(): Any? {
            require(s.startsWith("null", pos)) { "bad literal at $pos" }
            pos += 4
            return null
        }

        private fun num(): Long {
            val start = pos
            if (!end() && s[pos] == '-') pos++
            while (!end() && s[pos].isDigit()) pos++
            require(pos > start) { "expected value at $start" }
            return s.substring(start, pos).toLong()
        }

        private fun expect(c: Char) {
            require(!end() && s[pos] == c) { "expected '$c' at $pos" }
            pos++
        }
    }
}
