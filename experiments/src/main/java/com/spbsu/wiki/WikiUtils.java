package com.spbsu.wiki;

import com.spbsu.commons.func.Processor;
import com.spbsu.commons.io.codec.seq.DictExpansion;
import com.spbsu.commons.io.codec.seq.Dictionary;
import com.spbsu.commons.math.vectors.impl.vectors.SparseVec;
import com.spbsu.commons.seq.IntSeq;
import com.spbsu.commons.seq.Seq;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Юлиан on 05.10.2015.
 */
public class WikiUtils {

    private static DictExpansion<Integer> coding;

    private static Pattern pattern;

    static
    {
        String regexp = "\\p{javaSpaceChar}|" +
                "\\([^()]+?\\)|" +
//                "[,“”!?\"/\\(\\)\\[\\]<>\\\\;&]"
                "[,“”!?\"/\\|\\(\\)\\[\\]<>\\\\;&#_\\*~…×♥♣■°″→«»•—─²^§¿̀„]|" + //“lavelaer”
                "[\\x{4E00}-\\x{9FA5}\\x{3041}-\\x{3096}\\x{30A0}-\\x{30FF}" +
                "\\x{3400}-\\x{4DB5}\\x{4E00}-\\x{9FCB}\\x{F900}-\\x{FA6A}\\x{2E80}-\\x{2FD5}" +
                "\\x{FF5F}-\\x{FF9F}\\x{3000}-\\x{303F}\\x{31F0}-\\x{31FF}\\x{3220}-\\x{3243}" +
                "\\x{3280}-\\x{337F}]+"
                ;
        pattern = Pattern.compile(regexp);

    }

    private WikiUtils(){

    }

    public static byte[] intToByteArray(int value){
        byte[] result = new byte[4];
        for(int i = 0; i < 4; i++)
            result[i] = (byte)(value >> i*8);
        return result;
    }

    public static int[] byteArrayToIntArray(byte[] array){
        int[] result = new int[array.length/4];
        for(int i = 0; i < array.length/4; i++)
            for(int j = 0; j < 4; j++){
                result[i] = result[i] | (int)(array[4*i + j] & 0xff) << 8*j; // It's not redundant!!!
            }
        return result;
    }

    public static byte[] intArrayToByteArray(int[] array){
        byte[] result = new byte[array.length*4];
        for(int i = 0; i < array.length; i++){
            byte[] bytes = intToByteArray(array[i]);
            System.arraycopy(bytes, 0, result, i*4, 4);
        }
        return result;
    }

    public static String[] splitIntoSentences(String text){
        ArrayList<String> sentences = new ArrayList<>();
        sentences.add(text.replaceAll("\\.\"", "\\.\" "));
        ArrayList<String> result = new ArrayList<>();
        String[] separators = new String[]{". ", ".\" ", ".” "};
        for(String sep : separators) {
            for(String str : sentences) {
                int index = str.indexOf(sep);
                while (index != -1) {
                    String supposition = str.substring(0, index);
                    if (checkSentence(supposition)) {
                        result.add(supposition);
                        str = str.substring(index + 2);
                        index = str.indexOf(sep);
                    } else {
                        index = str.indexOf(sep, index + 2);
                    }
                }
                if(str.endsWith("."))
                    result.add(str.substring(0, str.length() - 1));
                else
                    result.add(str);
            }
            sentences = result;
            result = new ArrayList<>();
        }

        for(String str : sentences) {
            int index = str.indexOf(".");
            while(index != -1) {
                if (str.substring(0, index).matches(".*[a-zA-Z]{3,}\\)?") &&
                        str.substring(index + 1).matches("[a-zA-Z]+.*") &&
                        !str.substring(index + 1).endsWith("com") &&
                        !str.substring(index + 1).endsWith("net") &&
                        !str.substring(index + 1).endsWith("de") &&
                        !str.substring(index + 1).endsWith("com") &&
                        !str.substring(index + 1).endsWith("exe") &&
                        !str.substring(index + 1).endsWith("jpg")) {
                    result.add(str.substring(0, index).trim());
                    str = str.substring(index + 1);
                }
                index = str.indexOf(".", index + 1);
                if(index > 10000)
                    break;
            }
            result.add(str.trim());
        }
        return result.toArray(new String[result.size()]);
    }

    public static SparseVec convertMapToVec(Map<Integer, Double> map){
        IntArrayList indices = new IntArrayList();
        DoubleArrayList values = new DoubleArrayList();
        for(int key : map.keySet()){
            indices.add(key);
            values.add(map.get(key));
        }
        return new SparseVec(0, indices.toIntArray(), values.toDoubleArray()); //Is 0 allowed?
    }

    private static boolean checkSentence(String sentence){
        return sentence.length() > 10 &&
                !sentence.endsWith(" e.i") &&
                !sentence.endsWith(" (b") &&
                !sentence.endsWith(" (d") &&
                !sentence.endsWith(" (cf") &&
                !sentence.endsWith(" e.g") &&
                !sentence.endsWith(" etc") &&
                !sentence.endsWith(" U.S") &&
                !sentence.endsWith(" Jr") &&
                !sentence.endsWith(" Mr") &&
                !sentence.endsWith(" Ms") &&
                !sentence.endsWith(" St") &&
                !sentence.endsWith(" Dr") &&
                !sentence.endsWith(" Co") &&
                !sentence.endsWith(" Inc") &&
                sentence.charAt(sentence.length() - 2) != '.' &&
                sentence.charAt(sentence.length() - 2) != ' ';
    }

    public static IntSeq stringToIntSeq(String str){
        return StringToIntSeq.convert(str);
    }

    public static String removePunctuation(String str){

        // |[^\s]+\.html

        // "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}|"
        if(str.length() > 1 && str.charAt(str.length() - 1) == '.')
            str = str.substring(0, str.length() - 1);
        str = str.toLowerCase().replaceAll("-{2,}", " ");
        String[] specTerms = new String[]{
                "en", "de", "fr", "it", "es", "eo", "nl", "pl", "er", "fi", "ja", "ru", "bg", "tr",
                "image", "file", "user", "wikt", "meanings", "text", "till", "talk", "follows", "template", "wikipedia",
                "ca", "into", "include"
        };

        StringBuffer sb = new StringBuffer();
        Matcher m = pattern.matcher(str);
        while(m.find()) {
            m.appendReplacement(sb, " ");
        }
        m.appendTail(sb);

        str = sb.toString().replaceAll("[’‘`′]", "\'");
        str = str.replaceAll("\\.{2,}", ".").replaceAll("\\s+", " ");
//        if(true)
//            return str;

        for(String term : specTerms){
            int index = sb.indexOf(term + ":");
            while(index != -1) {
                if(index == 0){
                    int last = sb.indexOf(" ");
                    if(last == -1)
                        last = sb.length();
                    sb.delete(0, last);
                    index = sb.indexOf(term + ":");
                }
                else{
                    if(sb.charAt(index - 1) == ':' || sb.charAt(index - 1) == ' '){
                        int first = sb.lastIndexOf(" ", index);
                        int last = sb.indexOf(" ", index);
                        if(first == -1)
                            first = 0;
                        if(last == -1)
                            last = sb.length();
                        sb.delete(first, last);
                        index = sb.indexOf(term + ":", first);
                    }
                    else{
                        index = sb.indexOf(term + ":", index + 1);
                    }
                }
            }
        }
        sb = new StringBuffer();

        for(String term : str.split("\\s")){
            if(!term.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")
                && !term.endsWith(".html"))
                sb.append(term).append(" ");
        }

        return sb.toString().replaceAll("\\s{2,}", " ").trim();
    }

    public static Map<String, Integer> getIndexes(){
        return StringToIntSeq.indexes;
    }

    public static Processor<String> getDefaultProcessor(){
        return (String text) -> {String[] sentences = WikiUtils.splitIntoSentences(text);
            for(String sentence : sentences){
                sentence = removePunctuation(sentence);
                sentence = sentence.toLowerCase();
                IntSeq seq = WikiUtils.stringToIntSeq(sentence);
                coding.accept(seq);
            }
        };
    }

    public static void readIndexes(File file) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        StringToIntSeq.clearIndexes();
        int maxIndex = 0;
        while((line = reader.readLine()) != null){
            int index = Integer.parseInt(line.split("\t")[1]);
            if(index > maxIndex){
                maxIndex = index;
                while(StringToIntSeq.reverseIndex.size() < maxIndex + 1){
                    StringToIntSeq.reverseIndex.add(null);
                }
            }
            String word = line.split("\t")[0];
//            StringToIntSeq.indexes.put(word, index);
            StringToIntSeq.reverseIndex.set(index, word);
        }
    }

    public static void writeIndexesInFile(File file) throws IOException{
        FileWriter fw = new FileWriter(file);
        Map<String, Integer> indexes = getIndexes();
        for(String key : indexes.keySet()){
            fw.write(key + "\t" + indexes.get(key) + "\n");
        }
        fw.close();
    }

    public static void setDefaultCoding(DictExpansion<Integer> expansion){
        coding = expansion;
    }

    public static Map<String, Integer> getWordsDictionary(){
        Object2IntOpenHashMap<String> result = new Object2IntOpenHashMap<>();
        for (int j = 0; j < getCurrentDictionary().size(); j++) {
            StringBuilder sb = new StringBuilder();
            Seq seq = getCurrentDictionary().alphabet().get(j);
            if (seq.length() < 2)
                continue;
            for (int t = 0; t < seq.length(); t++)
                sb.append(StringToIntSeq.reverseIndex.get((int)seq.at(t))).append(" ");
            result.put(sb.toString(), coding.resultFreqs()[j]);
        }
        return result;
    }

    public static String intSeqToWords(int ... integers){
        StringBuilder result = new StringBuilder();
        for(int i : integers){
            result.append(StringToIntSeq.reverseIndex.get(i)).append(" ");
        }
        return result.toString().trim();
    }

    public static <K, V extends Comparable> LinkedHashMap<K, V> sortByValues(Map<K, V> map){
        return sortByValues(map, false);
    }

    public static <K, V extends Comparable> LinkedHashMap<K, V> sortByValues(Map<K, V> map, boolean inverse){
        LinkedHashMap<K, V> result = new LinkedHashMap<>();
        HashMap<V, ArrayList<K>> inv = new HashMap<>();
        for(K key : map.keySet()){
            V value = map.get(key);
            if(!inv.containsKey(value))
                inv.put(value, new ArrayList<K>());
            inv.get(value).add(key);
        }
        ArrayList<V> values = new ArrayList<>(new HashSet<>(map.values()));
        if(inverse)
            Collections.sort(values, Collections.reverseOrder());
        else
            Collections.sort(values);
        for(V value : values){
            for(K key : inv.get(value))
                result.put(key, value);
        }
        return result;
    }

    public static Dictionary<Integer> getCurrentDictionary(){
        return coding.result();
    }

    public static Set<String> readDictionary(File file) throws IOException{
        HashSet<String> dictionary = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("[")) {
                if (!line.contains(","))
                    continue;
                line = line.split("\t")[0].substring(1, line.indexOf("]")).replaceAll(",", "");
            }
            dictionary.add(line.split("\t")[0]);
        }
        return dictionary;
    }

    public static Map<String, Double> readDictionaryWithWeights(File file) throws IOException{
        HashMap<String, Double> dictionary = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            double weight = Double.parseDouble(line.split("\t")[1]);
            if (line.contains("[")) {
                if (!line.contains(","))
                    continue;
                line = line.split("\t")[0].substring(1, line.indexOf("]")).replaceAll(",", "");
            }
            dictionary.put(line.split("\t")[0], weight);
        }
        return dictionary;
    }

    public static <T extends Number> double sumOfElements(Collection<T> collection){
        double res = 0;
        for(T element : collection){
            res += element.doubleValue();
        }
        return res;
    }

    private static class StringToIntSeq {

        private final static Object2IntOpenHashMap<String> indexes = new Object2IntOpenHashMap<>();
        private final static ArrayList<String> reverseIndex = new ArrayList<>();

        public static IntSeq convert(String str){
            IntArrayList result = new IntArrayList();
            for(String s : str.split(" ")){
                if(!indexes.containsKey(s)) {
                    indexes.put(s, indexes.size() + 1);
                    while(reverseIndex.size() < indexes.size() + 1) {
                        reverseIndex.add(null);
                    }
                    reverseIndex.set(indexes.size(), s);
                }
                result.add(indexes.getInt(s));

            }
            return new IntSeq(result.toIntArray());
        }

        public Object2IntOpenHashMap<String> getIndexes(){
            return indexes;
        }

        public static void clearIndexes(){
            reverseIndex.clear();
            indexes.clear();
        }

    }

    public static long twoIntsToLong(int ... ints){
        return (long) ints[0] << 32 | ints[1];
    }

    public static int[] longToTwoInts(long l){
        return new int[]{(int)(l >>> 32), (int)(l & 0xFFFFFFFFl)};
    }

}
