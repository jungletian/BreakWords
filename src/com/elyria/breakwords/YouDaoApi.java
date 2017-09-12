package com.elyria.breakwords;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class YouDaoApi {

  public final static int ERROR_CODE_SUCCESS = 0;
  public final static int ERROR_CODE_PARAMS = 101;  // 缺少必填的参数，出现这个情况还可能是et的值和实际加密方式不对应
  public final static int ERROR_CODE_UNSUPPORT_LANGUAGE = 102;  // 不支持的语言类型
  public final static int ERROR_CODE_TOLONG = 103;  // 翻译文本过长
  public final static int ERROR_CODE_UNSUPPORT_API = 104;  // 不支持的API类型
  public final static int ERROR_CODE_UNSUPPORT_SIGN = 105;  // 不支持的签名类型
  public final static int ERROR_CODE_UNSUPPORT_RESPONSE = 106;  // 不支持的响应类型
  public final static int ERROR_CODE_UNSUPPORT_ENCRYPT = 107;  // 不支持的传输加密类型
  public final static int ERROR_CODE_APPKEY_INVALID = 108;  // appKey无效，注册账号，登录后台创建应用和实例并完成绑定， 可获得应用ID和密钥等信息，其中应用ID就是appKey（ 注意不是应用密钥）
  public final static int ERROR_CODE_BATCHLOG_ERROR = 109; // batchLog格式不正确
  public final static int ERROR_CODE_NO_SERVICE = 110;  // 无相关服务的有效实例
  public final static int ERROR_CODE_ACCOUNT_INVALID = 111;  // 开发者账号无效，可能是账号为欠费状态
  public final static int ERROR_CODE_DEENCRYPT_ERROR = 201;  // 解密失败，可能为DES,BASE64,URLDecode的错误
  public final static int ERROR_CODE_SIGN_FAILURE = 202;  // 签名检验失败
  public final static int ERROR_CODE_UNSUPPORT_IP = 203;  // 访问IP地址不在可访问IP列表
  public final static int ERROR_CODE_QUERY_ERROR = 301;  // 辞典查询失败
  public final static int ERROR_CODE_TRANSLATE_ERROR = 302;  // 翻译查询失败
  public final static int ERROR_CODE_SERVER_ERROR = 303;  // 服务端的其它异常
  public final static int ERROR_CODE_ACCOUNT_ERROR = 401;  // 账户已经欠费停

  public static void main(String[] args) throws Exception {
    BreakWords breakWords = new BreakWords();
    breakWords.requestTranslate("context");
  }

  public static String requestForHttp(String url, Map<String, String> requestParams)
      throws Exception {
    String result = null;
    CloseableHttpClient httpClient = HttpClients.createDefault();
    /**HttpPost*/
    HttpPost httpPost = new HttpPost(url);
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    Iterator<Map.Entry<String, String>> it = requestParams.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> en = it.next();
      String key = en.getKey();
      String value = en.getValue();
      if (value != null) {
        params.add(new BasicNameValuePair(key, value));
      }
    }
    httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    /**HttpResponse*/
    System.out.println(params);
    CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
    try {
      HttpEntity httpEntity = httpResponse.getEntity();
      result = EntityUtils.toString(httpEntity, "utf-8");
      EntityUtils.consume(httpEntity);
    } finally {
      try {
        if (httpResponse != null) {
          httpResponse.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  /**
   * 生成32位MD5摘要
   */
  public static String md5(String string) {
    if (string == null) {
      return null;
    }
    char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F'};
    try {
      byte[] btInput = string.getBytes("utf-8");
      /** 获得MD5摘要算法的 MessageDigest 对象 */
      MessageDigest mdInst = MessageDigest.getInstance("MD5");
      /** 使用指定的字节更新摘要 */
      mdInst.update(btInput);
      /** 获得密文 */
      byte[] md = mdInst.digest();
      /** 把密文转换成十六进制的字符串形式 */
      int j = md.length;
      char str[] = new char[j * 2];
      int k = 0;
      for (byte byte0 : md) {
        str[k++] = hexDigits[byte0 >>> 4 & 0xf];
        str[k++] = hexDigits[byte0 & 0xf];
      }
      return new String(str);
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      return null;
    }
  }

  /**
   * 根据api地址和参数生成请求URL
   */
  public static String getUrlWithQueryString(String url, Map<String, String> params) {
    if (params == null) {
      return url;
    }
    StringBuilder builder = new StringBuilder(url);
    if (url.contains("?")) {
      builder.append("&");
    } else {
      builder.append("?");
    }

    int i = 0;
    for (String key : params.keySet()) {
      String value = params.get(key);
      if (value == null) { // 过滤空的key
        continue;
      }
      if (i != 0) {
        builder.append('&');
      }
      builder.append(key);
      builder.append('=');
      builder.append(encode(value));

      i++;
    }
    return builder.toString();
  }

  /**
   * 进行URL编码
   */
  public static String encode(String input) {
    if (input == null) {
      return "";
    }
    try {
      return URLEncoder.encode(input, "utf-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return input;
  }
}
