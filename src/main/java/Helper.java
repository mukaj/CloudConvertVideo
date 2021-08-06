import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaInfo;
import ws.schild.jave.MultimediaObject;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

public class Helper {
    /**
     *
     * @param originalFile Local Unconverted File
     * @return boolean (If the file is audio or not)
     */
    public static boolean hasVideo(File originalFile) throws EncoderException {
        MultimediaInfo vidInfo = new MultimediaObject(originalFile).getInfo();
        if(vidInfo.getVideo() == null)
            return false;
        // Some mp4 files have video, but it's non-existent and the bitrate is usually <= 0
        else if(vidInfo.getVideo().getBitRate() <= 0)
            return false;
        else
            return true;
    }

    /**
     *
     * @param originalFile URL of Unconverted File
     * @return boolean (If the file is audio or not)
     */
    public static boolean hasVideo(URL originalFile) throws EncoderException {
        MultimediaInfo vidInfo = new MultimediaObject(originalFile).getInfo();
        if(vidInfo.getVideo() == null)
            return false;
        // Some mp4 files have video, but it's non-existent and the bitrate is usually <= 0
        else if(vidInfo.getVideo().getBitRate() <= 0)
            return false;
        else
            return true;
    }

    public static Runnable addFile(File originalFile, HashMap<String, String> optionsMap, CloudConvert convertClient) {
        return () -> {
            try {
                convertClient.convertFile(originalFile, optionsMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    public static Runnable addFile(String originalFile, HashMap<String, String> optionsMap, CloudConvert convertClient) {
        return () -> {
            try {
                convertClient.convertFile(originalFile, optionsMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
