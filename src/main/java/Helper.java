import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaInfo;
import ws.schild.jave.MultimediaObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

public class Helper {
    /**
     *
     * @param originalFile Local Unconverted File
     * @return boolean (If the file is audio or not)
     * @throws EncoderException
     */
    public static boolean hasVideo(File originalFile) throws EncoderException {
        MultimediaInfo vidInfo = new MultimediaObject(originalFile).getInfo();
        return (vidInfo.getVideo() != null);
    }

    /**
     *
     * @param originalFile URL of Unconverted File
     * @return boolean (If the file is audio or not)
     * @throws EncoderException
     */
    public static boolean hasVideo(URL originalFile) throws EncoderException {
        MultimediaInfo vidInfo = new MultimediaObject(originalFile).getInfo();
        return (vidInfo.getVideo() != null);
    }

    public static Runnable addFile(File originalFile, HashMap<String, String> optionsMap, CloudConvert convertClient) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    convertClient.convertFile(originalFile, optionsMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public static Runnable addFile(String originalFile, HashMap<String, String> optionsMap, CloudConvert convertClient) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    convertClient.convertFile(originalFile, optionsMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
