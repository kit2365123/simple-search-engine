import java.io.Serializable;
import java.util.List;

public class Webpage implements Serializable {
    String url;
    String title;
    List<Integer> wordPosition;

    public Webpage(String url, String title, List<Integer> wordPosition) {
        this.url = url;
        this.title = title;
        this.wordPosition = wordPosition;
    }
}
