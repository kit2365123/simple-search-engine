import javax.swing.text.html.parser.ParserDelegator;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CollectWords {
    public static String pageContent = "";
    public static HashMap<String, List<KeywordDetail>> keywordTable = new HashMap<>();
    public static HashMap<Integer, Webpage> webpageTable = new HashMap<>();
    public static List<String> urlPool = new ArrayList<>();
    public static List<String> processedUrl = new ArrayList<>();
    public static List<String> blackListURLs = new ArrayList<>();
    public static List<String> blackListWords = new ArrayList<>();
    public static int x;
    public static int y;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        //System.out.print("Enter the seed URL: ");
        //String url = reader.readLine();
        String url = "https://www.comp.hkbu.edu.hk";
        urlPool.add(url);
        System.out.print("Enter the size of URL pool: ");
        x = Integer.parseInt(reader.readLine());
        System.out.print("Enter the size of Processed URL pool: ");
        y = Integer.parseInt(reader.readLine());

        blackListURLs = readFileInList("blacklistUrls.txt");
        blackListWords = readFileInList("blacklistWords.txt");

        do {
            try {
                collectKeyword(urlPool.get(0));
            } catch (Exception e) {
                e.printStackTrace();
                urlPool.remove(0);
            }
        } while (!urlPool.isEmpty() && processedUrl.size() < y);
        // save the tables into tmp files
        writeHashMapFile(keywordTable, "keywordTable.tmp");
        writeHashMapFile(webpageTable, "webpageTable.tmp");

        // testing
        //System.out.println("\nTest:");
        // read the keyword table from file
        HashMap<String, List<KeywordDetail>> testKeywordTable = readHashMapFile("keywordTable.tmp");
        List<KeywordDetail> testKeywordDetail = testKeywordTable.get("hk");
        List<Integer> urlIdList = new ArrayList<>();
        for(KeywordDetail kd: testKeywordDetail) {
            urlIdList.add(kd.urlId);
            //System.out.println(kd.type + " " + kd.id + " " + kd.urlId);
        }
        // read the webpage table from file
        HashMap<Integer, Webpage> testWebpageTable = readHashMapFile("webpageTable.tmp");
        for(int urlId: urlIdList) {
            Webpage testWebpage = testWebpageTable.get(urlId);
            //System.out.println(testWebpage.title);
        }
    }

    // get and process keywords from a webpage
    static void collectKeyword(String url) throws IOException {
        System.out.println("Processing: " + url);

        pageContent = loadWebPage(url);

        MyParserCallback callback = new MyParserCallback();
        ParserDelegator parser = new ParserDelegator();
        Reader reader = new StringReader(pageContent);
        parser.parse(reader, callback, true);

        List<String> pageUrls = getURLs(url);
        String pageUrlString = "";

        for(int i=0; i<pageUrls.size(); i++)
            pageUrlString += " " + pageUrls.get(i);

        // get all the keywords with different type
        List<String> pageTitle = getUniqueWords(callback.title);
        List<String> pageText = getUniqueWords(callback.text);
        List<String> pageLinks = getUniqueWords(callback.linkText);
        List<String> pageUrlWords = getUniqueWords(pageUrlString);

        int webpageId = webpageTable.size();
        webpageTable.put(webpageId, new Webpage(url, callback.title, null));
        System.out.println(callback.title);
        webpageTable.get(0);

        // add all words to the keyword table
        for(String keyword: pageText) {
            if(!blackListWords.contains(keyword))
                addToKeywordTable(keyword, "text", webpageId);
        }
        for(String keyword: pageTitle) {
            if(!blackListWords.contains(keyword))
                addToKeywordTable(keyword, "title", webpageId);
        }
        for(String keyword: pageLinks) {
            if(!blackListWords.contains(keyword))
                addToKeywordTable(keyword, "link", webpageId);
        }
        for(String keyword: pageUrlWords) {
            if(!blackListWords.contains(keyword))
                addToKeywordTable(keyword, "url", webpageId);
        }

        // add the webpage detail to the webpages table
        webpageTable.replace(webpageId, new Webpage(url, callback.title, getPosition(getWords(callback.content))));

        // add new url from the webpage to the url pool
        for(int i=0; i<pageUrls.size(); i++) {
            if(urlPool.size() < x && !processedUrl.contains(pageUrls.get(i)) && !urlPool.contains(pageUrls.get(i)) && !isBlacklistedWebpage(pageUrls.get(i))) {
                urlPool.add(pageUrls.get(i));
            }
        }
        urlPool.remove(0);
        processedUrl.add(url);
        System.out.println("Success!");
    }

    // Download the whole page content
    static String loadWebPage(String urlString) {
        byte[] buffer = new byte[1024];
        String content = new String();
        try {
            URL url = new URL(urlString);
            InputStream in = url.openStream();
            int len;
            while((len = in.read(buffer)) != -1)
                content += new String(buffer);

        } catch (IOException e) {
            content = "<h1>Unable to download the pages</h1>" + urlString;
        }
        return content;
    }
    // get unuique words from
    public static List<String> getUniqueWords(String text) {
        String[] words = text.split("[0-9\\W]+");
        ArrayList<String> uniqueWords = new ArrayList<String>();
        for (String w : words) {
            w = w.toLowerCase();
            if (!uniqueWords.contains(w))
                uniqueWords.add(w);
        }
        return uniqueWords;
    }

    public static List<String> getWords(String text) {
        String[] words = text.split("[0-9\\W]+");
        ArrayList<String> uniqueWords = new ArrayList<String>();
        for (String w : words) {
            w = w.toLowerCase();
            uniqueWords.add(w);
        }
        return uniqueWords;
    }

    static List <String> getURLs(String pageUrl) throws IOException {
        URL url = new URL(pageUrl);
        InputStreamReader reader = new InputStreamReader(url.openStream());
        ParserDelegator parser = new ParserDelegator();
        MyParserCallback callback = new MyParserCallback();
        parser.parse(reader, callback, true);

        for (int i=0; i<callback.urls.size(); i++) {
            String str = callback.urls.get(i);
            if (!isAbsURL(str))
                callback.urls.set(i, toAbsURL(str, url).toString());
        }
        return callback.urls;
    }

    static boolean isAbsURL(String str) {
        return str.matches("^[a-z0-9]+://.+");
    }

    static URL toAbsURL(String str, URL ref) throws MalformedURLException {
        URL url = null;
        String prefix = ref.getProtocol() + "://" + ref.getHost();
        if (ref.getPort() > -1)
            prefix += ":" + ref.getPort();
        if (!str.startsWith("/")) {
            int len = ref.getPath().length() - ref.getFile().length();
            try {
                String tmp = "/" + ref.getPath().substring(0, len) + "/";
                prefix += tmp.replace("//", "/");
            } catch (Exception e) {
            }
        }
        url = new URL(prefix + str);
        return url;
    }

    static void addToKeywordTable(String word, String type, int urlId) {
        if (keywordTable.containsKey(word)) {
            List<KeywordDetail> keywordDetail = keywordTable.get(word);
            int id = keywordDetail.get(0).id;
            keywordDetail.add(new KeywordDetail(id, urlId, type));
            keywordTable.put(word, keywordDetail);
        } else {
            List<KeywordDetail> keywordDetail = new ArrayList<>();
            keywordDetail.add(new KeywordDetail(keywordTable.size(), urlId, type));
            keywordTable.put(word, keywordDetail);
        }
    }

    static List<Integer> getPosition(List<String> pageContent) {
        List<Integer> positions = new ArrayList<>();
        for(int i=0; i<pageContent.size(); i++) {
            if (keywordTable.containsKey(pageContent.get(i))) {
                positions.add(keywordTable.get(pageContent.get(i)).get(0).id);
            } else {
                positions.add(-1);
            }
        }
        return positions;
    }

    static boolean isBlacklistedWebpage(String url) {
        boolean ignore = false;
        String newUrl = url.replaceFirst("https", "http");

        for(String blackListUrl: blackListURLs) {
            String bUrl = blackListUrl.replaceFirst("https", "http");
            if(blackListUrl.endsWith("*")) {
                bUrl = bUrl.replaceAll("\\*", "");
                if(newUrl.replaceAll("/", "").contains(bUrl.replaceAll("/", "")))
                    ignore = true;
            } else {
                if(newUrl.replaceAll("/", "").equals(bUrl.replaceAll("/", "")))
                    ignore = true;
            }
        }
        return ignore;
    }

    public static List<String> readFileInList(String fileName) {
        List<String> lines = Collections.emptyList();
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    static void writeHashMapFile(HashMap hashMap, String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(hashMap);
        oos.close();
    }

    static HashMap readHashMapFile(String filename) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(filename);
        ObjectInputStream in = new ObjectInputStream(fis);
        HashMap hashMap = (HashMap) in.readObject();
        return hashMap;
    }

}
