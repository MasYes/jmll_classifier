package com.spbsu.exp;

import com.spbsu.wiki.Rocchio;
import com.spbsu.wiki.SimpleSplitter;
import com.spbsu.wiki.WeighedSplitter;
import junitparams.JUnitParamsRunner;

import com.spbsu.commons.seq.IntSeq;
import com.spbsu.wiki.WikiUtils;
import junit.framework.TestCase;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;


import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;

import java.io.*;
import java.util.*;


@RunWith(JUnitParamsRunner.class)

public class WikiTest {

  @Test
//  @Ignore
  public void splittingIntoSentences(){
    String[] sentences = WikiUtils.splitIntoSentences("It is the first sentence. It is the second U.S. sentence.And this is the third sentence.");
    assertEquals(3, sentences.length);
    assertEquals("It is the first sentence", sentences[0]);
    assertEquals("It is the second U.S. sentence", sentences[1]);
    assertEquals("And this is the third sentence", sentences[2]);
  }

  @Test
//  @Ignore
  public void removingPunctuations(){
    String result = WikiUtils.removePunctuation("There's!?/\" no: spoon-! 3.1415");
    assertEquals("there's no: spoon- 3.1415", result);

  }

  @Test
  @Ignore
  public void stringToIntSeq(){
    //
    IntSeq expected = new IntSeq(new int[]{1,2,1,3,1,4,2});
    IntSeq actual = WikiUtils.stringToIntSeq("a b a c a d b");
    assertEquals(expected, actual);
    Map<String, Integer> indexes = WikiUtils.getIndexes();
    HashMap<String, Integer> expectedMap = new HashMap<>();
    expectedMap.put("a", 1);
    expectedMap.put("b", 2);
    expectedMap.put("c", 3);
    expectedMap.put("d", 4);
    assertEquals(expectedMap, indexes);
  }

  @Test
//  @Ignore
  public void simpleSplitter(){

    final String test = "a b c d a b c b c a d b c a";

    {
      SimpleSplitter splitter = new SimpleSplitter();
      String[] result = splitter.split(test);
      assertArrayEquals(test.split(" "), result);
    }

    {
      SimpleSplitter splitter = new SimpleSplitter(new HashSet<>(Arrays.asList(new String[]{"a b c", "a b c e", "b c a", "b c a d"})));
      String[] expected = new String[]{"a b c", "d", "a b c", "b c a d", "b c a"};
      String[] result = splitter.split(test);
      assertArrayEquals(expected, result);
    }

  }

  @Test
//  @Ignore
  public void weighedSplitter(){

    final String test = "a b c d a b c b c a d b c a";

    {
      WeighedSplitter splitter = new WeighedSplitter();
      String[] result = splitter.split(test);
      assertArrayEquals(test.split(" "), result);
    }

    {
      HashMap<String, Double> weights = new HashMap<>();
      weights.put("a b c", 5.);
      weights.put("a b c d", 3.);
      weights.put("a b c b", 5.);
      weights.put("b c a", 8.);
      weights.put("d b c e", 15.);
      WeighedSplitter splitter = new WeighedSplitter(weights);
      String[] expected = new String[]{"a b c", "d", "a b c", "b c a", "d", "b c a"};
      String[] result = splitter.split(test);
      assertArrayEquals(expected, result);
    }

  }

  @Test
//  @Ignore
  public void rocchio() throws Exception{

    final String doc11 = "g a b c d e f";
    final String doc21 = "g b c d e f a";
    final String doc31 = "g c d e f a b";
    final String doc12 = "b b c d e f g";
    final String doc22 = "c b c d e f g";
    final String doc32 = "d b c d e f g";

    final String test = "a a a b c d e f g";

    {
      SimpleSplitter splitter = new SimpleSplitter();
      Rocchio rocchio = new Rocchio(splitter::split);
      rocchio.addDocument(doc11, 1);
      rocchio.addDocument(doc21, 1);
      rocchio.addDocument(doc31, 1);
      rocchio.addDocument(doc12, 2);
      rocchio.addDocument(doc22, 2);
      rocchio.addDocument(doc32, 2);
      assertEquals("1", rocchio.classify(test));
    }

    {
      SimpleSplitter splitter = new SimpleSplitter(new HashSet<>(Arrays.asList(new String[]{"b c d e f g"})));
      Rocchio rocchio = new Rocchio(splitter::split);
      rocchio.addDocument(doc11, 1);
      rocchio.addDocument(doc21, 1);
      rocchio.addDocument(doc31, 1);
      rocchio.addDocument(doc12, 2);
      rocchio.addDocument(doc22, 2);
      rocchio.addDocument(doc32, 2);
      assertEquals("2", rocchio.classify(test));
    }

  }

}
