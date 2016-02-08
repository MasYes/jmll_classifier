package com.spbsu.wiki;

import com.spbsu.commons.io.codec.seq.DictExpansion;
import com.spbsu.commons.io.codec.seq.Dictionary;
import com.spbsu.commons.seq.IntSeq;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.*;
import java.util.*;


public class CreateWikiWordDict {

  public static void main(String[] args) throws Exception {

    if (args[0].equals("s")) { //split into sentences
      //args = [s, path input file, path output file]
      final FileWriter fw = new FileWriter(args[2]);
      BufferedReader reader = new BufferedReader(new FileReader(args[1]));
      String line;
      int n = 0;
      while ((line = reader.readLine()) != null) {
        if (n++ % 1000 == 0)
          System.out.print("\r" + n);
        for (String s : WikiUtils.splitIntoSentences(line)) {
          fw.write(s + "\n");
        }
      }
      fw.close();
    }
    if (args[0].equals("rp")) { //remove punctuations
      //args = [rp, path input file, path output file]
      final FileWriter fw = new FileWriter(args[2]);
      BufferedReader reader = new BufferedReader(new FileReader(args[1]));
      String line;
      while ((line = reader.readLine()) != null) {
        fw.write(WikiUtils.removePunctuation(line) + "\n");
      }
      fw.close();
    }
    if (args[0].equals("cts")) { //convert phrases to IntSeq
      //args = [cts, path input file, path output file, path to indexes file]
      final FileWriter fw = new FileWriter(args[2]);
      BufferedReader reader = new BufferedReader(new FileReader(args[1]));
      String line;
      while ((line = reader.readLine()) != null) {
        IntSeq seq = WikiUtils.stringToIntSeq(line.toLowerCase());
        StringBuilder sb = new StringBuilder();
        for (int i : seq.arr) {
          sb.append(i).append(" ");
        }
        fw.write(sb.toString().trim() + "\n");
      }
      WikiUtils.writeIndexesInFile(new File(args[3]));
      fw.close();
    }
    if (args[0].equals("cd")) { //coding
      //args = [cd, path input file, path output file]
      final DictExpansion<Integer> expansion = new DictExpansion<>((Dictionary<Integer>) Dictionary.EMPTY, 500000, System.out);
      WikiUtils.setDefaultCoding(expansion);
      BufferedReader bf = new BufferedReader(new FileReader(args[1]));
      String line;
      String path = args[3].substring(0, args[3].lastIndexOf("."));
      int iter = 0;
      while ((line = bf.readLine()) != null) {
        IntArrayList doc = new IntArrayList();
        for (String i : line.split("\\s"))
          doc.add(Integer.parseInt(i));
        expansion.accept(new IntSeq(doc.toIntArray()));
        if (++iter % 20000000 == 0) {
          System.out.println("Number of iterations: " + iter);
          Dictionary<Integer> dictionary = WikiUtils.getCurrentDictionary();
          FileWriter fw = new FileWriter(path + "_" + iter + ".dict");
          for (int j = 0; j < dictionary.alphabet().size(); j++) {
            Object sequence = dictionary.get(j);
            String str = "";
            IntSeq seq = (IntSeq) sequence;
            if (seq.length() > 1) {
              for (int i = 0; i < seq.length(); i++)
                str += seq.at(i) + " ";
              fw.write(str.trim() + "\t" + expansion.resultFreqs()[j] + "\n");
            }
          }
          fw.close();
        }
      }
    }
    if (args[0].equals("ctw")) { //convert dictionary to words
      //args = [cd, path input file, path output file, path to indexes file]
      WikiUtils.readIndexes(new File(args[3]));
      BufferedReader bf = new BufferedReader(new FileReader(args[1]));
      String line;
      FileWriter fw = new FileWriter(args[2]);
      while ((line = bf.readLine()) != null) {
        IntArrayList doc = new IntArrayList();
        for (String i : line.split("\t")[0].split("\\s"))
          doc.add(Integer.parseInt(i));
        String text = WikiUtils.intSeqToWords(doc.toIntArray());
        fw.write(text + "\t" + line.split("\t")[1] + "\n");
      }
      fw.close();
    }

    if (args[0].equals("td")) { //test dictionary
      //args = ["td", path to the dictionary/name of the dictionary]

      final String resources = "experiments/src/test/resources";

      if(!args[1].contains("/"))
        args[1] = resources + "/dictionaries/" + args[1];

      //Можно выбрать любой, SimpleSplitter или WeighedSplitter;
      //первый работает быстрее, хотя второй должен выдавать более
      //качественный результат; также я думаю, что его можно ускорить

      SimpleSplitter usualDictionary = new SimpleSplitter();
      SimpleSplitter extendedDictionary = new SimpleSplitter(WikiUtils.readDictionary(new File(args[1])));

//      WeighedSplitter usualDictionary = new WeighedSplitter();
//      WeighedSplitter extendedDictionary = new WeighedSplitter(WikiUtils.readDictionaryWithWeights(new File(args[1])));

      String collections[] = new String[]{
              "20ng", "r52", "r8", "mini20"
      };

      for (String collection : collections) {
        System.out.println("Collection: " + collection);

        Rocchio usual = new Rocchio(usualDictionary::split);//классификатор с обычным словарём
        Rocchio extended = new Rocchio(extendedDictionary::split);//классификатор с расширенным словарём

        BufferedReader reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(
                resources + "/datasets/" + collection + "-train.txt.bz2"
        ))));
        String line;
        while ((line = reader.readLine()) != null) {
          String cls = line.split("\t")[0];
          String body = line.split("\t")[1];
          usual.addDocument(body, cls);
          extended.addDocument(body, cls);
        }
        usual.buildClassifier();
        extended.buildClassifier();
        reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(
                resources + "/datasets/" + collection + "-test.txt.bz2"
        ))));

        int numberOfDocuments = 0;
        int usualCorrect = 0; // число верных классификаций с обычным словарем
        int extendedCorrect = 0; // число верных классификаций с расширенным словарем
        int confidence = 0; // число совпадений ответов обоих классификаторов

        while ((line = reader.readLine()) != null) {
          String cls = line.split("\t")[0];
          String body = line.split("\t")[1];
          String usualPrediction = usual.classify(body);
          String extendedPrediction = extended.classify(body);
          numberOfDocuments++;

          if (usualPrediction.equals(cls))
            usualCorrect++;

          if (extendedPrediction.equals(cls))
            extendedCorrect++;

          if (usualPrediction.equals(extendedPrediction))
            confidence++;
        }
        System.out.println("Extended dictionary correct : " + 1. * extendedCorrect / numberOfDocuments);
        System.out.println("Usual dictionary correct : " + 1. * usualCorrect / numberOfDocuments);
        System.out.println("Confidence : " + 1. * confidence / numberOfDocuments + "\n");
      }
      System.out.println("Used : " + Rocchio.usedSize()); //число использованных последовательностей из расширенного словаря

    }

  }
}
