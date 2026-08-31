import java.io.File
val bodyStr = File("cbm.html").readText()
val regex = """<td>USD</td>\s*<td[^>]*>.*?</td>\s*<td[^>]*>([\d.,]+)\s*</td>\s*<td[^>]*>([\d.,]+)\s*</td>""".toRegex()
val match = regex.find(bodyStr)
println("Match: " + match?.groupValues?.get(2))
