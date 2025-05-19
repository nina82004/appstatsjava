package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.net.http.*;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;

import util.Config;
import util.SessionContext;

import org.json.JSONObject;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        try {


            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("password", password);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Config.getApiBaseUrl() + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            System.out.println("Envoi JSON : " + json.toString());
            System.out.println("URL appelée : " + Config.getApiBaseUrl() + "/auth/login");

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject body = new JSONObject(response.body());

                String token = body.getString("token");
                System.out.println("Token reçu : " + token);

                SessionContext.setToken(token);

                DecodedJWT decoded = JWT.decode(token);
                String role = decoded.getClaim("function").asString();

                if ("administrator".equals(role)) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) emailField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } else {
                    errorLabel.setText("Accès réservé aux administrateurs.");
                }
            } else {
                errorLabel.setText("Email ou mot de passe incorrect.");
            }

        } catch (Exception e) {
            errorLabel.setText("Erreur lors de la connexion.");
            e.printStackTrace();
        }
    }
}
