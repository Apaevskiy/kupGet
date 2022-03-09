package kup.get.controller.FX.asu.traffic;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.util.converter.IntegerStringConverter;
import kup.get.config.FX.FxmlLoader;
import kup.get.config.FX.MyAnchorPane;
import kup.get.config.FX.MyContextMenu;
import kup.get.config.MyTable;
import kup.get.controller.socket.SocketService;
import kup.get.model.alfa.Person;
import kup.get.model.traffic.TrafficPerson;
import kup.get.model.traffic.TrafficTeam;
import kup.get.model.traffic.TrafficVehicle;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

@FxmlLoader(path = "/fxml/traffic/TeamAndVehicle.fxml")
public class TeamAndVehicleController extends MyAnchorPane {

    @FXML
    private AnchorPane briefingPane;

    @FXML
    private TextField searchBriefing;

    @FXML
    private MyTable<TrafficVehicle> vehicleTable;
    @FXML
    private MyTable<TrafficTeam> teamTable;
    @FXML
    private MyTable<TrafficPerson> trafficPeopleTable;
    @FXML
    private MyTable<Person> peopleTable;


    private final SocketService socketService;
    private final ObservableList<Person> people = FXCollections.observableArrayList();

    public TeamAndVehicleController(SocketService socketService) {
        this.socketService = socketService;
        peopleTable.setItems(people);
        trafficPeopleTable
                .headerColumn("Водители экипажа")
                    .column("Таб.№", p -> parsePerson(Person::getPersonnelNumber, p)).build()
                    .column("Фамилия", p -> parsePerson(Person::getLastName, p)).build()
                    .column("Имя", p -> parsePerson(Person::getFirstName, p)).build()
                    .column("Отчество", p -> parsePerson(Person::getMiddleName, p)).build();
        teamTable
                .headerColumn("Экипажи")
                    .column("id экипажа", TrafficTeam::getId).setInvisible().build()
                    .column("№ экипажа", TrafficTeam::getNumber).setEditable((tt, value) -> {
                        tt.setNumber(value);
                        saveTrafficTeam(tt);
                        }, TextFieldTableCell.forTableColumn()).build()
                    .column("Режим работы", TrafficTeam::getWorkingMode).setEditable( (tt, value) -> {
                        tt.setWorkingMode(value);
                        saveTrafficTeam(tt);
                        }, TextFieldTableCell.forTableColumn()).build();

        vehicleTable
                .headerColumn("Транспортные стредства")
                    .column("Имя", TrafficVehicle::getId).setInvisible().build()
                    .column("№ ТС", TrafficVehicle::getNumber)
                        .setEditable((tv, value) -> {
                            tv.setNumber(value);
                            saveTrafficVehicle(tv);
                        }, TextFieldTableCell.forTableColumn(new IntegerStringConverter()))
                        .build()
                    .column("Модель ТС", TrafficVehicle::getModel)
                        .setEditable((tv, value) -> {
                            tv.setModel(value);
                            saveTrafficVehicle(tv);
                        }, TextFieldTableCell.forTableColumn())
                        .build()
                    .column("id экипажа", tv -> tv.getTeam().getId()).setInvisible().build()
                    .column("№ экипажа", tv -> tv.getTeam().getNumber()).build()
                    .column("Режим работы", tv -> tv.getTeam().getWorkingMode()).build();

        peopleTable
                .headerColumn("Сотрудники")
                    .column("id", Person::getId).setInvisible().build()
                    .column("Таб.№", Person::getPersonnelNumber).build()
                    .column("Фамилия", Person::getLastName).build()
                    .column("Имя", Person::getFirstName).build()
                    .column("Отчество", Person::getMiddleName).build()
                    .column("Подразделение", p -> p.getDepartment().getName()).setInvisible().build()
                    .column("Должность", p -> p.getPosition().getName()).setInvisible().build();


        vehicleTable.addEventHandler(MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                vehicleTable.setContextMenu(vehicleCM());
            }
        });

        peopleTable.addEventHandler(MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                peopleTable.setContextMenu(peopleCM());
            }
        });

        teamTable.addEventHandler(MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                teamTable.setContextMenu(teamCM());
            }
        });
        trafficPeopleTable.addEventHandler(MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                trafficPeopleTable.setContextMenu(trafficPeopleCM());
            }
        });

        vehicleTable.setRowFactory(tv -> {
            TableRow<TrafficVehicle> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    if (row.getItem().getTeam() != null && row.getItem().getTeam().getTrafficPeople() != null) {
                        trafficPeopleTable.getItems().clear();
                        socketService.getPeopleByTeam(row.getItem().getTeam())
                                .doOnCancel(() -> trafficPeopleTable.refresh())
                                .subscribe(trafficPeopleTable.getItems()::add);
                    } else trafficPeopleTable.getItems().clear();
                }
            });
            return row;
        });
        teamTable.setRowFactory(tv -> {
            TableRow<TrafficTeam> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    if (row.getItem().getTrafficPeople() != null) {
                        trafficPeopleTable.getItems().clear();
                        socketService.getPeopleByTeam(row.getItem())
                                .doOnCancel(() -> trafficPeopleTable.refresh())
                                .subscribe(trafficPeopleTable.getItems()::add);
                    } else trafficPeopleTable.getItems().clear();
                }
            });
            return row;
        });
    }

    private String parsePerson(Function<Person, String> function, TrafficPerson p) {
        try {
            return people.stream()
                    .filter(person -> person.getId().equals(p.getPersonnelNumber()))
                    .findFirst()
                    .map(function)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveTrafficTeam(TrafficTeam tt) {
        socketService
                .saveTrafficTeam(tt)
                .onErrorResume(e -> error())
                .doOnSuccess(tt::setTeam)
                .subscribe();
    }

    private void saveTrafficVehicle(TrafficVehicle tv) {
        socketService
                .saveTrafficVehicle(tv)
                .onErrorResume(e -> error())
                .doOnSuccess(p -> vehicleTable.refresh())
                .subscribe(tv::setVehicle);
    }

    private ContextMenu vehicleCM() {
        return MyContextMenu.builder()
                .item("Добавить ТС", event -> {
                    vehicleTable.getItems().add(TrafficVehicle.builder().build());
                    vehicleTable.getSelectionModel().selectLast();
                })
                .item("Удалить ТС", event -> {
                    SelectionModel<TrafficVehicle> model = vehicleTable.getSelectionModel();
                    if (model != null && model.getSelectedItem() != null) {
                        socketService.deleteTrafficVehicle(model.getSelectedItem())
                                .onErrorResume(e -> error())
                                .doOnSuccess(b -> vehicleTable.getItems().remove(model.getSelectedItem()))
                                .subscribe();
                    }
                })
                .item("Открепить экипаж", event -> {
                    SelectionModel<TrafficVehicle> model = vehicleTable.getSelectionModel();
                    if (model != null && model.getSelectedItem() != null) {
                        model.getSelectedItem().setTeam(null);
                        saveTrafficVehicle(model.getSelectedItem());
                    }
                });
    }

    private ContextMenu peopleCM() {
        return MyContextMenu
                .builder()
                .item("Закрепить сотрудника", event -> {
                    SelectionModel<TrafficTeam> teamModel = teamTable.getSelectionModel();
                    if (teamModel != null && teamModel.getSelectedItem() != null) {
                        SelectionModel<Person> personModel = peopleTable.getSelectionModel();
                        if (personModel != null && personModel.getSelectedItem() != null) {
                            TrafficPerson person = new TrafficPerson();
                            person.setPersonnelNumber(personModel.getSelectedItem().getId());
                            person.setTeam(teamModel.getSelectedItem());
                            socketService
                                    .saveTrafficPerson(person)
                                    .onErrorResume(e -> error())
                                    .doOnSuccess(tt -> {
                                        trafficPeopleTable.getItems().add(tt);
                                        trafficPeopleTable.refresh();
                                    })
                                    .subscribe();
                        }
                    }
                });
    }

    private ContextMenu trafficPeopleCM() {
        return MyContextMenu
                .builder()
                .item("Открепить сотрудника", event -> {
                    SelectionModel<TrafficPerson> personModel = trafficPeopleTable.getSelectionModel();
                    if (personModel != null && personModel.getSelectedItem() != null) {
                        TrafficTeam team = teamTable.getSelectionModel().getSelectedItem();
                        team.getTrafficPeople().remove(personModel.getSelectedItem());
                        socketService
                                .saveTrafficTeam(team)
                                .onErrorResume(e -> error())
                                .doOnSuccess(tt -> {
                                    team.setTeam(tt);
                                    trafficPeopleTable.setItems(FXCollections.observableArrayList(tt.getTrafficPeople()));
                                    trafficPeopleTable.refresh();
                                })
                                .subscribe();
                    }
                });
    }

    private ContextMenu teamCM() {
        return MyContextMenu.builder()
                .item("Добавить экипаж", event -> {
                    teamTable.getItems().add(new TrafficTeam());
                    teamTable.getSelectionModel().selectLast();
                })
                .item("Удалить экипаж", event -> {
                    SelectionModel<TrafficTeam> model = teamTable.getSelectionModel();
                    if (model != null && model.getSelectedItem() != null) {
                        socketService.deleteTrafficTeam(model.getSelectedItem())
                                .onErrorResume(e -> error())
                                .doOnSuccess(b -> teamTable.getItems().remove(model.getSelectedItem()))
                                .subscribe();
                    }
                })
                .item("Закрепить экипаж", event -> {
                    SelectionModel<TrafficTeam> teamModel = teamTable.getSelectionModel();
                    if (teamModel != null && teamModel.getSelectedItem() != null) {
                        SelectionModel<TrafficVehicle> vehicleModel = vehicleTable.getSelectionModel();
                        if (vehicleModel != null && vehicleModel.getSelectedItem() != null) {
                            vehicleModel.getSelectedItem().setTeam(teamModel.getSelectedItem());
                            saveTrafficVehicle(vehicleModel.getSelectedItem());
                        }
                    }
                });
    }

    private <t> Mono<t> error() {
        Platform.runLater(() -> createAlert("Ошибка", "Не удалось удалить элемент\nПри необходимости обратитесь к администратору"));
        return Mono.empty();
    }

    @Override
    public void clearData(){
        trafficPeopleTable.getItems().clear();
        teamTable.getItems().clear();
        vehicleTable.getItems().clear();
        people.clear();
    }
    @Override
    public void fillData() {
        people.addAll(socketService.getDriver());
        peopleTable.setItems(FXCollections.observableArrayList(socketService.getDriver()));
        socketService.getTrafficVehicle().subscribe(vehicleTable.getItems()::add);
        socketService.getTrafficTeam().subscribe(teamTable.getItems()::add);
    }
}