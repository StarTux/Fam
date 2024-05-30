package com.cavetale.fam.sql;

import com.cavetale.core.skin.PlayerSkin;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;
import lombok.Data;

/**
 * This table maps unique Mojang texture URLs to their content.
 */
@Data
@Name("skins")
public final class SQLPlayerSkin implements SQLRow, PlayerSkin {
    @Id private Integer id;
    @VarChar(255) @Unique private String textureUrl;
    @Text private String textureBase64;
    @VarChar(1024) private String faceImageBase64;
    private transient BufferedImage image;
    private transient BufferedImage faceImage;

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
            url = new URI(textureUrl).toURL();
        } catch (MalformedURLException murle) {
            throw new UncheckedIOException(murle);
        } catch (URISyntaxException urise) {
            throw new IllegalStateException(urise);
        }
        try {
            image = ImageIO.read(url);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        textureBase64 = imageToBase64(image);
        getFaceImage();
    }

    public static String imageToBase64(BufferedImage theImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(theImage, "png", baos);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
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

    public BufferedImage getFaceImage() {
        if (faceImage == null) {
            getImage();
            faceImage = new BufferedImage(8, 8, image.getType());
            Graphics2D gfx = (Graphics2D) faceImage.getGraphics();
            gfx.drawImage(image, // Face
                          0, 0, 8, 8, // dest
                          8, 8, 16, 16, // src
                          null);
            gfx.drawImage(image, // Helmet
                          0, 0, 8, 8, // dest
                          40, 8, 48, 16, // src
                          null);
            gfx.dispose();
            faceImageBase64 = imageToBase64(faceImage);
        }
        return faceImage;
    }

    @Override
    public String getFaceBase64() {
        return imageToBase64(getFaceImage());
    }

    @Override
    public BufferedImage getTextureImage() {
        return image;
    }
}
