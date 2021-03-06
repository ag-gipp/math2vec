package org.nii.math.post.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractDataProcessor {
    private static final Logger LOG = LogManager.getLogger(AbstractDataProcessor.class.getName());

    public static final String NL = System.lineSeparator();

    public static final Pattern LLAMAPUNG_PATTERN =
            Pattern.compile(".*<annotation.*?encoding=\"application/x-llamapun\".*?>(.*?)<.*");

    public static final Pattern ABBR_PATTERN =
            Pattern.compile(".*title=\"(.*?)\".*");

    public static final Pattern TOKEN_PATTERN =
            Pattern.compile(
                            "(__H\\d+_\\d+__)|" + // group 1
                            "(__MATH_\\d+__)|" + // group 2 ...
                            "(__ABBR_\\d+__)|" + // 3
                            "(__CITE_\\d+__)|" + // 4
                            "(__UL_\\d+__)|" + // 5
                            "([\\s]{2,})|" + // 6
                            "(\\t+)");      // 7

    public static final Pattern LLAMAPUN_SUBSCRIPT_PATTERN =
            Pattern.compile("(\\w:.*?)\\s+POSTSUBSCRIPT:start\\s+(.*?)\\s+POSTSUBSCRIPT:end");

    public static final Pattern MATH_PATTERN =
            Pattern.compile("(__MATH_\\d+__)");

    public static int LOWER_LIMIT_LINE_LENGTH = 35;

    private Path annFile, txtFile, outFile;

    private Path origIn;

    private HashMap<String, String> annotations;

    protected void loadInitFiles(Path inDir, Path outDir, Path file){
        origIn = file;
        String filename = file.getFileName().toString().split("\\.txt")[0];
        annFile = inDir.resolve(filename + ".ann");
        txtFile = inDir.resolve(filename + ".txt");
        outFile = outDir.resolve(filename + ".txt");
    }

    protected Path getAnnFile() {
        return annFile;
    }

    protected Path getTxtFile() {
        return txtFile;
    }

    protected Path getOutFile() {
        return outFile;
    }

    public Path getOrigIn() {
        return origIn;
    }

    protected void loadAnnotations() throws RuntimeException {
        annotations = new HashMap<>();

        LOG.debug("Load annotations for Ann-File: " + getAnnFile().toString());
        try ( Stream<String> lineStream = Files.lines(getAnnFile()) ) {
            String[] lastTag = new String[]{""};
            lineStream.forEach( l -> {
                if ( l.matches("[\\s\\t]*") ){ // empty annotation file
                    LOG.debug("Skip because of empty annotation file.");
                    throw new BreakException(); // BAD PRACTICE! but it seems to be the only way to terminate forEach...
                } else if ( l.startsWith("T") ){
                    lastTag[0] = l.split("\t")[2];
                } else if ( l.startsWith("#") ) {
                    String data = l.split("\t")[2];
                    if (lastTag[0].matches("__MATH.*")) { // math token
                        Matcher m = LLAMAPUNG_PATTERN.matcher(data);
                        if (m.matches()) {
                            annotations.put(lastTag[0],
                                    tagByMath(m.group(1)));
                        } else {
                            LOG.error("MML doesn't match LLAMAPUN pattern. Ann-File: " + annFile.toString() +
                                    ", Token: " + lastTag[0]);
                        }
                    } else if (lastTag[0].matches("__ABBR.*")) { // abbreviation token
                        Matcher m = ABBR_PATTERN.matcher(data);
                        if (m.matches()){
                            annotations.put(lastTag[0], m.group(1));
                        } else {
                            LOG.error("ABBR doesn't match abbreviation pattern. Ann-File: " + annFile.toString() +
                                    ", Abbr: " + data);
                        }
                    } else { // header token
                        annotations.put(lastTag[0], data);
                    }
                } else {
                    throw new RuntimeException("Unknown start of annotation. No T or #? " + annFile.toString());
                }
            });
            LOG.debug("Finish loading annotations of file: " + annFile.toString());
        } catch ( BreakException be ){
            // this exception will be thrown when there is only an empty annotation file available
        } catch ( IOException ioe ){
            LOG.error("Cannot read from annotation file " + annFile.toString());
            throw new RuntimeException("Cannot read from annotation file " + annFile.toString());
        }
    }

    public static String tagByMath(String in){
        Set<String> cache = new TreeSet<>();
        String[] t = in.split(" ");

        boolean skipRELOP = false;
        if (t.length > 30) skipRELOP = true;

        int tokens = 0;

        for (int i = 0; i < t.length; i++) {
            String aT = t[i];

            // ignore right-hand side of math
            if ( skipRELOP && aT.matches("RELOP.*") ){
                for (int j=i; j < t.length; j++){
                    t[j] = "";
                }
                break;
            }

            t[i] = "";
            if ( aT.matches("POSTSUBSCRIPT:start")
                    && i >= 1 && !t[i-1].isEmpty()){
                StringBuffer b = new StringBuffer();
                for (int j = i+1; j < t.length; j++){
                    String bT = t[j];
                    t[j] = "";
                    if ( bT.matches("POSTSUBSCRIPT:end") ) {
                        t[i-1] = t[i-1]+"_{"+b.toString()+"}";
                        i = j;
                        break;
                    }

                    Matcher m = IgnorableLLamapunTokens.GENERAL_TOKEN_PATTERN.matcher(bT);
                    if (m.matches()){
                        if ( m.group(1) == null )
                            b.append(m.group(2));
                        else b.append(m.group(1));
                    }
                }

                continue;
            }

            if ( !IgnorableLLamapunTokens.ignore(aT)
                    && !aT.matches("ATOM:and")){
                aT = IgnoreTypes.clean(aT);
                if ( cache.contains(aT) ||
                        aT.isEmpty() ||
                        aT.matches("\\s*")
                        ) continue;
                else cache.add(aT);

                if (aT.startsWith("-"))
                    t[i] = "math"+aT;
                else t[i] = "math-"+aT;
            }
        }

        StringBuffer sb = new StringBuffer();
        for (String s : t){
            if(!s.isEmpty()) {
                tokens++;
                sb.append(s).append(" ");
            }
        }

        return sb.toString();
    }

    public abstract void start() throws RuntimeException;

    public void createOutPutFile() throws RuntimeException {
        try {
            Files.deleteIfExists(getOutFile());
            Files.createFile(getOutFile());
        } catch (IOException e) {
            LOG.error("Cannot create output file at " + getOutFile());
            throw new RuntimeException("Cannot create output file.");
        }
    }

    public String tokenReplacement( String in ) {
        if (annotations == null){
            return in;
        }

        StringBuffer sentence = new StringBuffer();
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(in);

        while(tokenMatcher.find()){
            if ( tokenMatcher.group(1) != null ){ // headers such in H1, H2, ...
                String rep = annotations.get(tokenMatcher.group(1));
                tokenMatcher.appendReplacement(sentence, rep == null ? "" : rep);
            } else if ( tokenMatcher.group(2) != null ){ // math
                String rep = annotations.get(tokenMatcher.group(2));
                tokenMatcher.appendReplacement(sentence, rep == null ? "" : rep);
            } else if ( tokenMatcher.group(3) != null ){ // abbr
                String rep = annotations.get(tokenMatcher.group(3));
                tokenMatcher.appendReplacement(sentence, rep == null ? "" : rep);
            } else if ( tokenMatcher.group(6) != null ||
                    tokenMatcher.group(7) != null ) {
                tokenMatcher.appendReplacement(sentence, " ");
            } else {
                tokenMatcher.appendReplacement(sentence, "");
            }
        }

        tokenMatcher.appendTail(sentence);
        if (sentence.toString().endsWith(":")
                || sentence.length() < 250)
            return sentence.toString() + " ";
        else return sentence.toString() + NL;
    }

    private String mathReplaceAndSplit( String paragraph ){
        String[] sentences = paragraph.split("\\.");
        StringBuilder sb = new StringBuilder();

        for ( String s : sentences ){
            LinkedList<String> replaced = mathRecursiveReplacement(s);
            StringBuffer buffer = new StringBuffer();
            while ( !replaced.isEmpty() ){
                buffer.append(replaced.removeFirst());
                if ( replaced.size() >= 1 )
                    buffer.append(". ");
            }
            sb.append(buffer).append(". ");
        }
        return sb.toString();
    }

    private LinkedList<String> mathRecursiveReplacement(String para){
        LinkedList<String> list = new LinkedList<>();
        Matcher tokenMatcher = MATH_PATTERN.matcher(para);
        Set<String> tokens = new HashSet<>();

        if ( tokenMatcher.find() ) {
            String rep = annotations.get(tokenMatcher.group(1));
            if (rep == null) rep = "";
            String[] identifiers = rep.split(" ");
            String untilHere = para.substring(0, tokenMatcher.start());
            String rest = para.substring(tokenMatcher.end(), para.length());
            LinkedList<String> innerList = mathRecursiveReplacement(rest);

            for ( int i = 0; i < identifiers.length; i++ ){
                if ( tokens.contains(identifiers[i]) ) continue;
                else tokens.add(identifiers[i]);

                if ( innerList.isEmpty() ){
                    list.add(untilHere + identifiers[i]);
                }
                for ( String in : innerList ){
                    String line = untilHere + identifiers[i] + in;
                    list.add(line);
                }
            }
        } else {
            list.add(para);
        }

        return list;
    }

    private class BreakException extends RuntimeException {}
}
