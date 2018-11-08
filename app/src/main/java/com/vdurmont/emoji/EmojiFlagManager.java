package com.vdurmont.emoji;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the loaded emojis and provides search functions.
 *
 * @author Vincent DURMONT [vdurmont@gmail.com]
 */
public final class EmojiFlagManager {
    private static final String PATH = "/assets/emojis.json";
    private static final Map<String, Emoji> EMOJIS_BY_ALIAS = new HashMap<>();

    static {
        try (InputStream stream = EmojiLoader.class.getResourceAsStream(PATH)) {
            List<Emoji> emojis = EmojiLoader.loadEmojis(stream);
            for (Emoji emoji : emojis) {
                EMOJIS_BY_ALIAS.put(emoji.getAliases().get(0), emoji);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * No need for a constructor, all the methods are static.
     */
    private EmojiFlagManager() {}

    /**
     * Returns the {@link com.vdurmont.emoji.Emoji} for a given alias.
     *
     * @param alias the alias
     *
     * @return the associated {@link com.vdurmont.emoji.Emoji}, null if the alias
     * is unknown
     */
    @Nullable
    public static Emoji getForAlias(@NonNull String alias) {
        return EMOJIS_BY_ALIAS.get(alias);
    }
}
