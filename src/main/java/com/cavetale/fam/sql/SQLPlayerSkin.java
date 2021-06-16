package com.cavetale.fam.sql;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

/**
 * This table maps unique Mojang texture URLs to their content.
 */
@Data @Table(name = "skins")
public final class SQLPlayerSkin {
    @Id
    private Integer id;
    @Column(length = 255, unique = true)
    private String textureUrl;
    @Column(length = 4096)
    private String textureBase64;
    private transient BufferedImage image;

    public SQLPlayerSkin() { }

    public SQLPlayerSkin(final String textureUrl) {
        this.textureUrl = textureUrl;
    }

    /**
     * Load the texture from the web and store it as Base64 in the
     * appropriate field. Also save it to disk in an appropriately
     * named png file.
     *
     * Blocking! Never run in main thread.
     */
    public void loadTexture() {
        URL url;
        try {
            url = new URL(textureUrl);
        } catch (MalformedURLException murle) {
            throw new UncheckedIOException(murle);
        }
        try {
            image = ImageIO.read(url);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        textureBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public String getFilename() {
        int index = textureUrl.lastIndexOf("/");
        return textureUrl.substring(index + 1);
    }

    /**
     * Lazy-loading image getter. It is wrong to call this if the
     * textureBase64 field is not set correctly.
     */
    public BufferedImage getImage() {
        if (image == null) {
            byte[] bytes = Base64.getDecoder().decode(textureBase64);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try {
                image = ImageIO.read(bais);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
        return image;
    }

    public void saveToDisk(final File folder) {
        File file = new File(folder, getFilename() + ".png");
        try {
            ImageIO.write(getImage(), "png", file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
