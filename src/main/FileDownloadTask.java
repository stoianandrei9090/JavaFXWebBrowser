package main;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileDownloadTask extends Task<File> {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private HttpClient httpClient;
    private String remoteUrl;
    private File localFile;
    private int bufferSize;

    public FileDownloadTask(String remoteUrl, File localFile)
    {
        this(new DefaultHttpClient(), remoteUrl, localFile, DEFAULT_BUFFER_SIZE);
    }

    public FileDownloadTask(HttpClient httpClient, String remoteUrl, File localFile, int bufferSize)
    {
        this.httpClient = httpClient;
        this.remoteUrl = remoteUrl;
        this.localFile = localFile;
        this.bufferSize = bufferSize;

        stateProperty().addListener(new ChangeListener<State>()
        {
            public void changed(ObservableValue<? extends State> source, State oldState, State newState)
            {
                if (newState.equals(State.SUCCEEDED))
                {
                    Main.decreaseNumDownloadThreads();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success!");
                    alert.setHeaderText(null);
                    alert.setContentText("Download succeeded!");

                    alert.showAndWait();
                }
                else if (newState.equals(State.FAILED))
                {
                    Main.decreaseNumDownloadThreads();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error!");
                    alert.setHeaderText(null);
                    alert.setContentText("Download failed!");
                    alert.showAndWait();
                }
            }
        });
    }

    protected File call() throws Exception
    {

        HttpGet httpGet = new HttpGet(this.remoteUrl);
        HttpResponse response = httpClient.execute(httpGet);
        InputStream remoteContentStream = response.getEntity().getContent();
        OutputStream localFileStream = null;
        try
        {
            long fileSize = response.getEntity().getContentLength();
            File dir = localFile.getParentFile();
            dir.mkdirs();

            localFileStream = new FileOutputStream(localFile);
            byte[] buffer = new byte[bufferSize];
            int sizeOfChunk;
            int amountComplete = 0;
            while ((sizeOfChunk = remoteContentStream.read(buffer)) != -1)
            {
                localFileStream.write(buffer, 0, sizeOfChunk);
                amountComplete += sizeOfChunk;
                updateProgress(amountComplete, fileSize);
                }
            return localFile;
        }
        finally
        {
            remoteContentStream.close();
            if (localFileStream != null)
            {
                localFileStream.close();
            }
        }
    }



}