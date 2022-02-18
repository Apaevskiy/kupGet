package kup.get.controller.FX.traffic;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import kup.get.config.*;
import kup.get.controller.socket.SocketService;
import kup.get.model.traffic.TrafficItem;
import kup.get.model.alfa.Person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

@FxmlLoader(path = "/fxml/traffic/briefing.fxml")
public class BriefingController extends MyAnchorPane {

    @FXML
    private AnchorPane briefingPane;

    @FXML
    private MyTable<TrafficItem> briefingTable;
    @FXML
    private TextField searchBriefing;
    @FXML
    private Button excelButton;

    @FXML
    private AnchorPane addBriefingPage;

    @FXML
    private TextArea commentField;
    @FXML
    private DatePicker dateStartField;
    @FXML
    private DatePicker dateFinishField;
    @FXML
    private Button addButton;
    @FXML
    private TextField searchPerson;
    @FXML
    private MyTable<Person> peopleTable;


    private SequentialTransition transition;
    private final SocketService socketService;

    public BriefingController(SocketService socketService) {
        this.socketService = socketService;

        briefingTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        briefingTable
                .headerColumn("Инструктажи по охране труда")
                .column("Таб.№", trafficItem -> trafficItem.getPerson().getPersonnelNumber())
                .column("Фамилия", trafficItem -> trafficItem.getPerson().getLastName())
                .column("Имя", trafficItem -> trafficItem.getPerson().getFirstName())
                .column("Отчество", trafficItem -> trafficItem.getPerson().getMiddleName())
                .column("Комментарий", TrafficItem::getDescription)
                .column("Дата последнего\nинструктажа", TrafficItem::getDateStart, TrafficItem::setDateStart, p ->  new DateEditingCell<>())
                .column("Дата необходимого\nинструктажа", TrafficItem::getDateFinish, TrafficItem::setDateFinish, p ->  new DateEditingCell<>());


        peopleTable
                .headerColumn("Сотрудники")
                .invisibleColumn("id", Person::getId)
                .column("Таб.№", Person::getPersonnelNumber)
                .column("Фамилия", Person::getLastName)
                .column("Имя", Person::getFirstName)
                .column("Отчество", Person::getMiddleName)
                .invisibleColumn("Подразделение", person -> person.getDepartment().getName())
                .invisibleColumn("Должность", person -> person.getPosition().getName());
                ContextMenu cm = createContextMenu();

        briefingTable.addEventHandler(MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                briefingTable.setContextMenu(cm);
            }
        });

        addButton.setOnAction(event -> {
            /*if (transition == null || transition.getStatus().equals(Animation.Status.STOPPED)) {
                transition = createTransition(addBriefingPage, briefingPane);
                transition.play();
            }*/
            fillInTheTables();
        });
        dateStartField.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            dateFinishField.setValue(LocalDate.parse(newValue, formatter).plusMonths(3));
        });
    }

    private ContextMenu createContextMenu() {
        return MyContextMenu.builder()
                .item("Добавить инструктаж", e -> {
                    if (transition == null || transition.getStatus().equals(Animation.Status.STOPPED)) {
                        transition = createTransition(briefingPane, addBriefingPage);
                        transition.play();
                    }
                });
    }

    private void fillInTheTables() {
        socketService.getBriefing(LocalDate.now())
                .doOnNext(trafficItem -> briefingTable.getItems().add(trafficItem))
                .subscribe();
        socketService.getPeople().doOnNext(person -> peopleTable.getItems().add(person)).subscribe();
//        peopleTable.getItems().add(person)
    }
}
