package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class StatistiquesClientsController {

    @FXML
    public BarChart<String, Number> barTopClients;
    @FXML
    public PieChart pieTypeClient;
    @FXML
    public BarChart<String, Number> barTarifParType;
    @FXML
    public PieChart pieTopClients;

    @FXML
    public void initialize() {
        afficherTop5ParRevenu();
        afficherRepartitionParType();
        afficherTarifParTypeClient();
        afficherRepartitionTop5Clients();
    }

    private void afficherTop5ParRevenu() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Top 5 clients par chiffre d’affaires");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/stats_client/top-clients"))
                    .GET()
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray data = new JSONArray(response.body());

            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.getJSONObject(i);
                series.getData().add(new XYChart.Data<>(obj.getString("name"), obj.getDouble("revenue")));
            }

            barTopClients.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherRepartitionParType() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/stats_client/client-type-distribution"))
                    .GET()
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                    new PieChart.Data("Starter", json.getInt("Starter")),
                    new PieChart.Data("Basic", json.getInt("Basic")),
                    new PieChart.Data("Premium", json.getInt("Premium"))
            );

            pieTypeClient.setData(data);
            pieTypeClient.setTitle("Répartition par type de client");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherTarifParTypeClient() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tarif annuel par type de client (€)");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/stats_client/tariff-by-type"))
                    .GET()
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject data = new JSONObject(response.body());

            for (String key : data.keySet()) {
                series.getData().add(new XYChart.Data<>(key, data.getDouble(key)));
            }

            barTarifParType.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherRepartitionTop5Clients() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/stats_client/top-clients-pie"))
                    .GET()
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray array = new JSONArray(response.body());

            double total = 0;
            for (int i = 0; i < array.length(); i++) {
                total += array.getJSONObject(i).getDouble("total");
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                double percent = (obj.getDouble("total") / total) * 100;
                data.add(new PieChart.Data(obj.getString("name") + String.format(" (%.1f%%)", percent), obj.getDouble("total")));
            }

            pieTopClients.setData(data);
            pieTopClients.setTitle("Répartition CA Top 5 clients");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void retourAccueil(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Page principale - Business Care");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void telechargerGraphiquesPDF(ActionEvent event) {
        WritableImage image1 = barTopClients.snapshot(null, null);
        WritableImage image2 = pieTypeClient.snapshot(null, null);
        WritableImage image3 = barTarifParType.snapshot(null, null);
        WritableImage image4 = pieTopClients.snapshot(null, null);

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
            File file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());

            if (file != null) {
                File temp1 = File.createTempFile("graph1", ".png");
                File temp2 = File.createTempFile("graph2", ".png");
                File temp3 = File.createTempFile("graph3", ".png");
                File temp4 = File.createTempFile("graph4", ".png");

                ImageIO.write(SwingFXUtils.fromFXImage(image1, null), "png", temp1);
                ImageIO.write(SwingFXUtils.fromFXImage(image2, null), "png", temp2);
                ImageIO.write(SwingFXUtils.fromFXImage(image3, null), "png", temp3);
                ImageIO.write(SwingFXUtils.fromFXImage(image4, null), "png", temp4);

                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                ImageData img1 = ImageDataFactory.create(temp1.getAbsolutePath());
                ImageData img2 = ImageDataFactory.create(temp2.getAbsolutePath());
                ImageData img3 = ImageDataFactory.create(temp3.getAbsolutePath());
                ImageData img4 = ImageDataFactory.create(temp4.getAbsolutePath());

                document.add(new com.itextpdf.layout.element.Image(img1).scaleToFit(500, 400));
                document.add(new com.itextpdf.layout.element.Image(img2).scaleToFit(500, 400));
                document.add(new com.itextpdf.layout.element.Image(img3).scaleToFit(500, 400));
                document.add(new com.itextpdf.layout.element.Image(img4).scaleToFit(500, 400));

                document.close();
                System.out.println("PDF généré avec succès : " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WritableImage getChartImage(Node chart) {
        return chart.snapshot(new SnapshotParameters(), null);
    }
}
