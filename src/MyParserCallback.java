import javax.swing.text.html.*;
import javax.swing.text.html.HTML.*;
import javax.swing.text.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

class MyParserCallback extends HTMLEditorKit.ParserCallback {
    public String content = "";
    public String text = "";
    public String title = "";
    public String linkText = "";
    public List<String> urls = new ArrayList<>();
    public boolean gotTitle = false;
    private int max = 1000;
    int startPos = -1;

    @Override
    public void handleText(char[] data, int pos) {
        if (startPos >= 0)
            startPos = pos;

        if (startPos == -1)
            text += " " + new String(data);

        content += " " + new String(data);
    }

    @Override
    public void handleStartTag(Tag tag, MutableAttributeSet attrSet, int pos)
    {
        if (tag.toString().equals("a")) {
            Enumeration e = attrSet.getAttributeNames();
            while (e.hasMoreElements()) {
                Object aname = e.nextElement();
                if (aname.toString().equals("href")) {
                    String u = (String) attrSet.getAttribute(aname);
                    if (urls.size() < max && !urls.contains(u))
                        urls.add(u);
                }
            }
            startPos = pos;
        }

        if (tag.toString().equals("title") && !gotTitle) {
            startPos = pos;
        }

    }

    @Override
    public void handleEndTag(Tag tag, int pos) {
        super.handleEndTag(tag, pos);
        if (tag.toString().equals("a")) {
            String t = CollectWords.pageContent.substring(startPos, pos);
            if (!t.contains("<") && !t.contains(">")) {
                linkText += " " + t;
                //System.out.println(ReadPage.pageContent.substring(startPos, pos));
            }
            startPos = -1;
        }

        if (tag.toString().equals("title") && !gotTitle) {
            title = CollectWords.pageContent.substring(startPos, pos);
            gotTitle = true;
            //System.out.println(title);
            startPos = -1;
        }
    }

    @Override
    public void handleEndOfLineString(String eol) {
        super.handleEndOfLineString(eol);
        //System.out.println(linkText);
    }
}