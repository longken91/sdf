package com.ltao.pdict.parser.stardict;



import com.ltao.pdict.parser.stardict.dictionary.DictionaryDefinitions;
import com.ltao.pdict.parser.stardict.dictionary.DictionaryIndex;
import com.ltao.pdict.parser.stardict.dictionary.DictionaryInfo;
import com.ltao.pdict.parser.stardict.dictionary.IndexEntry;
import com.ltao.pdict.parser.stardict.parsers.IdxParser;
import com.ltao.pdict.parser.stardict.parsers.IfoParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;

public class Dictionary {

    protected final DictionaryDefinitions definitions;
    protected final DictionaryIndex index;
    protected final DictionaryInfo info;

    /**
     * Make a dictionary, using the ifo at path
     *
     * The remaining files (minimally an idx and a dict or dict.dz) should be in the same directory, with the same name,
     * but their respective extensions.
     *
     * @param path
     * @return
     */
    public static Dictionary fromIfo(String path) throws IOException, DataFormatException {
        File ifo = new File(path);
        String abs = ifo.getAbsolutePath();

        if (!ifo.isFile() || !ifo.exists()) {
            throw new IllegalArgumentException("File at path is not a file, or does not exist.");
        }

        String ifoName = ifo.getName();
        int dotIndex = ifoName.lastIndexOf(".");
        if (dotIndex < 0 || !ifoName.substring(dotIndex + 1).equals("ifo")) {
            throw new IllegalArgumentException("File at path must be a .ifo file.");
        }

        String ifoPath = ifo.getParentFile().getAbsolutePath();
        String name = ifoName.substring(0, ifoName.lastIndexOf("."));

        File index = new File(ifoPath, name + ".idx");
        boolean hasIdx = index.exists() && index.isFile();

        if (!hasIdx) {
            throw new IllegalArgumentException("Idx file does not exist");
        }

        File dict = new File(ifoPath, name + ".dict");
        boolean hasDict = false;
        hasDict = dict.exists() && dict.isFile();

        if (!hasDict) {
            dict = new File(ifoPath, name + ".dict.dz");
            hasDict = dict.exists() && dict.isFile();
        }

        if (!hasDict) {
            throw new IllegalArgumentException("Dict file does not exist");
        }

        Reader ifoReader = null;
        DictionaryInfo dictionaryInfo = null;

        try {
            ifoReader = new InputStreamReader(new FileInputStream(ifo), StandardCharsets.UTF_8);

            IfoParser ifoParser = new IfoParser();
            dictionaryInfo = ifoParser.parse(ifoReader);
        } finally {
            if (ifoReader != null) {
                ifoReader.close();
            }
        }

        BufferedInputStream indexStream = null;
        DictionaryIndex dictionaryIndex = null;
        try {
            IdxParser idxParser = new IdxParser(dictionaryInfo);

            indexStream = new BufferedInputStream(new FileInputStream(index));
            dictionaryIndex = idxParser.parse(indexStream);
        } finally {
            if (indexStream != null) {
                indexStream.close();
            }
        }

        return new Dictionary(
                dictionaryInfo,
                dictionaryIndex,
                new DictionaryDefinitions(dict, dictionaryInfo)
        );
    }

    public Dictionary(DictionaryInfo info, DictionaryIndex index, DictionaryDefinitions definitions) {
        this.info = info;
        this.index = index;
        this.definitions = definitions;
    }

    public String getName() {
        return info.getName();
    }

    public long getWordCount() {
        return info.getWordCount();
    }

    public boolean containsWord(String word) {
        return index.containsWord(word);
    }

    public List<String> getDefinitions(String word) throws DataFormatException, IOException {
        List<IndexEntry> entries = index.getWordEntries(word.toLowerCase());

        if (entries.size() == 0) {
            return Collections.emptyList();
        } else {
            return definitions.getDefinitions(entries.get(0));
        }
    }

    public List<String> getWords() {
        return index.getWords();
    }

    public List<String> searchForWord(String search) {
        String searchLower = search.toLowerCase();
        List<String> words = index.getWords();

        List<String> results = new ArrayList<>();
        for (String word : words) {
           if (word.toLowerCase().equals(searchLower) || word.toLowerCase().startsWith(searchLower)) {
               results.add(word);
           }
        }

        return results;
    }
}
