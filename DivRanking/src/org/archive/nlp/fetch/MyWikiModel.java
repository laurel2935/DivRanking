package org.archive.nlp.fetch;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.WikiModel;
import info.bliki.wiki.namespaces.INamespace;

public class MyWikiModel extends WikiModel{

  public MyWikiModel(Configuration configuration, Locale locale,
    String imageBaseURL, String linkBaseURL) {
      super(configuration, locale, imageBaseURL, linkBaseURL);
  }
  public MyWikiModel(Configuration configuration,
    ResourceBundle resourceBundle, INamespace namespace,
    String imageBaseURL, String linkBaseURL) {
      super(configuration, resourceBundle, namespace, imageBaseURL, linkBaseURL);
  }
  public MyWikiModel(Configuration configuration, String imageBaseURL,
    String linkBaseURL) {
      super(configuration, imageBaseURL, linkBaseURL);
  }
  public MyWikiModel(String imageBaseURL, String linkBaseURL) {
    super(imageBaseURL, linkBaseURL);
  }

  @Override
  public String getRawWikiContent(String namespace, String articleName,
    Map<String, String> templateParameters) {
      String rawContent = super.getRawWikiContent(namespace, articleName, templateParameters);

      if (rawContent == null){
        return "";
      }
      else {
        return rawContent;
      }
    }
}
