import com.cloudconvert.client.CloudConvertClient;
import com.cloudconvert.client.setttings.StringSettingsProvider;
import com.cloudconvert.dto.request.*;
import com.cloudconvert.dto.response.JobResponse;
import com.cloudconvert.dto.response.TaskResponse;
import com.google.common.collect.ImmutableMap;
import org.apache.tika.io.IOUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ws.schild.jave.*;

public class CloudConvert {
    // !! Please put API Key for the code to run properly !!
    private static final String APIKey = "";
    private final CloudConvertClient cloudConvertClient = new CloudConvertClient(new StringSettingsProvider(APIKey, "webhook-signing-secret", false));

    public CloudConvert() throws IOException {}

    /**
     * Set properties for conversion task
     * @param task Conversion task
     * @param inOptions HashMap for all the conversion options
     * @return
     */
    private ConvertFilesTaskRequest setProperties(ConvertFilesTaskRequest task, HashMap<String, String> inOptions) {
        inOptions.forEach((key, value) ->
                task.setProperty(key, value));

        return task;
    }

    private void startConversion(String inputID, String outputType, HashMap<String, String> inOptions) throws IOException, URISyntaxException {
        // Creating the convert task request and applying the options to it.
        final ConvertFilesTaskRequest conversionTask = setProperties(new ConvertFilesTaskRequest(), inOptions);

        // Create a job to Convert and Export Video
        final JobResponse createJobResponse = cloudConvertClient.jobs().create(
                ImmutableMap.of(
                        "ConvertVideo", conversionTask
                                .setInput(inputID)
                                .setOutputFormat(outputType),
                        "ExportVideo", new UrlExportRequest().setInput("ConvertVideo")
                )).getBody();

        //Get Export TaskResponse
        final TaskResponse waitUrlExportTaskResponse = cloudConvertClient.tasks().wait(createJobResponse.getTasks().get(1).getId()).getBody();

        final String exportUrl = waitUrlExportTaskResponse.getResult().getFiles().get(0).get("url");

        // Export into file
        InputStream convertedFile = cloudConvertClient.files().download(exportUrl).getBody();
        OutputStream outputStream = null;

        // Creating an output stream to Save the converted file
        final String filename = waitUrlExportTaskResponse.getResult().getFiles().get(0).get("filename");
        File outputFile = new File("converted/", filename);
        outputFile.getParentFile().mkdir();

        outputStream = new FileOutputStream(outputFile);


        IOUtils.copy(convertedFile, outputStream);

        // Clean-up
        convertedFile.close();
        outputStream.close();
    }

    public void convertHTML5Video(File inputFile, HashMap<String, String> inOptions) throws IOException, URISyntaxException {
        final TaskResponse uploadImportTaskResponse = cloudConvertClient.importUsing().upload(new UploadImportRequest(), inputFile).getBody();
        final String fileType = inputFile.getName().substring(inputFile.getName().lastIndexOf('.') + 1);

        if(!fileType.equals("mp4")) {
            startConversion(uploadImportTaskResponse.getId(), "mp4", inOptions);
        }
        if(!fileType.equals("webm")) {
            startConversion(uploadImportTaskResponse.getId(), "webm", inOptions);
        }
        // CloudConvert doesn't Support conversions to ogv
    }

    public void convertHTML5Video(String inputURL, HashMap<String, String> inOptions) throws IOException, URISyntaxException {
        final TaskResponse uploadImportTaskResponse = cloudConvertClient.importUsing().url(new UrlImportRequest().setUrl(inputURL)).getBody();
        final String fileType = inputURL.substring(inputURL.lastIndexOf('.') + 1);

        if(!fileType.equals("mp4")) {
            startConversion(uploadImportTaskResponse.getId(), "mp4", inOptions);
        }
        if(!fileType.equals("webm")) {
            startConversion(uploadImportTaskResponse.getId(), "webm", inOptions);
        }
        // CloudConvert doesn't Support conversions to ogv
    }

    public void convertHTML5Audio(File inputFile, HashMap<String, String> inOptions) throws IOException, URISyntaxException {
        final TaskResponse uploadImportTaskResponse = cloudConvertClient.importUsing().upload(new UploadImportRequest(), inputFile).getBody();
        final String fileType = inputFile.getName().substring(inputFile.getName().lastIndexOf('.') + 1);

        // The we need to make CloudConvert think that the format is m4a because otherwise it tries to convert it as a video
        if(fileType.equals("mp4"))
            inOptions.put("input_format", "m4a");

        if(!fileType.equals("aac")) {
            startConversion(uploadImportTaskResponse.getId(), "aac", inOptions);
        }
        if(!fileType.equals("mp3")) {
            startConversion(uploadImportTaskResponse.getId(), "mp3", inOptions);
        }
        if(!fileType.equals("wav")) {
            startConversion(uploadImportTaskResponse.getId(), "wav", inOptions);
        }
        // CloudConvert doesn't Support conversions to oga
    }

    public void convertHTML5Audio(String inputURL, HashMap<String, String> inOptions) throws IOException, URISyntaxException {
        final TaskResponse uploadImportTaskResponse = cloudConvertClient.importUsing().url(new UrlImportRequest().setUrl(inputURL)).getBody();
        final String fileType = inputURL.substring(inputURL.lastIndexOf('.') + 1);

        // The we need to make CloudConvert think that the format is m4a because otherwise it tries to convert it as a video
        if(fileType.equals("mp4"))
            inOptions.put("input_format", "m4a");

        if(!fileType.equals("aac")) {
            startConversion(uploadImportTaskResponse.getId(), "aac", inOptions);
        }
        if(!fileType.equals("mp3")) {
            startConversion(uploadImportTaskResponse.getId(), "mp3", inOptions);
        }
        if(!fileType.equals("wav")) {
            startConversion(uploadImportTaskResponse.getId(), "wav", inOptions);
        }
        // CloudConvert doesn't Support conversions to oga
    }

    public void convertFile(String inputURL, HashMap<String, String> inOptions)
            throws IOException, EncoderException, URISyntaxException {
        if(Helper.hasVideo(new URL(inputURL))) {
            convertHTML5Video(inputURL,inOptions);
        }
        else {
            convertHTML5Audio(inputURL,inOptions);
        }
    }

    public void convertFile(File inputFile, HashMap<String, String> inOptions)
            throws IOException, EncoderException, URISyntaxException {
        if(Helper.hasVideo(inputFile)) {
            convertHTML5Video(inputFile,inOptions);
        }
        else {
            convertHTML5Audio(inputFile,inOptions);
        }
    }

    public static void main(String args[]) throws IOException, URISyntaxException, InterruptedException, EncoderException {

        CloudConvert convertClient = new CloudConvert();

        HashMap<String, String> optionsMap = new HashMap<String, String>();
        optionsMap.put("volume", "0.5");

        //Declaring Executor service that will hold all runnables for the conversions
        ExecutorService conversionTaskService = Executors.newCachedThreadPool();

        //File Audio Conversion
        File testAudio = new File("testfiles/notVideo.mp4");
        conversionTaskService.execute(Helper.addFile(testAudio, optionsMap, convertClient));
        //URL Audio Conversion
        conversionTaskService.execute(
                Helper.addFile("http://www.lindberg.no/hires/mqa-cd-2018/2L-145_01_stereo.mqacd.mqa.flac",optionsMap,convertClient));
        //File Video Conversion
        File testVideo = new File("testfiles/testVideo.avi");
        conversionTaskService.execute(Helper.addFile(testVideo, optionsMap, convertClient));
        //URL Video Conversion
        conversionTaskService.execute(
                Helper.addFile("https://www.engr.colostate.edu/me/facil/dynamics/files/drop.avi",optionsMap,convertClient));

        // Start All Conversions
        conversionTaskService.shutdown();
        // Wait for Task endings (waiting for 60 minutes before timeout)
        conversionTaskService.awaitTermination(60, TimeUnit.MINUTES);

        System.out.println("All conversions are compleated!");

        return;
    }
}
