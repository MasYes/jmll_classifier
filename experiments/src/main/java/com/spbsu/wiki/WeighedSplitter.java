package com.spbsu.wiki;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.StringUtils.endsWith;


import java.util.*;


/**
 * Разбивает строку на слова и фразы из заданного словаря.
 * <p/>
 * Строка разбивается так, чтобы сумма весов найденных фраз
 * была максимальной. В качестве весов берутся частоты
 * фраз в исходной коллекции, на которой фразы построены.
 * <p/>
 * Если использован конструктор WeighedSplitter(Map<String, Double> weights),
 * то фразы берутся из weights.
 * <p/>
 * Если словарь не задан, то текст просто разбивается на слова.
 */
public class WeighedSplitter {


  private final HashMap<String, Object2DoubleOpenHashMap<String>> phrases; //last word -> phrase -> weight; для ускорения
  private final static double Z = 0; //вес, который имеет одно слово

  public WeighedSplitter() {
    phrases = new HashMap<>();
  }

  public WeighedSplitter(Map<String, Double> weights) {
    phrases = new HashMap<>();
    for (String term : weights.keySet()) {
      String last = twoLastWords(term);
      if (!phrases.containsKey(last))
        phrases.put(last, new Object2DoubleOpenHashMap<>());
      phrases.get(last).put(term, weights.get(term));
    }
  }

  /**
   * Разбивает входную строку на фразы и слова
   *
   * @param str - входная строка
   * @return разбитый текст
   */

  public String[] split(String str) {
    String[] words = str.split(" ");
    String[] substrings = substrings(str);
    double[] weights = new double[substrings.length + 1]; //i-ый элемент содержит максимальный вес, который можно
                                                          //получить для str.substring(0, i)
    int[] prevs = new int[substrings.length + 1]; //i-ый элемент содержит индекс, с которого начинается
                                                  //последняя фраза при разбиении с максимальным весом
    prevs[0] = 0;
    for (int i = 0; i < substrings.length; i++) {
      //в начале для каждой строки полагаем, что лучшее разбиение для substring(0, i + 1)
      //это лучшее разбиение для substring(0, i) плюс i-е слово.
      //далее пытаемся найти максимальное разбиение с помощью фраз.
      weights[i + 1] = weights[i] + Z;
      prevs[i + 1] = i;
      String last = twoLastWords(substrings[i]);
      if (last == null)
        continue;
      Map<String, Double> map = phrases.get(last);
      if (map != null) {
        for (String phrase : map.keySet()) {
          if (endsWith(substrings[i], phrase)) {
            int prev = i + 1 - phrase.split(" ").length;
            double weight = phrases.get(last).getDouble(phrase) + weights[prev];
            if (weights[i + 1] < weight) {
              weights[i + 1] = weight;
              prevs[i + 1] = prev;
            }
          }
        }
      }
    }
    //получаем конкретное разбиение,
    //восстанавливая слова с помощью prevs
    LinkedList<String> list = new LinkedList<>();
    int index = weights.length - 1;
    while (index > 0) {
      StringBuilder sb = new StringBuilder();
      int prev = prevs[index];
      sb.append(words[prev]);
      while (++prev < index) {
        sb.append(" ").append(words[prev]);
      }
      list.offerFirst(sb.toString());
      index = prevs[index];
    }
    String[] res = new String[list.size()];
    list.toArray(res);
    return res;
  }

  /**
   * Возвращает два последних слова строки.
   *
   * @param str - входная строка
   * @return строку с двумя последними словами
   */
  private static String twoLastWords(String str) {
    String[] strings = str.split(" ");
    if (strings.length == 1)
      return null;
    return strings[strings.length - 2] + " " + strings[strings.length - 1];
  }

  /**
   *
   * Возвращает массив подстрок, начинающихся с первого слова.
   *
   * @param str - входная строка
   * @return массив всех подстрок
   */

  private static String[] substrings(String str) {
    String[] words = str.split(" ");
    String[] res = new String[words.length];
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < words.length; i++) {
      sb.append(words[i]).append(" ");
      res[i] = sb.toString().trim();
    }
    return res;
  }

}
