package kup.get.controller.FX.asu.traffic;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.util.converter.IntegerStringConverter;
import kup.get.config.FX.FxmlLoader;
import kup.get.config.FX.MyAnchorPane;
import kup.get.config.FX.MyContextMenu;
import kup.get.config.MyTable;
import kup.get.controller.socket.SocketService;
import kup.get.model.traffic.TrafficItemType;
import reactor.core.publisher.Mono;

import java.util.function.BiConsumer;

import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

@FxmlLoader(path = "/fxml/traffic/itemType.fxml")
public class TrafficItemTypeController extends MyAnchorPane {

    @FXML
    private MyTable<TrafficItemType> itemTypeTable;
    @FXML
    private TextField searchItemTypeField;

    private final SocketService socketService;

    public TrafficItemTypeController(SocketService socketService) {
        this.socketService = socketService;
        itemTypeTable
                .headerColumn("Перечень пунктов для ОТ")
                .column("id", TrafficItemType::getId).setInvisible().build()
                .column("Статус", type -> {
                    CheckBox checkBox = new CheckBox();
                    checkBox.selectedProperty().setValue(type.isStatus());
                    checkBox.selectedProperty().addListener(
                            (ov, old_val, new_val) ->
                                    saveTrafficType(type, TrafficItemType::setStatus, new_val));
                    return checkBox;
                }).build()
                .column("Наименование", TrafficItemType::getName)
                .setEditable((type, value) -> saveTrafficType(type, TrafficItemType::setName, value), TextFieldTableCell.forTableColumn()).build()
                .column("Повториять каждые\n(месяцев)", TrafficItemType::getDefaultDurationInMonth)
                .setEditable((type, value) -> saveTrafficType(type, TrafficItemType::setDefaultDurationInMonth, value), TextFieldTableCell.forTableColumn(new IntegerStringConverter())).build();

        itemTypeTable.addEventHandler(MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                itemTypeTable.setContextMenu(createContextMenu());
            }
        });
    }

    private <t> void saveTrafficType(TrafficItemType it, BiConsumer<TrafficItemType, t> consumer, t value) {
        consumer.accept(it, value);
        socketService
                .saveItemType(it)
                .onErrorResume(e -> {
                    Platform.runLater(() -> createAlert("Ошибка", "Не удалось удалить элемент\nПри необходимости обратитесь к администратору"));
                    return Mono.empty();
                })
                .doOnSuccess(it::setItemType)
                .subscribe();
    }

    private ContextMenu createContextMenu() {
        return MyContextMenu.builder()
                .item("Добавить", e -> {
                    if (itemTypeTable.getItems().size() == 0 || itemTypeTable.getItems().get(itemTypeTable.getItems().size() - 1).isNotEmpty()) {
                        itemTypeTable.getItems().add(new TrafficItemType());
                        itemTypeTable.getSelectionModel().selectLast();
                    } else {
                        itemTypeTable.getSelectionModel().selectLast();
                    }
                });
    }

    @Override
    public void clearData() {
        itemTypeTable.getItems().clear();

    }

    @Override
    public void fillData() {
        socketService.getItemsType()
                .doOnNext(item -> itemTypeTable.getItems().add(item))
                .doOnComplete(() -> itemTypeTable.refresh())
                .subscribe();
    }
}