package kup.get.controller.traffic;

import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import kup.get.config.FX.FxmlLoader;
import kup.get.config.FX.MyAnchorPane;
import kup.get.config.FX.MyContextMenu;
import kup.get.config.MyTable;
import kup.get.service.Services;
import kup.get.entity.alfa.Person;
import kup.get.entity.traffic.*;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

@FxmlLoader(path = "/fxml/traffic/items.fxml")
public class ItemController extends MyAnchorPane {
    @FXML
    private Button excelButton;

    @FXML
    private MyTable<TrafficItem> itemTable;

    @FXML
    private ComboBox<Owner> ownerComboBox;

    @FXML
    private DatePicker finishDatePicker;
    @FXML
    private DatePicker startDatePricker;
    @FXML
    private ComboBox<TrafficItemType> filterItemTypeComboBox;

    @FXML
    private GridPane addItemPane;
    @FXML
    private GridPane itemsPane;

    @FXML
    private TextArea commentField;
    @FXML
    private ComboBox<TrafficItemType> itemTypeComboBox;
    @FXML
    private DatePicker dateStartField;
    @FXML
    private DatePicker dateFinishField;
    @FXML
    private Label infoLabel;
    @FXML
    private Button addButton;
    @FXML
    private Button canselButton;
    @FXML
    private TextField searchPerson;
    @FXML
    private TabPane tabPane;
    @FXML
    private MyTable<Person> peopleTable;
    @FXML
    private MyTable<TrafficTeam> teamTable;
    @FXML
    private MyTable<TrafficVehicle> vehicleTable;

    private final Services services;
    private final AtomicReference<SequentialTransition> transition;
    private final String nameItemColumn = "Предметы";

    public ItemController(Services services, AtomicReference<SequentialTransition> sequentialTransition) {
        this.services = services;
        this.transition = sequentialTransition;
        ownerComboBox.setItems(FXCollections.observableArrayList(Owner.values()));

        itemTable
                .headerColumn(nameItemColumn)
                .column("id", TrafficItem::getId).setInvisible().build()
                .column("Наименование", ti -> ti.getType().getName()).build()
                .column("Описание", TrafficItem::getDescription).build()
                .column("Начало периода", TrafficItem::getDateStart).build()
                .column("Конец периода", TrafficItem::getDateFinish).build()
                .and()
                .headerColumn(Owner.PERSON.title).setInvisible()
                .column("Таб. №", ti -> parsePerson(Person::getPersonnelNumber, ti.getPerson())).setInvisible().build()
                .column("Фамилия", ti -> parsePerson(Person::getLastName, ti.getPerson())).setInvisible().build()
                .column("Имя", ti -> parsePerson(Person::getFirstName, ti.getPerson())).setInvisible().build()
                .column("Отчество", ti -> parsePerson(Person::getMiddleName, ti.getPerson())).setInvisible().build()
                .and()
                .headerColumn(Owner.TEAM.title).setInvisible()
                .column("Номер экипажа", ti -> ti.getTeam().getNumber()).setInvisible().build()
                .column("Режим работы", ti -> ti.getTeam().getWorkingMode()).setInvisible().build()
                .and()
                .headerColumn(Owner.VEHICLE.title).setInvisible()
                .column("Номер ТС", ti -> ti.getVehicle().getNumber()).setInvisible().build()
                .column("Модель ТС", ti -> ti.getVehicle().getModel()).setInvisible().build();

        itemTable.addEventHandler(MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                itemTable.setContextMenu(itemTableContextMenu());
            }
        });

        ownerComboBox.setOnAction(event -> {
            Owner owner = ownerComboBox.getSelectionModel().getSelectedItem();
            if (owner.equals(Owner.All)) {
                itemTable.getColumns().forEach(column -> column.setVisible(true));
            } else {
                itemTable.getColumns().forEach(column -> column.setVisible(column.getText().equals(owner.title) || column.getText().equals(nameItemColumn)));
            }
        });

        canselButton.setOnAction(event -> switchPane(addItemPane, itemsPane));

        dateStartField.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if(itemTypeComboBox.getValue()!=null && !newValue.isEmpty()){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                dateFinishField.setValue(LocalDate.parse(newValue, formatter).plusMonths(itemTypeComboBox.getValue().getDefaultDurationInMonth()));
            }
        });

        peopleTable
                .headerColumn(Owner.PERSON.title)
                .column("Таб. №", Person::getPersonnelNumber).build()
                .column("Фамилия", Person::getLastName).build()
                .column("Имя", Person::getFirstName).build()
                .column("Отчество", Person::getMiddleName).build();
        teamTable.headerColumn(Owner.TEAM.title)
                .column("Номер экипажа", TrafficTeam::getNumber).build()
                .column("Режим работы", TrafficTeam::getWorkingMode).build();
        vehicleTable
                .headerColumn(Owner.VEHICLE.title)
                .column("Номер ТС", TrafficVehicle::getNumber).build()
                .column("Модель ТС", TrafficVehicle::getModel).build()
                .column("Номер экипажа", tv -> tv.getTeam().getNumber()).build()
                .column("Режим работы", tv -> tv.getTeam().getWorkingMode()).build();
        addButton.setOnAction(event -> {
            SelectionModel<?> model = null;
            infoLabel.setStyle("-fx-text-fill: red");
            TrafficItem item = new TrafficItem();
            System.out.println(tabPane.getSelectionModel().getSelectedItem().getText());
            if (tabPane.getSelectionModel().getSelectedItem().getText().equals("Водители")) {
                model = peopleTable.getSelectionModel();
                item.setPerson(new TrafficPerson((Person) model.getSelectedItem()));
            } else if (tabPane.getSelectionModel().getSelectedItem().getText().equals("Экипажи")) {
                model = teamTable.getSelectionModel();
                item.setTeam((TrafficTeam) model.getSelectedItem());
            } else if (tabPane.getSelectionModel().getSelectedItem().getText().equals("Транспортные средства")) {
                model = vehicleTable.getSelectionModel();
                item.setVehicle((TrafficVehicle) model.getSelectedItem());
            }
            if (model != null && model.getSelectedItem() != null) {
                if (!itemTypeComboBox.getSelectionModel().isEmpty()) {
                    if (!commentField.getText().isEmpty()) {
                        if (dateStartField.getValue() != null && dateFinishField.getValue() != null) {
                            if (dateFinishField.getValue().isAfter(dateStartField.getValue())) {
                                item.setType(itemTypeComboBox.getValue());
                                item.setDescription(commentField.getText());
                                item.setDateStart(dateStartField.getValue());
                                item.setDateFinish(dateFinishField.getValue());
                                services.getTrafficService().saveTrafficItem(item)
                                        .doOnSuccess(tt -> {
                                            item.setId(tt.getId());
                                            itemTable.getItems().add(item);
                                            Platform.runLater(() -> {
                                                infoLabel.setStyle("-fx-text-fill: green");
                                                infoLabel.setText("Успешно");
                                                commentField.clear();
                                                dateStartField.setValue(null);
                                                dateFinishField.setValue(null);
                                                itemTypeComboBox.getSelectionModel().clearSelection();
                                            });
                                        })
                                        .doOnError(throwable -> Platform.runLater(() -> infoLabel.setText("Ошибка " + throwable.getMessage())))
                                        .subscribe();
                            } else infoLabel.setText("Дата корректность введённых данных");
                        } else infoLabel.setText("Выберите даты");
                    } else infoLabel.setText("Введите комментарий");
                } else infoLabel.setText("Выберите пункт выпадающего списка");
            } else infoLabel.setText("Выберите элемент из таблицы");
        });
    }

    private ContextMenu itemTableContextMenu() {
        return MyContextMenu.builder()
                .item("Добавить", event -> switchPane(itemsPane, addItemPane))
                .item("", event -> {
                });
    }

    @AllArgsConstructor
    private enum Owner {
        All("Все"),
        PERSON("Водители"),
        TEAM("Экипажи"),
        VEHICLE("Траспортные средства");

        private final String title;

        @Override
        public String toString() {
            return title;
        }
    }

    private void switchPane(Pane disappearancePane, Pane appearancePane) {
        transition.set(switchPaneTransition(disappearancePane, appearancePane));
        transition.get().play();
    }

    private String parsePerson(Function<Person, String> function, TrafficPerson p) {
        try {
            return peopleTable.getItems().stream()
                    .filter(person -> person.getId().equals(p.getPersonnelNumber()))
                    .findFirst()
                    .map(function)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void clearData() {
        itemTable.getItems().clear();
        itemTypeComboBox.getItems().clear();
        filterItemTypeComboBox.getItems().clear();
        peopleTable.getItems().clear();
        teamTable.getItems().clear();
        vehicleTable.getItems().clear();
    }

    @Override
    public void fillData() {
        services.getTrafficService().getTrafficItems().subscribe(itemTable.getItems()::add);
        services.getTrafficService().getItemsType().subscribe(type -> {
            itemTypeComboBox.getItems().add(type);
            filterItemTypeComboBox.getItems().add(type);
        });
        peopleTable.getItems().addAll(services.getPersonService().getPeople());
        services.getTrafficService().getAllTrafficTeam()
                .doOnComplete(() -> teamTable.refresh())
                .subscribe(teamTable.getItems()::add);
        services.getTrafficService().getTrafficVehicle()
                .doOnComplete(() -> vehicleTable.refresh())
                .subscribe(vehicleTable.getItems()::add);
    }
}