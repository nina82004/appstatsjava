package controller;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.io.IOException;
import  util.SessionContext;
import com.itextpdf.layout.element.Image;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class MainController {

    @FXML
    public void afficherClients(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/StatistiquesClients.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Statistiques Clients");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void afficherEvenements(ActionEvent e) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/StatistiquesEvents.fxml"));
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Statistiques Events");

            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    @FXML
    public void afficherPrestations(ActionEvent e) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/StatistiquesPrestations.fxml"));
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Statistiques Prestations");

            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    @FXML
    public void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Déconnexion");
        alert.setContentText("Voulez-vous vraiment vous déconnecter ?");

        Window owner = Stage.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);
        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String token = SessionContext.getToken();
                    System.out.println("Token utilisé pour logout : " + token);

                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8000/auth/logout"))
                            .header("token", token)
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(response -> {
                                if (response.statusCode() == 200) {
                                    System.out.println(" Déconnexion réussie !");
                                    SessionContext.clear();

                                    Platform.runLater(() -> {
                                        try {
                                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                                            Parent root = loader.load();
                                            Stage stage = (Stage) Stage.getWindows().stream()
                                                    .filter(Window::isShowing)
                                                    .findFirst()
                                                    .orElse(null);
                                            if (stage != null) {
                                                stage.setScene(new Scene(root));
                                                stage.setTitle("Connexion Business Care");
                                                stage.setMaximized(false);
                                                stage.show();
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });

                                } else {
                                    System.out.println("Échec de déconnexion : " + response.body());
                                }
                            });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void genererPDF(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = fileChooser.showSaveDialog(null);
            if (file == null) return;


            FXMLLoader loaderClients = new FXMLLoader(getClass().getResource("/fxml/StatistiquesClients.fxml"));
            Parent rootClients = loaderClients.load();
            StatistiquesClientsController controllerClients = loaderClients.getController();

            FXMLLoader loaderPrestations = new FXMLLoader(getClass().getResource("/fxml/StatistiquesPrestations.fxml"));
            Parent rootPrestations = loaderPrestations.load();
            StatistiquesPrestationsController controllerPrestations = loaderPrestations.getController();


            Scene dummyScene = new Scene(new VBox(rootClients, rootPrestations));
            Stage dummyStage = new Stage();
            dummyStage.setScene(dummyScene);

            Platform.runLater(() -> {
                try {
                    SnapshotParameters params = new SnapshotParameters();
                    params.setFill(Color.WHITE);

                    List<WritableImage> chartImages = new ArrayList<>();


                    controllerClients.barTopClients.applyCss(); controllerClients.barTopClients.layout();
                    controllerClients.pieTypeClient.applyCss(); controllerClients.pieTypeClient.layout();
                    controllerClients.barTarifParType.applyCss(); controllerClients.barTarifParType.layout();
                    controllerClients.pieTopClients.applyCss(); controllerClients.pieTopClients.layout();

                    controllerPrestations.barServiceParType.applyCss(); controllerPrestations.barServiceParType.layout();
                    controllerPrestations.pieRepartitionIntervention.applyCss(); controllerPrestations.pieRepartitionIntervention.layout();
                    controllerPrestations.barServiceParPrix.applyCss(); controllerPrestations.barServiceParPrix.layout();
                    controllerPrestations.pieTopPrestations.applyCss(); controllerPrestations.pieTopPrestations.layout();


                    chartImages.add(controllerClients.barTopClients.snapshot(params, null));
                    chartImages.add(controllerClients.pieTypeClient.snapshot(params, null));
                    chartImages.add(controllerClients.barTarifParType.snapshot(params, null));
                    chartImages.add(controllerClients.pieTopClients.snapshot(params, null));

                    chartImages.add(controllerPrestations.barServiceParType.snapshot(params, null));
                    chartImages.add(controllerPrestations.pieRepartitionIntervention.snapshot(params, null));
                    chartImages.add(controllerPrestations.barServiceParPrix.snapshot(params, null));
                    chartImages.add(controllerPrestations.pieTopPrestations.snapshot(params, null));


                    PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                    PdfDocument pdf = new PdfDocument(writer);
                    Document document = new Document(pdf);

                    for (WritableImage chartImage : chartImages) {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        ImageIO.write(SwingFXUtils.fromFXImage(chartImage, null), "png", os);
                        ImageData imageData = ImageDataFactory.create(os.toByteArray());
                        com.itextpdf.layout.element.Image pdfImage = new com.itextpdf.layout.element.Image(imageData);
                        pdfImage.setAutoScale(true);
                        document.add(pdfImage);
                        document.add(new Paragraph("\n"));
                    }

                    document.close();
                    System.out.println("PDF combiné généré avec succès.");
                    dummyStage.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void fermerApplication(ActionEvent e) {
        System.exit(0);
    }
}
