import com.cloudconvert.client.CloudConvertClient;
import com.cloudconvert.client.setttings.StringSettingsProvider;
import com.cloudconvert.dto.request.ConvertFilesTaskRequest;
import com.cloudconvert.dto.request.UploadImportRequest;
import com.cloudconvert.dto.request.UrlExportRequest;
import com.cloudconvert.dto.request.UrlImportRequest;
import com.cloudconvert.dto.response.JobResponse;
import com.cloudconvert.dto.response.TaskResponse;
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
     *  Conversion Function for a file from an URL
     *
     * @param inputFileURL URL of the file we want to Convert
     * @param convertedOutputFile Converted File
     * @param inputType Extension of input file
     * @param outputType Extension of output file
     * @param inOptions Custom Options for Conversion
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public void convert(final String inputFileURL, File convertedOutputFile, String inputType, String outputType,
                        HashMap<String, String> inOptions) throws IOException, URISyntaxException, InterruptedException {

        // Create a job to Convert and Export Video
        final JobResponse createJobResponse = cloudConvertClient.jobs().create(
                ImmutableMap.of(
                        "ImportVideo", new UrlImportRequest().setUrl(inputFileURL),
                        "ConvertVideo", new ConvertFilesTaskRequest()
                                .setInput("ImportVideo")
                                .setInputFormat(inputType)
                                .setOutputFormat(outputType),
                        "ExportVideo", new UrlExportRequest().setInput("ConvertVideo")
                )
        ).getBody();

        //Get Export TaskResponse
        final TaskResponse waitUrlExportTaskResponse = cloudConvertClient.tasks().wait(createJobResponse.getTasks().get(2).getId()).getBody();

        final String exportUrl = waitUrlExportTaskResponse.getResult().getFiles().get(0).get("url");
        final String filename = waitUrlExportTaskResponse.getResult().getFiles().get(0).get("filename");


        // Export into file
        InputStream convertedFile = cloudConvertClient.files().download(exportUrl).getBody();
        OutputStream outputStream = new FileOutputStream(new File(filename));
        IOUtils.copy(convertedFile, outputStream);

        convertedOutputFile = new File(filename);

        // Clean-up
        convertedFile.close();
        outputStream.close();
    }

    /**
     *  Conversion Function for a file on local Storage
     *
     * @param inputFile File we want to Convert
     * @param convertedOutputFile Converted File
     * @param inputType Extension of input file
     * @param outputType Extension of output file
     * @param inOptions Custom Options for Conversion
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public void convert(final File inputFile, File convertedOutputFile, String inputType, String outputType,
                        HashMap<String, String> inOptions) throws IOException, URISyntaxException, InterruptedException {

        // Upload file using import/upload task
        final TaskResponse uploadImportTaskResponse = cloudConvertClient.importUsing().upload(new UploadImportRequest(), inputFile).getBody();

        // Wait for import/upload task to be finished
        final TaskResponse waitUploadImportTaskResponse = cloudConvertClient.tasks().wait(uploadImportTaskResponse.getId()).getBody();

        // Create a job to Convert and Export Video
        final JobResponse createJobResponse = cloudConvertClient.jobs().create(
                ImmutableMap.of(
                        "ConvertVideo", new ConvertFilesTaskRequest()
                                .setInput(waitUploadImportTaskResponse.getId())
                                .setInputFormat(inputType)
                                .setOutputFormat(outputType),
                        "ExportVideo", new UrlExportRequest().setInput("ConvertVideo")
                )
        ).getBody();

        //Get Export TaskResponse
        final TaskResponse waitUrlExportTaskResponse = cloudConvertClient.tasks().wait(createJobResponse.getTasks().get(1).getId()).getBody();

        final String exportUrl = waitUrlExportTaskResponse.getResult().getFiles().get(0).get("url");
        final String filename = waitUrlExportTaskResponse.getResult().getFiles().get(0).get("filename");


        // Export into file
        InputStream convertedFile = cloudConvertClient.files().download(exportUrl).getBody();
        OutputStream outputStream = new FileOutputStream(new File(filename));
        IOUtils.copy(convertedFile, outputStream);

        convertedOutputFile = new File(filename);

        // Clean-up
        convertedFile.close();
        outputStream.close();
    }



    public static void main(String args[]) throws IOException, URISyntaxException, InterruptedException {

        File input = new File("testVideo.webm");
        File output = null;

        CloudConvert skr = new CloudConvert();

        skr.convert("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4",output,"mp4", "webm", null);

        return;
    }
}