package searchengine.services.search;

import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class XMLParser {

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    public String getTitle(String content) {
        String title = "";
        String patternString = "<title>(.*?)</title>";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            title = matcher.group();
        }
        title = title.replaceAll("<title>","");
        title = title.replaceAll("</title>", "");
        return title;
        //for testing branch 2
    }

    public String parseContent(String content, String query) {
        String noScriptHtml = content.replaceAll("(?s)<script.*?>.*?</script>", " ");
        String noHtml = noScriptHtml.replaceAll("<[^А-Яа-я>]*>", " ");
        String noEntities = noHtml.replaceAll("&nbsp;", " ");
        String noArrows = noEntities.replaceAll("---->", " ");
        String normalizedText = noArrows.replaceAll("\\s+", " ").trim();
        String sentenceRegex = "[^.!?]+";
        Pattern sentencePattern = Pattern.compile(sentenceRegex);
        Matcher sentenceMatcher = sentencePattern.matcher(normalizedText);
        StringBuilder builder = new StringBuilder();
        while (sentenceMatcher.find()) {
            builder.append(sentenceMatcher.group().trim()).append(" ");
        }
        return getSnippet(refine(builder.toString()), query).trim();
    }

    private String refine(String content) {
        String[] words = content.split(" ");
        StringBuilder builder = new StringBuilder();
        String word;
        for (String s : words) {
            word = s.replaceAll("[^А-Яа-я0-9-():\\s]*", "");
            if (word.length() > 1) {
                builder.append(word).append(" ");
            } else {
                if (word.length() == 1) {
                    if (Character.isDigit(word.charAt(0)) || Character.isLetter(word.charAt(0))) {
                        builder.append(word).append(" ");
                    }
                }
            }
        }
        return builder.toString().trim();
    }

    private String getSnippet(String content, String query) {
        int wordLimit = 5;
        String[] queryWords = query.split(" ");
        String[] contentWords = content.split(" ");
        int queryIndex = -1;
        for (int i = 0; i < contentWords.length; i++) {
            boolean match = false;
            for (String queryWord : queryWords) {
                if (contentWords[i].equalsIgnoreCase(queryWord)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                queryIndex = i;
                break;
            }
        }
        if (queryIndex == -1) {
            return "";
        }
        int start = Math.max(0, queryIndex - wordLimit);
        int end = Math.min(contentWords.length, queryIndex + queryWords.length + wordLimit);
        StringBuilder snippetBuilder = new StringBuilder();
        snippetBuilder.append("<b>");
        for (int i = start; i < end; i++) {
            snippetBuilder.append(contentWords[i]).append(" ");
        }
        snippetBuilder.append("</b>");
        return snippetBuilder.toString().trim();
    }

}
