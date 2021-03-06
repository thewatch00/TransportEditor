package com.oskopek.transport.view;

import com.oskopek.transport.controller.SaveDiscardController;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Dialog for entering a String. Creates a DialogPane encapsulated in it's controller.
 */
@Singleton
public class SaveDiscardDialogPaneCreator {

    @Inject
    @Named("fxmlloader")
    private Instance<FXMLLoader> fxmlLoader;

    @Inject
    @Named("mainApp")
    private TransportEditorApplication application;

    /**
     * Create the dialog for entering a String.
     *
     * @param prompt the string message to prompt the user with
     * @return the controller of the dialog window, enabling to display the dialog and read the selected result
     */
    public Optional<ButtonType> show(String prompt) {
        FXMLLoader fxmlLoader = this.fxmlLoader.get();
        DialogPane dialogPane = null;
        try (InputStream is = getClass().getResourceAsStream("SaveDiscardDialogPane.fxml")) {
            dialogPane = fxmlLoader.load(is);
        } catch (IOException e) {
            AlertCreator.handleLoadLayoutError(fxmlLoader.getResources(),
                    a -> application.centerInPrimaryStage(a, -200, -50), e);
        }
        dialogPane.setHeaderText(prompt);
        SaveDiscardController saveDiscardController = fxmlLoader.getController();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setDialogPane(dialogPane);
        saveDiscardController.setDialog(dialog);
        application.centerInPrimaryStage(dialog, -200, -250);
        dialog.setTitle("TransportEditor");
        return saveDiscardController.getDialog().showAndWait();
    }
}
