package org.archive.nlp.fetch;

import info.bliki.htmlcleaner.ContentToken;
import info.bliki.htmlcleaner.Utils;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.tags.HTMLTag;

import java.io.IOException;

public class MyHtmlConverter extends ExtendedHtmlConverter {

  public MyHtmlConverter() {
    super();
  }

  public MyHtmlConverter(boolean noLinks) {
    super(noLinks);
  }

  public MyHtmlConverter(boolean noLinks, boolean noImages) {
    super(noLinks, noImages);
  }

  protected void renderContentToken(Appendable resultBuffer,
      ContentToken contentToken, IWikiModel model) throws IOException {
    String content = contentToken.getContent();
    content = content.replaceAll("\\(,", "(").replaceAll("\\(\\)", "()");
    content = Utils.escapeXml(content, true, true, true);
    resultBuffer.append(content);
  }

  protected void renderHtmlTag(Appendable resultBuffer, HTMLTag htmlTag,
      IWikiModel model) throws IOException {
    String tagName = htmlTag.getName();
    if ((!tagName.equals("ref"))) {
      super.renderHtmlTag(resultBuffer, htmlTag, model);
    }
  }
}
