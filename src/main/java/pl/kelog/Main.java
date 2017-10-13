package pl.kelog;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

class Main {
    
    public static final String BASE_URL = "http://www.giszowiec.org";
    
    public static final String TARGET_FOLDER = "pdfs";
    
    public static final int THREAD_COUNT = 4;
    
    public static void main(String[] args) throws Exception {
        Document document = Jsoup.connect(BASE_URL + "/index.php/spiewnik").get();
        
        Elements alphabeticIndexLinks = document.select(".module-body > .custom > h5 > a");
        
        new ForkJoinPool(THREAD_COUNT).submit(
                createMainTask(alphabeticIndexLinks)
        ).get();
    }
    
    static Runnable createMainTask(Elements alphabeticIndexLinks) {
        return () -> alphabeticIndexLinks.parallelStream()
                .map(element -> BASE_URL + element.attr("href"))
                .flatMap(Main::findAllSongLinks)
                .map(Main::getPdfLink)
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .forEach(Main::downloadFileAtUrl);
    }
    
    static Stream<String> findAllSongLinks(String indexLink) {
        System.out.println("Finding song links " + indexLink);
        
        return connect(indexLink)
                .select("a.mod-articles-category-title")
                .stream()
                .map(e -> BASE_URL + e.attr("href"));
    }
    
    static Optional<String> getPdfLink(String songPageLink) {
        Elements downloadLink = connect(songPageLink).select("p > strong > a");
        
        if (downloadLink.size() == 0 || !downloadLink.attr("href").endsWith(".pdf")) {
            return Optional.empty();
        } else {
            String href = downloadLink.attr("href");
            return Optional.of(BASE_URL + href);
        }
    }
    
    static void downloadFileAtUrl(String url) {
        System.out.println("Downloading " + url);
        try {
            FileUtils.copyURLToFile(new URL(url), new File(filenameForUrl(url)));
            System.out.println("Stored " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static Document connect(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    static String filenameForUrl(String href) {
        String filename = href.substring(href.lastIndexOf("/"), href.length()).replace("%20", "_");
        return TARGET_FOLDER + File.separator + filename;
    }
}