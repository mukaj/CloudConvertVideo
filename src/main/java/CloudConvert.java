import com.cloudconvert.client.CloudConvertClient;
import com.cloudconvert.client.setttings.StringSettingsProvider;
import com.cloudconvert.dto.request.ConvertFilesTaskRequest;
import com.cloudconvert.dto.request.UploadImportRequest;
import com.cloudconvert.dto.request.UrlExportRequest;
import com.cloudconvert.dto.request.UrlImportRequest;
import com.cloudconvert.dto.response.JobResponse;
import com.cloudconvert.dto.response.TaskResponse;
import com.cloudconvert.dto.result.Result;
import com.google.common.collect.ImmutableMap;
import org.apache.tika.io.IOUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class CloudConvert {
    private static final String APIKey = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIxIiwianRpIjoiNDNjMzRiYTI0ZjE3ZGM3NDRkNjY5ZGNjYjQ5NDFjMDg0OTdmNjhkZTJhYWNlZjUwYWEyMjNmNTNiNzIxZDIzMzM5OTRjMTgzNThjNjMxZGUiLCJpYXQiOjE2MjQyMTIyMTEuMjQxMDU1LCJuYmYiOjE2MjQyMTIyMTEuMjQxMDU4LCJleHAiOjQ3Nzk4ODU4MTEuMjAzOTU4LCJzdWIiOiI1MTgyNzI3MCIsInNjb3BlcyI6WyJ1c2VyLnJlYWQiLCJ1c2VyLndyaXRlIiwidGFzay5yZWFkIiwidGFzay53cml0ZSIsIndlYmhvb2sucmVhZCIsIndlYmhvb2sud3JpdGUiLCJwcmVzZXQucmVhZCIsInByZXNldC53cml0ZSJdfQ.pZiWXTZcgvfMFseyPQDOfClxu8nPNTGXgyryhP2pEBWBgWZ-_BVkVmyl8l-GeTZg-b34m8ByVdSRtygolrxAPEYzWU8pgkyD3NJwVzpLWZjxJ_8MHzACleC--1wfO_Y4RwmvNO9uLhYdFHvOjiU7EuYw6EdxE59xiZl4L80QzCd9Xs53KPyKiWcwyGSqGsPZO5D1AS-EAy3O7DdM7TmFTVZJhLSIVVqC5OKUc5HfqS057Bc7IgOlrp35jO_U3gxsrtjOaY-gLTrIxniIqpGvz90K6kFnX8dBFrdgflVokL66wmmAopjds2A9Xttw76GznZ1HuAHGU2wjB2L5TAXLR1N0VDG83MMvhEtfSxZjQYn0bEFl-ZzfSoQK8WXR_Tlhodiwjo6SFa2NJzqfwWb0YU4fE_xh1DLGGFAdmw5kjBY7qH5x0zByUp43ocX9IItNbrO3NsIvwFdOOeRcbOcBrLwHfBW2DUsk2tQFVs_OuCUSnNI3dzK1HzfcwRFTGvbr_asgDgW05uCrpqEmBmvaq5TyV2QMxQd1lT7VQ0TaMhds8TrHnSx-5nnw-disJonSLWTeB9u8K487wH8wBN_RQ6IZgKrj6XTGXsSNIC1eeiH1JC30XC7zwaJdpVVg55C9kehvCikDzL0CKcTbXdfXZPky-kosMFGSHqajhHN0vFM";
    private final CloudConvertClient cloudConvertClient = new CloudConvertClient(new StringSettingsProvider(APIKey, "webhook-signing-secret", false));

    public CloudConvert() throws IOException {}

    /**
     *
     * @param task Conversion task
     * @param properties HashMap for all the conversion settings
     * @return
     */
    private ConvertFilesTaskRequest setProperties(ConvertFilesTaskRequest task, HashMap<String, String> properties) {
        properties.forEach((key, value) ->
                task.setProperty(key, value));

        return task;
    }

    /**
     *  Conversion Function for a file from an URL
     *
     * @param inputFileURL URL of the file we want to Convert
     * @param convertedOutputFile Converted File
     * @param inputType Extension of input file
     * @param outputType Extension of output file
     * @param outputName Name of output file (MUST NOT INCLUDE EXTENSION)
     * @param inOptions Custom Options for Conversion
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public void convert(final String inputFileURL, File convertedOutputFile, String outputType,
                        final String outputName, HashMap<String, String> inOptions) throws IOException, URISyntaxException, InterruptedException {

        // Creating the convert task request and applying the options to it.
        final ConvertFilesTaskRequest conversionTask = setProperties(new ConvertFilesTaskRequest(), inOptions);

        // Create a job to import, convert and export the video
        final JobResponse createJobResponse = (JobResponse) cloudConvertClient.jobs().create(
                ImmutableMap.of(
                "ImportVideo", new UrlImportRequest().setUrl(inputFileURL),
                "ConvertVideo", conversionTask
                        .setFilename(outputName.concat(".").concat(outputType))
                        .setInput("ImportVideo")
                        .setOutputFormat(outputType),
                "ExportVideo", new UrlExportRequest().setInput("ConvertVideo")
        )).getBody();

        //Get Export TaskResponse
        final TaskResponse waitUrlExportTaskResponse = cloudConvertClient.tasks().wait(createJobResponse.getTasks().get(2).getId()).getBody();

        final String exportUrl = waitUrlExportTaskResponse.getResult().getFiles().get(0).get("url");

        // Export into file
        InputStream convertedFile = cloudConvertClient.files().download(exportUrl).getBody();
        OutputStream outputStream = null;

        // Creating an output stream to Save the converted file
        final String filename = waitUrlExportTaskResponse.getResult().getFiles().get(0).get("filename");
        outputStream = new FileOutputStream(new File(filename));

        IOUtils.copy(convertedFile, outputStream);

        // Clean-up
        convertedFile.close();
        outputStream.close();
    }


    /**
     *  Conversion Function for a file on local Storage
     *
     * @param inputFile File we want to Convert
     * @param convertedOutputFile Converted File
     * @param outputType Extension of output file
     * @param outputName Name of output file (MUST NOT INCLUDE EXTENSION)
     * @param inOptions Custom Options for Conversion
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public void convert(final File inputFile, File convertedOutputFile, String outputType,
                        final String outputName, HashMap<String, String> inOptions) throws IOException, URISyntaxException, InterruptedException {

        // Upload file using import/upload task
        final TaskResponse uploadImportTaskResponse = cloudConvertClient.importUsing().upload(new UploadImportRequest(), inputFile).getBody();

        // Creating the convert task request and applying the options to it.
        final ConvertFilesTaskRequest conversionTask = setProperties(new ConvertFilesTaskRequest(), inOptions);

        // Create a job to Convert and Export Video
        final JobResponse createJobResponse = cloudConvertClient.jobs().create(
            ImmutableMap.of(
                    "ConvertVideo", conversionTask
                            .setFilename(outputName.concat(".").concat(outputType))
                            .setInput(uploadImportTaskResponse.getId())
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
        outputStream = new FileOutputStream(new File(filename));


        IOUtils.copy(convertedFile, outputStream);

        // Clean-up
        convertedFile.close();
        outputStream.close();
    }


    public static void main(String args[]) throws IOException, URISyntaxException, InterruptedException {

        File output = null;

        CloudConvert convertClient = new CloudConvert();

        HashMap<String, String> testmap = new HashMap<String, String>();
        // Put Values into the map to add to the converted file
        testmap.put("height", "412");
        testmap.put("volume", "0");

        // Uncomment to Test url
        convertClient.convert("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",output, "webm", "convertedFile",testmap);

        // Uncomment to Test LocalFile
        File input = new File("testVideo.mp4");
        convertClient.convert(input,output, "webm", "converted",testmap);
        return;
    }
}