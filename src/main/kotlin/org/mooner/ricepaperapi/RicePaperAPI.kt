package org.mooner.ricepaperapi

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets

class RicePaperAPI {
    enum class RiceType(val tag: String) {
        BREAKFAST("조식"), LUNCH("중식"), DINNER("석식")
    }

    companion object {
        private fun zeroValue(value: Int): String {
            return if (value < 10) {
                "0$value"
            } else {
                value.toString()
            }
        }

        private fun readJsonFromUrl(url: String): JSONObject {
            try {
                URL(url).openStream().use {
                    val rd = BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8))
                    val sb = StringBuilder()
                    var cp: Int
                    while (rd.read().also { value -> cp = value } != -1) sb.append(cp.toChar())
                    return JSONObject(sb.toString())
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        private fun getRiceInfo(s: String): List<String> {
            val strings = s.split("<br/>".toRegex()).dropLastWhile { it.isEmpty() }.toMutableList()
            val v = strings.size
            for (i in 0 until v) {
                strings[i] = strings[i].replace("\\(([^(^)]+)\\)".toRegex(), "").replace("([.*\\-]+)".toRegex(), "")
                if (strings[i].length <= 1) continue
                while (true) {
                    val b = strings[i][strings[i].length - 1]
                    if (b.code in 32..126) {
                        strings[i] = strings[i].substring(0, strings[i].length - 1)
                    } else {
                        break
                    }
                }
            }
            return strings
        }

        fun getRice(schoolTag: String, schoolCode: String, riceType: RiceType, year: Int, month: Int, day: Int): Rice? {
            val data: JSONObject = readJsonFromUrl("https://open.neis.go.kr/hub/mealServiceDietInfo?KEY=83814d97c86242f7ae8e68e83a051933&Type=json&ATPT_OFCDC_SC_CODE=$schoolTag&SD_SCHUL_CODE=$schoolCode&MLSV_YMD=$year${zeroValue(month)}${zeroValue(day)}")

            if(data.has("mealServiceDietInfo")) {
                val array: JSONArray = data.getJSONArray("mealServiceDietInfo").getJSONObject(1).getJSONArray("row")
                val length = array.length()
                for (i in 0..length) {
                    val json = array.getJSONObject(i)
                    if(json.getString("MMEAL_SC_NM").equals(riceType.tag)) {
                        return Rice(getRiceInfo(json.getString("DDISH_NM")))
                    }
                }
            }
            return null
        }
    }

    data class Rice(
        val items: List<String>
    )
}