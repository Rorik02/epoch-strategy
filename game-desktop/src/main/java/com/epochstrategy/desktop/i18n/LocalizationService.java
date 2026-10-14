
package com.epochstrategy.desktop.i18n;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class LocalizationService {

    private static final String LANGUAGE_KEY = "language";

    private final Preferences preferences =
            Preferences.userNodeForPackage(LocalizationService.class);

    private Locale locale;
    private ResourceBundle bundle;

    public LocalizationService() {
        String savedLanguage = preferences.get(LANGUAGE_KEY, "en");

        if (!savedLanguage.equals("en") && !savedLanguage.equals("pl")) {
            savedLanguage = "en";
        }

        setLanguage(savedLanguage);
    }

    public void setLanguage(String languageCode) {

        if (!languageCode.equals("en") && !languageCode.equals("pl")) {
            throw new IllegalArgumentException(
                    "Unsupported language: " + languageCode
            );
        }

        locale = Locale.forLanguageTag(languageCode);

        bundle = ResourceBundle.getBundle(
                "i18n.messages",
                locale,
                ResourceBundle.Control.getNoFallbackControl(
                        ResourceBundle.Control.FORMAT_DEFAULT
                )
        );

        preferences.put(LANGUAGE_KEY, languageCode);
    }

    public String getText(String key) {
        return bundle.getString(key);
    }

    public String getLanguageCode() {
        return locale.getLanguage();
    }
}
