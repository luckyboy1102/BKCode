package com.totoro.bkcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class GetMyCode {

  private static final String url = "https://tellburgerking.com.cn/";

  public static void main(String[] args) {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input = null;
    System.out.println("请输入客户调查代码，以','分隔");

    try {
      input = br.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }

    String[] array = input.split(",");
    for (String code : array) {
      Thread thread = new GetMyCodeThread(code, true);
      thread.start();
    }
  }

  static final class GetMyCodeThread extends Thread {

    private String suffix;
    private String ionf;
    private String postedFNS;
    private String result;
    private String code;
    private Map<String, String> map;
    private RequestSender sender;
    private boolean debug = false;

    public GetMyCodeThread(String code) {
      this(code, false);
    }

    public GetMyCodeThread(String code, boolean debug) {
      this.code = code;
      this.debug = debug;
      init();
    }

    private void init() {
      sender = new RequestSender();

      map = new HashMap<>();
      map.put("JavaScriptEnabled", "1");
      map.put("FIP", "True");
      map.put("AcceptCookies", "Y");
      map.put("NextButton", "%E7%BB%A7%E7%BB%AD");
    }

    private void splitCode() {
      //每隔3个字符分割字符串
      int length = code.length();
      for (int i = 1; i < length; i++) {

        if (i % 3 == 0) {
          map.put("CN" + String.valueOf(map.size() - 2),
              code.substring(i - 3, i));
        }

        if (i == length - 1) {
          map.put("CN" + String.valueOf(map.size() - 2),
              code.substring(length - 1));
        }
      }
    }

    private String getAction(String result) {
      Document document = Jsoup.parse(result);
      Element element = document.getElementById("surveyEntryForm");
      return suffix = element.attr("action");
    }

    @Override
    public void run() {
      System.out.println("正在获取" + code + "网站调查代码，请稍后...");

      //对首页进行解析
      result = sender.execute(url);
      suffix = getAction(result);
      result = sender.execute(url + suffix, map);

      map = new HashMap<>();
      map.put("JavaScriptEnabled", "1");
      map.put("FIP", "True");
      map.put("NextButton", "%E5%BC%80%E5%A7%8B");
      splitCode();

      result = sender.execute(url, map);
      suffix = getAction(result);

      do {
        result = sender.execute(url + suffix, map);

        //构造新的请求参数
        map = new HashMap<>();

        Document doc = Jsoup.parse(result);
        Element element = doc.getElementById("surveyForm");
        //验证代码页面无此element
        if (element == null) {
          element = doc.getElementsByClass("ValCode").first();
          System.out.println(code + " " + element.childNode(0).outerHtml());
          break;
        } else {
          suffix = element.attr("action");
        }

        element = doc.getElementById("PostedFNS");
        postedFNS = element.attr("value");
        map.put("PostedFNS", postedFNS);

        element = doc.getElementById("IoNF");
        ionf = element.attr("value");
        map.put("IoNF", ionf);

        if (debug) {
          element = doc.getElementById("ProgressPercentage");
          System.out.println("IoNF=" + ionf + ",Progress=" + element.text());
        }

        //第一步，特殊处理
        if ("2".equals(ionf)) {
          map.put("R001000", "2");
          continue;
        }

        //对于您在光临BK时是否遇到了一些问题 选择否
        if (result.contains("时是否遇到了一些问题？")) {
          map.put(postedFNS, "2");
          continue;
        }

        String[] options = postedFNS.split("\\|");
        String tagName;
        for (String option : options) {
          element = doc.select("[name=" + option + "]").first();
          tagName = element.tagName();

          if ("textarea".equals(tagName)) {
            map.put(option, "");
          }

          if ("input".equals(tagName)) {
            map.put(option, element.attr("value"));
          }
        }

      } while (!result.contains("验证代码："));
    }
  }
}
