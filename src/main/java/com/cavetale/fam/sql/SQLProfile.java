package com.cavetale.fam.sql;

import com.cavetale.core.util.Json;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This table is a mapping of player UUIDs to their profile.
 */
@Data
@Name("profiles")
public final class SQLProfile implements SQLRow {
    @Id private Integer id;
    @NotNull @Unique private UUID uuid;
    @VarChar(16) private String name;
    @Text private String json;
    @VarChar(255) private String textureUrl;
    @NotNull private Date updated;
    private transient Tag tag;

    /**
     * Json tag.
     */
    public static final class Tag {
        private Property textures = new Property();
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static final class Property {
        String value;
        String signature;

        public static Property of(ProfileProperty prop) {
            return new Property(prop.getValue(), prop.isSigned() ? prop.getSignature() : null);
        }

        public ProfileProperty toProfileProperty(String name) {
            return new ProfileProperty(name, value, signature);
        }
    }

    /**
     * Created from SQL module.
     */
    public SQLProfile() { }

    /**
     * New row.
     */
    public SQLProfile(final UUID uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
        this.tag = new Tag();
    }

    public void pack() {
        json = Json.serialize(tag);
        updated = new Date();
    }

    public void unpack() {
        tag = Json.deserialize(json, Tag.class, Tag::new);
    }

    public Tag getTag() {
        if (tag == null) unpack();
        return tag;
    }

    /**
     * Load data from the given profile.
     * @return true if this row was updated with new information, false otherwise.
     */
    public boolean load(PlayerProfile profile) {
        boolean result = false;
        if (tag == null) unpack();
        for (ProfileProperty prop : profile.getProperties()) {
            switch (prop.getName()) {
            case "textures":
                String newUrl = getTextureUrl(prop.getValue());
                if (!Objects.equals(textureUrl, newUrl)) {
                    Property textures = Property.of(prop);
                    tag.textures = textures;
                    textureUrl = newUrl;
                    result = true;
                }
                break;
            default: break;
            }
        }
        return result;
    }

    public void fill(PlayerProfile profile) {
        if (tag == null) unpack();
        profile.setProperty(tag.textures.toProfileProperty("textures"));
    }

    @SuppressWarnings("unchecked")
    public static String getTextureUrl(String textureBase) {
        if (textureBase == null) return null;
        byte[] bs = Base64.getDecoder().decode(textureBase);
        String js = new String(bs);
        Map<Object, Object> map = Json.deserialize(js, Map.class);
        if (map == null) return null;
        Object o = map.get("textures");
        if (!(o instanceof Map)) return null;
        map = (Map<Object, Object>) o;
        o = map.get("SKIN");
        if (!(o instanceof Map)) return null;
        map = (Map<Object, Object>) o;
        o = map.get("url");
        if (!(o instanceof String)) return null;
        return (String) o;
    }

    public void fetchPlayerSkinAsync() {
        if (textureUrl == null) return;
        Database.fetchPlayerSkinAsync(textureUrl);
    }
}
