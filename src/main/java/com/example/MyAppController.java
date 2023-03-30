package com.example;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MyAppController {
	private String path = "data.json";
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Type todosType = new TypeToken<ArrayList<ToDo>>(){}.getType();
	
	private final String TODO_COMPLETED = "完了";
	private final String TODO_TITLE = "タイトル";
	private final String TODO_DATE = "日付";
	private final LinkedHashMap<String, Integer> MENU = new LinkedHashMap<String, Integer>() {
		{
			put(TODO_COMPLETED, 0);
			put(TODO_TITLE, 1);
			put(TODO_DATE, 2);
		}
	};
	private final String SORT_ASCENDANT = "昇順";
	private final String SORT_DESCENDANT = "降順";

	private ArrayList<ToDo> todos = new ArrayList<>() {
		// Use instance initializer of anonymouse class
		{
			add(new ToDo(0, "Design", "2022-12-01", true));
			add(new ToDo(1, "Implementation", "2022-12-07", false));
		}
	};

	@FXML
	private Button addBtn;

	@FXML
	private DatePicker headerDatePicker;

	@FXML
	private MenuItem menuItemAbout;

	@FXML
	private MenuItem menuItemClose;

	@FXML
	private ChoiceBox<String> sortOrderMenu;

	@FXML
	private ChoiceBox<String> sortTypeMenu;

	@FXML
	private TextField headerTitleField;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private VBox todoListVBox;

	private ObservableList<Node> todoListItems;

	private void save(String path) {
		var json = gson.toJson(todos, todosType);
		try {
			Files.writeString(Path.of(path), json);
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	private void load(String path) {
		try {
			var loadData = Files.readString(Path.of(path));
			ArrayList<ToDo> loadedList = gson.fromJson(loadData, todosType);
			todos = loadedList;
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	private HBox createToDoHBox(ToDo todo) {
		var completedCheckBox = new CheckBox();
		completedCheckBox.setSelected(todo.isCompleted());
		completedCheckBox.getStyleClass().add("todo-completed");
		completedCheckBox.setOnAction(e -> {
			System.out.println("チェック更新[" + todo.getId() + "] " + completedCheckBox.isSelected());
			todo.setCompleted(completedCheckBox.isSelected());
			save(path);
		});

		var titleField = new TextField(todo.getTitle());
		titleField.getStyleClass().add("todo-title");
		HBox.setHgrow(titleField, Priority.ALWAYS);
		titleField.setOnAction(e -> {
			System.out.println("タイトル更新[" + todo.getId() + "] " + titleField.getText());
			todo.setTitle(titleField.getText());
			save(path);
		});
		titleField.focusedProperty().addListener((observable, oldProperty, newProperty) -> {
			if (!newProperty) {
				System.out.println("タイトル更新[" + todo.getId() + "] " + titleField.getText());
				todo.setTitle(titleField.getText());
				save(path);
			}
		});

		var datePicker = new DatePicker(todo.getLocalDate());
		datePicker.getStyleClass().add("todo-date");
		datePicker.setPrefWidth(105);
		HBox.setHgrow(datePicker, Priority.NEVER);
		datePicker.setOnAction(e -> {
			System.out.println("日付更新[" + todo.getId() + "] " + datePicker.getValue().toString());
			todo.setDate(datePicker.getValue().toString());
			save(path);
		});

		var deleteBtn = new Button("削除");
		deleteBtn.getStyleClass().add("todo-delete");

		var todoItem = new HBox(completedCheckBox, titleField, datePicker, deleteBtn);
		todoItem.getStyleClass().add("todo-item");

		deleteBtn.setOnAction(e -> {
			System.out.println("削除[" + todo.getId() + "]");
			todos.remove(todo);
			todoListItems.remove(todoItem);
			save(path);
		});

		return todoItem;
	}

	private ToDo create(String title, String date) {
		int newId;
		if (todos.size() > 0)
			newId = todos.stream().max((todo1, todo2) -> todo1.getId() - todo2.getId()).get().getId() + 1;
		else
			newId = 0;
		var newToDo = new ToDo(newId, title, date, false);
		todos.add(newToDo);

		return newToDo;
	}

	private void showInfo(String txt) {
		Alert dialog = new Alert(AlertType.INFORMATION);
		dialog.setTitle("アプリの情報");
		dialog.setHeaderText(null);
		dialog.setContentText(txt);
		dialog.showAndWait();
	}

	private void sort(String type, String order) {
		Comparator<Node> comp = null;
		switch (type) {
		case TODO_COMPLETED:
			comp = Comparator.comparing(
					node -> ((CheckBox) ((HBox) node).getChildren().get(MENU.get(TODO_COMPLETED))).isSelected());
			break;
		case TODO_TITLE:
			comp = Comparator
					.comparing(node -> ((TextField) ((HBox) node).getChildren().get(MENU.get(TODO_TITLE))).getText());
			break;
		case TODO_DATE:
		default:
			comp = Comparator
					.comparing(node -> ((DatePicker) ((HBox) node).getChildren().get(MENU.get(TODO_DATE))).getValue());
			break;
		}
		if (order.equals(SORT_DESCENDANT)) {
			comp = comp.reversed();
		}
		FXCollections.sort(todoListItems, comp);
	}

	public void initialize() {
		sortTypeMenu.getItems().addAll(MENU.keySet());
		sortTypeMenu.setValue(TODO_DATE);
		sortTypeMenu.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> sort(newValue, sortOrderMenu.getValue()));

		sortOrderMenu.getItems().addAll(SORT_ASCENDANT, SORT_DESCENDANT);
		sortOrderMenu.setValue(SORT_ASCENDANT);
		sortOrderMenu.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> sort(sortTypeMenu.getValue(), newValue));

		// Set today
		headerDatePicker.setValue(LocalDate.now());

		todoListItems = todoListVBox.getChildren();

		load(path);
		
		todos.stream()
				.sorted(Comparator.comparing(ToDo::getDate))
				.forEach(todo -> {
					todoListItems.add(createToDoHBox(todo));
				});

		EventHandler<ActionEvent> handler = e -> {
			var title = headerTitleField.getText();
			if (title.equals(""))
				return;
			LocalDate localDate = headerDatePicker.getValue(); // 2022-12-01
			ToDo newToDo = create(title, localDate.toString());
			todoListItems.add(createToDoHBox(newToDo));
			sort(sortTypeMenu.getValue(), sortOrderMenu.getValue());
			headerTitleField.clear();
			System.out.println("追加[" + newToDo.getId() + "] " + title);
			save(path);
		};
		headerTitleField.setOnAction(handler);
		addBtn.setOnAction(handler);

		menuItemAbout.setOnAction(e -> showInfo("ToDo App"));
		menuItemClose.setOnAction(e -> Platform.exit());

		// VBoxの末尾にあるScrollPaneを、ウィンドウサイズに応じて高さ最大化
		VBox.setVgrow(scrollPane, Priority.ALWAYS);
	}
}
