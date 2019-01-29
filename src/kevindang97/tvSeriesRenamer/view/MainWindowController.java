package kevindang97.tvSeriesRenamer.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kevindang97.tvSeriesRenamer.MainApp;
import kevindang97.tvSeriesRenamer.model.RenameAction;
import kevindang97.tvSeriesRenamer.util.Util;

public class MainWindowController {

  @FXML
  private TextField seriesNameTextField;
  @FXML
  private TextField seasonNumberTextField;

  @FXML
  private TableView<RenameAction> fileTable;
  @FXML
  private TableColumn<RenameAction, String> beforeFilenameColumn;
  @FXML
  private TableColumn<RenameAction, String> afterFilenameColumn;

  @FXML
  private Button moveModeButton;

  private MainApp mainApp;
  private final DirectoryChooser directoryChooser = new DirectoryChooser();
  private boolean moveModeSwap = true;

  @FXML
  private void initialize() {
    beforeFilenameColumn
        .setCellValueFactory(cellData -> cellData.getValue().beforeFilenameProperty());
    afterFilenameColumn
        .setCellValueFactory(cellData -> cellData.getValue().afterFilenameProperty());

    // Add event handler only to the cells in the beforeFilename column
    beforeFilenameColumn.setCellFactory(tableCell -> {
      TableCell<RenameAction, String> cell = new TableCell<RenameAction, String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);
          setText(empty ? null : item);
        }
      };

      cell.setOnDragDetected(event -> {
        Dragboard dragboard = cell.startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString(Integer.toString(cell.getTableRow().getIndex()));
        dragboard.setContent(content);

        event.consume();
      });

      cell.setOnDragOver(event -> {
        if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
          event.acceptTransferModes(TransferMode.MOVE);

          event.consume();
        }
      });

      cell.setOnDragEntered(event -> {
        if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
          // cell.setOpacity(0.5);
          cell.setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN, null, null)));
        }

        event.consume();
      });

      cell.setOnDragExited(event -> {
        // cell.setOpacity(1);
        cell.setBackground(null);

        event.consume();
      });

      cell.setOnDragDropped(event -> {
        // swap the two before filenames
        int index1 = Integer.parseInt(event.getDragboard().getString());
        int index2 = cell.getTableRow().getIndex();
        // System.out.println("Moving cells from rows " + index1 + " & " + index2);

        if (moveModeSwap) {
          mainApp.getSeriesRenamer().moveBeforeFilenameSwap(index1, index2);
        } else {
          mainApp.getSeriesRenamer().moveBeforeFilenameInsert(index1, index2);
        }

        event.consume();
      });

      cell.setOnDragDone(DragEvent::consume);

      return cell;
    });

    // Add event handler only to the cells in the afterFilename column
    afterFilenameColumn.setCellFactory(tableCell -> {
      TableCell<RenameAction, String> cell = new TableCell<RenameAction, String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);
          setText(empty ? null : item);
        }
      };

      cell.setOnMouseClicked(event -> {
        handleTableMouseClicked(event);
      });

      return cell;
    });

    // set up change listener for series name and season number text fields
    seriesNameTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      mainApp.getSeriesRenamer().setSeriesName(newValue);
    });

    seasonNumberTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      int seasonNum = 0;
      if (!newValue.equals("")) {
        seasonNum = Integer.parseInt(newValue);
      }
      mainApp.getSeriesRenamer().setSeasonNumber(seasonNum);
    });

    // restrict series name text field to not allow illegal filename chars
    seriesNameTextField.setTextFormatter(Util.getRestricterIllegalFilenameChars());

    // set up text formatter for season number text field to restrict input to only digit characters
    seasonNumberTextField.setTextFormatter(new TextFormatter<>(change -> {
      if (Pattern.matches("\\d*", change.getText())) {
        return change;
      } else {
        return null;
      }
    }));
  }

  public void setMainApp(MainApp mainApp) {
    this.mainApp = mainApp;

    // add observable rename actions list to the table
    fileTable.setItems(mainApp.getSeriesRenamer().getRenameActions());
  }

  @FXML
  private void handleOpenFolder() {
    // open directory chooser in previous folder selected if it exists
    Path previousDirectory = mainApp.getSeriesRenamer().getFolder();
    if (previousDirectory != null) {
      directoryChooser.setInitialDirectory(previousDirectory.getParent().toFile());
    }

    File directory = directoryChooser.showDialog(mainApp.getPrimaryStage());
    if (directory != null) {
      mainApp.getSeriesRenamer().openFolder(directory.toPath());
      if (mainApp.getSeriesRenamer().getAutoSetSeriesInfo()) {
        seriesNameTextField.setText(mainApp.getSeriesRenamer().getSeriesName());
        seasonNumberTextField
            .setText(Integer.toString(mainApp.getSeriesRenamer().getSeasonNumber()));
      }
    }
  }

  @FXML
  private void handleSettings() {
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(MainWindowController.class.getResource("SettingsWindow.fxml"));
      BorderPane page = (BorderPane) loader.load();

      Stage dialogStage = new Stage();
      dialogStage.setTitle("Settings");
      dialogStage.initModality(Modality.WINDOW_MODAL);
      dialogStage.initOwner(mainApp.getPrimaryStage());
      Scene scene = new Scene(page);
      dialogStage.setScene(scene);

      SettingsWindowController controller = loader.getController();
      controller.setSeriesRenamer(mainApp.getSeriesRenamer());
      controller.setDialogStage(dialogStage);

      dialogStage.showAndWait();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void handleMoveMode() {
    moveModeSwap = !moveModeSwap;

    if (moveModeSwap) {
      moveModeButton.setText("Move Mode: Swap");
    } else {
      moveModeButton.setText("Move Mode: Insert");
    }
  }

  @FXML
  private void handleAutofillEpisodeNames() {
    mainApp.getSeriesRenamer().autoFillEpisodeNames();
  }

  @FXML
  private void handleChangeEpisodeNames() {
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(MainWindowController.class.getResource("EpisodeNameChange.fxml"));
      AnchorPane page = (AnchorPane) loader.load();

      Stage dialogStage = new Stage();
      dialogStage.setTitle("Enter episode names");
      dialogStage.initModality(Modality.WINDOW_MODAL);
      dialogStage.initOwner(mainApp.getPrimaryStage());
      Scene scene = new Scene(page);
      dialogStage.setScene(scene);

      EpisodeNameChangeController controller = loader.getController();
      controller.setSeriesRenamer(mainApp.getSeriesRenamer());
      controller.setDialogStage(dialogStage);

      dialogStage.showAndWait();

      if (controller.isOkClicked()) {
        mainApp.getSeriesRenamer().clearEpisodeNames();
        List<String> episodeNames = controller.getEpisodeNames();

        for (int i = 0; i < Math.min(mainApp.getSeriesRenamer().getNumFiles(),
            episodeNames.size()); i++) {
          mainApp.getSeriesRenamer().setEpisodeName(i, episodeNames.get(i));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @FXML
  private void handlePerformRename() {
    // validation handling
    String errorMessage = mainApp.getSeriesRenamer().performValidation();

    if (!errorMessage.equals("")) {
      Alert alert = new Alert(AlertType.ERROR);
      alert.initOwner(mainApp.getPrimaryStage());
      alert.setTitle("Invalid fields");
      alert.setHeaderText("Please correct invalid fields:");
      alert.setContentText(errorMessage);

      alert.showAndWait();
      return;
    }

    // perform the rename operation
    mainApp.getSeriesRenamer().performRename();
  }

  /**
   * Handles event for double clicking cells in the afterFilename column. This is for the purpose of
   * individually setting episode names for files.
   * 
   * @param event
   */
  private void handleTableMouseClicked(MouseEvent event) {
    if (event.getClickCount() == 2) {
      TextInputDialog dialog = new TextInputDialog(
          fileTable.getSelectionModel().getSelectedItem().episodeNameProperty().get());
      dialog.setTitle("Edit episode name");
      dialog.setHeaderText(
          "Edit episode " + (fileTable.getSelectionModel().getSelectedIndex() + 1) + "'s name:");

      // restrict text input field so illegal filename chars can't be inputted
      dialog.getEditor().setTextFormatter(Util.getRestricterIllegalFilenameChars());

      Optional<String> result = dialog.showAndWait();
      if (result.isPresent()) {
        mainApp.getSeriesRenamer().setEpisodeName(fileTable.getSelectionModel().getSelectedIndex(),
            result.get());
      }
    }
  }
}
