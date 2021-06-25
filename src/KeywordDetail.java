import java.io.Serializable;

public class KeywordDetail implements Serializable   {
    int id;
    int urlId;
    String type;

    public KeywordDetail(int id, int urlId, String type) {
        this.id = id;
        this.urlId = urlId;
        this.type = type;
    }

}
