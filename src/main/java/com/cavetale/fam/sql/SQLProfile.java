package com.cavetale.fam.sql;

import com.cavetale.fam.util.Json;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.winthier.sql.SQLRow;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This table is a mapping of player UUIDs to their profile.
 */
@Data @Table(name = "profiles")
public final class SQLProfile implements SQLRow {
    @Id
    private Integer id;
    @Column(nullable = false, unique = true)
    private UUID uuid;
    @Column(length = 16)
    private String name;
    @Column(length = 4096)
    private String json;
    @Column(length = 255)
    private String textureUrl;
    @Column(nullable = false)
    private Date updated;
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

    public static SQLProfile of(PlayerProfile profile) {
        SQLProfile row = new SQLProfile(profile.getId(), profile.getName());
        row.load(profile);
        return row;
    }

    /**
     * Load data from the given profile.
     * @return true if this row was updated with new information, false otherwise.
     */
    public boolean load(PlayerProfile profile) {
        boolean result = false;
        if (!Objects.equals(name, profile.getName())) {
            name = profile.getName();
            result = true;
        }
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
