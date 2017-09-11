package com.elyria.breakwords

import com.elyria.breakwords.YouDaoApi.ERROR_CODE_SUCCESS
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.http.util.TextUtils
import java.awt.Color
import java.util.*

class BreakWords : AnAction() {

  private val URL = "https://openapi.youdao.com/api"
  // 应用信息 http://ai.youdao.com/docs/api.s
  private val APP_KEY = "2a9903a060d0511a"
  private val APP_SECRET = "hHcIipkvVFSeUBr1KnyeGFbHPbAknQQn"
  // 参数名
  private val PARAMS_QUERY = "q"
  private val PARAMS_APPKEY = "appKey"
  private val PARAMS_FROM = "from"
  private val PARAMS_TO = "to"
  private val PARAMS_SALT = "salt"
  private val PARAMS_SIGN = "sign"

  private val DEFAULT_VALUE_FROM = "EN"
  private val DEFAULT_VALUE_TO = "zh-CHS"

  private var latestClickTime: Long = 0
  private var mEditor: Editor? = null

  override fun actionPerformed(event: AnActionEvent?) {
    if (!isFastClick(1000)) {
      event?.let {
        var query: String? = null
        mEditor = it.getData(PlatformDataKeys.EDITOR)
        mEditor?.let {
          if (TextUtils.isBlank(it.selectionModel.selectedText)) {
            query = getCurrentWords(mEditor!!)
          } else {
            query = it.selectionModel.selectedText
          }
        }
        requestTranslate(strip(addBlanks(query!!)))
      }
    }
  }

  fun requestTranslate(query: String) {
    Thread(Runnable {
      kotlin.run {
        val httpPost = HttpPost(URL)
        httpPost.entity = UrlEncodedFormEntity(createTranslateParams(query), "UTF-8")
        val client = HttpClients.createDefault()
        val response = client.execute(httpPost)
        val status = response.statusLine.statusCode
        if (status in 200..299) {
          val entity = response.entity
          val json = EntityUtils.toString(entity, "UTF-8")
          val translation = Gson().fromJson<Translation>(json, Translation::class.java)
          translation.errorMessage = when (translation.errorCode) {
            YouDaoApi.ERROR_CODE_PARAMS -> "缺少必填的参数，出现这个情况还可能是et的值和实际加密方式不对应"
            YouDaoApi.ERROR_CODE_UNSUPPORT_LANGUAGE -> "不支持的语言类型"
            YouDaoApi.ERROR_CODE_TOLONG -> "翻译文本过长"
            YouDaoApi.ERROR_CODE_UNSUPPORT_API -> "不支持的API类型"
            YouDaoApi.ERROR_CODE_UNSUPPORT_SIGN -> "不支持的签名类型"
            YouDaoApi.ERROR_CODE_UNSUPPORT_RESPONSE -> "不支持的响应类型"
            YouDaoApi.ERROR_CODE_UNSUPPORT_ENCRYPT -> "不支持的传输加密类型"
            YouDaoApi.ERROR_CODE_APPKEY_INVALID -> "appKey无效，注册账号"
            YouDaoApi.ERROR_CODE_BATCHLOG_ERROR -> "batchLog格式不正确"
            YouDaoApi.ERROR_CODE_NO_SERVICE -> "无相关服务的有效实例"
            YouDaoApi.ERROR_CODE_ACCOUNT_INVALID -> "开发者账号无效，可能是账号为欠费状态"
            YouDaoApi.ERROR_CODE_DEENCRYPT_ERROR -> "解密失败，可能为DES,BASE64,URLDecode的错误"
            YouDaoApi.ERROR_CODE_SIGN_FAILURE -> "签名检验失败"
            YouDaoApi.ERROR_CODE_UNSUPPORT_IP -> "访问IP地址不在可访问IP列表"
            YouDaoApi.ERROR_CODE_QUERY_ERROR -> "辞典查询失败"
            YouDaoApi.ERROR_CODE_TRANSLATE_ERROR -> "翻译查询失败"
            YouDaoApi.ERROR_CODE_SERVER_ERROR -> "服务端的其它异常"
            YouDaoApi.ERROR_CODE_ACCOUNT_ERROR -> "账户已经欠费停"
            ERROR_CODE_SUCCESS -> "成功"
            else -> "出现未知错误"
          }
          showPopupBalloon(translation.toString())
        } else {
          showPopupBalloon(response.statusLine.reasonPhrase)
        }
      }
    }).start()
  }

  fun getCurrentWords(editor: Editor): String? {
    val document = editor.document
    val caretModel = editor.caretModel
    val caretOffset = caretModel.offset
    val lineNum = document.getLineNumber(caretOffset)
    val lineStartOffset = document.getLineStartOffset(lineNum)
    val lineEndOffset = document.getLineEndOffset(lineNum)
    val lineContent = document.getText(TextRange(lineStartOffset, lineEndOffset))
    val chars = lineContent.toCharArray()
    var end = 0
    val cursor = caretOffset - lineStartOffset

    if (!Character.isLetter(chars[cursor])) {
      return null
    }

    val start = (cursor downTo 0)
        .firstOrNull { !Character.isLetter(chars[it]) }
        ?.let { it + 1 }
        ?: 0

    var lastLetter = 0
    for (ptr in cursor..lineEndOffset - lineStartOffset - 1) {
      lastLetter = ptr
      if (!Character.isLetter(chars[ptr])) {
        end = ptr
        break
      }
    }
    if (end == 0) {
      end = lastLetter + 1
    }

    val ret = String(chars, start, end - start)
    return ret
  }

  fun addBlanks(str: String): String {
    val temp = str.replace("_".toRegex(), " ")
    if (temp == temp.toUpperCase()) {
      return temp
    }
    val result = temp.replace("([A-Z]+)".toRegex(), " $0")
    return result
  }

  fun strip(str: String): String {
    return str.replace("/\\*+".toRegex(), "")
        .replace("\\*+/".toRegex(), "")
        .replace("\\*".toRegex(), "")
        .replace("//+".toRegex(), "")
        .replace("\r\n".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
  }

  fun isFastClick(timeMillis: Long): Boolean {
    val time = System.currentTimeMillis()
    val timeD = time - latestClickTime
    if (timeD in 1..(timeMillis - 1)) {
      return true
    }
    latestClickTime = time
    return false
  }

  private fun showPopupBalloon(result: String) {
    ApplicationManager.getApplication().invokeLater {
      val factory = JBPopupFactory.getInstance()
      factory.createHtmlTextBalloonBuilder(result, null, JBColor(Color(186, 238, 186), Color(73, 117, 73)), null)
          .setFadeoutTime(5000)
          .createBalloon()
          .show(factory.guessBestPopupLocation(mEditor!!), Balloon.Position.below)
    }
  }

  fun createTranslateParams(query: String): List<NameValuePair> {
    val salt = System.currentTimeMillis().toString()
    val params = ArrayList<NameValuePair>()
    params.add(BasicNameValuePair(PARAMS_QUERY, query))
    params.add(BasicNameValuePair(PARAMS_FROM, DEFAULT_VALUE_FROM))
    params.add(BasicNameValuePair(PARAMS_TO, DEFAULT_VALUE_TO))
    params.add(BasicNameValuePair(PARAMS_SIGN, YouDaoApi.md5(APP_KEY + query + salt + APP_SECRET)))
    params.add(BasicNameValuePair(PARAMS_SALT, salt))
    params.add(BasicNameValuePair(PARAMS_APPKEY, APP_KEY))
    return params
  }
}

data class TranslationBasic(
    @SerializedName("us-phonetic") val phonetic_us: String?,
    @SerializedName("uk-phonetic") val phonetic_uk: String?,
    @SerializedName("phonetic") val phonetic: String?,
    @SerializedName("explains") val explains: Array<String>?
)

data class WebTranslation(
    @SerializedName("key") val key: String?,
    @SerializedName("value") val values: Array<String>?
) {
  override fun toString(): String {
    return key + ": " + values?.joinToString(" , ")
  }
}

data class Translation(
    @SerializedName("translation") val translations: Array<String>?,
    @SerializedName("errorCode") val errorCode: Int,
    @SerializedName("query") val query: String?,
    @SerializedName("basic") val basic: TranslationBasic?,
    @SerializedName("web") val webTranslations: Array<WebTranslation>?,
    var errorMessage: String?
) {
  override fun toString(): String {
    if (errorCode != ERROR_CODE_SUCCESS) {
      return "错误代码: $errorCode \n $errorMessage"
    } else {
      val translationTop = "<h3 style=\"font-size: 15px\"> $query: ${translations?.joinToString(", ")}<h3/>"
      var phoneticUs: String? = ""
      var phoneticUk: String? = ""
      var phonetic: String? = ""
      basic?.phonetic_us?.let {
        phoneticUs = """美式: [$it]"""
      }
      basic?.phonetic_uk?.let {
        phoneticUk = """英式: [$it]"""
      }
      if (TextUtils.isBlank(phoneticUs) && TextUtils.isBlank(phoneticUk)) {
        basic?.phonetic?.let {
          phonetic = """发音: [$it]""" + "${System.lineSeparator()}<br/>"
        }
      } else {
        phonetic = "<span style=\"font-size: 9px\">$phoneticUs ; $phoneticUk</span> ${System.lineSeparator()} <br/>"
      }
      var translationBody: String? = ""
      basic?.explains?.let {
        translationBody = it.joinToString(separator = System.lineSeparator(), prefix = "<span style=\"font-size: 10px\">", postfix = "</span>") + "${System.lineSeparator()}<br/>"
      }
      var translationWeb: String? = ""
      webTranslations?.let {
        translationWeb = "<span style=\"font-size: 9px\">网络释义: <br/> ${it.joinToString(System.lineSeparator())}</span>"
      }
      return "$translationTop $phonetic $translationBody $translationWeb"
    }
  }
}
