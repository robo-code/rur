package rossum;

import net.sf.robocode.io.Logger;
import rossum.marius.MariusLogic;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class VisualUtils {
    public static BufferedImage create() {
        return new BufferedImage(MariusLogic.BATTLEFIELD_WIDTH, MariusLogic.BATTLEFIELD_WIDTH, BufferedImage.TYPE_INT_ARGB);
    }

    public static void save(BufferedImage screenshot, String filename) {
        File file = new File(filename);
        FileImageOutputStream output = null;
        ImageWriter writer = null;

        Logger.logMessage("Saved screenshot to " + file.getAbsolutePath());

        try {
            // Instantiate an ImageWriteParam object with default compression options
            Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName(filename.substring(filename.lastIndexOf(".") + 1));

            writer = (ImageWriter) it.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();

            // If compression is supported, then set the compression mode
            if (iwp.canWriteCompressed()) {
                // Use explicit compression mode
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                // Set compression quality, where 1 specifies minimum compression and maximum quality
                iwp.setCompressionQuality(1); // float between 0 and 1
            }
            // Write the screen shot to file
            output = new FileImageOutputStream(file);
            writer.setOutput(output);
            IIOImage image = new IIOImage(screenshot, null, null);

            writer.write(null, image, iwp);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.dispose();
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
