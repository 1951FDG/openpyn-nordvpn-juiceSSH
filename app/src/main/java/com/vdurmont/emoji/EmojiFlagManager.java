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

    private final Map<String, Emoji> EMOJIS_BY_ALIAS = new HashMap<>();

    /**
     * Returns the {@link com.vdurmont.emoji.Emoji} for a given alias.
     *
     * @param alias the alias
     * @return the associated {@link com.vdurmont.emoji.Emoji}, null if the alias
     * is unknown
     */
    @Nullable
    public Emoji getForAlias(@NonNull String alias) {
        return EMOJIS_BY_ALIAS.get(alias);
    }

    public void load(@NonNull String name) throws IOException {
        try (InputStream stream = EmojiLoader.class.getResourceAsStream(name)) {
            List<Emoji> emojis = EmojiLoader.loadEmojis(stream);
            for (Emoji emoji : emojis) {
                List<String> aliases = emoji.getAliases();
                EMOJIS_BY_ALIAS.put(aliases.get(0), emoji);
            }
        } catch (IOException e) {
            throw e;
        }
    }
}
